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
package de.unik.ines.soeasy.flex.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import de.soeasy.common.model.flex.AcceptedRejectedType;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferResponse;
import energy.usef.core.exception.BusinessValidationException;

/**
 * @author Sascha Holzhauer
 *
 */
public class FlexUtils {

	/**
	 * @param area
	 * @param loc
	 * @return
	 */
	public static boolean checkLoc(String area, String loc) {
		return area.contains(loc);
	}

	public static int getMarketProductPatternId(FlexOffer fo) {
		Pattern pattern = Pattern.compile("MP_ID:(.*?)#", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(fo.getContractID());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		} else {
			throw new IllegalStateException(
					"FlexOffer " + fo + " has no valid ContractID (was " + fo.getContractID()
							+ ", should be of pattern MP_ID:<id>#<more>)");
		}
	}

	public static FlexOfferResponse getFoResponse(FlexOffer fo, AcceptedRejectedType rejectedType) {
		FlexOfferResponse response = fillResponse(fo, rejectedType);
		return response;
	}

	private static FlexOfferResponse fillResponse(FlexOffer fo, AcceptedRejectedType rejectedType) {
		FlexOfferResponse response = new FlexOfferResponse(fo.getMessageID(), rejectedType);
		response.setSenderDomain(fo.getRecipientDomain());
		response.setRecipientDomain(fo.getSenderDomain());
		response.setTimeStamp(new DateTime(DateTimeUtils.currentTimeMillis()));
		response.setConversationID(fo.getConversationID());
		return response;
	}

	public static FlexOfferResponse getFoResponse(FlexOffer fo, BusinessValidationException ex) {
		FlexOfferResponse response = fillResponse(fo, AcceptedRejectedType.Rejected);
		response.setRejectionReason(ex.getMessage());
		return response;
	}
}
