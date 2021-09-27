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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

/**
 * @author Sascha Holzhauer
 *
 */
@Entity
public class Role {
	
	public static final String ADMIN = "ADMIN";
	public static final String USER = "USER";
	public static final String INSPECTOR = "INSPECTOR";
	
  
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToMany(mappedBy = "roles")
    private Collection<UserAccount> users;

    @ManyToMany
    @JoinTable(name = "roles_privileges", 
    	joinColumns = @JoinColumn(name = "role_id",
    	referencedColumnName = "id"),
    	inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id"))
    private Collection<Privilege> privileges;

    private String name;

    /**
     * 
     */
    public Role() {
        super();
    }

    /**
     * @param name
     */
    public Role(final String name) {
        super();
        this.name = name;
    }

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return users
     */
    public Collection<UserAccount> getUsers() {
        return users;
    }

    /**
     * @param users
     */
    public void setUsers(final Collection<UserAccount> users) {
        this.users = users;
    }

    /**
     * @return privileges
     */
    public Collection<Privilege> getPrivileges() {
        return privileges;
    }

    /**
     * @param privileges
     */
    public void setPrivileges(final Collection<Privilege> privileges) {
        this.privileges = privileges;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Role role = (Role) obj;
        if (!this.name.equals(role.name)) {
            return false;
        }
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Role [name=").append(name).append("]").append("[id=").append(id).append("]");
        return builder.toString();
    }
}