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
package de.unik.ines.soeasy.flex.repos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;

import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferStatus;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

/**
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.model", "de.unik.ines.soeasy.flex.scheduling",
		"de.unik.ines.soeasy.flex.clearing", "de.unik.ines.soeasy.flex.repos" })
class FlexOfferWrapperReposTest {

	@Autowired
	FlexOfferRepository fowRepos;

	@Autowired
	UserAccountRepository userRepos;

	UserAccount flex1;
	FlexOffer fo1, fo2, fo3;
	FlexOfferWrapper fow1, fow2, fow3;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		flex1 = userRepos.findByName("flex1");
		fo1 = FlexTestUtils.buildFlexOffer();
		fow1 = new FlexOfferWrapper(fo1, flex1);
		fowRepos.save(fow1);

		fo2 = FlexTestUtils.buildFlexOffer();
		fow2 = new FlexOfferWrapper(fo2, flex1);
		fowRepos.save(fow2);

		fo3 = FlexTestUtils.buildFlexOffer();
		fow3 = new FlexOfferWrapper(fo3, flex1);
		fowRepos.save(fow3);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		fowRepos.deleteAll();
	}

	@Test
	void testRetrieval() {
		List<FlexOfferWrapper> fows = new ArrayList<>();
		fowRepos.findAll().forEach(action -> fows.add(action));
		assertEquals(3, fows.size());

		List<FlexOfferWrapper> fows2 = fowRepos.findAllWrapper();
		assertEquals(3, fows2.size());
	}

	@Test
	void testInterval() {
		fo1.setPeriod(
				new Interval(new DateTime().minusDays(3).withTimeAtStartOfDay(),
						new DateTime().minusDays(2).withTimeAtStartOfDay()));

		fo3.setPeriod(new Interval(new DateTime().plusDays(20).withTimeAtStartOfDay(),
				new DateTime().plusDays(30).withTimeAtStartOfDay()));
		fowRepos.save(fow1);
		fowRepos.save(fow3);
		List<FlexOfferWrapper> fows = fowRepos.findAllUnmatchedByProductAndIntervalTime(
				new DateTime().plusDays(1).withTimeAtStartOfDay().getMillis(),
				new DateTime().plusDays(2).withTimeAtStartOfDay().getMillis(), 3);

		assertFalse(fows.contains(fow1), "Too early");
		assertTrue(fows.contains(fow2), "Matching");
		assertFalse(fows.contains(fow3), "Too late");
	}

	@Test
	void testMarketProduct() {
		fo1.setContractID("MP_ID:1#SampleContractID");
		fow1 = new FlexOfferWrapper(fo1, flex1);
		fowRepos.save(fow1);
		List<FlexOfferWrapper> fows = fowRepos.findAllUnmatchedByProductAndIntervalTime(
				new DateTime().plusDays(1).withTimeAtStartOfDay().getMillis(),
				new DateTime().plusDays(2).withTimeAtStartOfDay().getMillis(), 3);
		assertFalse(fows.contains(fow1), "MP 1");
		assertTrue(fows.contains(fow2), "MP 3");
		assertTrue(fows.contains(fow3), "MP 3");
	}

	@Test
	void testStatus() {
		fow1.setStatus(FlexOfferStatus.ACCEPTED);
		fow3.setStatus(FlexOfferStatus.DECLINED);
		fowRepos.save(fow1);
		fowRepos.save(fow3);
		List<FlexOfferWrapper> fows = fowRepos.findAllUnmatchedByProductAndIntervalTime(
				new DateTime().plusDays(1).withTimeAtStartOfDay().getMillis(),
				new DateTime().plusDays(2).withTimeAtStartOfDay().getMillis(), 3);
		assertFalse(fows.contains(fow1), "ACCEPTED");
		assertTrue(fows.contains(fow2), "CREATED");
		assertFalse(fows.contains(fow3), "DECLINED");
	}
}