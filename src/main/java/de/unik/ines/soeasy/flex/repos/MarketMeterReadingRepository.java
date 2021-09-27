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

import de.unik.ines.soeasy.flex.model.MarketMeterReading;
import de.unik.ines.soeasy.flex.model.UserAccount;

/**
 * @author Sascha Holzhauer
 *
 */
public interface MarketMeterReadingRepository extends PagingAndSortingRepository<MarketMeterReading, Long> {

	/**
	 * @param userAccount
	 * @return
	 */
	@Query("SELECT r FROM MarketMeterReading r where r.userAccount = :userAccount") 
	public MarketMeterReading findByUser(@Param("userAccount") UserAccount userAccount);
	
	/**
	 * @param userAccount
	 * @param meteringEndtime
	 * @return
	 */
	@Query("SELECT r FROM MarketMeterReading r where r.userAccount = :userAccount AND "
			+ "r.reading.meteringEndtime = :meteringEndtime") 
	public MarketMeterReading findByUserAndTime(@Param("userAccount") UserAccount userAccount, 
			@Param("meteringEndtime") long meteringEndtime);
}
