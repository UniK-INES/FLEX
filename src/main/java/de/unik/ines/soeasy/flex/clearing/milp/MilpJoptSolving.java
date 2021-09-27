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

import com.google.ortools.linearsolver.MPSolver;

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
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPInfeasibleException;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class MilpJoptSolving implements ClearingMethod {

	/**
	 * Logger
	 */
	private Log log = LogFactory.getLog(MilpJoptSolving.class);

	@Autowired
	FlexOrderRepository forderRepos;

	/**
	 * 
	 * @see de.unik.ines.soeasy.flex.clearing.ClearingMethod#clearMarket(List, List,
	 *      ClearingInfo)
	 */
	@Override
	public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers, ClearingInfo cInfo) {

		// loop through demand locations:
		for (Schedule_MarketDocument smd : demands) {

			for (TimeSeries ts : smd.getTimeSeries()) {

				String location = ts.getMarketEvaluationPoint().getmRID();
				
				MIP mip = new MIP();
				mip.setObjectiveMax(false);
				
				Map<FlexOfferOptionType, Variable> xOptions = new HashMap<>();
				Map<FlexOfferOptionType, Variable> aFactors = new HashMap<>();
			
				// constraint for the number of options per offer (ie. 1)
				Map<FlexOfferWrapper, Constraint> cOptionsLeq = new HashMap<>();
				Map<FlexOfferWrapper, Constraint> cOptionsGeq = new HashMap<>();
				Map<Integer, Constraint> cDemand = new HashMap<>();

				// Allow identification of FlexOffer by FlexOfferOption:
				Map<FlexOfferOptionType, FlexOffer> optionOfferMap = new HashMap<>();

				// Add demand as constraints:
				for (Point p : ts.getPeriod().getPoints()) {
					if (p.getQuantity().doubleValue() >= 0) {
						cDemand.put(p.getPosition(), new Constraint(CompareType.GEQ, p.getQuantity().doubleValue()));
					} else {
						cDemand.put(p.getPosition(), new Constraint(CompareType.LEQ, p.getQuantity().doubleValue()));
					}
					mip.add(cDemand.get(p.getPosition()));
				}

				// loop through offers:
				for (FlexOfferWrapper foffer : offers) {

					if (foffer.getFlexOffer().getCongestionPoint().getEntityAddress().startsWith(location)) {

						cOptionsLeq.put(foffer, new Constraint(CompareType.GEQ, 0));
						mip.add(cOptionsLeq.get(foffer));
						cOptionsGeq.put(foffer, new Constraint(CompareType.LEQ, 1));
						mip.add(cOptionsGeq.get(foffer));

						for (FlexOfferOptionType option : foffer.getFlexOffer().getOfferOptions()) {
							optionOfferMap.put(option, foffer.getFlexOffer());

							// add variables for options
							xOptions.put(option,
									new Variable("x_" + foffer.getFlexOffer().messageID + "_" + option.optionReference,
											VarType.INT, 0, 1));
							mip.add(xOptions.get(option));
							aFactors.put(option,
									new Variable("af_" + foffer.getFlexOffer().messageID + "_" + option.optionReference,
											VarType.DOUBLE,
									option.minActivationFactor.getValue().doubleValue(), 1));
							// mip.add(aFactors.get(option));

							// objective
							mip.addObjectiveTerm(option.getPrice().getAmount().doubleValue(), xOptions.get(option)); // ,
							// aFactors.get(option));

							// make sure at most one option is chosen (constraint)
							cOptionsLeq.get(foffer).addTerm(1, xOptions.get(option));
							cOptionsGeq.get(foffer).addTerm(1, xOptions.get(option));

							for (FlexOptionISPType isp : option.getIsps()) {
								// add demand as constraints:
								for (int i = 0; i < isp.duration; i++) {
									cDemand.get(isp.start + i).addTerm(isp.power, xOptions.get(option)); // ,
									// aFactors.get(option));
								}
							}
						}

					}
				}

				// solve:
				SolverClient solverClient = new SolverClient();
				try {
					IMIPResult resultStatus = solverClient.solve(mip);

					log.info("Result status: " + resultStatus);

					// <- LOGGING
					if (log.isDebugEnabled()) {
						log.debug("Objective value = " + resultStatus.getObjectiveValue());
					}
					// LOGGING ->

					log.debug("Variable values:" + System.getProperty("line.separator"));
					for (Map.Entry<FlexOfferOptionType, Variable> var : xOptions.entrySet()) {

						log.debug(optionOfferMap.get(var.getKey()).getContractID() + " > "
								+ var.getKey().getOptionReference() + ": " + resultStatus.getValue(var.getValue()));
						if (resultStatus.getValue(var.getValue()) == 1.0) {
							FlexOrder fo = SimpleMatching.buildFlexOrderFromFlexOffer(optionOfferMap.get(var.getKey()),
									var.getKey());// ,

							// resultStatus.getValue(aFactors.get(var.getKey())));
							forderRepos.save(fo);
						}
					}
				} catch (MIPInfeasibleException ex) {
					String message = "The problem does not have an optimal solution (" + ex.getMessage() + ")!";
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
