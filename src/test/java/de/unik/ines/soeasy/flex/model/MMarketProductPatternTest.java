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
package de.unik.ines.soeasy.flex.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;
import org.joda.time.Interval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.soeasy.common.model.MarketProductPattern;
import de.unik.ines.soeasy.flex.clearing.ClearingMethod;
import de.unik.ines.soeasy.flex.clearing.ClearingProvider;
import de.unik.ines.soeasy.flex.clearing.MarketProductClearer;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

/**
 * @author Sascha Holzhauer
 *
 */
@TestPropertySource(properties = "de.unik.enavi.market.time.matchbasetime=false")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.model", "de.unik.ines.soeasy.flex.scheduling",
		"de.unik.ines.soeasy.flex.clearing", "de.unik.ines.soeasy.flex.repos", "de.unik.ines.soeasy.flex.grid" })
@Import(FlexTestUtils.class)
public class MMarketProductPatternTest {
	
	private Log log = LogFactory.getLog(MMarketProductPatternTest.class);

	protected class PseudoClearingMethod implements ClearingMethod {

		@Override
		public synchronized void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers,
				ClearingInfo cInfo) {
			executionRecorder.put(counter, System.currentTimeMillis());
			log.info("Increased counter: " + counter++ + " | "
					+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(System.currentTimeMillis())) + " ("
					+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(cInfo.getClearingTime())) + ")");
		}
	}
	
	@Autowired
	TimeInitializingBean timebean;
	
	MMarketProductPattern mmproduct;
	MarketProductPattern product = new MarketProductPattern();
	
	@Autowired
	MarketProductClearer mpClearer;
	
	@Autowired
	@Qualifier("taskScheduler")
	ThreadPoolTaskScheduler taskScheduler;

	@Autowired
	FlexOfferRepository foRepos;

	@Autowired
	MarketProductRepository mProductRepos;

	@Autowired
	FlexTestUtils flexTestUtils;

	final Map<Integer, Long> executionRecorder = new HashMap<>();
	int counter = 0;
	int tolerance = 200;

	@BeforeEach
	public void setUp() throws Exception {
		this.foRepos.findAll();
		timebean.setBasetime(System.currentTimeMillis());
		timebean.applyChanges();
		taskScheduler.shutdown();
		taskScheduler.initialize();
		executionRecorder.clear();
		// set up product pattern
		product.productId = 5;
		product.closingTime = "-1s";
		product.openingTime = "-4s";
		product.auctionInterval = "1s";
		product.deliveryPeriodDuration = 1000*1;
		product.auctionDeliverySpan = product.deliveryPeriodDuration;
		product.firstDeliveryPeriodStart = System.currentTimeMillis() + 1000 * 5;
		product.energyResolutionKWh = 0;
		mProductRepos.save(product);

		mmproduct = new MMarketProductPattern(product);
		ClearingMethod cmethod = new PseudoClearingMethod();
		mpClearer.setClearingProvider(new ClearingProvider() {
			@Override
			public ClearingMethod getClearing(String id) {
				return cmethod;
			}
		});
		
		// adding requests not needed
	}

	/**
	 * @formatter:off
	 *  
	 * 1st delivery period: 1-2-3------------ (scheduled at -1:000s)
	 * 2nd delivery period: --2-3-4---------- (scheduled at 0:000s)
	 * 3rd delivery period: ----3-4-5-------- (scheduled at 1:000s)
	 * 4th delivery period: ------4-5-6------ (scheduled at 2:000s)
	 * -- stop ---
	 * (numbers give seconds after opening time of first delivery period)
	 * 
	 * @formatter:on
	 *  
	 * Test method for {@link de.unik.ines.soeasy.flex.model.MMarketProductPattern#schedule()}.
	 * @throws Exception 
	 */
	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testSchedule() throws Exception {
		
		Interval initialDdeliveryInterval = new Interval(product.firstDeliveryPeriodStart,
				product.firstDeliveryPeriodStart + product.auctionDeliverySpan);
		flexTestUtils.generateDemandAndOffer(initialDdeliveryInterval, product, 4);

		Thread check  = new Thread() {
			 public void run(){
				 mpClearer.schedule(mmproduct);
			 }
		};
		check.start();
		
		Thread.sleep(1000 * 3 + 200);
		mmproduct.stopClearing();
		Thread.sleep(1000 * 4 + 200);
		
		log.info("Start JUnit checks...");
		
		int count = 0;
		log.info("1st execution: " + Math.abs(executionRecorder.get(count) - (product.firstDeliveryPeriodStart - 1000*3)));
		assertTrue("1st execution invalid (executed: "
				+ DateFormat.getTimeInstance(DateFormat.LONG).format(new Date(Math.abs(executionRecorder.get(count))))
				+ "; expected: "
				+ DateFormat.getTimeInstance(DateFormat.LONG)
						.format(new Date(product.firstDeliveryPeriodStart - 1000 * 3))
				+ 
				")", Math.abs(executionRecorder.get(count) -
				(product.firstDeliveryPeriodStart - 1000*3)) < tolerance);
		
		log.info("2nd execution: "
				+ Math.abs(executionRecorder.get(++count) - (product.firstDeliveryPeriodStart - 1000 * 2)));
		assertTrue("2nd execution invalid", Math.abs(executionRecorder.get(count) -
				(product.firstDeliveryPeriodStart - 1000*2)) < tolerance);

		log.info("3rd execution: " + new SimpleDateFormat("hh:mm:ss.SSS")
				.format(new Date(executionRecorder.get(++count).longValue())) + " - "
				+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(product.firstDeliveryPeriodStart)));
		assertTrue("3rd execution invalid",
				Math.abs(executionRecorder.get(count) -
				(product.firstDeliveryPeriodStart - 1000*2)) < tolerance);
		
		assertTrue(
				"4th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart - 1000)) < tolerance);

		assertTrue(
				"5th execution invalid  (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart - 1000)) < tolerance);

		log.info("6th execution: " + new SimpleDateFormat("hh:mm:ss.SSS")
				.format(new Date(executionRecorder.get(++count).longValue())) + " - "
						+ new SimpleDateFormat("hh:mm:ss.SSS")
						.format(new Date(product.firstDeliveryPeriodStart)));
		assertTrue("6th execution invalid (" + (executionRecorder.get(count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) -
				(product.firstDeliveryPeriodStart - 1000)) < tolerance);
		
		assertTrue(
				"7th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart)) < tolerance);

		assertTrue(
				"8th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart)) < tolerance);

		assertTrue(
				"9th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart)) < tolerance);
		
		assertTrue(
				"7th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart + 1000*1)) < tolerance);

		assertTrue(
				"8th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart + 1000*1)) < tolerance);
		
		assertTrue(
				"9th execution invalid (" + (executionRecorder.get(++count) - product.firstDeliveryPeriodStart) + ")",
				Math.abs(executionRecorder.get(count) - 
						(product.firstDeliveryPeriodStart + 1000 * 2)) < tolerance);
	}

	/**
	 * Test method for
	 * {@link de.unik.ines.soeasy.flex.model.MMarketProductPattern#stopClearing()}.
	 * 
	 * @formatter:off
	 * 
	 * 1st delivery period: 1-2-3----------  (scheduled at -1:000s)
	 * -- stop --- 
	 * (numbers give seconds after opening time of first delivery period)
	 * 
	 * @formatter:on
	 * 
	 * @throws Exception
	 * 
	 */
	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testStopClearing() throws Exception {
		Interval initialDdeliveryInterval = new Interval(product.firstDeliveryPeriodStart,
				product.firstDeliveryPeriodStart + product.auctionDeliverySpan);
		flexTestUtils.generateDemandAndOffer(initialDdeliveryInterval, product, 1);
		
		Thread check  = new Thread() {
			 public void run(){
				log.info("Scheduling...");
				mpClearer.schedule(mmproduct);
			 }
		};
		check.start();
		Thread.sleep(200); // to await scheduling
		log.info("Stop scheduling...");
		mmproduct.stopClearing();
		Thread.sleep(4200); // first delivery is 5s from test start. 1s before, all clearings should have
							// been performed
		
		int count = 0;
		
		assertEquals("Incorect number of clearings scheduled", 3, this.executionRecorder.size());
		
		assertEquals("1st execution invalid", product.firstDeliveryPeriodStart - 1000 * 3,
				this.executionRecorder.get(count++), tolerance);
		
		assertTrue("2nd execution invalid", Math.abs(this.executionRecorder.get(count++) - 
				(product.firstDeliveryPeriodStart - 1000*2)) < tolerance);
		
		assertTrue("3rd execution invalid", Math.abs(this.executionRecorder.get(count) - 
				(product.firstDeliveryPeriodStart - 1000)) < tolerance);
		
	}
	
	/**
	 * @formatter:off
	 * 
	 * 1st delivery period: 1-3------------ (scheduled at -1:000s)
	 * -- stop ---
	 * (numbers give seconds after opening time of first delivery period)
	 * 
	 * @formatter:on
	 * 
	 * Test method for {@link de.unik.ines.soeasy.flex.model.MMarketProductPattern#schedule()}.
	 * @throws InterruptedException 
	 */
	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testClosingTimeGuarantee() throws InterruptedException {
		product = new MarketProductPattern();
		product.productId = 6;
		product.closingTime = "-1s";
		product.openingTime = "-4s";
		product.auctionInterval = "2s";
		product.deliveryPeriodDuration = 1000*1;
		product.auctionDeliverySpan = product.deliveryPeriodDuration;
		product.firstDeliveryPeriodStart = DateTimeUtils.currentTimeMillis() + 1000*5;
		product.energyResolutionKWh = 0;
		mProductRepos.save(product);
		
		mmproduct = new MMarketProductPattern(product);
		mpClearer.setClearingProvider(new ClearingProvider() {
			@Override
			public ClearingMethod getClearing(String id) {
				return new PseudoClearingMethod();
			}
		});
		
		Interval initialDdeliveryInterval = new Interval(product.firstDeliveryPeriodStart,
				product.firstDeliveryPeriodStart + product.auctionDeliverySpan);
		flexTestUtils.generateDemandAndOffer(initialDdeliveryInterval, product, 1);
		
		Thread check  = new Thread() {
			 public void run(){
				 mpClearer.schedule(mmproduct);
			 }
		};
		check.start();
		
		
		Thread.sleep(200);
		mmproduct.stopClearing();
		Thread.sleep(1000*4+500);
		
		log.info("Start JUnit checks...");
		
		int count = 0;
		log.info("1st execution: " + Math.abs(executionRecorder.get(count) - (product.firstDeliveryPeriodStart - 1000*3)));
		assertTrue("1st execution invalid", Math.abs(executionRecorder.get(count++) - 
				(product.firstDeliveryPeriodStart - 1000*3)) < tolerance);
		
		log.info("2nd execution: " + Math.abs(executionRecorder.get(count) - (product.firstDeliveryPeriodStart - 1000*1)));
		assertTrue("2nd execution invalid", Math.abs(executionRecorder.get(count++) - 
				(product.firstDeliveryPeriodStart - 1000*1)) < tolerance);
	}
}
