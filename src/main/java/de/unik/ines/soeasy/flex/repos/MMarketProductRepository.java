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

import de.unik.ines.soeasy.flex.model.MMarketProductPattern;

/**
 * @author Sascha Holzhauer
 *
 */
public interface MMarketProductRepository extends PagingAndSortingRepository<MMarketProductPattern, Integer> {

	@Query("SELECT p from MMarketProductPattern p where p.mmproductId = (select min(mp.mmproductId) from MMarketProductPattern mp)")
	public MMarketProductPattern getFirstRecord();
}
