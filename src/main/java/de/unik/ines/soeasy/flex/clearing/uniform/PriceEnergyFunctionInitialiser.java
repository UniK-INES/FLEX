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
package de.unik.ines.soeasy.flex.clearing.uniform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.util.DemandEnergyFunction;
import de.unik.ines.soeasy.flex.util.SupplyPriceEnergyFunction;

/**
 * Used by {@link UniformPriceClearing}.
 * 
 * @author Sascha Holzhauer
 *
 */
public class PriceEnergyFunctionInitialiser {
	
	private DemandEnergyFunction demandFunction = new DemandEnergyFunction();
	private SupplyPriceEnergyFunction supplyFunction = new SupplyPriceEnergyFunction();
	
	private TreeSet<MarketEnergyRequest> bids;
	private TreeSet<MarketEnergyRequest> offers;
	
	public PriceEnergyFunctionInitialiser(List<MarketEnergyRequest> requests) {
		this.initPriceEnergyFunctions(requests);
	}
	
	/**
	 * Orders bids in decreasing order (lower submission times first in case of tie, then by ID), 
	 * and orders offers in increasing order (lower submission times first in case of tie, then by ID).
	 * @param requests
	 */
	private void initPriceEnergyFunctions(List<MarketEnergyRequest> requests) {
		// separate bids and offers before ordering
		this.demandFunction = new DemandEnergyFunction();
		bids = new TreeSet<>(new Comparator<MarketEnergyRequest>() {
			@Override
			public int compare(MarketEnergyRequest r1, MarketEnergyRequest r2) {
				// consider ID in case of equality:
				return r2.getRequest().priceRequested == r1.getRequest().priceRequested ?
						r1.getSubmissionTime() == r2.getSubmissionTime() ?
						r1.getId().compareTo(r2.getId()) :
						Long.compare(r1.getSubmissionTime(), r2.getSubmissionTime()) :
						Float.compare(r2.getRequest().priceRequested, r1.getRequest().priceRequested);
			}
		});
		
		this.supplyFunction = new SupplyPriceEnergyFunction();
		offers = new TreeSet<>(new Comparator<MarketEnergyRequest>() {
			@Override
			public int compare(MarketEnergyRequest r1, MarketEnergyRequest r2) {
				return r1.getRequest().priceRequested == r2.getRequest().priceRequested ?
						(r1.getSubmissionTime() == r2.getSubmissionTime() ?
								r1.getId().compareTo(r2.getId()) :
						Long.compare(r1.getSubmissionTime(), r2.getSubmissionTime())) :
							Float.compare(r1.getRequest().priceRequested, r2.getRequest().priceRequested);
			}
		});
		
		for (MarketEnergyRequest request : requests) {
			if (request.getRequest().energyRequested > 0.0) {
				bids.add(request);
			} else {
				request.getRequest().energyRequested = request.getRequest().energyRequested;
				offers.add(request);
			}
		}
		
		// construct functions
		if (!bids.isEmpty()) {
			demandFunction.addRequests(new ArrayList<MarketEnergyRequest>(bids));
		}
		if (!offers.isEmpty()) {
			supplyFunction.addRequests(new ArrayList<MarketEnergyRequest>(offers));
		}
	}

	public DemandEnergyFunction getDemandPriceEnergyFunction() {
		return this.demandFunction;
	}
	
	public SupplyPriceEnergyFunction getOfferPriceEnergyFunction() {
		return this.supplyFunction;
	}
	
	public TreeSet<MarketEnergyRequest> getBids() {
		return this.bids;
	}
	
	public TreeSet<MarketEnergyRequest> getOffers() {
		return this.offers;
	}
}
