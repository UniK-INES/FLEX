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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.soeasy.common.model.EnergyRequest;
import de.unik.ines.soeasy.flex.clearing.ClearingMethod;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.util.DemandEnergyFunction;
import de.unik.ines.soeasy.flex.util.SupplyPriceEnergyFunction;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class UniformPriceClearing implements ClearingMethod {

	private Log log = LogFactory.getLog(UniformPriceClearing.class);

	@Autowired
	MarketEnergyRequestRepository requestRepository;

	@Autowired
	ClearingInfoRepository clearingPriceReposistory;

	@Autowired
	@Qualifier("taskScheduler")
	ThreadPoolTaskScheduler taskScheduler;

	/**
	 * Clears the market unless list of requests is empty. In this case, 0 is
	 * assigned to cleared energy and <code>Float.NAN</code> to price cleared.
	 * 
	 * @see de.unik.ines.soeasy.flex.clearing.ClearingMethod#clearMarket(List, List,
	 *      ClearingInfo)
	 */
	@Override
	public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers, ClearingInfo cInfo) {
		log.info("Clear " + cInfo);
		if (log.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Considered requests (" + offers.size() + "):" + System.getProperty("line.separator"));
			offers.forEach(request -> sb.append("\t" + request + System.getProperty("line.separator")));
			log.trace(sb);
		}
		if (offers.size() > 0) {
			doClearing(offers, cInfo);
		} else {
			log.debug(cInfo + "> No requests to clear"); 
			this.clearingPriceReposistory.save(cInfo.setNothingToClear());
		}
	}

	protected void doClearing(List<FlexOfferWrapper> requests, ClearingInfo cInfo) {
		PriceEnergyFunctionInitialiser funInitialiser = null; // new PriceEnergyFunctionInitialiser(requests);
		DemandEnergyFunction demandFunc = funInitialiser.getDemandPriceEnergyFunction();
		SupplyPriceEnergyFunction supplyFunc = funInitialiser.getOfferPriceEnergyFunction();
		ClearingInfo clearingInfo;

		if (funInitialiser.getOffers().size() == 0 || funInitialiser.getBids().size() == 0) {
			log.debug(cInfo + "> Nothing to clear (" + funInitialiser.getOffers().size() + " offers, " + 
					funInitialiser.getBids().size() + " bids)");
			clearingInfo = cInfo.setNothingToClear();
		} else {
			// identify clearing price
			clearingInfo = findIntersection(supplyFunc, demandFunc, cInfo);
			clearingInfo.setNumConsideredRequests(requests.size());
			// float clearingEnergy =

			log.info(cInfo + "> Clearing price is: " + clearingInfo.getPriceCleared());
		}

		this.clearingPriceReposistory.save(clearingInfo);

		// write status
		float energySum = 0.0f;
		// assumes asks are ordered decreasingly by price (and submission date as second
		// criterion).
		for (MarketEnergyRequest request : funInitialiser.getBids()) {
			if (request.getRequest().priceRequested >= clearingInfo.getPriceCleared()) {

				if ((clearingInfo.getEnergyCleared() - energySum) >= request.getRequest().energyRequested) {
					request.getRequest().energyAccepted = request.getRequest().energyRequested;
					request.setStatus(EnergyRequest.Status.ACCEPTED);
				} else if ((clearingInfo.getEnergyCleared() - energySum) > 0.0f) {
					request.getRequest().energyAccepted = clearingInfo.getEnergyCleared() - energySum;
					request.setStatus(EnergyRequest.Status.PARTLY_ACCEPTED);
				} else {
					request.getRequest().energyAccepted = 0.0f;
					request.setStatus(EnergyRequest.Status.DECLINED);
				}
				energySum += request.getRequest().energyRequested;
			} else {
				request.setStatus(EnergyRequest.Status.DECLINED);
				request.getRequest().energyAccepted = 0.0f;
			}
			request.getRequest().priceCleared = clearingInfo.getPriceCleared();
			requestRepository.save(request);
		}

		// assumes offers are ordered increasingly by price (and submission date as
		// second criterion).
		energySum = 0.0f; // becomes negative as offers are negative
		for (MarketEnergyRequest request : funInitialiser.getOffers()) {
			if (request.getRequest().priceRequested <= clearingInfo.getPriceCleared()) {
				if ((-clearingInfo.getEnergyCleared() - energySum) <= request.getRequest().energyRequested) {
					request.getRequest().energyAccepted = request.getRequest().energyRequested;
					request.setStatus(EnergyRequest.Status.ACCEPTED);
				} else if ((-clearingInfo.getEnergyCleared() - energySum) < 0.0f) {
					request.getRequest().energyAccepted = -clearingInfo.getEnergyCleared() - energySum;
					request.setStatus(EnergyRequest.Status.PARTLY_ACCEPTED);
				} else {
					request.getRequest().energyAccepted = 0.0f;
					request.setStatus(EnergyRequest.Status.DECLINED);
				}
				energySum += request.getRequest().energyRequested;
			} else {
				request.setStatus(EnergyRequest.Status.DECLINED);
				request.getRequest().energyAccepted = 0.0f;
			}
			request.getRequest().priceCleared = clearingInfo.getPriceCleared();
			requestRepository.save(request);
		}
	}

	public ClearingInfo findIntersection(SupplyPriceEnergyFunction supplyFunction, DemandEnergyFunction demandFunction,
			ClearingInfo cInfo) {

		int sindex = 0;
		int dindex = 0;

		if (supplyFunction.getPriceByIndex(0) > demandFunction.getPriceByIndex(demandFunction.maxPriceIndex())) {
			log.info(cInfo + "> All offers are above bids - no supplyFunctionintersection!");
			cInfo.setPriceCleared(Float.NaN);
			cInfo.setEnergyCleared(0.0f);
			return cInfo;
		}

		if (supplyFunction.getPriceByIndex(supplyFunction.maxPriceIndex()) <= demandFunction.getPriceByIndex(0)) {
			log.info(cInfo + "> All offers are below bids - assign offer price of max energy that is demanded (and supplied)");
			if (demandFunction.getEnergyByIndex(0) < supplyFunction.getEnergyByIndex(supplyFunction.maxPriceIndex())) {
				// supply exceeds demand:
				while (supplyFunction.getEnergyByIndex(sindex) < demandFunction.getEnergyByIndex(0) &&
						sindex < supplyFunction.maxPriceIndex()) {
					sindex++;
					log.trace("supply excceds demand");
				}
				cInfo.setPriceCleared(supplyFunction.getPriceByIndex(sindex));
			} else {
				// supply is smaller / equals demand:
				dindex = demandFunction.maxPriceIndex();
				while (supplyFunction.getEnergyByIndex(supplyFunction.maxPriceIndex()) > demandFunction.getEnergyByIndex(dindex) &&
						dindex > 0) {
					dindex--;
					log.trace("supply <= demand");
				}
				cInfo.setPriceCleared(demandFunction.getPriceByIndex(dindex));
			}
			// assign overall energy supply:
			cInfo.setEnergyCleared(Math.min(supplyFunction.getEnergyByIndex(supplyFunction.maxPriceIndex()),
					demandFunction.getEnergyByIndex(0)));
			return cInfo;
		}

		// when moving demand's index, demand determines price (demand cuts supply vertically)
		// When moving supply's index, supply determines price (supply cuts demand vertically)

		// Increase indices in turns until price and energy are between consecutive values of the other indices
		// demandAdvancedAttempt and supplyAdvancedAttempt are used as markers to apply weaker checks when the other index
		// has be attempted to increment unsuccessfully.
		
		boolean demandAdvancedAttempt = false;
		boolean supplyAdvancedAttempt = false;
		
		while (true) {
			log.trace("Find intersection: outer while loop...");
			if (checkPriceAndEnergyForDemandIndex(supplyFunction, demandFunction, sindex, dindex)) {
				
				return setAndReturnCInfoDemand(supplyFunction, demandFunction, cInfo, sindex, dindex);
			}
			
			demandAdvancedAttempt = true;
			while (dindex < demandFunction.maxPriceIndex() && 
					(demandFunction.getPriceByIndex(dindex + 1) <= supplyFunction.getPriceByIndex(sindex)
					// if sindex has been attempted to increment unsuccessfully, the current demand price is considered
					|| (supplyAdvancedAttempt && demandFunction.getPriceByIndex(dindex) <= supplyFunction.getPriceByIndex(sindex))
					// as long as demand energy is above supply energy, dindex can be incremented unless demand price exceeds next supply price 
					|| (demandFunction.getEnergyByIndex(dindex) > supplyFunction.getEnergyByIndex(sindex) &&
					demandFunction.getPriceByIndex(dindex) <= supplyFunction.getPriceByIndex(sindex + 1))
					)) {
				
				log.trace("Find intersection: inner while loop (incrementing dindex) [" + sindex + "/" + dindex + "]...");
				dindex++;
				demandAdvancedAttempt = false;
				supplyAdvancedAttempt = false;
				
				if (checkPriceAndEnergyForDemandIndex(supplyFunction, demandFunction, sindex, dindex)) {
					
					return setAndReturnCInfoDemand(supplyFunction, demandFunction, cInfo, sindex, dindex);
				}
			}
			
			if (checkPriceAndEnergyForSupplyIndex(supplyFunction, demandFunction, sindex, dindex)) {
				return setAndReturnCInfoSupply(supplyFunction, demandFunction, cInfo, sindex, dindex);
			}
			
			supplyAdvancedAttempt = true;
			while (sindex < supplyFunction.maxPriceIndex() && 
					(demandFunction.getPriceByIndex(dindex) >= supplyFunction.getPriceByIndex(sindex + 1) ||
					// if dindex has been attempted to increment unsuccessfully, the current supply price is considered
					(demandAdvancedAttempt && demandFunction.getPriceByIndex(dindex) >= supplyFunction.getPriceByIndex(sindex))
					// || (demandFunction.getEnergyByIndex(dindex) > supplyFunction.getEnergyByIndex(sindex) &&
					//			demandFunction.getPriceByIndex(dindex + 1) >= supplyFunction.getPriceByIndex(sindex))
							)) {
				
				log.trace("Find intersection: inner while loop (incrementing sindex) [" + sindex + "/" + dindex + "]...");
				sindex++;
				demandAdvancedAttempt = false;
				supplyAdvancedAttempt = false;
				
				if (checkPriceAndEnergyForSupplyIndex(supplyFunction, demandFunction, sindex, dindex)) {

					return setAndReturnCInfoSupply(supplyFunction, demandFunction, cInfo, sindex, dindex);
				}
			}
		}
	}

	/**
	 * Checks whether current demand price is between current and next demand price and whether current supply energy is
	 * between current and next supply energy.
	 * 
	 * @param supplyFunction
	 * @param demandFunction
	 * @param sindex
	 * @param dindex
	 * @return
	 */
	protected boolean checkPriceAndEnergyForDemandIndex(SupplyPriceEnergyFunction supplyFunction,
			DemandEnergyFunction demandFunction, int sindex, int dindex) {
		return demandFunction.getPriceByIndex(dindex) >= supplyFunction.getPriceByIndex(sindex) &&
			demandFunction.getPriceByIndex(dindex) <= supplyFunction.getPriceByIndex(sindex + 1) && 
			supplyFunction.getEnergyByIndex(sindex) <= demandFunction.getEnergyByIndex(dindex) &&
			supplyFunction.getEnergyByIndex(sindex) > demandFunction.getEnergyByIndex(dindex + 1);
	}

	/**
	 * Checks whether current supply price is between previous and current demand price and whether current demand energy is
	 * between previous and current supply energy.
	 * 
	 * NOTE: For the method's invocation within the inner while loop, testing for sindex==0 is actually not required.
	 * 
	 * @param supplyFunction
	 * @param demandFunction
	 * @param sindex
	 * @param dindex
	 * @return
	 */
	public boolean checkPriceAndEnergyForSupplyIndex(SupplyPriceEnergyFunction supplyFunction,
			DemandEnergyFunction demandFunction, int sindex, int dindex) {
		return (dindex == 0 || (supplyFunction.getPriceByIndex(sindex) >= demandFunction.getPriceByIndex(dindex - 1))) &&
				supplyFunction.getPriceByIndex(sindex) <= demandFunction.getPriceByIndex(dindex) && 
				(sindex == 0 || demandFunction.getEnergyByIndex(dindex) >= supplyFunction.getEnergyByIndex(sindex - 1)) &&
				demandFunction.getEnergyByIndex(dindex) < supplyFunction.getEnergyByIndex(sindex);
	}
	
	protected ClearingInfo setAndReturnCInfoDemand(SupplyPriceEnergyFunction supplyFunction, DemandEnergyFunction demandFunction,
			ClearingInfo cInfo, int sindex, int dindex) {
		log.debug(cInfo + "> Indices of clearing price: Supply> " + sindex + " / Demand> " + dindex);
		cInfo.setPriceCleared(demandFunction.getPriceByIndex(dindex));
		cInfo.setEnergyCleared(supplyFunction.getEnergyByIndex(sindex));
		return cInfo;
	}

	protected ClearingInfo setAndReturnCInfoSupply(SupplyPriceEnergyFunction supplyFunction, DemandEnergyFunction demandFunction,
			ClearingInfo cInfo, int sindex, int dindex) {
		log.debug(cInfo + "> Indices of clearing price: Supply> " + sindex + " / Demand> " + dindex);
		cInfo.setPriceCleared(supplyFunction.getPriceByIndex(sindex));
		cInfo.setEnergyCleared(demandFunction.getEnergyByIndex(dindex));
		return cInfo;
	}
}