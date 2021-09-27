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
package de.unik.ines.soeasy.flex.exceptions;

import de.soeasy.common.model.flex.offer.FlexOffer;
import energy.usef.core.exception.BusinessError;
import energy.usef.core.exception.BusinessValidationException;

/**
 * @author Sascha Holzhauer
 *
 */
public class FlexBusinessValidationException extends BusinessValidationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3008040452029692271L;

	protected FlexOffer fo;

	public FlexBusinessValidationException(FlexOffer fo, BusinessError businessError, Object... errorValues) {
		super(businessError, errorValues);
		this.fo = fo;
	}

	public FlexOffer getFlexOffer() {
		return this.fo;
	}
}
