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

import de.iwes.enavi.cim.schedule51.TimeSeries;

public interface TimeSeriesRepository extends PagingAndSortingRepository<TimeSeries, String> {

	/**
	 * Retrieved time series need to be covered by requested day.
	 * 
	 * @param location
	 * @param starttime
	 * @param endtime
	 * @return
	 */
	@Query(value = "SELECT * FROM time_series AS t INNER JOIN schedule_market_document AS s ON s.id = t.smd_id "
			+ "where :location LIKE CONCAT('%',t.market_evaluation_point_mRID, '%') AND "
			+ "s.interval_start_millis >= :starttime AND "
			+ "s.interval_end_millis <= :endtime"
		, nativeQuery = true) 
	public List<TimeSeries> findTsByLocationAndTime(@Param("location") String location, 
			@Param("starttime") long starttime, @Param("endtime") long endtime);
	
	@Query(value = "SELECT * FROM time_series AS t INNER JOIN schedule_market_document AS s ON s.id = t.smd_id "
			+ "where :location LIKE CONCAT('%',t.market_evaluation_point_mRID, '%') AND "
			+ "s.interval_start_millis >= :starttime AND " + "s.interval_end_millis <= :endtime", nativeQuery = true)
	public TimeSeries findLatestTs();

	/**
	 * @param mRID
	 * @return
	 */
	@Query("SELECT t FROM TimeSeries t where :mRID = t.parent.id") 
	public List<TimeSeries> findTsBySmd(@Param("mRID") String mRID);
	
	/**
	 * @param mRID
	 * @param location
	 * @return
	 */
	@Query("SELECT t FROM TimeSeries t where :mRID = t.parent.mRID AND t.marketEvaluationPoint.mRID LIKE CONCAT('%', :location, '%')") 
	public List<TimeSeries> findTsBySmdAndLocation(@Param("mRID") String mRID, @Param("location") String location);
}
