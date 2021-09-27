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
package de.unik.ines.soeasy.flex.grid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.iwes.enavi.cim.schedule51.Point;
import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.unik.ines.soeasy.flex.model.GridData;
import de.unik.ines.soeasy.flex.repos.GridFlexDemandSmdRepos;
import de.unik.ines.soeasy.flex.repos.TimeSeriesRepository;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class GridFlexDemandManager {
	
	@Value("${de.unik.ines.soeasy.flex.grid.flexDemandConversionFactorToKW:1000}")
	public float gridFlexDemandConversionFactorToKW;

	private Log log = LogFactory.getLog(GridFlexDemandManager.class);
	
	@Autowired
	protected GridFlexDemandSmdRepos gridflexrepos;
	
	@Autowired
	protected TimeSeriesRepository timeSeriesRepos;

	public GridFlexDemandManager() {
	}

	/**
	 * Parse {@link Schedule_MarketDocument} and create {@link GridData}. Assumes,
	 * SMD and time series have been stored to DB.
	 * 
	 * TODO implement for matching
	 * 
	 * @param gridSchedule
	 * @param iteration
	 * @return true if the grid schedule requires flexibility
	 */
	public boolean analyseGridSchedule(Schedule_MarketDocument gridSchedule, int iteration) {
		// identify flexibility needs per branch

		boolean requiresAction = false;

		ArrayList<Point> minPoints;
		ArrayList<Point> maxPoints;

		for (TimeSeries tSeries : timeSeriesRepos.findTsBySmd(gridSchedule.getmRID())) {
			tSeries.getInDomain_mRID();

			// e.g. "Transformer01":
			String location = tSeries.getIn().getmRID().getmRID();

			// iterate time steps:
			List<Point> points = tSeries.getPeriod().getPoints();

			if (log.isDebugEnabled()) {
				log.debug("Process "
						+ (tSeries.getPeriod().getResolution() > 0
								? ((int) tSeries.getPeriod().getTimeInterval().toDuration().getStandardMinutes()
										/ tSeries.getPeriod().getResolution())
								: 1)
						+ " schedule(s) (resolution: " + tSeries.getPeriod().getResolution() + " minutes; iteration "
						+ iteration + ") for " + tSeries.getIn().getmRID().getmRID() + ".");
			}

			minPoints = new ArrayList<>(points.size() / 2);
			maxPoints = new ArrayList<>(points.size() / 2);

			// sort according to position and assign delivery interval:
			for (Point p : points) {
				switch (p.getDescription()) {
				case "min":
					minPoints.add(p.getPosition(), p);
					break;
				case "max":
					maxPoints.add(p.getPosition(), p);
					break;
				}
			}

			long starttime = tSeries.getPeriod().getTimeInterval().getStartMillis();

			for (int i = 0; i < minPoints.size(); i++) {
				// store grid data:

				// work around as resolution not properly set by schedule service:
				int resolution = tSeries.getPeriod().getResolution() > 0 ? tSeries.getPeriod().getResolution() : 15;

				GridData gridData = new GridData(location,
						// assumes resolution is in minutes:
						starttime + resolution * 1000 * 60 * i, starttime + resolution * 1000 * 60 * (i + 1) - 1,
						iteration,
						minPoints.get(i).getQuantity().multiply(new BigDecimal(gridFlexDemandConversionFactorToKW)),
						maxPoints.get(i).getQuantity().multiply(new BigDecimal(gridFlexDemandConversionFactorToKW)));

				// TODO schedule
				// processGridSim(gridData);

				if (gridData.getMax().doubleValue() < 0 || gridData.getMin().doubleValue() > 0)
					requiresAction = true;
			}
		}
		return requiresAction;
	}
	
	public void storeGridFlexDemandSmd(Schedule_MarketDocument smd) {
		log.debug("Store " + smd + "...");
		
		if (smd.getmRID()== null) {				
			log.warn("Grid schedule (" + smd + ") has null for mRID! Assigning default...");
			smd.setmRID("GridFlex_" + smd.getSender_MarketParticipant().getmRID().getmRID() + "_" +  
					smd.getTimeInterval().getStartMillis());
		}
		
		if (smd.getRevisionNumber() == null || smd.getRevisionNumber() == "") {
			smd.setRevisionNumber("1.0");
		}
		Set<TimeSeries> timeSeries = smd.getTimeSeries();
		
		// check location (metering point identification)
		for (TimeSeries ts : timeSeries) {
			if (ts.getMarketEvaluationPoint().getmRID().equals("")) {
				log.warn("Location ist not set for time series " + ts + " in " + smd.getmRID());
			}
		}
		
		// check time interval 
		
		// check future
		if (log.isDebugEnabled()) {
			log.debug("Time interval: " + new Date(
					smd.getTimeInterval().getStartMillis()) + " >> " + new Date(smd.getTimeInterval().getEndMillis()));
		}
		if (smd.getTimeInterval().getStartMillis() < DateTimeUtils.currentTimeMillis()) {
			log.warn("Time interval in past for " + smd.getmRID() + " (" +
					new Date(smd.getTimeInterval().getStartMillis()) + " < " +
					new Date(DateTimeUtils.currentTimeMillis()) + ")");
		}
		
		int counter=0;
		for (TimeSeries ts : timeSeries) {
			ts.setParent(smd);
			ts.setmRID(smd.getmRID() + "_" + counter++);
		}
		
		gridflexrepos.save(smd);
		log.debug("Stored " + smd + ".");
	}
	
	/**
	 * Query {@link TimeSeries} based on parent {@link Schedule_MarketDocument} and location.
	 * 
	 * @param smd
	 * @param location
	 * @return the first {@link TimeSeries} matching the location, <code>null</code> if none matches location
	 */
	public TimeSeries getTimeSeries(Schedule_MarketDocument smd, String location) { 
		return timeSeriesRepos.findTsBySmdAndLocation(smd.getmRID(), location).get(0);
	}
}
