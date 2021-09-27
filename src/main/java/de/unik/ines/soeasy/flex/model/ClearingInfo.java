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
package de.unik.ines.soeasy.flex.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import de.soeasy.common.model.MarketProductPattern;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author Sascha Holzhauer
 *
 *         TODO link with {@link MarketProductPattern}
 * 
 *         TODO link with market area (from SMD)
 */
@Entity
public class ClearingInfo {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)    
	private long id = -1l;
	
	private float priceCleared;
	
	private float energyCleared;
	
	private int	numConsideredRequests;

	private long clearingTime;
	
	@ManyToOne(targetEntity=MarketProductPattern.class, fetch=FetchType.EAGER, optional=false)
	@ApiModelProperty(notes = "Market product pattern")
	private  MarketProductPattern productPattern;
	
	private long deliveryPeriodStart;
	
	public ClearingInfo() {
	}

	/**
	 * May only be instantiated once per clearing in the moment of clearing!
	 * 
	 * @param productPattern
	 * @param startTime
	 * @param clearingTime
	 */
	public ClearingInfo(MarketProductPattern productPattern, long startTime, long clearingTime) {
		this.clearingTime = clearingTime;
		this.productPattern = productPattern;
		this.deliveryPeriodStart = startTime;
	}
	
	/**
	 * May only be instantiated once per clearing in the moment of clearing!
	 * 
	 * @param productPattern
	 * @param startTime
	 * @param clearingTime
	 * @param clearingPrice
	 * @param clearingEnergy
	 */
	public ClearingInfo(MarketProductPattern productPattern,
			long startTime, long clearingTime, float clearingPrice, float clearingEnergy) {
		this(productPattern, startTime, clearingTime);
		this.priceCleared = clearingPrice;
		this.energyCleared = clearingEnergy;
	}
	
	/**
	 * @param priceCleared
	 */
	public void setPriceCleared(float priceCleared) {
		this.priceCleared = priceCleared;
	}
	
	/**
	 * @param energyCleared
	 */
	public void setEnergyCleared(float energyCleared) {
		this.energyCleared = energyCleared;
	}
	
	/**
	 * @return clearing price
	 */
	public float getPriceCleared() {
		return this.priceCleared;
	}
	
	/**
	 * @return cleared energy
	 */
	public float getEnergyCleared() {
		return this.energyCleared;
	}
	
	/**
	 * @return time of clearing in lon
	 */
	public long getClearingTime() {
		return this.clearingTime;
	}
	
	/**
	 * @return market product pattern
	 */
	public MarketProductPattern getProductPattern() {
		return this.productPattern;
	}
	
	/**
	 * @return interval start
	 */
	public long getIntervalStart() {
		return this.deliveryPeriodStart;
	}
	
	/**
	 * @return number of considered requests
	 */
	public int getNumConsideredRequests() {
		return numConsideredRequests;
	}

	/**
	 * @param numConsideredRequests
	 */
	public void setNumConsideredRequests(int numConsideredRequests) {
		this.numConsideredRequests = numConsideredRequests;
	}
	
	/**
	 * @return clearing info
	 */
	public ClearingInfo setNothingToClear() {
		this.setEnergyCleared(0);
		this.setPriceCleared(Float.NaN);
		this.setNumConsideredRequests(0);
		return this;
	}

	/**
	 * @return clearing info
	 */
	public ClearingInfo clearingInfeasible(int consideredRequests) {
		this.setEnergyCleared(0);
		this.setPriceCleared(Float.POSITIVE_INFINITY);
		this.setNumConsideredRequests(consideredRequests);
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(productPattern + " @ " + new SimpleDateFormat("dd/MM-HH:mm:ss").format(new Date(this.getClearingTime())));
		buffer.append(": " + this.energyCleared + "@" + this.priceCleared);
		buffer.append(" for " + new SimpleDateFormat("dd/MM-HH:mm:ss").format(new Date(this.deliveryPeriodStart)));
		return buffer.toString();
	}
}
