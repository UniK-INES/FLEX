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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import de.iwes.enavi.cim.CimFormats;
import de.iwes.enavi.cim.codelists.StandardAuctionTypeList;
import de.iwes.enavi.cim.codelists.StandardBusinessTypeList;
import de.iwes.enavi.cim.codelists.StandardCodingSchemeTypeList;
import de.iwes.enavi.cim.codelists.StandardCurrencyTypeList;
import de.iwes.enavi.cim.codelists.StandardCurveTypeList;
import de.iwes.enavi.cim.codelists.StandardMessageTypeList;
import de.iwes.enavi.cim.codelists.StandardRoleTypeList;
import de.iwes.enavi.cim.codelists.StandardUnitOfMeasureTypeList;
import de.iwes.enavi.cim.publication73.DateTimeInterval;
import de.iwes.enavi.cim.publication73.MarketParticipant;
import de.iwes.enavi.cim.publication73.PartyID_String;
import de.iwes.enavi.cim.publication73.Point;
import de.iwes.enavi.cim.publication73.Publication_MarketDocument;
import de.iwes.enavi.cim.publication73.Series_Period;
import de.iwes.enavi.cim.publication73.TimeSeries;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * @author Sascha Holzhauer
 *
 */
public class PublicationMarketDocumentFactory {
	// TODO pass and add specific client information
	
	public static Publication_MarketDocument getPublicationDocument(List<ClearingInfo> clearingInfos, 
			int productId, long deliveryStart, long deliveryEnd, TimeInitializingBean timebean) {
		Publication_MarketDocument doc = new Publication_MarketDocument();
		
		String cinfo = !clearingInfos.isEmpty() ? clearingInfos.get(0).toString() : "ClearingInfoNA";
		
		// add document information
		doc.setmRID("marketprices_" + cinfo);
		doc.setRevisionNumber("1");
		doc.settype(StandardMessageTypeList.A44);
		
		PartyID_String partyID = new PartyID_String();
		partyID.setmRID("DEX");
		partyID.setcodingScheme(StandardCodingSchemeTypeList.NDE);
		
		MarketParticipant sender = new MarketParticipant();
		sender.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "DEX"));
		sender.setMarketRole(StandardRoleTypeList.A11);
		doc.setsender_MarketParticipant(sender);
		
		MarketParticipant receiver = new MarketParticipant();
		receiver.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "Client"));
		receiver.setMarketRole(StandardRoleTypeList.A32);
		doc.setreceiver_MarketParticipant(receiver);
		
		doc.setCreatedDateTime(CimFormats.DATA_TIME_FORMATTER.format(ZonedDateTime.now(ZoneId.of(timebean.getTimeZone()))));
		DateTimeInterval timeInterval = new DateTimeInterval();
		timeInterval.setStart(CimFormats.DATA_TIME_FORMATTER.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(deliveryStart), 
				ZoneId.of(timebean.getTimeZone()))));
		timeInterval.setStart(CimFormats.DATA_TIME_FORMATTER.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(deliveryEnd), 
				ZoneId.of(timebean.getTimeZone()))));
		doc.settimeInterval(timeInterval);
		
		// add time series
		TimeSeries times = new TimeSeries();
		times.setmRID("timeseries" + cinfo);
		times.setbusinessType(StandardBusinessTypeList.A62);
		times.setCurrency_Unit_name(StandardCurrencyTypeList.EUR);
		times.setPrice_Measure_Unit_name(StandardUnitOfMeasureTypeList.KWH);
		times.setcurveType(StandardCurveTypeList.A01);
		times.settype(StandardAuctionTypeList.A02);
		//times.setAuction_category(StandardCategoryTypeList.A04);
		times.setAuction_category(productId);
		
		Series_Period period = new Series_Period();
		long s = !clearingInfos.isEmpty() ? (clearingInfos.get(0).getProductPattern().deliveryPeriodDuration / 1000) : 0;
		period.setresolution(String.format("PT%02dM", s / 60));
		
		int counter = 0;
		for (ClearingInfo cInfo : clearingInfos) {
			period.addPoint(new Point(counter++, 
					Float.isNaN(cInfo.getEnergyCleared()) ? null : new BigDecimal(cInfo.getEnergyCleared()), 
					Float.isNaN(cInfo.getPriceCleared()) ? null : new BigDecimal(cInfo.getPriceCleared())));
		}
		period.settimeInterval(timeInterval);
		
		times.setPeriod(period);
		doc.setTimeSeries(times);
		
		return doc;
	}
}
