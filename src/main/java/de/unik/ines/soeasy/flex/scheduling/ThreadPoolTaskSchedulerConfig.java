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
package de.unik.ines.soeasy.flex.scheduling;
	
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 
 * @author Sascha Holzhauer
 *
 */
@Configuration
public class ThreadPoolTaskSchedulerConfig {

	@Value("${de.unik.ines.soeasy.flex.threadpool.size:5}")
	private int poolSize;
	
	/**
	 * Configures and initialises thread pool task scheduler.
	 * 
	 * @see FlexThreadPoolTaskScheduler
	 * 
	 * @return
	 */
	@Bean
	@Qualifier("taskScheduler")
    public ThreadPoolTaskScheduler schedJuler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler
          = new FlexThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(poolSize);
        threadPoolTaskScheduler.setThreadNamePrefix(
          "ThreadPoolTaskScheduler");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}
