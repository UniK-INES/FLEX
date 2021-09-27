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
package de.unik.ines.soeasy.flex.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import de.unik.ines.soeasy.flex.model.Privilege;
import de.unik.ines.soeasy.flex.model.Role;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;

/**
 * Configures the management of roles, privileges, and authorities.
 * Loads {@link UserDetails}.
 * 
 * @author Sascha Holzhauer
 *
 */
@Configuration
class FlexGlobalAuthenticationConfigurerAdapter extends GlobalAuthenticationConfigurerAdapter {
	
	private Log log = LogFactory.getLog(FlexGlobalAuthenticationConfigurerAdapter.class);
			
	@Autowired
	UserAccountRepository accountRepository;

    
	/**
	 * @see org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter#init(org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder)
	 */
	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService());
	}

	/**
	 * @return
	 */
	@Bean
	UserDetailsService userDetailsService() {
		return new UserDetailsService() {

			/**
			 * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
			 */
			@Override
			@Transactional
			public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				UserAccount account = accountRepository.findByName(username);
				log.debug("Request username " + username + " and receive " + account);
				if (account != null) {
					log.debug("Request user " + username + " with roles " + account.getRoles());
					return new User(account.getName(), account.getPassword(), true, true, true, true,
							getAuthorities(account.getRoles()));
				} else {
					throw new UsernameNotFoundException("could not find the user '" + username + "'");
				}
			}

		};
	}
	
    /**
     * @param roles
     * @return
     */
    private final Collection<? extends GrantedAuthority> getAuthorities(final Collection<Role> roles) {
        return getGrantedAuthorities(getPrivileges(roles));
    }

    /**
     * @param roles
     * @return
     */
    private final List<String> getPrivileges(final Collection<Role> roles) {
        final List<String> privileges = new ArrayList<String>();
        final List<Privilege> collection = new ArrayList<Privilege>();
        for (final Role role : roles) {
            collection.addAll(role.getPrivileges());
        }
        for (final Privilege item : collection) {
            privileges.add(item.getName());
        }
        
        return privileges;
    }

    /**
     * @param privileges
     * @return
     */
    private final List<GrantedAuthority> getGrantedAuthorities(final List<String> privileges) {
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }
}
