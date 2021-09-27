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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;

/**
 * @author Sascha Holzhauer
 *
 */
public abstract class PriceEnergyFunction {
	
	protected Log log = LogFactory.getLog(PriceEnergyFunction.class);
	
	protected int numRequests = 0;
	
	protected ArrayList<Float> prices;
	protected ArrayList<Float> energy;
	
	protected float currentPrice;
	
	public PriceEnergyFunction() {
	}
	
	/**
	 * Adds requests to this function. Order of requests depends on type (offer/Ask).
	 * 
	 * @param requests
	 */
	public abstract void addRequests(List<MarketEnergyRequest> requests);
	
	public Iterator<Float> getEnergyIterator() {
		return this.energy.iterator();
	}
	
	public float getPriceByIndex(int index) {
		return this.prices.get(index);
	}
	
	public float getEnergyByIndex(int index) {
		return this.energy.get(index);
	}
	
	public int maxPriceIndex() {
		return this.prices.size() - 2;
	}
	
	public float[] getPriceSeries() {
		return ArrayUtils.toPrimitive(this.prices.toArray(new Float[0]), Float.NaN);
	}
	
	public float[] getEnergySeries() {
		return ArrayUtils.toPrimitive(this.energy.toArray(new Float[0]), Float.NaN);
	}
}
