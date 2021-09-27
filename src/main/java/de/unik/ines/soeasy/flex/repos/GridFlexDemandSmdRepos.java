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
package de.unik.ines.soeasy.flex.repos;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;

/**
 * @author Sascha Holzhauer
 *
 */
public interface GridFlexDemandSmdRepos extends PagingAndSortingRepository<Schedule_MarketDocument, String> {

	@Query(value = "SELECT * FROM schedule_market_document s where "
			+ "s.interval_start_millis >= :starttime AND "
			+ "s.interval_end_millis <= :endtime", nativeQuery = true)
	public List<Schedule_MarketDocument> findSmdsByTime(
			@Param("starttime") long starttime, @Param("endtime") long endtime);
}
