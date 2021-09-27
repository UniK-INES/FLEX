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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * @author Sascha Holzhauer
 *
 */
@Entity
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", updatable = false, nullable = false)
	private long id;
	
	@Column(unique=true)
	private String name;

	private String password;
	
	private String location;

	
	@ManyToMany
	@JsonIgnore
    @JoinTable( 
        name = "users_roles", 
        joinColumns = @JoinColumn(
          name = "user_id", referencedColumnName = "id"), 
        inverseJoinColumns = @JoinColumn(
          name = "role_id", referencedColumnName = "id")) 
    private Collection<Role> roles;
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@OneToMany(mappedBy = "userAccount")
	private Set<MarketEnergyRequest> bids = new HashSet<>();
	
	protected UserAccount() {
	}
	
	public UserAccount(String name, Collection<Role> roles) {
		this.name = name;
		this.roles = roles;
	}
	
	public UserAccount(String name, long id, Collection<Role> roles) {
		this(name, roles);
		this.id = id;
	}
	
	public UserAccount(String name, Collection<Role> roles, String location) {
		this(name, roles);
		this.location = location;
	}
	
	public UserAccount(String name, String location) {
		this.name = name;
		this.location = location;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getUserId() {
		return id;
	}
	
	public void setRoles(Collection<Role> roles) {
		this.roles = roles;
	}
	
	public Collection<Role> getRoles() {
		return this.roles;
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
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer desc = new StringBuffer();
		desc.append(this.name + " [");
		desc.append(this.id + "] (");
		Iterator<Role> roles = this.roles.iterator();
		while (roles.hasNext()) {
			desc.append(roles.next());
			if (roles.hasNext()) desc.append("/");
		}
		desc.append(")");
		return desc.toString();
	}
}
