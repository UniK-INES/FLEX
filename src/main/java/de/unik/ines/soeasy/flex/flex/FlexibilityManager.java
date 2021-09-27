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
package de.unik.ines.soeasy.flex.flex;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.soeasy.common.model.EnergyRequest;
import de.unik.ines.soeasy.flex.model.GridData;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.repos.GridDataRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.TimeSeriesRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;

/**
 * TODO Implement matching TODO store grid data
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class FlexibilityManager {
	
	private Log log = LogFactory.getLog(FlexibilityManager.class);

	@Value("${de.unik.ines.soeasy.flex.grid.potentials.calculate:false}")
	public boolean calculatePotentials;
	
	@Autowired
	protected TimeSeriesRepository timeSeriesRepos;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	MarketEnergyRequestRepository requestRepos;
	
	@Autowired
	GridDataRepository gridDataRepos;



	/**
	 * Check for required action an perform it (calling
	 * {@link FlexibilityManager#increaseLoad(GridData)} and
	 * {@link FlexibilityManager#increaseGeneration(GridData)}.
	 * 
	 * @param gridData
	 */
	private void processGridSim(GridData gridData) {
		if (log.isDebugEnabled()) {
			log.debug("Process " + gridData + "...");
		}
		
		if (gridData.getMax().doubleValue() < 0 || calculatePotentials) {
			// load needs to be reduced	
			increaseGeneration(gridData);
		}
		if (gridData.getMin().doubleValue() > 0 || calculatePotentials) {
			// generation needs to be reduced	
			increaseLoad(gridData);
		}
		gridDataRepos.save(gridData);
	}

	/**
	 * TODO test increaseLoad
	 * TODO introduce security factor (to reduce more than necessary)
	 * 
	 * @param gridData
	 * @return
	 */
	private GridData increaseLoad(GridData gridData) {
		// find load to reduce:
		List<MarketEnergyRequest> requests = requestRepos.findNotAcceptedLoadByIntersectionAndLocation(
				gridData.getStarttime(), gridData.getEndtime(), gridData.getLocation());
		
		
		log.info("Found " + requests.size() + " to potentially increase load (target: " + gridData.getMin().floatValue() + ").");
		
		float increasableLoad = 0.0f;
		float cost = 0.0f;
		for (MarketEnergyRequest request : requests) {
			// TODO test case!
			float energyToUse = request.getRequest().energyRequested - request.getRequest().energyAccepted;
			if (increasableLoad < gridData.getMin().floatValue()) {
				// accept request:
				if (increasableLoad + energyToUse <= gridData.getMin().floatValue()) {
					log.debug("Increase accepted energy of " + request + " from " + request.getRequest().energyAccepted + " to " +
							(request.getRequest().energyAccepted + energyToUse) + " Status: ACCEPTED.");
					request.getRequest().energyAccepted = request.getRequest().energyAccepted + energyToUse;
					// TODO this price is actually only for additional energy (when PARTLY_ACCEPTED before)
					request.getRequest().priceCleared = request.getRequest().priceRequested;
					cost += request.getRequest().priceCleared * energyToUse;
					request.setStatus(EnergyRequest.Status.ACCEPTED);
				} else {
					log.debug("Increase accepted energy of " + request + " from " + request.getRequest().energyAccepted + " to " +
							(request.getRequest().energyAccepted + 
									gridData.getMin().floatValue() - increasableLoad) + " Status: PARTLY_ACCEPTED.");
					request.getRequest().energyAccepted = request.getRequest().energyAccepted + 
							gridData.getMin().floatValue() - increasableLoad;
					// TODO this price is actually only for additional energy (when PARTLY_ACCEPTED before)
					request.getRequest().priceCleared = request.getRequest().priceRequested;
					cost += request.getRequest().priceCleared * (gridData.getMin().floatValue() - increasableLoad);
					request.setStatus(EnergyRequest.Status.PARTLY_ACCEPTED);
				}
				requestRepos.save(request);
			}
			increasableLoad += energyToUse;
		}
		
		log.debug("Increasable load: " + increasableLoad);
		
		gridData.setAvailableLoadIncrease(new BigDecimal(increasableLoad));
		gridData.setCostload(new BigDecimal(cost));
		return gridData;
	}
	
	/**
	 * TODO test increaseGeneration
	 * TODO introduce security factor (to reduce more than necessary)
	 * 
	 * @param gridData
	 * @return
	 */
	private GridData increaseGeneration(GridData gridData) {
		// find generation to reduce:
		List<MarketEnergyRequest> requests = requestRepos.findNotAcceptedGenerationByIntersectionAndLocation(
				gridData.getStarttime(), gridData.getEndtime(), gridData.getLocation());
		
		log.info("Found " + requests.size() + " to potentially increase generation (target: " + gridData.getMax().floatValue() + ").");
		
		float increasableGeneration = 0.0f;
		float cost = 0.0f;
		for (MarketEnergyRequest request : requests) {
			// energyToGenerate is negative
			float energyToGenerate = request.getRequest().energyRequested - request.getRequest().energyAccepted;
			if (increasableGeneration > gridData.getMax().floatValue()) {
				// accept request:
				if (increasableGeneration + energyToGenerate > gridData.getMax().floatValue()) {
					if (log.isDebugEnabled()) {
						log.debug("Increase accepted generation of " + request + " from " + request.getRequest().energyAccepted + " to " +
								(request.getRequest().energyAccepted + energyToGenerate) + " Status: ACCEPTED.");
					}
					request.getRequest().energyAccepted = request.getRequest().energyAccepted + energyToGenerate;
					// TODO this price is actually only for additional energy (when PARTLY_ACCEPTED before)
					request.getRequest().priceCleared = request.getRequest().priceRequested;
					cost -= request.getRequest().priceCleared * energyToGenerate;
					request.setStatus(EnergyRequest.Status.ACCEPTED);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Increase accepted generation of " + request + " from " + request.getRequest().energyAccepted + " to " +
								(request.getRequest().energyAccepted + 
										gridData.getMax().floatValue() - increasableGeneration) + " Status: PARTLY_ACCEPTED.");
					}
					request.getRequest().energyAccepted = request.getRequest().energyAccepted + 
							gridData.getMax().floatValue() - increasableGeneration;
					// TODO this price is actually only for additional energy (when PARTLY_ACCEPTED before)
					request.getRequest().priceCleared = request.getRequest().priceRequested;
					cost -= request.getRequest().priceCleared * (gridData.getMax().floatValue() - increasableGeneration);
					request.setStatus(EnergyRequest.Status.PARTLY_ACCEPTED);
				}
				requestRepos.save(request);
			}
			increasableGeneration += energyToGenerate;
		}
		
		log.debug("Increasable generation: " + increasableGeneration);
		
		gridData.setAvailableGenIncrease(new BigDecimal(increasableGeneration));
		gridData.setCostgen(new BigDecimal(cost));
		return gridData;
	}
}
