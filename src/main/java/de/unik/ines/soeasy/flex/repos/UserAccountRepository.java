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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import de.unik.ines.soeasy.flex.model.UserAccount;
import io.swagger.annotations.Api;

/**
 * @author sascha
 *
 */
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
@Api(value="Access to user accounts")
public interface UserAccountRepository extends PagingAndSortingRepository<UserAccount, Long> {
	
	/**
	 * Method that returns a user account doing a search by the name parameter.
	 *  
	 * @param name
	 * @return user account
	 */
	@Query("SELECT a FROM UserAccount a where a.name = :name") 
	UserAccount findByName(@Param("name") String name);
	
	/**
	 * @return
	 */
	@Query("SELECT a FROM UserAccount a where a.name NOT IN ('admin', 'enavi', 'inspector')") 
	Iterable<UserAccount> findAllDealingClients();
	
	/**
	 * @param location
	 * @return
	 */
	@Query("SELECT a FROM UserAccount a where a.location = :location") 
	Iterable<UserAccount> findAllClientsAtLocation(@Param("location") String location);
}
