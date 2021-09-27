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
package de.unik.ines.soeasy.flex.doc;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @author Sascha Holzhauer
 *
 */
@Configuration
public class SwaggerConfig {
	
	/**
	 * If true, also invalid requests are stored to the DB, wrapped by a new
	 * {@link MarketEnergyRequest}.
	 */
	@Value("${de.unik.ines.soeasy.flex.swagger.basepath:null}")
	protected String swaggerBasePath;

	@Bean
    public Docket swaggerSpringMvcPlugin() { 
        return new Docket(DocumentationType.SWAGGER_2)
        		//.groupName("enavi-market-api")
        		.apiInfo(this.apiInfo())
          .select()
          .apis(RequestHandlerSelectors.any())
          .paths(paths())                         
				.build().pathMapping(swaggerBasePath);
    }
	
	private Predicate<String> paths() {
		return regex("/admin.*").or(regex("/inspect.*").or(regex("/api.*")));
	}
	
	@Bean
	public Docket swaggerSpringDso() {
	    return new Docket(DocumentationType.SWAGGER_2)
	    	.apiInfo(this.apiInfo())
	        .groupName("DSO")
	        .select()
	            .apis(RequestHandlerSelectors.basePackage("de.unik.ines.soeasy.flex"))
	            .paths(regex("/api/dso.*"))
	        .build();
	}
	
	@Bean
	public Docket swaggerSpringHems() {
	    return new Docket(DocumentationType.SWAGGER_2)
	    	.apiInfo(this.apiInfo())
	        .groupName("HEMS")
	        .select()
	            .apis(RequestHandlerSelectors.basePackage("de.unik.ines.soeasy.flex"))
	            .paths(regex("/api/hems.*"))
	        .build();
	}
	
	/**
	 * @return
	 */
	private ApiInfo apiInfo() {
	    @SuppressWarnings("rawtypes")
		ApiInfo apiInfo = new ApiInfo(
	      "FLEX Market REST API",
	      "Description of FLEX Market Server API.",
	      "API 0.1",
	      "",
	      new Contact("Sascha Holzhauer", "http://ines.uni-kassel.de", "sascha.holzhauer@uni-kassel.de"),
	      "LGPL 3.0",
	      "http://www.gnu.org/licenses/#LGPL",
	      new ArrayList<VendorExtension>());
	    return apiInfo;
	}
}
