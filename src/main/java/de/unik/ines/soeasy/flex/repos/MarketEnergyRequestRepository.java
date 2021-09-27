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

import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.UserAccount;
import io.swagger.annotations.Api;

/**
 * @author sascha
 *
 */
@RepositoryRestResource(collectionResourceRel = "mrequests", path = "mrequests")
@Api(value="Access to market energy requests (bids and offers)")
public interface MarketEnergyRequestRepository extends PagingAndSortingRepository<MarketEnergyRequest, Long> {

	/**
	 * Method that returns a list of bids doing a search by the request's id parameter.
	 *  
	 * @param id
	 * @return list of clients
	 */
	@Query("SELECT r FROM MarketEnergyRequest r where r.id = :id") 
	List<MarketEnergyRequest> findAllById(@Param("id") Long id);
	
	@Query("SELECT r FROM MarketEnergyRequest r") 
	List<MarketEnergyRequest> listAll();

	
	@Query("SELECT r FROM MarketEnergyRequest r where r.submissionTime >= :submissionTime") 
	List<MarketEnergyRequest> findAllSince(@Param("submissionTime") long submissionTime);
	
	@Query("SELECT r FROM MarketEnergyRequest r where r.submissionTime > :since AND r.submissionTime <= :until") 
	List<MarketEnergyRequest> findAllBySubmissionInterval(@Param("since") long sinceTime, @Param("until") long untilTime);
	
	@Query("SELECT r FROM MarketEnergyRequest r where r.request.startTime >= :startTime AND r.request.endTime <= :endTime") 
	List<MarketEnergyRequest> findAllByStarttimeInterval(@Param("startTime") long startTime, @Param("endTime") long endTime);
	
	@Query("SELECT r FROM MarketEnergyRequest r where r.userAccount = :userAccount AND "
			+ "r.request.startTime <= :startTime AND r.request.endTime >= :endTime") 
	List<MarketEnergyRequest> findAllByIntersectionAndUser(@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("userAccount") UserAccount userAccount);

	@Query("SELECT r FROM MarketEnergyRequest r where r.userAccount = :userAccount AND "
			+ "r.request.startTime <= :startTime AND r.request.endTime >= :endTime AND (r.request.status = 1 OR r.request.status =2)") 
	List<MarketEnergyRequest> findAllAtLeastPartlySuccessfulByIntersectionAndUser(@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("userAccount") UserAccount userAccount);
	
	@Query("SELECT r FROM MarketEnergyRequest r where r.userAccount.location = :location AND "
			+ "r.request.startTime <= :startTime AND r.request.endTime >= :endTime AND r.request.status IN (0,2,3) AND "
			+ "r.request.energyRequested < 0 ORDER BY r.request.priceRequested ASC") 
	List<MarketEnergyRequest> findNotAcceptedGenerationByIntersectionAndLocation(@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("location") String location);

	@Query("SELECT r FROM MarketEnergyRequest r WHERE r.userAccount.location = :location AND "
			+ "r.request.startTime <= :startTime AND r.request.endTime >= :endTime AND r.request.status IN (0,2,3) AND "
			+ "r.request.energyRequested > 0 ORDER BY r.request.priceRequested DESC")
	List<MarketEnergyRequest> findNotAcceptedLoadByIntersectionAndLocation(@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("location") String location);

	@Query("SELECT r FROM MarketEnergyRequest r where r.request.startTime >= :startTime AND r.request.endTime <= :endTime AND "
			+ "r.submissionTime > :since AND r.submissionTime <= :until") 
	List<MarketEnergyRequest> findAllByIntervalAndSubmissionTime(@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("since") long sinceTime, @Param("until") long untilTime);

	@Query("SELECT r FROM MarketEnergyRequest r where r.request.productId = :productId AND r.request.startTime >= :startTime AND r.request.endTime <= :endTime AND "
			+ "r.submissionTime > :since AND r.submissionTime <= :until") 
	List<MarketEnergyRequest> findAllByProductAndIntervalAndSubmissionTime(@Param("startTime") long startTime, @Param("endTime") long endTime,
			@Param("since") long sinceTime, @Param("until") long untilTime, @Param("productId") int productId);
	
	/**
	 * Method that returns requests when searched by the request's user's name.
	 * 
	 * @param username
	 * @return client of the id passed as parameter.
	 */   
    @Query("SELECT r FROM MarketEnergyRequest r where r.userAccount.name = :username") 
	MarketEnergyRequest findByUsername(@Param("username") String username);
			
		    
	/**
	 * Method that returns an requests's user ID when searched by the request's id parameter.
	 * 
	 * @param id
	 * @return client of the id passed as parameter.
	 */   
    @Query("SELECT a.userAccount.id FROM MarketEnergyRequest a where a.id = :id") 
	Long finduserIdById(@Param("id") long id);

    /**
     * @param uid user id
     * @param cid user's custom id
     * @return an request which is identified by user id and user's custom id
     */
    @Query("SELECT r FROM MarketEnergyRequest r where r.userAccount.id = :uid AND r.request.cid = :cid")
	MarketEnergyRequest findByCustomId(@Param("uid") long uid, @Param("cid") String cid);
}
