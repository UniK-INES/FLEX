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
package de.unik.ines.soeasy.flex.balance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import de.soeasy.common.model.MarketProductPattern;
import de.soeasy.common.model.MeterReading;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.MarketInformationRepos;
import de.unik.ines.soeasy.flex.repos.MarketMeterReadingRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketUserBalanceRepos;
import de.unik.ines.soeasy.flex.repos.RoleRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;
import de.unik.ines.soeasy.flex.util.FlexUtils;

/**
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.balance",
		"de.unik.ines.soeasy.flex.grid", "de.unik.ines.soeasy.flex.scheduling" })
@Import({ FlexUtils.class, FlexTestUtils.class })
public class BalanceManagerTest {
	
	protected long start;
	
	protected float fine;
	
	@Autowired
	MarketProductRepository mproductRepos;
	
	@Autowired
	MMarketProductRepository mmproductRepos;
	
	@Autowired
	MarketEnergyRequestRepository mrequestRepos;
	
	@Autowired
	MarketUserBalanceRepos balanceRepos;
	
	@Autowired
	MarketMeterReadingRepository meteringRepos;
	
	@Autowired
	RoleRepository roleRepos;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	MarketInformationRepos minfoRepos;
	
	@Autowired
	ClearingInfoRepository cinfoRepos;
	

	@Autowired
	protected BalanceManager balanceManager;
	
	@Autowired
	protected MeterReadingManager readingManager;
	
	@Autowired
	protected FlexTestUtils testUtils;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		this.start =  System.currentTimeMillis();
		
		this.meteringRepos.deleteAll();
		this.mrequestRepos.deleteAll();
		this.balanceRepos.deleteAll();
		this.userRepos.deleteAll();
		UserAccount user = new UserAccount("enavi3", this.roleRepos.findAll());
		this.userRepos.save(user);
		
		cinfoRepos.deleteAll();
		mmproductRepos.deleteAll();
		mproductRepos.deleteAll();
		
		MarketProductPattern marketProductPattern = new MarketProductPattern();
		marketProductPattern.productId = 11;
		marketProductPattern.firstDeliveryPeriodStart = start + 1000;
		marketProductPattern.deliveryPeriodDuration = 1000*6;
		marketProductPattern.openingTime = "-1M";
		marketProductPattern.closingTime = "-1s";
		marketProductPattern.auctionInterval = "-1s";
		marketProductPattern.minPrice = -3000.0f;
		marketProductPattern.maxPrice = 3000.0f;
		marketProductPattern = mproductRepos.save(marketProductPattern);
		mmproductRepos.save(new MMarketProductPattern(marketProductPattern));
		
		marketProductPattern = new MarketProductPattern();
		marketProductPattern.productId = 12;
		marketProductPattern.firstDeliveryPeriodStart = start + 1000;
		marketProductPattern.deliveryPeriodDuration = 1000*9;
		marketProductPattern.openingTime = "-1M";
		marketProductPattern.closingTime = "-1s";
		marketProductPattern.auctionInterval = "-1s";
		marketProductPattern.minPrice = -3000.0f;
		marketProductPattern.maxPrice = 3000.0f;
		marketProductPattern = mproductRepos.save(marketProductPattern);
		mmproductRepos.save(new MMarketProductPattern(marketProductPattern));
		
		this.readingManager.initProperties();
		
		this.fine = this.minfoRepos.findById(1).get().finePerUntradedKwh;
		balanceManager.setInActive();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		mmproductRepos.deleteAll();
	}

	@Test
	public void testBalancingOverconsumption() throws InterruptedException {
		// add requests
		testUtils.storeNewProcessedRequest(1, "enavi3", 1, 2, start + 1000, start + 7000);
		testUtils.storeNewProcessedRequest(2, "enavi3", 5, 6, start + 1000, start + 10000);
		testUtils.storeNewProcessedRequest(3, "enavi3", 5, 6, start + 12000, start + 15000);
		
		// wait
		Thread.sleep(10*1000+500);
		
		// add meter readings
		testUtils.storeMeterReading(1, "enavi3", 2.5f, 	start + 1000, start + 4000);
		testUtils.storeMeterReading(2, "enavi3", 3, 	start + 4000, start + 7000);
		testUtils.storeMeterReading(3, "enavi3", 2.5f, 	start + 7000, start + 10000);
		
		// calculate balance
		balanceManager.calculateBalances(start + 4000);
		balanceManager.calculateBalances(start + 7000);
		balanceManager.calculateBalances(start + 10000);
		
		// check first balance (underconsumption)
		assertEquals("Expected asked energy is 3.0", 3.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 1000, start + 4000).getUserBalance().askedEnergy, 0.001f);
		assertEquals("Expected read energy is 2.5", 2.5f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 1000, start + 4000).getUserBalance().realEnergy, 0.001f);
		assertEquals("Expected cost is 1.0 * 1.0 + 5 * 2", 11.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 1000, start + 4000).getUserBalance().costs, 0.001f);
		assertEquals("Expected fine is 0.5*" + this.fine, 0.5f * this.fine, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 1000, start + 4000).getUserBalance().fines, 0.001f);
		
		// check 2nd balance (balanced)
		assertEquals("Expected asked energy is 3.0", 3.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 4000, start + 7000).getUserBalance().askedEnergy, 0.001f);
		assertEquals("Expected read energy is 3.0", 3.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 4000, start + 7000).getUserBalance().realEnergy, 0.001f);
		assertEquals("Expected cost is 1.0 * 1.0 + 5 * 2", 11.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 4000, start + 7000).getUserBalance().costs, 0.001f);
		assertEquals("Expected fine is 0.5*" + this.fine, 0.0f * this.fine, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 4000, start + 7000).getUserBalance().fines, 0.001f);
		
		// check 3rd balance (over-consumption)
		assertEquals("Expected asked energy is 2.0", 2.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 7000, start + 10000).getUserBalance().askedEnergy, 0.001f);
		assertEquals("Expected read energy is 2.5", 2.5f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 7000, start + 10000).getUserBalance().realEnergy, 0.001f);
		assertEquals("Expected cost is 5.0*2.0", 10.0f, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 7000, start + 10000).getUserBalance().costs, 0.001f);
		assertEquals("Expected fine is 0.5*" + this.fine, 0.5f * this.fine, balanceRepos.getSingleBalance(userRepos.findByName("enavi3"),
				start + 7000, start + 10000).getUserBalance().fines, 0.001f);
	}
	
	@Test
	public void testInvalidMeterReading() {
		MeterReading reading = this.testUtils.createMeterReading(2, 3.0f, start - 99, start + 199);
		assertFalse(this.readingManager.validate(reading));
		
		reading = this.testUtils.createMeterReading(2, 3.0f, start + 1000, start + 1199);
		assertFalse(this.readingManager.validate(reading));
		
		reading = this.testUtils.createMeterReading(2, 3.0f, start + 1050, start + 4000);
		assertFalse(this.readingManager.validate(reading));
		
		reading = this.testUtils.createMeterReading(2, 3.0f, start + 1000, start + 4000);
		assertTrue(this.readingManager.validate(reading));
	}
}
