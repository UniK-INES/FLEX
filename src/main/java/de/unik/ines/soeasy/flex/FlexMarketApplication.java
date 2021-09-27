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
package de.unik.ines.soeasy.flex;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.unik.ines.soeasy.flex.util.serialize.DateTimeDeserializer;
import de.unik.ines.soeasy.flex.util.serialize.DateTimeSerializer;
import de.unik.ines.soeasy.flex.util.serialize.DurationDeserializer;
import de.unik.ines.soeasy.flex.util.serialize.DurationSerializer;
import de.unik.ines.soeasy.flex.util.serialize.IntervalDeserializer;
import de.unik.ines.soeasy.flex.util.serialize.IntervalSerializer;
import de.unik.ines.soeasy.flex.util.serialize.LocalDateTimeDeserializer;
import de.unik.ines.soeasy.flex.util.serialize.LocalDateTimeSerializer;

/**
 * @author Sascha Holzhauer
 *
 */
@SpringBootApplication
@EntityScan(basePackages = {
		"de.iwes.enavi.cim.schedule51", "de.soeasy.common.model", "de.unik.ines.soeasy.flex.model" })
@EnableScheduling
public class FlexMarketApplication extends SpringBootServletInitializer {

	private static Log log = LogFactory.getLog(FlexMarketApplication.class);
	
	private static ObjectMapper objectMapper;
	
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(FlexMarketApplication.class, args);
		log.info("Context " + context.getId() + " started at " + 
				new SimpleDateFormat("HH:mm:ss").format(new Date(context.getStartupDate())));
	}

	/**
	 * Used to encrypt user passwords in
	 * {@link FlexMarketAdminController#addUser(de.unik.ines.soeasy.flex.model.UserAccount)}.
	 * 
	 * @return
	 */
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	/**
	 * Required for serialisation of CIM objects into and from JSON
	 * 
	 * @return message converter
	 */
	@Bean
	public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setPrettyPrint(true);
		ObjectMapper objectMapper = getObjectMapper();
		converter.setObjectMapper(objectMapper);
		return converter;
	}
	
	public static ObjectMapper getObjectMapper() {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();

			SimpleModule module = new SimpleModule();
			
			module.addSerializer(Interval.class, new IntervalSerializer());
			module.addDeserializer(Interval.class, new IntervalDeserializer());

			module.addSerializer(Duration.class, new DurationSerializer());
			module.addDeserializer(Duration.class, new DurationDeserializer());
			
			module.addSerializer(DateTime.class, new DateTimeSerializer());
			module.addDeserializer(DateTime.class, new DateTimeDeserializer());
			
			module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
			module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
			
			objectMapper.registerModule(module);
		}
		return objectMapper;
	}
}
