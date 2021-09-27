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
 * TODO correct (according to MilpActivationFactorGortSolving)
 * 
 * @author Sascha Holzhauer
 * 
 *         See docs/various/FLEX_MILP_ActivationFactor.pdf
 *
 */
@Component
public class MilpActivationFactorJoptSolving implements ClearingMethod {

	/**
	 * Logger
	 */
	private Log log = LogFactory.getLog(MilpActivationFactorJoptSolving.class);

	@Autowired
	FlexOrderRepository forderRepos;

	/**
	 * 
	 * @see de.unik.ines.soeasy.flex.clearing.ClearingMethod#clearMarket(List, List,
	 *      ClearingInfo)
	 */
	@Override
	public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers, ClearingInfo cInfo) {

		// <- LOGGING
		log.info("Start flexibility offer matching with " + this.getClass().getSimpleName());
		// LOGGING ->

		// loop through demand locations:
		for (Schedule_MarketDocument smd : demands) {

			for (TimeSeries ts : smd.getTimeSeries()) {

				String location = ts.getMarketEvaluationPoint().getmRID();
				
				// setup solver
				MIP mip = new MIP();
				mip.setObjectiveMax(false);

				// variables
				Map<FlexOfferOptionType, Variable> xOptions = new HashMap<>();
				Map<FlexOfferOptionType, Variable> aFactors = new HashMap<>();
				
				// constraints
				
				//number of options per offer (ie. 1)
				Map<FlexOfferWrapper, Constraint> cOptionsLeq = new HashMap<>();
				Map<FlexOfferWrapper, Constraint> cOptionsGeq = new HashMap<>();
				
				Map<Integer, Constraint> cDemand = new HashMap<>();
				
				// respect min activation factor
				Map<FlexOfferOptionType, Constraint> cMinActivation = new HashMap<>();

				// constraint to sync activation factor with binary option:
				Map<FlexOfferOptionType, Constraint> cSync = new HashMap<>();


				// Allow identification of FlexOffer by FlexOfferOption:
				Map<FlexOfferOptionType, FlexOffer> optionOfferMap = new HashMap<>();

				// Add demand as constraints:
				for (Point p : ts.getPeriod().getPoints()) {
					if (p.getQuantity().doubleValue() == 0) {
						cDemand.put(p.getPosition(), new Constraint(CompareType.GEQ, Double.NEGATIVE_INFINITY));
					} else if (p.getQuantity().doubleValue() > 0) {
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
											VarType.BOOLEAN, 0, 1));
							mip.add(xOptions.get(option));
							
							// add variables for activation factor
							aFactors.put(option,
									new Variable("a_" + foffer.getFlexOffer().messageID + "_" + option.optionReference,
											VarType.DOUBLE,
											0, 1.0));
							mip.add(aFactors.get(option));

							// add constraints
							cMinActivation.put(option, new Constraint(CompareType.GEQ, 0));
							mip.add(cMinActivation.get(option));
							
							cSync.put(option, new Constraint(CompareType.LEQ, 0));
							mip.add(cSync.get(option));

							// objective
							log.debug("Price: " + option.getPrice().getAmount().doubleValue() + " ("
									+ aFactors.get(option) + ")");
							mip.addObjectiveTerm(option.getPrice().getAmount().doubleValue(), aFactors.get(option));


							// set constraints 
							// make sure at most one option is chosen (constraint)
							cOptionsLeq.get(foffer).addTerm(1, xOptions.get(option));
							cOptionsGeq.get(foffer).addTerm(1, xOptions.get(option));
							
							// ensure min activation factor:
							cMinActivation.get(option).addTerm(1, aFactors.get(option));
							cMinActivation.get(option).addTerm(-1 * option.minActivationFactor.getValue().doubleValue(),
									xOptions.get(option));


							// sync activation factor with binary option:
							cSync.get(option).addTerm(1, aFactors.get(option));
							cSync.get(option).addTerm(-1, xOptions.get(option));

							for (FlexOptionISPType isp : option.getIsps()) {
								// add demand as constraints:
								for (int i = 0; i < isp.duration; i++) {
									cDemand.get(isp.start + i).addTerm(isp.power, aFactors.get(option));
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
						// option can be selected while it's activation is 0...
						if (resultStatus.getValue(var.getValue()) == 1.0
								&& resultStatus.getValue(aFactors.get(var.getKey())) > 0.0) {
							FlexOrder fo = SimpleMatching.buildFlexOrderFromFlexOffer(optionOfferMap.get(var.getKey()),
									var.getKey(), resultStatus.getValue(aFactors.get(var.getKey())));
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
}
