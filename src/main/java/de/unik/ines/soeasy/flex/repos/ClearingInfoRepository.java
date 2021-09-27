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
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import de.unik.ines.soeasy.flex.model.ClearingInfo;

/**
 * @author Sascha Holzhauer
 *
 */
@RepositoryRestResource(collectionResourceRel = "cinfos", path = "cinfos")
public interface ClearingInfoRepository extends PagingAndSortingRepository<ClearingInfo, Long> {

    /**
	 * Retrieves clearing price records with a clearing time after or equal to
	 * \code{clearingTimeFrom} and earlier (not equal!) than \code{clearingTimeTo}.
	 * 
	 * @param clearingTimeAfter
	 * @param clearingTimeBefore
	 * @return
	 */
    @Query("SELECT p FROM ClearingInfo p where p.clearingTime >= :clearingTimeAfter and p.clearingTime < :clearingTimeBefore") 
    List<ClearingInfo> findClearingInfoAfterBefore(@Param("clearingTimeAfter") long clearingTimeAfter, 
    		@Param("clearingTimeBefore") long clearingTimeBefore);
    
    /**
     * Retrieves clearing price records with a clearing time after or equal to \code{clearingTimeAfter} and earlier (not equal!)
     * than \code{clearingTimeBefore} for the given product ID.
     * 
     * @param productId
     * @param clearingTimeAfter
     * @param clearingTimeBefore
     * @return
     */
    @Query("SELECT p FROM ClearingInfo p where p.productPattern.productId = :product and p.clearingTime >= :clearingTimeAfter and p.clearingTime < :clearingTimeBefore") 
    List<ClearingInfo> findClearingInfoByProductByClearingTime(@Param("product") int productId, 
    		@Param("clearingTimeAfter") long clearingTimeAfter, 
    		@Param("clearingTimeBefore") long clearingTimeBefore);

    /**
     * Retrieves clearing price records with a delivery start time after or equal to \code{deliveryStartAfter} and earlier (not equal!)
     * than \code{deliveryStartBefore} for the given product ID.
     * 
     * @param deliveryStartAfter
     * @param deliveryStartBefore
     * @return
     */
    @Query("SELECT p FROM ClearingInfo p where "
    		+ "p.deliveryPeriodStart >= :deliveryStartAfter and p.deliveryPeriodStart < :deliveryStartBefore") 
    List<ClearingInfo> findClearingInfoByDeliveryStart(
    		@Param("deliveryStartAfter") long deliveryStartAfter,
    		@Param("deliveryStartBefore") long deliveryStartBefore);
    
    /**
     * Retrieves clearing price records with a delivery start time after or equal to \code{deliveryStartAfter} and earlier (not equal!)
     * than \code{deliveryStartBefore} for the given product ID.
     * 
     * @param productId
     * @param deliveryStartAfter
     * @param deliveryStartBefore
     * @return list of clearing infos
     */
    @Query("SELECT p FROM ClearingInfo p where p.productPattern.productId = :product and "
    		+ "p.deliveryPeriodStart >= :deliveryStartAfter and p.deliveryPeriodStart < :deliveryStartBefore") 
    List<ClearingInfo> findClearingInfoByProductByDeliveryStart(@Param("product") int productId,
    		@Param("deliveryStartAfter") long deliveryStartAfter,
    		@Param("deliveryStartBefore") long deliveryStartBefore);
    
    @Query("SELECT p FROM ClearingInfo p where p.clearingTime >= :clearingTime") 
    List<ClearingInfo> findClearingInfoFromTime(@Param("clearingTime") long clearingTime);
    
    /**
     * @return clearing info
     */
    @Query("SELECT p FROM ClearingInfo p where p.clearingTime = MAX(clearingTime)") 
    ClearingInfo findLatestClearingInfo();
    
    /**
     * @param id
     * @return clearing info
     */
    ClearingInfo findById(long id);
    
    /**
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    List<ClearingInfo> findAll();
}
