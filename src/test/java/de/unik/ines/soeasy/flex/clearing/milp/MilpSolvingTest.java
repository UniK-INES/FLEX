/**
 * This file is part of INES FLEX - 
 * INES (Integrated Energy Systems) FLexibility Energy eXchange
 * 
 * INES FLEX is free software: You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * INES FLEX is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020
 * Department of Integrated Energy Systems, University of Kassel, Kassel, Germany
 */
package de.unik.ines.soeasy.flex.clearing.milp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import de.iwes.enavi.cim.schedule51.Point;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.soeasy.common.model.flex.CurrencyAmountType;
import de.soeasy.common.model.flex.EntityAddressType;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferOptionType;
import de.soeasy.common.model.flex.offer.FlexOptionISPType;
import de.unik.ines.soeasy.flex.exceptions.NoMatchingSolutionException;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandManager;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

/**
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GridFlexDemandManager.class)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.model", "de.unik.ines.soeasy.flex.scheduling",
		"de.unik.ines.soeasy.flex.grid",
		"de.unik.ines.soeasy.flex.clearing", "de.unik.ines.soeasy.flex.repos",
		"de.unik.ines.soeasy.flex.clearing.milp" })
class MilpSolvingTest {

	@Autowired
	GridFlexDemandManager gridFlexManager;

	@Autowired
	FlexOrderRepository forRepos;

	@Autowired
	MilpSolving solving;

	@Autowired
	UserAccountRepository userRepos;

	FlexOffer fo1, fo2, fo3, fo4;
	List<Schedule_MarketDocument> demands = new ArrayList<>(1);
	List<FlexOfferWrapper> offers = new ArrayList<>(4);

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		// store Flex demand (05-08: 10kWh)
		Schedule_MarketDocument flexDemandSmd = FlexTestUtils.createFlexDemand();
		for (TimeSeries ts : flexDemandSmd.getTimeSeries()) {
			for (int i = 1; i <= 96; i++) {
				if (i > 16 && i <= 32) {
					ts.getPeriod().addPoint(new Point(i, new BigDecimal(10000.0)));
				} else {
					ts.getPeriod().addPoint(new Point(i, new BigDecimal(0.0)));
				}
			}
			ts.getMarketEvaluationPoint().setmRID("TestTransformer01");
		}
		demands.add(flexDemandSmd);
		
		// create Flex offers
		UserAccount flex1 = userRepos.findByName("flex1");
		fo1 = FlexTestUtils.buildFlexOffer();
		fo1.setContractID("MP_ID:3#Fo1");
		fo1.setCongestionPoint(new EntityAddressType("TestTransformer01_Node01"));

		// Option A 02-08: 5kW@0.5
		List<FlexOfferOptionType> options1 = fo1.getOfferOptions();
		options1.get(0).setPrice(new CurrencyAmountType(new BigDecimal("0.500")));
		List<FlexOptionISPType> isps = new ArrayList<>();
		for (int i = 1; i <= 7 * 4; i++) {
			isps.add(new FlexOptionISPType((short) (1 * 4 + i), 5000));
		}
		options1.get(0).setIsps(isps);
		offers.add(new FlexOfferWrapper(fo1, flex1));

		// Option B 07-08: 5kW@0.2 (*)
		options1.get(1).setPrice(new CurrencyAmountType(new BigDecimal("0.200")));
		List<FlexOptionISPType> ispsB = new ArrayList<>();
		for (int i = 1; i <= 2 * 4; i++) {
			ispsB.add(new FlexOptionISPType((short) (6 * 4 + i), 5000));
		}
		options1.get(1).setIsps(ispsB);

		// Option A 05-06: 5kW@0.2 (*)
		fo2 = FlexTestUtils.buildFlexOffer();
		fo2.setContractID("MP_ID:3#Fo2");
		fo2.setCongestionPoint(new EntityAddressType("TestTransformer01_Node02"));
		List<FlexOfferOptionType> options2 = fo2.getOfferOptions();
		options2.get(0).setPrice(new CurrencyAmountType(new BigDecimal("0.200")));
		isps = new ArrayList<>();
		for (int i = 1; i <= 2 * 4; i++) {
			isps.add(new FlexOptionISPType((short) (4 * 4 + i), 5000));
		}
		options2.get(0).setIsps(isps);
		options2.remove(1);
		offers.add(new FlexOfferWrapper(fo2, flex1));

		// Option A 04-08: 5kW@0.6 (*)
		fo3 = FlexTestUtils.buildFlexOffer();
		fo3.setContractID("MP_ID:3#Fo3");
		fo3.setCongestionPoint(new EntityAddressType("TestTransformer01_Node03"));
		List<FlexOfferOptionType> options3 = fo3.getOfferOptions();
		options3.get(0).setPrice(new CurrencyAmountType(new BigDecimal("0.600")));
		isps = new ArrayList<>();
		for (int i = 1; i <= 5 * 4; i++) {
			isps.add(new FlexOptionISPType((short) (3 * 4 + i), 5000));
		}
		options3.get(0).setIsps(isps);
		options3.remove(1);
		offers.add(new FlexOfferWrapper(fo3, flex1));

		// Option A 05-08: 10kW@1.5
		fo4 = FlexTestUtils.buildFlexOffer();
		fo4.setContractID("MP_ID:3#Fo4");
		fo4.setCongestionPoint(new EntityAddressType("TestTransformer01_Node04"));
		List<FlexOfferOptionType> options4 = fo4.getOfferOptions();
		options4.get(0).setPrice(new CurrencyAmountType(new BigDecimal("1.500")));
		options4.remove(1);
		isps = new ArrayList<>();
		for (int i = 1; i <= 4 * 4; i++) {
			isps.add(new FlexOptionISPType((short) (4 * 4 + i), 10000));
		}
		options4.get(0).setIsps(isps);
		offers.add(new FlexOfferWrapper(fo4, flex1));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		fo1 = null;
		fo2 = null;
		fo3 = null;
		fo4 = null;

		demands.clear();
		offers.clear();
	}

	@Test
	void test() {
		// perform optimisation
		solving.clearMarket(demands, offers, null);
		// check outcome (FlexOrders in DB)
		assertTrue(forRepos.findByFlexOfferId(fo1.messageID).isPresent());
		// check accepted option:
		assertEquals(forRepos.findByFlexOfferId(fo1.messageID).get().getOptionReference(),
				fo1.getOfferOptions().get(1).getOptionReference());
		assertTrue(forRepos.findByFlexOfferId(fo2.messageID).isPresent());
		assertTrue(forRepos.findByFlexOfferId(fo3.messageID).isPresent());
		assertFalse(forRepos.findByFlexOfferId(fo4.messageID).isPresent());
	}

	@Test
	void testUnsolvable() {
		demands.clear();
		Schedule_MarketDocument flexDemandSmd = FlexTestUtils.createFlexDemand();
		for (TimeSeries ts : flexDemandSmd.getTimeSeries()) {
			for (int i = 1; i <= 96; i++) {
				if (i > 16 && i <= 32) {
					ts.getPeriod().addPoint(new Point(i, new BigDecimal(-10000.0)));
				} else {
					ts.getPeriod().addPoint(new Point(i, new BigDecimal(0.0)));
				}
			}
			ts.getMarketEvaluationPoint().setmRID("TestTransformer01");
		}
		demands.add(flexDemandSmd);
		// perform optimisation
		assertThrows(NoMatchingSolutionException.class, () -> {
			solving.clearMarket(demands, offers, null);
		});
	}
}
