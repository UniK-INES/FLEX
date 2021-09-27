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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.iwes.enavi.cim.codelists.StandardBusinessTypeList;
import de.iwes.enavi.cim.codelists.StandardCodingSchemeTypeList;
import de.iwes.enavi.cim.codelists.StandardContractTypeList;
import de.iwes.enavi.cim.codelists.StandardCurveTypeList;
import de.iwes.enavi.cim.codelists.StandardEnergyProductTypeList;
import de.iwes.enavi.cim.codelists.StandardObjectAggregationTypeList;
import de.iwes.enavi.cim.codelists.StandardRoleTypeList;
import de.iwes.enavi.cim.codelists.StandardUnitOfMeasureTypeList;
import de.iwes.enavi.cim.schedule51.DomainID_String;
import de.iwes.enavi.cim.schedule51.MarketAgreement;
import de.iwes.enavi.cim.schedule51.MarketParticipant;
import de.iwes.enavi.cim.schedule51.MeasurementPointID_String;
import de.iwes.enavi.cim.schedule51.PartyID_String;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.soeasy.common.model.MarketProductPattern;
import de.unik.ines.soeasy.flex.exceptions.NoMatchingDemandDataException;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.TimeSeriesRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;
import de.unik.ines.soeasy.flex.util.cim.LocalFlexDemandScheduleMarketDocumentFactory;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class LocalFlexDemandManager {

	private Log log = LogFactory.getLog(LocalFlexDemandManager.class);
	
	@Autowired
	protected TimeSeriesRepository timeSeriesRepos;
	
	@Autowired
	protected TimeInitializingBean timeBean;

	@PersistenceContext
	private EntityManager entityManager;
	
	/**
	 * Retrieves FLEX demand for the given user's location for the next day from
	 * database. Queries {@link TimeSeries} directly.
	 * 
	 * @param userAccount
	 * @return with sequence number
	 */
	public Schedule_MarketDocument getLocalFlexDemandSmd(UserAccount userAccount) {
		String location = userAccount.getLocation();
		log.info("Retrieve local flexibility next day (" + timeBean.getNextDayString() + " / "
				+ timeBean.getNextDayStart() + ") for location " + location);
		List<TimeSeries> tslist = timeSeriesRepos.findTsByLocationAndTime(location, timeBean.getNextDayStart(), 
				timeBean.getDayAfterNextDayStart());
		if (tslist.size() == 0) {
			log.warn("Found no Time series for " + location);
			throw new NoMatchingDemandDataException("Found no Time series for " + location);
		} else if (tslist.size() > 1) {
			log.warn("Got more than one Time series for " + location);
			throw new NoMatchingDemandDataException("Got more than one Time series for " + location);
		}
		TimeSeries ts = tslist.get(0);
		entityManager.detach(ts);
		ts = completeLocalFlexTimeSeries(ts, location);
		return LocalFlexDemandScheduleMarketDocumentFactory.getScheduleDocument(timeBean, ts);
	}
	
	/**
	 * TODO testing
	 * 
	 * @param userAccount
	 * @param mmpPattern
	 * @param earliestDelivery if less than 0, the next delivery period is
	 *                         considered
	 * @return
	 */
	public Schedule_MarketDocument getLocalFlexDemandSmd(UserAccount userAccount,
			MMarketProductPattern mmpPattern, long earliestDelivery) {
		String location = userAccount.getLocation();
		log.info("Retrieve local flexibility for product " + mmpPattern + " for location " + location);

		// determine next delivery time span:
		MarketProductPattern mp = mmpPattern.getProductPattern();
		long earliest = earliestDelivery < 0 ? DateTimeUtils.currentTimeMillis() : earliestDelivery;
		long nextDeliveryStart = (long) (Math
				.ceil((earliest - mp.firstDeliveryPeriodStart) / (double) mp.auctionDeliverySpan)
				* mp.auctionDeliverySpan) + mp.firstDeliveryPeriodStart;
		if (log.isDebugEnabled()) {
			log.debug("Retrieve demand with delivery between "
					+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(nextDeliveryStart)) + " ("
					+ nextDeliveryStart + ") and "
					+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS")
							.format(new Date(nextDeliveryStart + mp.auctionDeliverySpan))
					+ " (" + (nextDeliveryStart + mp.auctionDeliverySpan) + ") - SimTime: "
					+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(DateTimeUtils.currentTimeMillis())));
		}

		List<TimeSeries> tslist = timeSeriesRepos.findTsByLocationAndTime(location, nextDeliveryStart,
				nextDeliveryStart + mp.auctionDeliverySpan);
		if (tslist.size() == 0) {
			log.warn("Found no Time series for " + location);
			throw new NoMatchingDemandDataException("Found no Time series for " + location);
		} else if (tslist.size() > 1) {
			log.warn("Got more than one Time series for " + location);
			throw new NoMatchingDemandDataException("Got more than one Time series for " + location);
		}
		TimeSeries ts = tslist.get(0);
		entityManager.detach(ts);
		ts = completeLocalFlexTimeSeries(ts, location);
		return LocalFlexDemandScheduleMarketDocumentFactory.getScheduleDocument(timeBean, ts);
	}

	/**
	 * Sets the following TS fields:
	 * 
	 * Assumes that period has been set!
	 * 
	 * <ul>
	 * <li>MR-ID (TS-<start millis>-<end millies>)</li>
	 * <li>StandardBusinessType (Remaining Capacity)</li>
	 * <li>StandardEnergyProductType (Active Energy)</li>
	 * <li>StandardObjectAggregationType (AC link)</li>
	 * <li>InDomain_mRID (<location>)</li>
	 * <li>OutDomain_mRID (<location>)</li>
	 * <li>MarketEvaluationPoint (<location>)</li>
	 * <li>OutDomain_mRID</li>
	 * <li>In (DSO)</li>
	 * <li>Out (DSO)</li>
	 * <li>MarketAgreement (daily)</li>
	 * <li>ConnectingLine (Sample)</li>
	 * <li>Measure_Unit_name (kW)</li>
	 * <li>Curve type (Sequential fixed size block)</li>
	 * </ul>
	 * 
	 * @param ts
	 * @return
	 */
	public TimeSeries completeLocalFlexTimeSeries(TimeSeries ts, String location) {
		ts.setmRID("TS_" + ts.getPeriod().getTimeInterval().getStartMillis() + "-" + 
				ts.getPeriod().getTimeInterval().getEndMillis());
		ts.setBusinessType(StandardBusinessTypeList.C01);
		ts.setProduct(StandardEnergyProductTypeList._8716867000030);
		ts.setObjectAggregation(StandardObjectAggregationTypeList.A10);
		
		ts.setInDomain_mRID(new DomainID_String(StandardCodingSchemeTypeList.NDE, location));
		ts.setOutDomain_mRID(new DomainID_String(StandardCodingSchemeTypeList.NDE, location));
		
		ts.setMarketEvaluationPoint(new MeasurementPointID_String(StandardCodingSchemeTypeList.NDE, location));
		
		MarketParticipant client = new MarketParticipant();
		client.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "DSO"));
		client.setMarketRole(StandardRoleTypeList.A18);
		ts.setIn(client);
		ts.setOut(client);
		
		ts.setMarketAgreement(new MarketAgreement(StandardContractTypeList.A01, "SampleContract"));
		ts.setConnectingLine_RegisteredResource_mRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "Sample"));
	
		ts.setMeasure_Unit_name(StandardUnitOfMeasureTypeList.KWT);
		ts.setCurveType(StandardCurveTypeList.A01);
		return ts;
	}
}
