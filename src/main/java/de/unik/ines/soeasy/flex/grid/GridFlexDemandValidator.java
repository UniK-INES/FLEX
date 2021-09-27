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
package de.unik.ines.soeasy.flex.grid;

import org.springframework.stereotype.Component;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.unik.ines.soeasy.flex.exceptions.FlexDemandValidationException;
import de.unik.ines.soeasy.flex.model.UserAccount;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class GridFlexDemandValidator {

	/**
	 * @param flexDemandSchedule
	 * @param user
	 * @return
	 */
	public boolean validateFlexDemandCim(Schedule_MarketDocument flexDemandSchedule, UserAccount user) {
		if (!user.getName().equals(flexDemandSchedule.getSender_MarketParticipant().getmRID().getmRID())) {
			throw new FlexDemandValidationException(
					"Submitting user (" + user.getName() + ") does not correspond to sender market participant ("
							+ flexDemandSchedule.getSender_MarketParticipant().getmRID().getmRID() + ")!");
		}
		return true;
	}
}
