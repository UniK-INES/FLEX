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
package de.unik.ines.soeasy.flex.util.cim;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import de.iwes.enavi.cim.codelists.StandardBusinessTypeList;
import de.iwes.enavi.cim.codelists.StandardClassificationTypeList;
import de.iwes.enavi.cim.codelists.StandardCodingSchemeTypeList;
import de.iwes.enavi.cim.codelists.StandardContractTypeList;
import de.iwes.enavi.cim.codelists.StandardCurveTypeList;
import de.iwes.enavi.cim.codelists.StandardEnergyProductTypeList;
import de.iwes.enavi.cim.codelists.StandardMessageTypeList;
import de.iwes.enavi.cim.codelists.StandardObjectAggregationTypeList;
import de.iwes.enavi.cim.codelists.StandardProcessTypeList;
import de.iwes.enavi.cim.codelists.StandardRoleTypeList;
import de.iwes.enavi.cim.codelists.StandardUnitOfMeasureTypeList;
import de.iwes.enavi.cim.schedule51.DomainID_String;
import de.iwes.enavi.cim.schedule51.MarketAgreement;
import de.iwes.enavi.cim.schedule51.MarketParticipant;
import de.iwes.enavi.cim.schedule51.MeasurementPointID_String;
import de.iwes.enavi.cim.schedule51.PartyID_String;
import de.iwes.enavi.cim.schedule51.Point;
import de.iwes.enavi.cim.schedule51.Process;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.Series_Period;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * @author Sascha Holzhauer
 *
 */
public class BaselineScheduleMarketDocumentFactory {

	private static Log log = LogFactory.getLog(BaselineScheduleMarketDocumentFactory.class);
	
	/**
	 * @param clearingInfos
	 * @param deliveryStart
	 * @param deliveryEnd
	 * @param timebean
	 * @param clients
	 * @param requestRepos
	 * @return
	 */
	public static Schedule_MarketDocument getScheduleDocument(List<ClearingInfo> clearingInfos, 
			long deliveryStart, long deliveryEnd, TimeInitializingBean timebean, Iterable<UserAccount> clients,
			MarketEnergyRequestRepository requestRepos) {
		return getScheduleDocument(clearingInfos, deliveryStart, deliveryEnd, 1, timebean, clients, requestRepos);
	}
	
	/**
	 * @param clearingInfos
	 * @param deliveryStart
	 * @param deliveryEnd
	 * @param iteration
	 * @param timebean
	 * @param clients
	 * @param requestRepos
	 * @return
	 */
	public static Schedule_MarketDocument getScheduleDocument(List<ClearingInfo> clearingInfos, 
			long deliveryStart, long deliveryEnd, int iteration, TimeInitializingBean timebean, Iterable<UserAccount> clients,
			MarketEnergyRequestRepository requestRepos) {
		
		Schedule_MarketDocument doc = new Schedule_MarketDocument();

		
		// add document information
		doc.setmRID("schedule_" + deliveryStart + "-" + deliveryEnd + "_" + iteration);
		doc.setRevisionNumber("1");
		
		doc.setType(StandardMessageTypeList.A25);
		
		Process process = new Process();
		process.setType(StandardProcessTypeList.A49); // alternatives: A02, A15
		process.setClassification(StandardClassificationTypeList.A01);
		
		MarketParticipant sender = new MarketParticipant();
		sender.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "DEX"));
		sender.setMarketRole(StandardRoleTypeList.A11);
		doc.setSender_MarketParticipant(sender);
		
		MarketParticipant receiver = new MarketParticipant();
		receiver.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "Client"));
		receiver.setMarketRole(StandardRoleTypeList.A32);
		doc.setReceiver_MarketParticipant(receiver);
		
		// doc.setCreatedDateTime(CimFormats.DATA_TIME_FORMATTER.format(ZonedDateTime.now(ZoneId.of(timebean.getTimeZone()))));
		doc.setCreatedDateTime(new DateTime(DateTimeUtils.currentTimeMillis(), DateTimeZone.forID(timebean.getTimeZone())));
		
		Interval timeInterval = new Interval(deliveryStart, deliveryEnd, 
				DateTimeZone.forID(timebean.getTimeZone()));
		doc.setTimeInterval(timeInterval);
		
		doc.setDomain_mRID("ExampleArea");
		
		// add time series
		List<TimeSeries> timeSeries = new ArrayList<TimeSeries>();
		
		for (UserAccount user : clients) {
			Series_Period period = new Series_Period();
			int counter = 0;
			for (ClearingInfo cInfo : clearingInfos) {
				
				log.debug("Calculate energy for client " + user + " for delivery start " + 
						new SimpleDateFormat("hh:mm:ss").format(new Date(cInfo.getIntervalStart())));
				
				// per clearing, for all (partly) successful requests of a client, sum the energy
				float energy = 0;
				
				for (MarketEnergyRequest request : requestRepos.findAllByIntersectionAndUser(cInfo.getIntervalStart(), 
						cInfo.getIntervalStart() + cInfo.getProductPattern().deliveryPeriodDuration, user)) {
					energy += Float.isNaN(request.getRequest().energyAccepted) ? 0.0 : request.getRequest().energyAccepted;
				}
				log.trace("Energy after aggregation:" + energy);
				// convert to power:
				energy = energy / (cInfo.getProductPattern().deliveryPeriodDuration / (1000*3600.0f));
				log.trace("Energy after conversion:" + energy);
				period.addPoint(new Point(counter++, 
					Float.isNaN(energy) | Float.isInfinite(energy)  ? null : new BigDecimal(energy)));
			}
			
			if (!period.getPoints().isEmpty()) {
				TimeSeries times = new TimeSeries();
				times.setmRID("timeseries_" + deliveryStart + "-" + deliveryEnd);
				times.setBusinessType(StandardBusinessTypeList.A07);
				times.setProduct(StandardEnergyProductTypeList._8716867000016);
				times.setObjectAggregation(StandardObjectAggregationTypeList.A02);
				times.setInDomain_mRID(new DomainID_String(StandardCodingSchemeTypeList.NDE, "ExampleArea"));
				times.setOutDomain_mRID(new DomainID_String(StandardCodingSchemeTypeList.NDE, "ExampleArea"));
				times.setMarketEvaluationPoint(new MeasurementPointID_String(StandardCodingSchemeTypeList.NDE, user.getName()));
				
				MarketParticipant client = new MarketParticipant();
				client.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, user.getName()));
				client.setMarketRole(StandardRoleTypeList.A13);
				times.setIn(client);
				times.setOut(client);
				
				times.setMarketAgreement(new MarketAgreement(StandardContractTypeList.A06, "SampleContract"));
				times.setConnectingLine_RegisteredResource_mRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "Sample"));
			
				times.setMeasure_Unit_name(StandardUnitOfMeasureTypeList.KWT);
				times.setCurveType(StandardCurveTypeList.A01);
				times.setParent(doc);
				
				// Determine global delivery duration:
				
				long s = !clearingInfos.isEmpty() ? (clearingInfos.get(0).getProductPattern().deliveryPeriodDuration / 1000) : 0;
				
				period.setResolution((int) (s / 60));
				period.setTimeInterval(timeInterval);
				times.setPeriod(period);
				
				timeSeries.add(times);
			}
		}	
		return doc;
	}
}
