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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;

/**
 * @author Sascha Holzhauer
 *
 */
@NoAutoStart
public class StartupTimeBasedTriggeringPolicy<E> extends SizeAndTimeBasedFNATP<E> {

	protected Log log = LogFactory.getLog(StartupTimeBasedTriggeringPolicy.class);

	boolean doRolling = true;

	@Override
	public boolean isTriggeringEvent(File activeFile, final E event) {
		// roll the first time when the event gets called
		if (doRolling) {
			doRolling = false;
			this.elapsedPeriodsFileName = getCurrentPeriodsFileNameWithoutCompressionSuffix();
			return true;
		}
		return super.isTriggeringEvent(activeFile, event);
	}
}
