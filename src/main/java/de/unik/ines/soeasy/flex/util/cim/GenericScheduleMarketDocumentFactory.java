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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import de.iwes.enavi.cim.codelists.StandardCodingSchemeTypeList;
import de.iwes.enavi.cim.codelists.StandardRoleTypeList;
import de.iwes.enavi.cim.schedule51.MarketParticipant;
import de.iwes.enavi.cim.schedule51.PartyID_String;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * @author Sascha Holzhauer
 *
 */
public class GenericScheduleMarketDocumentFactory {

	private static Log log = LogFactory.getLog(GenericScheduleMarketDocumentFactory.class);

	/**
	 * Returns a basic SMD instance with properties set which are valid throughout
	 * the FLEX server application:
	 * 
	 * <ul>
	 * <li>Revision Number</li>
	 * <li>Sender ("DEX")</li>
	 * <li>Receiver ("Client")</li>
	 * <li>CreatedDateTime</li>
	 * </ul>

	 * @return generic SMD
	 */
	public static Schedule_MarketDocument getScheduleDocument(TimeInitializingBean timebean) {
		
		Schedule_MarketDocument doc = new Schedule_MarketDocument();

		// add document information
		doc.setRevisionNumber("1");

		MarketParticipant sender = new MarketParticipant();
		sender.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "DEX"));
		sender.setMarketRole(StandardRoleTypeList.A11);
		doc.setSender_MarketParticipant(sender);
		
		MarketParticipant receiver = new MarketParticipant();
		receiver.setmRID(new PartyID_String(StandardCodingSchemeTypeList.NDE, "Client"));
		receiver.setMarketRole(StandardRoleTypeList.A32);
		doc.setReceiver_MarketParticipant(receiver);
		
		doc.setCreatedDateTime(new DateTime(DateTimeUtils.currentTimeMillis(), DateTimeZone.forID(timebean.getTimeZone())));
		
		log.info("Basic SMD instance filled.");
		return doc;
	}
}
