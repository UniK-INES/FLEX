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
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import de.soeasy.common.model.flex.order.FlexOrder;

/**
 * @author Sascha Holzhauer
 *
 */
public interface FlexOrderRepository extends PagingAndSortingRepository<FlexOrder, UUID> {

	@Query(value = "SELECT * FROM flex_order AS o WHERE o.interval_start_millis >= :starttime AND "
			+ "o.interval_end_millis <= :endtime", nativeQuery = true)
	public List<FlexOrder> findFordersByTime(@Param("starttime") long starttime, @Param("endtime") long endtime);

	@Query(value = "SELECT o FROM FlexOrder AS o WHERE o.flexOfferMessageID = :flexOfferId")
	public Optional<FlexOrder> findByFlexOfferId(@Param("flexOfferId") UUID fofferId);
}
