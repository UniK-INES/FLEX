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
package de.unik.ines.soeasy.flex.clearing.milp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import de.iwes.enavi.cim.schedule51.Point;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferOptionType;
import de.soeasy.common.model.flex.offer.FlexOptionISPType;
import de.soeasy.common.model.flex.order.FlexOrder;
import de.unik.ines.soeasy.flex.clearing.ClearingMethod;
import de.unik.ines.soeasy.flex.clearing.simplematch.SimpleMatching;
import de.unik.ines.soeasy.flex.exceptions.NoMatchingSolutionException;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class MilpActivationFactorGortSolving implements ClearingMethod {

	/**
	 * Logger
	 */
	private Log log = LogFactory.getLog(MilpActivationFactorGortSolving.class);

	@Autowired
	FlexOrderRepository forderRepos;

	@Autowired
	ClearingInfoRepository clearingPriceReposistory;

	/**
	 * TODO extract to util class
	 * 
	 * @param ts
	 * @param offers
	 */
	public static void printDemandAndOffer(TimeSeries ts, List<FlexOfferWrapper> offers) {
		List<Point> points = ts.getPeriod().getPoints();
		Map<FlexOfferOptionType, String[]> offerValues = new HashMap<>();
		for (FlexOfferWrapper fow : offers) {
			for (FlexOfferOptionType option : fow.getFlexOffer().getOfferOptions()) {
				String[] ovalues = new String[points.size()];
				for (FlexOptionISPType isp : option.getIsps()) {
					for (int j = isp.start; j < isp.start + isp.duration; j++) {
						ovalues[j - 1] = Long.valueOf(isp.power).toString();
					}
				}
				offerValues.put(option, ovalues);
			}
		}
		
		String[] headers = new String[offerValues.size() + 2];
		String formatString = "%-15s%-15s";
		String formatStringV = "%-15s%-15s";
		headers[0] = "Slot";
		headers[1] = "Demand";
		int k = 2;
		for (FlexOfferOptionType option : offerValues.keySet()) {
			formatString += "%-15s";
			formatStringV += "%-15s";
			headers[k] = option.getOptionReference();
			k++;
		}
		formatString += "%n";
		formatStringV += "%n";
		System.out.format(formatString, (Object[]) headers);

		for (int i = 0; i < points.size(); i++) {
			String[] values = new String[offerValues.size() + 2];
			values[0] = "" + (i + 1);
			values[1] = points.get(i).getQuantity().toString();
			int l = 2;
			for (FlexOfferOptionType option : offerValues.keySet()) {
				values[l] = offerValues.get(option)[i];
				l++;
			}
			System.out.format(formatStringV, (Object[]) values);
		}
	}

	/**
	 * 
	 * @see de.unik.ines.soeasy.flex.clearing.ClearingMethod#clearMarket(List, List,
	 *      ClearingInfo)
	 */
	@Override
	public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers, ClearingInfo cInfo) {

		log.info("Start market clearing in MilpActivationFactorGortSolving");

		double infinity = java.lang.Double.POSITIVE_INFINITY;
		Loader.loadNativeLibraries();

		// loop through demand locations:
		for (Schedule_MarketDocument smd : demands) {
			log.debug("Clear Schedule " + smd);
			for (TimeSeries ts : smd.getTimeSeries()) {
				log.debug("Clear TimeSeries " + ts);
				if (log.isTraceEnabled()) {
					printDemandAndOffer(ts, offers);
				}
				String location = ts.getMarketEvaluationPoint().getmRID();

				// setup solver
				MPSolver solver = setupSolver();

				if (log.isDebugEnabled()) {
					solver.enableOutput();
				}
				MPObjective objective = solver.objective();
				objective.setMinimization();

				// variables
				Map<FlexOfferOptionType, MPVariable> xOptions = new HashMap<>();
				Map<FlexOfferOptionType, MPVariable> aFactors = new HashMap<>();

				// constraints
				
				//number of options per offer (ie. 1)
				Map<FlexOfferWrapper, MPConstraint> cOptions = new HashMap<>();
				
				Map<Integer, MPConstraint> cDemand = new HashMap<>();
				
				// respect min activation factor
				Map<FlexOfferOptionType, MPConstraint> cMinActivation = new HashMap<>();
				
				// constraint to sync activation factor with binary option:
				Map<FlexOfferOptionType, MPConstraint> cSync = new HashMap<>();


				// Allow identification of FlexOffer by FlexOfferOption:
				Map<FlexOfferOptionType, FlexOffer> optionOfferMap = new HashMap<>();

				// Add demand as constraints:
				for (Point p : ts.getPeriod().getPoints()) {
					if (p.getQuantity().doubleValue() == 0) {
						cDemand.put(p.getPosition(), solver.makeConstraint(Double.NEGATIVE_INFINITY, infinity));
					} else if (p.getQuantity().doubleValue() > 0) {
						cDemand.put(p.getPosition(), solver.makeConstraint(p.getQuantity().doubleValue(), infinity));
					} else {
						cDemand.put(p.getPosition(), solver.makeConstraint(-infinity, p.getQuantity().doubleValue()));
					}
				}

				// loop through offers:
				for (FlexOfferWrapper foffer : offers) {

					if (log.isDebugEnabled()) {
						log.debug("Considered FlexOffer: " + foffer.getFlexOffer());
					}

					if (foffer.getFlexOffer().getCongestionPoint().getEntityAddress().startsWith(location)) {

						cOptions.put(foffer, solver.makeConstraint(0.0, 1.0));

						for (FlexOfferOptionType option : foffer.getFlexOffer().getOfferOptions()) {
							optionOfferMap.put(option, foffer.getFlexOffer());

							// add variables for options
							xOptions.put(option, solver.makeBoolVar(
									"x_" + foffer.getFlexOffer().messageID + "_" + option.optionReference));
			
							// add variables for activation factor
							aFactors.put(option, solver.makeNumVar(0.0, 1.0,
									"a_" + foffer.getFlexOffer().messageID + "_" + option.optionReference));
							
							// add constraints 
							cMinActivation.put(option, solver.makeConstraint(0.0, infinity));
							
							cSync.put(option, solver.makeConstraint(-infinity, 0.0));
							
							// objective
							objective.setCoefficient(aFactors.get(option), option.getPrice().getAmount().doubleValue());

							// set constraints 
							
							// make sure at most one option is chosen (constraint)
							cOptions.get(foffer).setCoefficient(xOptions.get(option), 1.0);
							
							// ensure min activation factor:
							cMinActivation.get(option).setCoefficient(aFactors.get(option), 1.0);
							cMinActivation.get(option).setCoefficient(xOptions.get(option), 
									-1.0 * option.minActivationFactor.getValue().doubleValue());
							
							cSync.get(option).setCoefficient(aFactors.get(option), 1.0);
							cSync.get(option).setCoefficient(xOptions.get(option), -1.0);
							
							
							for (FlexOptionISPType isp : option.getIsps()) {
								// add demand as constraints:
								for (int i = 0; i < isp.duration; i++) {
									cDemand.get(isp.start + i - 1).setCoefficient(aFactors.get(option), isp.power);
								}
							}
						}
					}
				}

				// solve:
				final MPSolver.ResultStatus resultStatus = solver.solve();

				log.info("Result status: " + resultStatus);

				if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
					// <- LOGGING
					if (log.isDebugEnabled()) {
						log.debug("Objective value = " + objective.value());

					}
					// LOGGING ->

					log.debug("Variable values:" + System.getProperty("line.separator"));

					float energy = 0;

					for (Map.Entry<FlexOfferOptionType, MPVariable> var : aFactors.entrySet()) {

						log.debug(optionOfferMap.get(var.getKey()).getContractID() + " > "
								+ var.getKey().getOptionReference() + ": " + var.getValue().solutionValue());

						// option can be selected while it's activation is 0...
						if (aFactors.get(var.getKey()).solutionValue() > 0.0) {
							FlexOrder fo = SimpleMatching.buildFlexOrderFromFlexOffer(optionOfferMap.get(var.getKey()),
									var.getKey(), var.getValue().solutionValue());
							forderRepos.save(fo);
							for (FlexOptionISPType isp : var.getKey().getIsps()) {
								energy += Math.abs(var.getValue().solutionValue() * isp.getPower());
							}
						}
					}
					cInfo.setEnergyCleared(energy);
					cInfo.setPriceCleared((float) objective.value());
					cInfo.setNumConsideredRequests(offers.size());
					this.clearingPriceReposistory.save(cInfo);

				} else {
					String message = "The problem does not have an optimal solution!";
					log.warn(message);
					this.clearingPriceReposistory.save(cInfo.clearingInfeasible(offers.size()));

					throw new NoMatchingSolutionException(message);
				}
			}
		}
	}

	protected MPSolver setupSolver() {
		// Create the linear solver with the SCIP backend.
		MPSolver solver = MPSolver.createSolver("SCIP");
		if (solver == null) {

			// <- LOGGING
			if (log.isDebugEnabled()) {
				log.debug("Could not create solver SCIP");
			}
			// LOGGING ->
			return null;
		}
		return solver;
	}
}
