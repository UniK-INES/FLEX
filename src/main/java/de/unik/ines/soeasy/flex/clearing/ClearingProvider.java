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

/**
 * @author Sascha Holzhauer
 *
 */
public interface ClearingProvider {
	
	public static final String UNIFORM = "UNIFORM";
	
	public static final String MATCHING_CLOSED_SIMPLE = "MATCHING_CLOSED_SIMPLE";

	/**
	 * Consider options' activation factor
	 */
	public static final String MATCHING_CLOSED_ACTIVATION = "MATCHING_CLOSED_ACTIVATION";

	/**
	 * Consider grid effects / boundaries (?)
	 */
	public static final String MATCHING_CLOSED_SENSITIVITY = "MATCHING_CLOSED_SENSITIVITY";
	public static final String MATCHING_CONTINUOUS_SENSITIVITY = "MATCHING_CONTINUOUS_SENSITIVITY";
	
	public ClearingMethod getClearing(String id);
}
