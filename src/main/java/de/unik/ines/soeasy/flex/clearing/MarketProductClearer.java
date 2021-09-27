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
package de.unik.ines.soeasy.flex.clearing;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.soeasy.common.model.MarketProductPattern;
import de.soeasy.common.utils.Utils;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.GridFlexDemandSmdRepos;

/**
 * Schedules a {@link MMarketProductPattern} for clearing.
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class MarketProductClearer {

	private Log log = LogFactory.getLog(MarketProductClearer.class);
	
	public static String zone = "CET";
	
	@Autowired
	ClearingProvider clearingProvider;
	
	@Autowired
	GridFlexDemandSmdRepos gridFlexDemandRepos;

	@Autowired
	FlexOfferRepository foRepos;
	
	@Autowired
	@Qualifier("taskScheduler")
	ThreadPoolTaskScheduler taskScheduler;
	
	@Autowired
	ClearingInfoRepository clearingPriceReposistory;
	
	/**
	 * Schedules a clearing scheduler for the first delivery period. 
	 * 
	 * The clearing scheduler schedules all auction rounds and the clearing scheduler for the 
	 * next delivery period. 
	 */
	public void schedule(MMarketProductPattern mmProductPattern) {
		// schedule clearing scheduler
		log.trace("1st delivery period start: " + new Date(mmProductPattern.getProductPattern().
				firstDeliveryPeriodStart));
		getClearingsScheduleTask(mmProductPattern, 
				mmProductPattern.getProductPattern().
				firstDeliveryPeriodStart).run();
	}
	
	/**
	 * Schedules all according single auction tasks and the next 'clearings schedule task' at delivery period opening time.
	 * 
	 * @param deliveryPeriodStart
	 * @return
	 */
	protected Runnable getClearingsScheduleTask(MMarketProductPattern mmProductPattern, long deliveryPeriodStart) {
			
		return new Runnable() {
			@Override
			public void run() {
				if (mmProductPattern.isActive()) {
					log.debug("MmProductPattern (" + mmProductPattern + ") is active (delivery: "
							+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(deliveryPeriodStart))
							+ ")");
					MarketProductPattern productPattern = mmProductPattern.getProductPattern();
					long auctionInterval = Utils.getDurationFromDurationString(productPattern.auctionInterval).toMillis();
					if (auctionInterval < 0) {
						long cStart = Utils.getMarketClosingTimeForBidStartTime(deliveryPeriodStart, productPattern.auctionInterval, ZoneId.of(zone));
						if (cStart >= DateTimeUtils.currentTimeMillis()) {
							log.trace("Schedule product " + mmProductPattern + " for "
									+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(cStart))
									+ " (simulation time)...");
							taskScheduler.schedule(getClearingTask(mmProductPattern, deliveryPeriodStart, cStart), new Date(cStart));
						}

					} else {
						for (long cStart = Utils.getMarketClosingTimeForBidStartTime(deliveryPeriodStart,
								productPattern.closingTime, ZoneId.of(zone)); cStart > Utils
										.getMarketClosingTimeForBidStartTime(deliveryPeriodStart,
												productPattern.openingTime,
												ZoneId.of(zone)); cStart = cStart - auctionInterval) {
								if (cStart >= DateTimeUtils.currentTimeMillis()) {
								log.trace("Schedule product " + mmProductPattern + " for "
										+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(cStart))
										+ "...");
									taskScheduler.schedule(getClearingTask(mmProductPattern, deliveryPeriodStart, cStart), new Date(cStart));
								}
						}
					}
					// task to schedule clearings of next delivery period
					taskScheduler.schedule(
							getClearingsScheduleTask(mmProductPattern,
									deliveryPeriodStart + productPattern.auctionDeliverySpan),
							new Date(Utils.getMarketClosingTimeForBidStartTime(deliveryPeriodStart,
									productPattern.openingTime, ZoneId.of(zone))));
				}
				else {
					log.debug("MmProductPattern (" + mmProductPattern + ") is inactive (delivery: "
							+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(deliveryPeriodStart)));
				}
			}
		};
	}
	
	/**
	 * Provides the actual clearing task
	 * 
	 * @param mmProductPattern
	 * @param deliveryPeriodStart
	 * @param clearingTime
	 * @return the tasks
	 */
	public Runnable getClearingTask(MMarketProductPattern mmProductPattern, long deliveryPeriodStart, long clearingTime) {
		return new Runnable() {
			@Override
			public void run() {
				clearProduct(mmProductPattern, deliveryPeriodStart, clearingTime);
			}
		};
	}
	
	/**
	 * Retrieves and filters requests, instantiates clearing method and triggers clearing.
	 * 
	 * @param mmProductPattern
	 * @param deliveryPeriodStart
	 * @param clearingTime
	 */
	public void clearProduct(MMarketProductPattern mmProductPattern, long deliveryPeriodStart, long clearingTime) {
		log.info("Perform clearing for " + mmProductPattern + " at "
				+ new SimpleDateFormat("dd/MM>HH:mm:ss.SSS").format(new Date(DateTimeUtils.currentTimeMillis()))
				+ " (delivery period start: "
				+ new SimpleDateFormat("dd/MM>HH:mm:ss.SSS").format(new Date(deliveryPeriodStart)) + " / "
				+ deliveryPeriodStart);
		MarketProductPattern productPattern = mmProductPattern.getProductPattern();
		ClearingInfo clearingInfo = new ClearingInfo(productPattern, deliveryPeriodStart, clearingTime);
		
		// retrieve demands:
		List<Schedule_MarketDocument> demands = gridFlexDemandRepos.findSmdsByTime(deliveryPeriodStart,
				deliveryPeriodStart + productPattern.auctionDeliverySpan);

		if (log.isDebugEnabled()) {
			log.debug("Retrieved demand: " + demands);
		}

		// retrieve offers:
		long intervalStart = Utils.getDurationFromDurationString(productPattern.auctionInterval).toMillis() > 0 ?
				Utils.getMarketClosingTimeForBidStartTime(clearingTime, "-" + productPattern.auctionInterval, ZoneId.of(zone)) :
					Utils.getMarketClosingTimeForBidStartTime(deliveryPeriodStart, productPattern.openingTime, ZoneId.of(zone));
		if (log.isDebugEnabled()) {
			log.debug(clearingInfo + "> Retrieve requests for period "
					+ new SimpleDateFormat("dd/MM>HH:mm:ss.SSS").format(new Date(deliveryPeriodStart)) + " to "
					+ new SimpleDateFormat("dd/MM>HH:mm:ss.SSS")
							.format(new Date(deliveryPeriodStart + productPattern.auctionDeliverySpan))
					+ ", submitted between "
					+ new SimpleDateFormat("dd/MM>HH:mm:ss.SSS").format(new Date(intervalStart)) + " and "
					+ new SimpleDateFormat("dd/MM>HH:mm:ss.SSS").format(new Date(clearingTime)) + ".");
		}
		List<FlexOfferWrapper> offers = this.foRepos.findAllUnmatchedByProductAndIntervalTime(
			deliveryPeriodStart,
			deliveryPeriodStart + productPattern.deliveryPeriodDuration,
			mmProductPattern.getProductPattern().productId);
		
		if (log.isDebugEnabled()) {
			log.debug(clearingInfo + "> Retrieved requests: " + offers.size());
			if (log.isTraceEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append(clearingInfo + "> Retrieved requests:" + System.getProperty("line.separator"));
				offers.forEach(request -> sb.append("\t" + request + System.getProperty("line.separator")));
				log.trace(sb);
			}
			log.trace("Before clearing: "
					+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS")
							.format(new Date(DateTimeUtils.currentTimeMillis()))
					+ " (sim) / scheduled: "
					+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(clearingTime))
					+ " (sim)");
		}
		
		if (demands.size() > 0 && offers.size() > 0) {
			Instant start = Instant.now();
			this.clearingProvider.getClearing(mmProductPattern.getClearingId()).clearMarket(demands, offers,
					clearingInfo);
			Instant end = Instant.now();

			if (log.isDebugEnabled()) {
				log.debug("Clearing took " + Duration.between(start, end) + " ("
						+ this.clearingProvider.getClearing(mmProductPattern.getClearingId()).getClass().getSimpleName()
						+ ")");
			}
		} else {
			log.warn("No clearing because "
					+ (demands.size() == 0 ? (offers.size() == 0 ? "no demand and no offers" : "no demand")
							: "no offers")
					+ "!");
			this.clearingPriceReposistory.save(clearingInfo.setNothingToClear());
		}

		if (log.isTraceEnabled()) {
			log.trace("After clearing: "
					+ new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(DateTimeUtils.currentTimeMillis()))
					+ " (sim) / scheduled: "
					+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(clearingTime))
					+ " (sim)");
		}
	}
	
	public void setClearingProvider(ClearingProvider clearingProvider) {
		this.clearingProvider = clearingProvider;
	}
}
