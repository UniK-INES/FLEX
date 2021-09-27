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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import de.soeasy.common.model.MeterReading;
import de.soeasy.common.model.MeterReading.Status;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author Sascha Holzhauer
 *
 */
@Entity
public class MarketMeterReading {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO) 
	@ApiModelProperty(notes = "Globally unique id")
	private long id = -1l;

	@ManyToOne(targetEntity=UserAccount.class, fetch=FetchType.EAGER)
	@ApiModelProperty(notes = "Account of submitting user")
	private UserAccount userAccount;
	
	@OneToOne(cascade=CascadeType.ALL)
	@ApiModelProperty(notes = "Submitted meter reading")
	private MeterReading reading;
	
	/**
	 * @param userAccount
	 * @param reading
	 */
	public MarketMeterReading(UserAccount userAccount, MeterReading reading) {
		this.userAccount = userAccount;
		this.reading = reading;
	}
	
	/**
	 * 
	 */
	public MarketMeterReading() {
	}
	
	/**
	 * @return meter reading
	 */
	public MeterReading getReading() {
		return this.reading;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(" + this.reading.id + " of ");
		buffer.append("(" + this.userAccount.getName() + "): ");
		buffer.append(new SimpleDateFormat("HH:mm:ss").format(new Date(this.reading.meteringStarttime)) + " > ");
		buffer.append(new SimpleDateFormat("HH:mm:ss").format(new Date(this.reading.meteringEndtime)) + ": ");
		buffer.append("+" + this.reading.energyConsumed);
		buffer.append(" / -" + this.reading.energyProduced + ")");
		buffer.append(" Status: " + Status.getStatusById(this.reading.status));
		return  buffer.toString();
	}
}
