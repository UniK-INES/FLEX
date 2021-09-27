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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.ErrorHandler;

/**
 * @author Sascha Holzhauer
 *
 */
@SuppressWarnings("serial")
public class FlexThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {


	private Log log = LogFactory.getLog(FlexThreadPoolTaskScheduler.class);
	
	@Autowired
	TimeInitializingBean timebean;
	

	private volatile ErrorHandler errorHandler;

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = timebean.convertSimToRealTime(startTime.getTime()) - System.currentTimeMillis();
		log.trace("Initial delay: " + Duration.ofMillis(initialDelay) + " (start time: "
				+ new SimpleDateFormat("dd/MM/yyyy>HH:mm:ss.SSS").format(new Date(startTime.getTime())) + ")");
		
		try {
			return executor.schedule(errorHandlingTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = timebean.convertSimToRealTime(startTime.getTime()) - System.currentTimeMillis();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), initialDelay, 
					timebean.convertSimToRealPeriodAfterBasetime(period), TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), 0, 
					timebean.convertSimToRealPeriodAfterBasetime(period), TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = timebean.convertSimToRealTime(startTime.getTime()) - System.currentTimeMillis();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), initialDelay, 
					timebean.convertSimToRealPeriodAfterBasetime(delay), TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), 0, 
					timebean.convertSimToRealPeriodAfterBasetime(delay), TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}
	
	/**
	 * Set a custom {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		super.setErrorHandler(errorHandler);
		this.errorHandler = errorHandler;
	}

	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}
}
