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
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferStatus;
import de.unik.ines.soeasy.flex.exceptions.FlexBusinessValidationException;
import de.unik.ines.soeasy.flex.flex.FlexOfferValidator;
import de.unik.ines.soeasy.flex.util.serialize.FlexOfferStatusConverter;

/**
 * @author Sascha Holzhauer
 *
 */
@Entity
public class FlexOfferWrapper {

	@Id
	private String id;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "fo_id", referencedColumnName = "message_id")
	private FlexOffer flexOffer;

	@Convert(converter = FlexOfferStatusConverter.class)
	private FlexOfferStatus status = FlexOfferStatus.EMPTY;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserAccount user;

	private int productId;

	public FlexOfferWrapper() {
	}

	public FlexOfferWrapper(FlexOffer flexOffer, UserAccount user) {
		this.flexOffer = flexOffer;
		this.status = FlexOfferStatus.CREATED;
		this.id = flexOffer.getMessageID().toString();
		this.user = user;
		try {
			this.productId = FlexOfferValidator.getProductId(flexOffer);
		} catch (FlexBusinessValidationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the flexOffer
	 */
	public FlexOffer getFlexOffer() {
		return flexOffer;
	}

	/**
	 * @param flexOffer the flexOffer to set
	 */
	public void setFlexOffer(FlexOffer flexOffer) {
		this.flexOffer = flexOffer;
		this.id = flexOffer.getMessageID().toString();
	}

	/**
	 * @return the status
	 */
	public FlexOfferStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(FlexOfferStatus status) {
		this.status = status;
	}

	/**
	 * @return the user
	 */
	public UserAccount getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(UserAccount user) {
		this.user = user;
	}

	/**
	 * @return the productId
	 */
	public int getProductId() {
		return productId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(int productId) {
		this.productId = productId;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((flexOffer == null) ? 0 : flexOffer.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlexOfferWrapper other = (FlexOfferWrapper) obj;
		if (flexOffer == null) {
			if (other.flexOffer != null)
				return false;
		} else if (!flexOffer.equals(other.flexOffer))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (status != other.status)
			return false;
		return true;
	}
}
