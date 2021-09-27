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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;

import de.soeasy.common.model.EnergyRequest;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author Sascha Holzhauer
 *
 */
@Entity
public class MarketEnergyRequest {
	
	static AtomicLong maxID = new AtomicLong(0);
	
	static private Log log = LogFactory.getLog(MarketEnergyRequest.class);


	@Id
	//@GeneratedValue(strategy=GenerationType.AUTO)
	@ApiModelProperty(notes = "Globally unique id")
	private long id = -1l;

	@ManyToOne(targetEntity=UserAccount.class, fetch=FetchType.EAGER)
	@ApiModelProperty(notes = "Account of submitting user")
	private UserAccount userAccount;
	
	@OneToOne(cascade=CascadeType.ALL)
	@ApiModelProperty(notes = "Submitted energy request")
	private EnergyRequest request;
	
	@ApiModelProperty(notes = "Server time when request is stored")
	private long submissionTime;
	
	protected MarketEnergyRequest() {
	}
	
	public MarketEnergyRequest(long id, UserAccount userAccount, EnergyRequest request) {
		this.id = id;
		this.userAccount = userAccount;
		this.request = request;
		this.submissionTime = DateTimeUtils.currentTimeMillis();
		maxID.updateAndGet(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				
				return Math.max(operand, id);
			}
		});
		log.debug("Creating MarketEnergyRequest with ID " + this.id + " for " + request);
	}
	
	/**
	 * TODO handle userAccount
	 * 
	 * @param request
	 */
	public MarketEnergyRequest(EnergyRequest request) {
		this(maxID.incrementAndGet(), null, request);
	}
	
	/**
	 * @param id
	 * @param request
	 */
	public MarketEnergyRequest(long id, EnergyRequest request) {
		this(id, null, request);
	}

	/**
	 * @param userAccount
	 * @param request
	 */
	public MarketEnergyRequest(UserAccount userAccount, EnergyRequest request) {
		
		this(maxID.incrementAndGet(), userAccount, request);
	}

	/**
	 * @return user account
	 */
	public UserAccount getUserAccount() {
		return userAccount;
	}

	/**
	 * @param userAccount
	 */
	public void setUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	/**
	 * @return
	 */
	public Long getId() {
		return id;
	}
	
	/**
	 * @param id
	 */
	public void setID(long id) {
		this.id = id;
		maxID.updateAndGet(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				
				return Math.max(operand, id);
			}
		});
	}

	/**
	 * @return
	 */
	public EnergyRequest getRequest() {
		return request;
	}

	/**
	 * @param request
	 */
	public void setRequest(EnergyRequest request) {
		this.request = request;
	}
	
	/**
	 * @param status
	 */
	public void setStatus(EnergyRequest.Status status) {
		this.request.status = status.getId();
	}
	
	/**
	 * @return
	 */
	public EnergyRequest.Status getStatus() {
		return EnergyRequest.Status.getStatusById(this.request.status);
	}
	
	/**
	 * @return
	 */
	public long getSubmissionTime() {
		return this.submissionTime;
	}
	
	/**
	 * 
	 */
	public void updateSubmissionTime() {
		this.submissionTime = DateTimeUtils.currentTimeMillis();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(" + this.getId() + " / ");
		buffer.append("(" + this.getRequest().cid + "): ");
		buffer.append(new SimpleDateFormat("HH:mm:ss").format(new Date(this.getRequest().startTime)) + " > ");
		buffer.append(new SimpleDateFormat("HH:mm:ss").format(new Date(this.getRequest().endTime)) + ": ");
		buffer.append(this.getRequest().priceRequested);
		buffer.append(" (" + this.getRequest().energyRequested + ")");
		buffer.append(" Status: " + this.getStatus() + ")");
		return  buffer.toString();
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		 if (this == other)
	            return true;
	        if (other == null)
	            return false;
	        if (getClass() != other.getClass())
	            return false;
		return this.id == ((MarketEnergyRequest) other).id;
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) this.id;
	}
}
