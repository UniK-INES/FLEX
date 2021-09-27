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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;

/**
 * 
 * @see https://stackoverflow.com/a/20698339
 * 
 * @author Sascha Holzhauer
 *
 */
public class PgSqlConfiguration extends PostgreSQL95Dialect {

	private Log log = LogFactory.getLog(PgSqlConfiguration.class);
	
	public PgSqlConfiguration() {
		log.info("PgSqlConfiguration instantiated.");
	}
	
	/**
	 * @see org.hibernate.dialect.PostgreSQL82Dialect#getDropSequenceString(java.lang.String)
	 */
	@Override
	public String getDropSequenceString(String sequenceName) {
		// Adding the "if exists" clause to avoid warnings
		return "drop sequence if exists " + sequenceName;
	}

	/**
	 * @see org.hibernate.dialect.PostgreSQL81Dialect#dropConstraints()
	 */
	@Override
	public boolean dropConstraints() {
		// We don't need to drop constraints before dropping tables, that just
		// leads to error messages about missing tables when we don't have a
		// schema in the database
		return false;
	}
}
