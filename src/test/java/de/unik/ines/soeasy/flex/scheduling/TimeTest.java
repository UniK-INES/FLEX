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
package de.unik.ines.soeasy.flex.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.soeasy.common.model.MarketProductPattern;
import de.unik.ines.soeasy.flex.clearing.ClearingMethod;
import de.unik.ines.soeasy.flex.clearing.ClearingProvider;
import de.unik.ines.soeasy.flex.clearing.MarketProductClearer;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandManager;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

/**
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.repos", "de.unik.ines.soeasy.flex.clearing",
		"de.unik.ines.soeasy.flex.scheduling",
		"de.unik.ines.soeasy.flex.grid" })
@Import(FlexTestUtils.class)
public class TimeTest {

	public Log log = LogFactory.getLog(TimeTest.class);

	final static long ALLOWED_DEVIATION = 500;

	public static final String FILENAME_JSON_FLEXDEMAND_SMD = "json/GridFlexDemand_Schedule_MarketDocument.json";

	protected class PseudoClearingMethod implements ClearingMethod {

		@Override
		public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers,
				ClearingInfo cInfo) {
			executionRecorderRealTime.put(counter, System.currentTimeMillis());
			executionRecorderSimTime.put(counter, DateTimeUtils.currentTimeMillis());
			log.info("Increased counter: " + counter++ + " | "
					+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(DateTimeUtils.currentTimeMillis())) + " ("
					+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(cInfo.getClearingTime())) + ")");
		}
	}

	final Map<Integer, Long> executionRecorderRealTime = new HashMap<>();
	final Map<Integer, Long> executionRecorderSimTime = new HashMap<>();

	int counter = 0;

	long starttime;

	@Autowired
	TimeInitializingBean timebean;

	@Autowired
	MarketProductRepository mproductRepos;

	MMarketProductPattern mmproduct;
	MarketProductPattern marketProductPattern = new MarketProductPattern();

	List<MarketEnergyRequest> requests = new ArrayList<>();

	@Autowired
	MarketProductClearer mpClearer;

	@Autowired
	FlexOfferRepository fowRepos;

	@Autowired
	UserAccountRepository userRepos;

	TimeInitializingBean storageTimeBean;

	@Autowired
	GridFlexDemandManager gridFlexManager;

	@Autowired
	@Qualifier("taskScheduler")
	ThreadPoolTaskScheduler taskScheduler;

	@Autowired
	FlexTestUtils flexTestUtils;

	/**
	 * - 1st clearing after 6s - 2nd clearing after 7s - 3rd clearing after 8s
	 * 
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		taskScheduler.initialize();
		if (this.storageTimeBean == null) {
			this.storageTimeBean = new TimeInitializingBean();
			this.storageTimeBean.basetime = this.timebean.basetime;
			this.storageTimeBean.offset = this.timebean.offset;
			this.storageTimeBean.factor = this.timebean.factor;
			this.storageTimeBean.zone = this.timebean.zone;
		}

		// wait for DB initialisation:
		Thread.sleep(5 * 1000);
		setupMarketProduct();

		mpClearer.setClearingProvider(new ClearingProvider() {
			@Override
			public ClearingMethod getClearing(String id) {
				return new PseudoClearingMethod();
			}
		});
	}

	public void setupMarketProduct() {
		starttime = System.currentTimeMillis();
		log.debug("Starttime: " + new Date(starttime));

		marketProductPattern = new MarketProductPattern();
		marketProductPattern.productId = 2;
		marketProductPattern.firstDeliveryPeriodStart = starttime + 15 * 1000;
		marketProductPattern.deliveryPeriodDuration = 1000 * 1000; // necessary to avoid recurrent scheduling
		marketProductPattern.auctionDeliverySpan = marketProductPattern.deliveryPeriodDuration;
		marketProductPattern.openingTime = "-5s";
		marketProductPattern.closingTime = "-2s";
		marketProductPattern.auctionInterval = "1s";
		marketProductPattern.minPrice = -3000.0f;
		marketProductPattern.maxPrice = 3000.0f;
		marketProductPattern = mproductRepos.save(marketProductPattern);

		mmproduct = new MMarketProductPattern(marketProductPattern);
	}

	@AfterEach
	public void tearDown() throws Exception {
		executionRecorderRealTime.clear();
		executionRecorderSimTime.clear();
		mmproduct.stopClearing();

		this.timebean.basetime_target = this.storageTimeBean.basetime_target;
		this.timebean.offset = this.storageTimeBean.offset;
		this.timebean.factor = this.storageTimeBean.factor;
		this.timebean.zone = this.storageTimeBean.zone;
		this.timebean.matchbasetime = false;
		this.timebean.applyChanges();

		this.taskScheduler.shutdown();
	}

	protected void testTimes(String desc, long was, Number expected) {
		assertTrue(
				desc + " ("
						+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(DateTimeUtils.currentTimeMillis()))
						+ "): was " + new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(was))
						+ " / expected: "
						+ new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(expected.longValue())),
				Math.abs(was - expected.longValue()) < ALLOWED_DEVIATION * timebean.getFactor());
	}

	private void testRealAndSimTimes() {
		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		int count = 0;
		System.out.println(">>> " + executionRecorderRealTime.size());
		testTimes("First clearing Real", executionRecorderRealTime.get(count++), timebean.basetime - timebean.offset
				+ (marketProductPattern.firstDeliveryPeriodStart - 4 * 1000 - timebean.basetime) / timebean.factor);
		testTimes("Second clearing Real", executionRecorderRealTime.get(count++), timebean.basetime - timebean.offset
				+ (marketProductPattern.firstDeliveryPeriodStart - 3 * 1000 - timebean.basetime) / timebean.factor);
		testTimes("Third clearing Real", executionRecorderRealTime.get(count++), timebean.basetime - timebean.offset
				+ (marketProductPattern.firstDeliveryPeriodStart - 2 * 1000 - timebean.basetime) / timebean.factor);

		count = 0;
		testTimes("First clearing Simulation", executionRecorderSimTime.get(count++),
				marketProductPattern.firstDeliveryPeriodStart - 4 * 1000);
		testTimes("Second clearing Simulation", executionRecorderSimTime.get(count++),
				marketProductPattern.firstDeliveryPeriodStart - 3 * 1000);
		testTimes("Third clearing Simulation", executionRecorderSimTime.get(count++),
				marketProductPattern.firstDeliveryPeriodStart - 2 * 1000);
	}
	
	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testDefault() throws Exception {
		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		timebean.factor = 1.0;
		timebean.offset = 0;
		timebean.basetime_target = starttime;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		/*
		 * Should schedule three clearings (4,3,2 seconds ahead delivery period start)
		 */
		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(15 * 1000);

		testRealAndSimTimes();
	}

	@Test
	public void testFutureBasetime() throws Exception {
		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		timebean.factor = 2.0;
		timebean.offset = 0;
		timebean.basetime_target = starttime + 1000 * 60 * 60 * 10;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		assertEquals(
				"For times before basetime, sim time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(timebean.convertRealToSimTime(starttime))
						+ ") should be equal to real time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(starttime) + ")",
				starttime, timebean.convertRealToSimTime(starttime));

		assertEquals("For times before basetime, real time ("
				+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(timebean.convertSimToRealTime(starttime))
				+ ") should be equal to sim time (" + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(starttime),
				starttime, timebean.convertSimToRealTime(starttime));
	}

	@Test
	public void testFutureBasetimeOffset() throws Exception {
		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		timebean.factor = 2.0;
		timebean.offset = 500;
		timebean.basetime_target = starttime + 1000 * 60 * 60 * 10;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		assertEquals(
				"For times before basetime, sim time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(timebean.convertRealToSimTime(starttime))
						+ ") should be equal to real time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(starttime) + ")",
				starttime + 500, timebean.convertRealToSimTime(starttime));

		assertEquals(
				"For times before basetime, real time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(timebean.convertSimToRealTime(starttime))
						+ ") should be equal to sim time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(starttime) + ")",
				starttime - 500, timebean.convertSimToRealTime(starttime));
	}

	@Test
	public void testFutureBasetimeOffsetBeyondBasetime() throws Exception {
		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		timebean.factor = 2.0;
		timebean.offset = 1000 * 60 * 5;
		timebean.basetime_target = starttime + 1000 * 60 * 4;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		assertEquals(
				"For times before basetime, sim time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(timebean.convertRealToSimTime(starttime))
						+ ") should be equal to real time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(starttime) + ")",
				starttime + 1000 * 60 * 6, timebean.convertRealToSimTime(starttime));

		assertEquals(
				"For times before basetime, real time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(timebean.convertSimToRealTime(starttime))
						+ ") should be equal to sim time ("
						+ DateFormat.getTimeInstance(DateFormat.MEDIUM).format(starttime) + ")",
				starttime, timebean.convertSimToRealTime(starttime + 1000 * 60 * 6));
	}

	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testOffset() throws Exception {
		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		timebean.factor = 1.0;
		timebean.offset = 5 * 1000;
		timebean.basetime_target = starttime;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(10 * 1000);

		testRealAndSimTimes();
	}

	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testFactor() throws Exception {
		timebean.factor = 2.0;
		timebean.offset = 5 * 1000;
		timebean.basetime_target = starttime;
		marketProductPattern.firstDeliveryPeriodStart = starttime + 30 * 1000;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(10 * 1000);

		testRealAndSimTimes();
	}

	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testMultiDeliveryPerAuction() throws Exception {
		starttime = DateTime.parse("2100/01/01 00:00:00", DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss")).getMillis();
		log.debug("Starttime: " + new Date(starttime));

		marketProductPattern = new MarketProductPattern();
		marketProductPattern.productId = 2;
		marketProductPattern.firstDeliveryPeriodStart = starttime;
		marketProductPattern.deliveryPeriodDuration = 4 * 1000; // necessary to avoid recurrent scheduling
		marketProductPattern.auctionDeliverySpan = 24 * 1000;
		marketProductPattern.openingTime = "-2s";
		marketProductPattern.closingTime = "-1s";
		marketProductPattern.auctionInterval = "1s";
		marketProductPattern.minPrice = -3000.0f;
		marketProductPattern.maxPrice = 3000.0f;
		marketProductPattern = mproductRepos.save(marketProductPattern);
		mmproduct = new MMarketProductPattern(marketProductPattern);

		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		// store flex demand and offers to enable clearing:
		deliveryInterval = new Interval(
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan,
				marketProductPattern.firstDeliveryPeriodStart + 2 * marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		timebean.factor = 5.0;
		timebean.offset = starttime - System.currentTimeMillis() - 3 * 1000;
		timebean.basetime_target = starttime;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		timebean.logTimesInfo(log);

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();

		Thread.sleep((long) (15 * 1000 / timebean.factor));

		testTimes("First clearing Simulation", executionRecorderSimTime.get(0),
				marketProductPattern.firstDeliveryPeriodStart - 1 * 1000);
		assertEquals("Only one clearing...", 1, executionRecorderSimTime.size());

		Thread.sleep((long) (29 * 1000 / timebean.factor));

		testTimes("First clearing Simulation", executionRecorderSimTime.get(1),
				marketProductPattern.firstDeliveryPeriodStart + (24 - 1) * 1000);
		assertEquals("Number of clearings...", 2, executionRecorderSimTime.size());
	}

	public void testFactorChange() throws Exception {
		timebean.applyChanges();

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(10 * 1000);

		testRealAndSimTimes();
		
		timebean.factor = 2.0;
		timebean.applyChanges();

		check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(10 * 1000);

		testRealAndSimTimes();
	}

	/**
	 * Negative offset -> simulation time in past
	 * 
	 * @throws Exception
	 */
	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testStarttimeMinus() throws Exception {
		timebean.factor = 2.0;
		timebean.offset = -8 * 1000;
		timebean.basetime_target = starttime - 8 * 1000;
		// when basetime is changed, firstDeliveryPeriodStart should be adapted
		// accordingly:
		marketProductPattern.firstDeliveryPeriodStart = starttime + 8 * 1000;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.auctionDeliverySpan);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		Thread.sleep(2 * 1000);

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(9 * 1000);

		testRealAndSimTimes();
	}

	/**
	 * Basetime > real time -> realtime in past (shifted to present via offset)
	 * 
	 * @throws Exception
	 */
	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testStarttimePlus() throws Exception {
		timebean.factor = 2.0;
		timebean.offset = 5 * 1000;
		timebean.basetime_target = starttime + 5 * 1000;
		// when basetime is changed, firstDeliveryPeriodStart should be adapted
		// accordingly:
		marketProductPattern.firstDeliveryPeriodStart = starttime + 20 * 1000;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.deliveryPeriodDuration);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();

		Thread.sleep(8 * 1000);

		testRealAndSimTimes();
	}

	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	public void testNegAuctionInterval() throws Exception {

		mmproduct.getProductPattern().auctionInterval = "-2s";
		mmproduct.getProductPattern().closingTime = "-4s";
		mmproduct.getProductPattern().firstDeliveryPeriodStart = starttime + 10 * 1000;
		timebean.factor = 1.0;
		timebean.offset = 0;
		timebean.basetime_target = starttime;
		timebean.matchbasetime = false;
		timebean.applyChanges();

		// store flex demand and offers to enable clearing:
		Interval deliveryInterval = new Interval(marketProductPattern.firstDeliveryPeriodStart,
				marketProductPattern.firstDeliveryPeriodStart + marketProductPattern.deliveryPeriodDuration);
		flexTestUtils.generateFo(deliveryInterval, marketProductPattern);
		flexTestUtils.generateSmd(deliveryInterval);

		Thread check = new Thread() {
			public void run() {
				mpClearer.schedule(mmproduct);
			}
		};
		check.start();
		Thread.sleep(10 * 1000);

		testTimes("First clearing Simulation", executionRecorderSimTime.get(0),
				marketProductPattern.firstDeliveryPeriodStart - 2 * 1000);
	}
}
