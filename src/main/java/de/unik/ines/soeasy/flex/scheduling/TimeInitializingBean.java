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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeUtils.MillisProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.iee.soeasy.common.util.Utils;
import de.soeasy.common.model.MarketProductPattern;
import de.soeasy.common.model.TimeInformation;
import de.unik.ines.soeasy.flex.balance.MeterReadingManager;
import de.unik.ines.soeasy.flex.clearing.MarketProductClearer;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;

/**
 * Conversion from real time to simulation time: BaseT + (RealT - BaseT +
 * Offset) / TFactor
 * 
 * E.g., offset can be applied to simulate an earlier or later time as it were
 * in present. Time factors are applied after considering offset.
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class TimeInitializingBean implements InitializingBean {
	
	private Log log = LogFactory.getLog(TimeInitializingBean.class);
	
	protected boolean started = false;

	@Value("${de.unik.ines.soeasy.flex.time.basetime.initial:9223372036854775807}")
	protected long basetime_initial;

	@Value("${de.unik.ines.soeasy.flex.time.basetime:9223372036854775807}")
	protected long basetime_target;

	@Value("${de.unik.ines.soeasy.flex.metering.deadline:Long.MAX_VALUE}")
	protected long meteringDeadline;

	@Value("${de.unik.ines.soeasy.flex.balance.waitingTime:Long.MAX_VALUE}")
	protected long balanceWaiting;

	protected long basetime;
	
	/**
	 * Positive value means shifting simulation time into the future, negative value means turning back simulation time. See also 
	 * de.unik.ines.soeasy.flex.time.matchbasetime.
	 */
	@Value("${de.unik.ines.soeasy.flex.time.offset:0}")
	protected long offset;
	
	/**
	 * If true, de.unik.ines.soeasy.flex.time.offset is ignored and offset is calculated such that current time matches basetime.
	 */
	@Value("${de.unik.ines.soeasy.flex.time.matchbasetime:false}")
	protected boolean matchbasetime; 
	
	/**
	 * E.g., a factor of 2.0 means that simulation time runs twice as fast as real time (starting from basetime).
	 */
	@Value("${de.unik.ines.soeasy.flex.time.factor:1.0}")
	protected double factor;

	@Value("${de.unik.ines.soeasy.flex.time.zone:CET}")
	protected String zone;
	
	protected boolean timeInfoChanged = true;
	
	protected TimeInformation tinfo;

	/**
	 * @param timeInfoChanged the timeInfoChanged to set
	 */
	public void setTimeInfoChanged(boolean timeInfoChanged) {
		this.timeInfoChanged = timeInfoChanged;
	}

	/**
	 * Sets the initial basetime (should only be called during initialisation!).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		long temp = this.basetime_target;
		this.basetime_target = this.basetime_initial;
		applyChanges();
		this.basetime_target = temp;
	}
	
	public void setOffset(long offset) {
		this.offset = offset;
		log.info("Offset changed to " + this.offset);
	}
	public void setFactor(double factor) {
		this.factor = factor;
		log.info("Time factor changed to " + this.factor);
	}

	public void setBasetime(long basetime) {
		this.basetime_target = basetime;
	}

	public void applyChanges() {
		
		if (basetime_target == Long.MAX_VALUE) {
			this.basetime = (System.currentTimeMillis() / (60*1000)) *  60*1000;
		} else {
			this.basetime = basetime_target;
		}
		
		if (matchbasetime) {
			log.info("Set offset to match basetime now... ");
			this.offset = this.basetime - System.currentTimeMillis();
		}
		
		log.info("Basetime set to " + new Date(this.basetime) + "(" + this.basetime + ")");
		log.info("Offset is " + Duration.ofMillis(this.offset) + "(" + this.offset + ")");
		log.info("Time factor is " + this.factor);
		
		DateTimeUtils.setCurrentMillisProvider(new MillisProvider() {
			
			@Override
			public long getMillis() {
				return TimeInitializingBean.this.convertRealToSimTime(System.currentTimeMillis());
			}
		});
		this.timeInfoChanged = true;
	}
	
	/**
	 * Converts simulation time to real time considering basetime, offset, and factor.
	 * 
	 * @param simTime
	 * @return real time
	 */
	public long convertSimToRealTime(long simTime) {
		// calculate difference between simulation time and base time (neg > basetime in future relative to sim time)
		long basetimediff = simTime - TimeInitializingBean.this.basetime;
		return (long) (TimeInitializingBean.this.basetime - TimeInitializingBean.this.offset +
				basetimediff / (basetimediff < 0 ? 1.0 :  
				TimeInitializingBean.this.factor));
	}

	/**
	 * Converts real time to simulation time considering basetime, offset, and factor.
	 * 
	 * Basetime in Zukunft, factor ab dann
	 * 
	 * offset = basetime - realStartTime
	 * @param realTime
	 * @return simulation time
	 * 
	 */
	public long convertRealToSimTime(long realTime) {
		long basetimediff = realTime + TimeInitializingBean.this.offset - TimeInitializingBean.this.basetime;
		if (log.isDebugEnabled()) {
			log.debug("Real time: " + new Date(realTime));
			log.debug("Simu time: " + new Date((long)(TimeInitializingBean.this.basetime + 
					basetimediff * (basetimediff > 0 ?  
					TimeInitializingBean.this.factor : 1.0))));
		}
		return (long) (TimeInitializingBean.this.basetime + 
				basetimediff * (basetimediff > 0 ?  
				TimeInitializingBean.this.factor : 1.0));
	}
	
	public void logTimesInfo(Log logger) {
		logger.info("Real time: "
				+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
		logger.info("Simu time: "
				+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(DateTimeUtils.currentTimeMillis())));
	}

	/**
	 * @return
	 */
	public TimeInformation getTimeInformation(MarketProductRepository mProductRepos,
			MeterReadingManager readingManager) {
		if (this.isTimeInfoChanged()) {
			this.tinfo = new TimeInformation();
			tinfo.offset = getOffset();
			tinfo.baseTime = getBasetime();
			tinfo.timeZone = getTimeZone();
			tinfo.meteringInterval = readingManager.getMeteringInterval();
			tinfo.meteringDeadline = this.meteringDeadline;
			tinfo.balanceDeadline = this.balanceWaiting;
			tinfo.simulationFactor = (float) getFactor();

			for (MarketProductPattern product : mProductRepos.findAll()) {
				long productOpeningTime = Utils.getMarketClosingTimeForBidStartTime(product.firstDeliveryPeriodStart,
						product.openingTime, ZoneId.of(MarketProductClearer.zone));
				tinfo.firstMarketOpening = productOpeningTime < tinfo.firstMarketOpening ? productOpeningTime
						: tinfo.firstMarketOpening;
			}
			setTimeInfoChanged(false);
		}
		this.tinfo.currentSystemTime = System.currentTimeMillis();
		this.tinfo.currentSimulationTime = DateTimeUtils.currentTimeMillis();

		return tinfo;
	}

	/**
	 * Converts a period in simulation time to a period in real time considering the factor and assuming the period
	 * is after basetime.
	 * 
	 * @param period
	 * @return period in real time
	 */
	public long convertSimToRealPeriodAfterBasetime(long period) {
		return (long) (period/this.factor);
	}
	
	public long getNextDayStart() {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(DateTimeUtils.currentTimeMillis()), ZoneId.of(this.zone));
		return zdt.toLocalDate().plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)*1000;		
	}
	
	public long getDayAfterNextDayStart() {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(DateTimeUtils.currentTimeMillis()), ZoneId.of(this.zone));
		return zdt.toLocalDate().plusDays(2).atStartOfDay().toEpochSecond(ZoneOffset.UTC)*1000;		
	}
	
	public String getNextDayString() {
		return new SimpleDateFormat("dd/MM/YY").format(new Date(getNextDayStart()));
	}
	public long getBasetime() {
		return this.basetime;
	}

	public long getOffset() {
		return offset;
	}

	public double getFactor() {
		return factor;
	}

	public String getTimeZone() {
		return this.zone;
	}
	
	public void setServerStarted() {
		this.started = true;
	}
	
	public boolean isServerStarted() {
		return this.started;
	}

	/**
	 * @return the timeInfoChanged
	 */
	public boolean isTimeInfoChanged() {
		return timeInfoChanged;
	}
}
