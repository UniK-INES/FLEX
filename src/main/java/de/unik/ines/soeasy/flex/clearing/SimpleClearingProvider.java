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
package de.unik.ines.soeasy.flex.clearing;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.unik.ines.soeasy.flex.clearing.milp.MilpActivationFactorGortSolving;
import de.unik.ines.soeasy.flex.clearing.milp.MilpActivationFactorJoptSolving;
import de.unik.ines.soeasy.flex.clearing.simplematch.SimpleMatching;
import de.unik.ines.soeasy.flex.clearing.uniform.UniformPriceClearing;
import edu.harvard.econcs.jopt.solver.server.cplex.CPLEXInstanceManager;

/**
 * Hold a list of instantiated clearing methods and create new one as requested.
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class SimpleClearingProvider implements ClearingProvider{

	protected Map<String, ClearingMethod> clearingMethods = new HashMap<>();
	
	@Autowired
	SimpleMatching simpleMatching;

	@Autowired
	UniformPriceClearing uniformPriceClearing;

	@Autowired
	MilpActivationFactorGortSolving milpActivationFactorGortSolving;

	@Autowired
	MilpActivationFactorJoptSolving milpActivationFactorJoptSolving;

	@Override
	public ClearingMethod getClearing(String id) {
		switch(id) {
		case ClearingProvider.UNIFORM:
			return uniformPriceClearing;
		case ClearingProvider.MATCHING_CLOSED_SIMPLE:
			return simpleMatching;
		case ClearingProvider.MATCHING_CLOSED_ACTIVATION:
			try {
				Object cplex = CPLEXInstanceManager.INSTANCE.checkOutCplex();
				if (cplex == null) {
					return milpActivationFactorGortSolving;
				} else {
					return milpActivationFactorJoptSolving;
				}
			} catch (UnsatisfiedLinkError | Exception e) {
				return milpActivationFactorGortSolving;
			}
		default:
			return uniformPriceClearing;
		}
	}
}
