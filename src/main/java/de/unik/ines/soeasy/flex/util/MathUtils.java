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

import java.util.Arrays;

/**
 * @author Sascha Holzhauer
 *
 */
public class MathUtils {
	
	private static long gcd(long x, long y) {
	    return (y == 0) ? x : gcd(y, x % y);
	}
	
	/**
	 * @param numbers
	 * @return greatest common divisor if the given set of numbers
	 * 
	 * @see https://stackoverflow.com/a/40531215/3957413
	 */
	public static long gcd(long... numbers) {
		 return Arrays.stream(numbers).reduce(0, (x, y) -> gcd(x, y));
	}
}
