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
package de.unik.ines.soeasy.flex.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.joda.time.Interval;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandManager;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class InitialDataProvisionBean implements InitializingBean {
	
	@Value("${de.unik.ines.soeasy.flex.init.demandSMD:json/GridFlexDemand_Schedule_MarketDocument_Initial.json}")
	protected String filenameFlexDemandJson; 

	@Autowired
	GridFlexDemandManager gridFlexManager;
	
	@Autowired
	TimeInitializingBean timeBean;
	
	@Autowired
	ObjectMapper mapper;

	Properties buildProperties;

	public void fillFlexDemand() throws URISyntaxException {
		if (!filenameFlexDemandJson.equals("NN")) {
			Schedule_MarketDocument flexDemandSmd = null;
			try {
				flexDemandSmd = mapper.readValue(
						new File(getClass().getClassLoader().getResource(filenameFlexDemandJson).toURI()),
						Schedule_MarketDocument.class);
			} catch (JsonGenerationException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Interval interval = new Interval(timeBean.getNextDayStart(), timeBean.getDayAfterNextDayStart() - 1);
			flexDemandSmd.setTimeInterval(interval);
			for (TimeSeries ts : flexDemandSmd.getTimeSeries()) {
				ts.getPeriod().setTimeInterval(interval);
			}

			gridFlexManager.storeGridFlexDemandSmd(flexDemandSmd);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.fillFlexDemand();
		this.readBuildProperties();
	}

	protected void readBuildProperties() throws IOException {
		final Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("build.properties"));
		this.buildProperties = properties;
	}

	public Properties getBuildProperties() {
		return this.buildProperties;
	}
}
