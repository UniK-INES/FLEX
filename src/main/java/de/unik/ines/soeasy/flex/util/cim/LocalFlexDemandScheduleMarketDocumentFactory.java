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

import java.util.HashSet;
import java.util.Set;

import org.joda.time.Interval;

import de.iwes.enavi.cim.codelists.StandardClassificationTypeList;
import de.iwes.enavi.cim.codelists.StandardMessageTypeList;
import de.iwes.enavi.cim.codelists.StandardProcessTypeList;
import de.iwes.enavi.cim.schedule51.Process;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * Generates {@link Schedule_MarketDocument}s containing flexibility demand to be sent to HEMS.
 * 
 * There is no need to store client-specific Schedule_MarketDocuments as these are not retrieved very often
 * by HEMS, and if so should be updated from VNB flex demand data.
 * 
 * @author Sascha Holzhauer
 *
 */
public class LocalFlexDemandScheduleMarketDocumentFactory extends GenericScheduleMarketDocumentFactory {

	/**
	 * Sets the following SMD fields:
	 * 
	 * <ul>
	 * <li>MR-ID ()</li>
	 * <li>StandardMessageType</li>
	 * <li>Process</li>
	 * <li>Time Interval</li>
	 * <li>Domain MRID</li>
	 * <li></li>
	 * </ul>
	 * 
	 * @return
	 */
	public static Schedule_MarketDocument getScheduleDocument(TimeInitializingBean timebean, TimeSeries ts) {
		
		Schedule_MarketDocument doc = GenericScheduleMarketDocumentFactory.getScheduleDocument(timebean);

		// add document information
		doc.setmRID("LFD_" + ts.getmRID());
				
		doc.setType(StandardMessageTypeList.A32);
		
		Process process = new Process();
		process.setType(StandardProcessTypeList.A15); // alternatives: A02, A15
		process.setClassification(StandardClassificationTypeList.A01);

		Interval timeInterval = ts.getPeriod().getTimeInterval();
		doc.setTimeInterval(timeInterval);
		
		doc.setDomain_mRID("ExampleArea");
		
		// add time series
		Set<TimeSeries> tsset = new HashSet<TimeSeries>();
		tsset.add(ts);
		doc.setTimeSeries(tsset);
		return doc;
	}
}
