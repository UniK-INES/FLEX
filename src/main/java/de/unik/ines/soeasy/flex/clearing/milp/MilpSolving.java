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
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;

/**
 * 
 * Does not consider activation level.
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class MilpSolving implements ClearingMethod {

	/**
	 * Logger
	 */
	private Log log = LogFactory.getLog(MilpSolving.class);
	
	@Autowired
	FlexOrderRepository forderRepos;

	/**
	 * 
	 * @see de.unik.ines.soeasy.flex.clearing.ClearingMethod#clearMarket(List, List,
	 *      ClearingInfo)
	 */
	@Override
	public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers, ClearingInfo cInfo) {

		double infinity = java.lang.Double.POSITIVE_INFINITY;
		Loader.loadNativeLibraries();

		// loop through demand locations:
		for (Schedule_MarketDocument smd : demands) {

			for (TimeSeries ts : smd.getTimeSeries()) {

				// query demand:
				String location = ts.getMarketEvaluationPoint().getmRID();

				MPSolver solver = setupSolver();

				MPObjective objective = solver.objective();
				objective.setMinimization();

				Map<FlexOfferOptionType, MPVariable> xOptions = new HashMap<>();
				Map<FlexOfferOptionType, Map<FlexOptionISPType, MPVariable>> yIsp = new HashMap<>();

				// constraint for the number of options per offer (ie. 1)
				Map<FlexOfferWrapper, MPConstraint> cOptions = new HashMap<>();
				Map<FlexOfferOptionType, MPConstraint> cSync = new HashMap<>();

				Map<Integer, MPConstraint> cDemand = new HashMap<>();

				// Allow identification of FlexOffer by FlexOfferOption:
				Map<FlexOfferOptionType, FlexOffer> optionOfferMap = new HashMap<>();

				// Add demand as constraints:
				for (Point p : ts.getPeriod().getPoints()) {
					if (p.getQuantity().doubleValue() >= 0) {
						cDemand.put(p.getPosition(), solver.makeConstraint(p.getQuantity().doubleValue(), infinity));
					} else {
						cDemand.put(p.getPosition(), solver.makeConstraint(-infinity, p.getQuantity().doubleValue()));
					}
				}

				// loop through offers:
				for (FlexOfferWrapper foffer : offers) {

					if (foffer.getFlexOffer().getCongestionPoint().getEntityAddress().startsWith(location)) {

						cOptions.put(foffer, solver.makeConstraint(0.0, 1.0));

						for (FlexOfferOptionType option : foffer.getFlexOffer().getOfferOptions()) {
							optionOfferMap.put(option, foffer.getFlexOffer());

							// add variables for options
							xOptions.put(option, solver.makeIntVar(0.0, 1.0, "x_" + option.optionReference));
							yIsp.put(option, new HashMap<>());

							// objective
							objective.setCoefficient(xOptions.get(option), option.getPrice().getAmount().doubleValue());

							cSync.put(option, solver.makeConstraint(0.0, 0.0));
							cSync.get(option).setCoefficient(xOptions.get(option), -1.0 * option.getIsps().size());

							// make sure at most one option is chosen (constraint)
							cOptions.get(foffer).setCoefficient(xOptions.get(option), 1.0);

							for (FlexOptionISPType isp : option.getIsps()) {
								// add variables for option time slots
								yIsp.get(option).put(isp,
										solver.makeIntVar(0.0, 1.0, "y_" + foffer.getFlexOffer().getContractID() + "_"
												+ option.getOptionReference() + "_" + isp.getStart()));

								// synchronise timeslot- and option-based variables (constraint - select all or none yISPs of an option)
								cSync.get(option).setCoefficient(yIsp.get(option).get(isp), 1);

								// add demand as constraints:
								for (int i = 0; i < isp.duration; i++) {
									cDemand.get(isp.start + i).setCoefficient(yIsp.get(option).get(isp), isp.power);
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
					for (Map.Entry<FlexOfferOptionType, MPVariable> var : xOptions.entrySet()) {

						log.debug(optionOfferMap.get(var.getKey()).getContractID() + " > "
								+ var.getKey().getOptionReference() + ": " + var.getValue().solutionValue());
						if (var.getValue().solutionValue() == 1.0) {
							FlexOrder fo = SimpleMatching.buildFlexOrderFromFlexOffer(optionOfferMap.get(var.getKey()),
									var.getKey());
							forderRepos.save(fo);
						}
					}

					for (FlexOfferOptionType option : xOptions.keySet()) {
						for (Map.Entry<FlexOptionISPType, MPVariable> var : yIsp.get(option).entrySet()) {
							log.debug(optionOfferMap.get(option).getContractID() + " > " + option.getOptionReference()
									+ " > " + var.getKey().toString() + ": "
									+ var.getValue().solutionValue());
						}
					}
				} else {
					String message = "The problem does not have an optimal solution!";
					log.warn(message);
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
