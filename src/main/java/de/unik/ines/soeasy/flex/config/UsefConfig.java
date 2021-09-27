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
 * 
 * 
 * Contains modified sources with
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.unik.ines.soeasy.flex.config;


import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import energy.usef.core.config.AbstractConfig;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.util.DateTimeUtil;

/**
 * @author sascha
 *
 */
@Component
public class UsefConfig extends AbstractConfig {

	private static final Log LOGGER = LogFactory.getLog(UsefConfig.class);

    private final ScheduledExecutorService updateTimeExecutor = Executors.newSingleThreadScheduledExecutor();
        
    /**
     * Initialize a bean after the instance has been constructed.
     */
    @PostConstruct
    public void initBean() {
        try {
        	/** For custom property values, system property "jboss.server.config.dir" needs to point to a 
        	 * directory containing a folder "usef" with property file "config.properties" in it!
        	 */
            readProperties();
            boolean usingServer = DateTimeUtil.updateSettings(getProperty(ConfigParam.TIME_SERVER),
                    getIntegerProperty(ConfigParam.TIME_SERVER_PORT));
            // if DateTimeUtil is using a server update the time at least every second.
            if (usingServer) {
                LOGGER.info("Running in TimeServer mode");
                updateTimeExecutor.scheduleAtFixedRate(DateTimeUtil::getCurrentDate, 1, 1, TimeUnit.SECONDS);
            } else {
                LOGGER.info("Running in system time mode");
            }
        } catch (IOException e) {
            LOGGER.error("Error while loading the properties: " + e.getMessage(), e);
        }
        startConfigWatcher();
    }

    /**
     * Clean up the bean before destroying this instance.
     */
    @PreDestroy
    public void cleanupBean() {
        updateTimeExecutor.shutdownNow();
        stopConfigWatcher();
    }

    /**
     * Gets a property value as a {@link String}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public String getProperty(ConfigParam configParam) {
        if (properties == null) {
            return null;
        }
        return properties.getProperty(configParam.name());
    }
    
    /**
     * Gets a property value as an {@link Integer}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Integer getIntegerProperty(ConfigParam configParam) {
        return Integer.parseInt(properties.getProperty(configParam.name()));
    }
}
