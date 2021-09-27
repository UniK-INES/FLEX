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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;

/**
 * @author Sascha Holzhauer
 *
 */
public class DemandEnergyFunction extends PriceEnergyFunction {
	
	public DemandEnergyFunction() {
		this.currentPrice = Float.POSITIVE_INFINITY;
	}
	
	protected static final int MAX_SIZE_INITIAL_ARRAYS = 50000;
	
	
	/**
	 * Requests in list must be ordered according to price (decreasing order).
	 * In the end, \code{prices} and \code{energy} contain prices in increasing order and according cummulative energy bids (decreasing), respectively.
	 * 
	 * NOTE: Possibly existing entries are overwritten.
	 * 
	 * @param requests
	 */
	public void addRequests(List<MarketEnergyRequest> requests) {
		this.numRequests = requests.size();
		
		if (this.numRequests == 0) {
			throw new IllegalStateException("List passed to AsksPriceList is empty");
		}
		this.prices = new ArrayList<Float>(Math.min(this.numRequests + 1, MAX_SIZE_INITIAL_ARRAYS));
		this.energy = new ArrayList<Float>(Math.min(this.numRequests + 1, MAX_SIZE_INITIAL_ARRAYS));
		
		for (int i = 0; i < this.numRequests ; i++) {
			MarketEnergyRequest request = requests.get(i);
			if (this.currentPrice < request.getRequest().priceRequested) {
				throw new IllegalStateException("Adding requests must be ordered according to requests' price, "
						+ "with highest price first!");
			} else if (this.currentPrice == request.getRequest().priceRequested) {
				// add energy to existing record:
				this.energy.set(this.prices.size()-1, this.energy.get(this.prices.size()-1) + request.getRequest().energyRequested);
			} else {
				// create new record:
				this.energy.add((this.energy.size() > 0 ? this.energy.get(this.prices.size() - 1) : 0) + request.getRequest().energyRequested);
				this.prices.add(request.getRequest().priceRequested);
			}
			this.currentPrice = request.getRequest().priceRequested;
		}
		Collections.reverse(this.prices);
		Collections.reverse(this.energy);
		
		// duplicate last entry to prevent ArrayIndexOutOfBoundsExceptions
		if (!this.prices.isEmpty()) {
			this.prices.add(Float.MAX_VALUE);
			this.energy.add(0.0f);
		}
	}
}
