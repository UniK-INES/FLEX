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

import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;

/**
 * @author Sascha Holzhauer
 *
 */
public interface FlexOfferRepository extends PagingAndSortingRepository<FlexOfferWrapper, String> {

	/**
	 * Fetch all offers that overlap with the given interval and match the given
	 * location.
	 * 
	 * @param starttime
	 * @param endtime
	 * @return
	 */
	@Query(value = "SELECT s FROM #{#entityName} s where " + "s.interval_end_millis > :starttime AND "
			+ "s.interval_start_millis < :endtime", nativeQuery = true)
	public List<FlexOfferWrapper> findFosByLocationAndTime(@Param("starttime") long starttime,
			@Param("endtime") long endtime);

	/**
	 * Fetch all unmatched offers that overlap with the given interval.
	 * 
	 * @param startTime
	 * @param endTime
	 * @param productId
	 * @return
	 */
	@Query(value = "SELECT * FROM flex_offer_wrapper AS w INNER JOIN flex_offer AS f ON w.fo_id = f.message_id WHERE w.product_id = :productId "
			+ "AND f.interval_start_millis < :endTime AND f.interval_end_millis > :startTime "
			+ "AND w.status in (-1,0,2) ", nativeQuery = true)
	List<FlexOfferWrapper> findAllUnmatchedByProductAndIntervalTime(@Param("startTime") long startTime,
			@Param("endTime") long endTime, @Param("productId") int productId);

	@Query(value = "SELECT * FROM flex_offer_wrapper AS w INNER JOIN flex_offer AS f ON w.fo_id = f.message_id WHERE "
			+ "f.interval_start_millis >= :startTime AND f.interval_end_millis <= :endTime", nativeQuery = true)
	List<FlexOfferWrapper> findAllByIntervalTime(@Param("startTime") long startTime, @Param("endTime") long endTime);

	@Query(value = "SELECT * FROM flex_offer_wrapper w", nativeQuery = true)
	List<FlexOfferWrapper> findAllWrapper();
}
