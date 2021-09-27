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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Enable access to resources (images etc.)
 * 
 * @author Sascha Holzhauer
 *
 */
@Configuration
@EnableResourceServer
@Order(-50)
public class OAuth2ResourceServerConfig 
  extends ResourceServerConfigurerAdapter {
    
	/**
	 * @see org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter#configure(org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer)
	 */
	@Override
    public void configure(ResourceServerSecurityConfigurer config) {
        config.tokenServices(tokenServices);
    }
	
	@Autowired
	DefaultTokenServices tokenServices;
 
    /**
     * @return JwtAccessTokenConverter
     */
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
    	   JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
    	    Resource resource = new ClassPathResource("public.key");
    	    String publicKey = null;
    	    try {
    	        publicKey = IOUtils.toString(resource.getInputStream(), (String) null);
    	    } catch (final IOException e) {
    	        throw new RuntimeException(e);
    	    }
    	    converter.setVerifierKey(publicKey);
    	    return converter;
    }
    
    /**
     * @see org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter#configure(org.springframework.security.config.annotation.web.builders.HttpSecurity)
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
    	http.cors().and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
        http.requestMatcher(new OAuthRequestedMatcher())
                .anonymous().disable()
                .authorizeRequests()
                .antMatchers(WebSecurityConfigBasic.AUTH_WHITELIST).permitAll()
                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .antMatchers("/admin/**").access("hasAnyRole('ROLE_admin')")
        		.antMatchers("/inspect/**").access("hasAnyRole('ROLE_inspect')");
    }

    private static class OAuthRequestedMatcher implements RequestMatcher {
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            // Determine if the client request contained an OAuth Authorization
            boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer");
            boolean haveAccessToken = request.getParameter("access_token")!=null;
            return haveOauth2Token || haveAccessToken;
        }
    }
}