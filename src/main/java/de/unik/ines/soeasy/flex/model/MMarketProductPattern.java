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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import de.soeasy.common.model.MarketProductPattern;
import de.unik.ines.soeasy.flex.clearing.ClearingProvider;
import io.swagger.annotations.ApiModelProperty;

/**
 * Complements {@link MarketProductPattern} with {@link ClearingProvider} and clearing (scheduling) functionality
 * @author Sascha Holzhauer
 *
 */
@Entity
public class MMarketProductPattern {
	

	@Transient
	ClearingProvider clearingProvider;
	
	/**
	 * A unique product id.
	 */
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)  
	protected int mmproductId;
	
	@OneToOne(cascade=CascadeType.MERGE)
	@ApiModelProperty(notes = "Basic product pattern")
	private MarketProductPattern productPattern;

	private String clearingId = ClearingProvider.UNIFORM;
	
	private boolean active = true;
	
	private String description;
	
	public MMarketProductPattern(MarketProductPattern productPattern) {
		this.productPattern = productPattern;
	}
	
	public MMarketProductPattern() {	
	}
	
	public MarketProductPattern getProductPattern() {
		return productPattern;
	}
	
	public void stopClearing() {
		this.active = false;
	}
	
	public void setActive() {
		this.active = true;
	}

	public boolean isActive() {
		return this.active;
	}
	
	public String getClearingId() {
		return this.clearingId;
	}
	
	public void setClearingId(String clearingId) {
		this.clearingId = clearingId;
	}
	
	public int getMmproductId() {
		return this.mmproductId;
	}
	
	public void setMmproductId(int id) {
		this.mmproductId = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String toString() {
		return this.productPattern + ": " + this.clearingId + " (" + (this.isActive() ? "A" : "P") + ")";
	}
}
