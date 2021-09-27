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

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import io.swagger.annotations.ApiModelProperty;

/**
 * Holds flexibility data for the given time interval for the given location.
 * 
 * @author Sascha Holzhauer
 *
 */
@Entity
public class GridData {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO) 
	@ApiModelProperty(notes = "Globally unique id")
	private long id = -1l;
	
	private String location;

	private long starttime;
	
	private long endtime;

	private int iteration;
	
	private BigDecimal min;
	
	private BigDecimal max;
	
	private BigDecimal availableLoadIncrease;
	
	private BigDecimal availableGenIncrease;
	
	private BigDecimal costLoad;
	
	private BigDecimal costGen;
	
	public GridData() {
	}
	
	/**
	 * @param location
	 * @param starttime
	 * @param endtime
	 * @param iteration
	 * @param min
	 * @param max
	 */
	public GridData(String location, long starttime, long endtime, int iteration, BigDecimal min, BigDecimal max) {
		this.location = location;
		this.starttime = starttime;
		this.endtime = endtime;
		this.iteration = iteration;
		this.min = min;
		this.max = max;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the min
	 */
	public BigDecimal getMin() {
		return min;
	}

	/**
	 * @param min the min to set
	 */
	public void setMin(BigDecimal min) {
		this.min = min;
	}

	/**
	 * @return the max
	 */
	public BigDecimal getMax() {
		return max;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(BigDecimal max) {
		this.max = max;
	}

	/**
	 * @return the availableLoadReduction
	 */
	public BigDecimal getAvailableLoadIncrease() {
		return availableLoadIncrease;
	}

	/**
	 * @param availableLoadReduction the availableLoadReduction to set
	 */
	public void setAvailableLoadIncrease(BigDecimal availableLoadReduction) {
		this.availableLoadIncrease = availableLoadReduction;
	}

	/**
	 * @return the availableGenReduction
	 */
	public BigDecimal getAvailableGenIncrease() {
		return availableGenIncrease;
	}

	/**
	 * @param availableGenReduction the availableGenReduction to set
	 */
	public void setAvailableGenIncrease(BigDecimal availableGenReduction) {
		this.availableGenIncrease = availableGenReduction;
	}

	/**
	 * @return the costLoad
	 */
	public BigDecimal getCostload() {
		return costLoad;
	}

	/**
	 * @param costLoad the costLoad to set
	 */
	public void setCostload(BigDecimal costLoad) {
		this.costLoad = costLoad;
	}

	/**
	 * @return the costGen
	 */
	public BigDecimal getCostgen() {
		return costGen;
	}

	/**
	 * @param costGen the costGen to set
	 */
	public void setCostgen(BigDecimal costGen) {
		this.costGen = costGen;
	}

	/**
	 * @return the starttime
	 */
	public long getStarttime() {
		return starttime;
	}

	/**
	 * @param starttime the starttime to set
	 */
	public void setStarttime(long starttime) {
		this.starttime = starttime;
	}

	/**
	 * @return the endtime
	 */
	public long getEndtime() {
		return endtime;
	}

	/**
	 * @param endtime the endtime to set
	 */
	public void setEndtime(long endtime) {
		this.endtime = endtime;
	}

	/**
	 * @return the iteration
	 */
	public int getIteration() {
		return iteration;
	}

	/**
	 * @param iteration the iteration to set
	 */
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	
	public String toString() {
		return "GridData for " + this.location + " (" +	this.iteration + ") with min = " + this.min + " and max = " + this.max + ".";
	}
}
