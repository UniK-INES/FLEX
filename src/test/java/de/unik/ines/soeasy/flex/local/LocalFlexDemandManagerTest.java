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
package de.unik.ines.soeasy.flex.local;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.joda.time.Interval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandManager;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, JsonbAutoConfiguration.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.local",
		"de.unik.ines.soeasy.flex.scheduling" })
@Import({ GridFlexDemandManager.class })
class LocalFlexDemandManagerTest {

	@Autowired
	GridFlexDemandManager gridFlexManager;
	
	@Autowired
	TimeInitializingBean timeBean;
	
	@Autowired
	LocalFlexDemandManager localFlexManager;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	ObjectMapper mapper;
	
	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Grid FLEX Schedule contains time series of two locations (Tranformer01 and Transformer02) for 8 intervals
	 * of 15 minutes.
	 * First 4 intervals are 10 kW, second 4 intervals are 0 kW for Transformer 1.
	 * First 4 intervals are 20 kW, second 48 intervals are -10 kW for Transformer 2.
	 */
	public static final String FILENAME_JSON_FLEXDEMAND_SMD = "json/GridFlexDemand_Schedule_MarketDocument.json"; 
	
	public static final String LOCATION_1 = "11X-KS-DIENST1-4_Transformer01";
	
	public static final String LOCATION_2 = "11X-KS-DIENST1-4_Transformer02";
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		// store DSO's flex demand to DB
		Schedule_MarketDocument flexDemandSmd = null;
		try {
			flexDemandSmd =  mapper.readValue(new File(getClass().getClassLoader().getResource(FILENAME_JSON_FLEXDEMAND_SMD).toURI()), 
					Schedule_MarketDocument.class);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Interval interval = new Interval(timeBean.getNextDayStart(), timeBean.getDayAfterNextDayStart()-1);
		flexDemandSmd.setTimeInterval(interval);
		for (TimeSeries ts : flexDemandSmd.getTimeSeries()) {
			ts.getPeriod().setTimeInterval(interval);
		}
		
		gridFlexManager.storeGridFlexDemandSmd(flexDemandSmd);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	void testLocalFlexRetrieval() {
		UserAccount userAccount1 = userRepos.findByName("flex1");
		Schedule_MarketDocument smd1 = localFlexManager.getLocalFlexDemandSmd(userAccount1);
		assertNotNull(smd1);
		assertEquals(10.0, smd1.getTimeSeries().iterator().next().getPeriod().getPoints().get(0).getQuantity().floatValue());
		assertEquals(0.0, smd1.getTimeSeries().iterator().next().getPeriod().getPoints().get(4).getQuantity().floatValue());
		
		UserAccount userAccount2 = userRepos.findByName("flex2");
		Schedule_MarketDocument smd2 = localFlexManager.getLocalFlexDemandSmd(userAccount2);
		assertNotNull(smd2);
		assertEquals(20.0, smd2.getTimeSeries().iterator().next().getPeriod().getPoints().get(0).getQuantity().floatValue());
		assertEquals(-10.0, smd2.getTimeSeries().iterator().next().getPeriod().getPoints().get(4).getQuantity().floatValue());
	}
}
