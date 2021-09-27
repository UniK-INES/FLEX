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

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeUtils;
import org.springframework.stereotype.Component;

import de.soeasy.common.model.EnergyRequest;
import de.soeasy.common.utils.Utils;
import de.unik.ines.soeasy.flex.clearing.MarketProductClearer;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class EnergyRequestValidator {
	
	private static Log log = LogFactory.getLog(EnergyRequestValidator.class);
	
	public static EnergyRequest.Status validate(EnergyRequest request, MMarketProductRepository mmProductRepos, 
			TimeInitializingBean timebean) {
		log.debug("Validate " + request + "...");
		
		// check product id
		MMarketProductPattern mmproductPattern = mmProductRepos.findById(request.productId).get(); 
		if (mmproductPattern == null) {
			log.info("Product ID in " + request + " does not exist!");
			return EnergyRequest.Status.INVALID_PRODUCT;
		}
		
		// closing time for start time needs to be in future
		if (Utils.getMarketClosingTimeForBidStartTime(request.startTime, mmproductPattern.getProductPattern().openingTime, 
				ZoneId.of(MarketProductClearer.zone)) > DateTimeUtils.currentTimeMillis()) {
			log.info("Opening time (" + 
					new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(Utils.getMarketClosingTimeForBidStartTime(
							request.startTime, mmproductPattern.getProductPattern().openingTime, 
							ZoneId.of(MarketProductClearer.zone)))) + 
					") for start time (" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(request.startTime)) + 
					") in " + request + " is in the future (current simulation time: " + 
					new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(DateTimeUtils.currentTimeMillis()))+ ")!");
			return EnergyRequest.Status.INVALID_TOO_EARLY;
		}

		// opening time for start time needs to be in past
		if (Utils.getMarketClosingTimeForBidStartTime(request.startTime, mmproductPattern.getProductPattern().closingTime, 
				ZoneId.of(MarketProductClearer.zone)) < DateTimeUtils.currentTimeMillis()) {
			log.info("Closing time for start time (" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(request.startTime)) + 
					") in " + request + " is in past (current simulation time: " + 
					new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(DateTimeUtils.currentTimeMillis()))+ ")!");
			return EnergyRequest.Status.INVALID_TOO_LATE;
		}
		
		// check start time (request's start time - 1st delivery start time needs to be multiple of duration)
		if ((request.startTime - mmproductPattern.getProductPattern().firstDeliveryPeriodStart) % 
			mmproductPattern.getProductPattern().deliveryPeriodDuration != 0) {
			log.info("Start time (" + new SimpleDateFormat("HH:mm:ss").format(new Date(request.startTime)) + 
					") in " + request + " does not match a delivery start time!");
			return EnergyRequest.Status.INVALID_STARTTIME;
		}
		
		if (request.startTime < mmproductPattern.getProductPattern().firstDeliveryPeriodStart) {
			log.info("Start time (" + new SimpleDateFormat("HH:mm:ss").format(new Date(request.startTime)) + ") in " + 
					request + " is before first delivery start time (" + 
					mmproductPattern.getProductPattern().firstDeliveryPeriodStart + ")");
			return EnergyRequest.Status.INVALID_STARTTIME;
		}
		
		// check end time
		if ((request.endTime - mmproductPattern.getProductPattern().firstDeliveryPeriodStart) % 
				mmproductPattern.getProductPattern().deliveryPeriodDuration != 0) {
			log.info("End time (" + new SimpleDateFormat("HH:mm:ss").format(new Date(request.endTime)) + ") in " + 
				request + " does not match a delivery end time!");
			return EnergyRequest.Status.INVALID_ENDTIME;
		}
		
		if (request.endTime < mmproductPattern.getProductPattern().firstDeliveryPeriodStart + 
				mmproductPattern.getProductPattern().deliveryPeriodDuration) {
			log.info("End time (" +  new SimpleDateFormat("HH:mm:ss").format(new Date(request.endTime)) + ") in " + 
				request + " is before first delivery end time (" + 
					(mmproductPattern.getProductPattern().firstDeliveryPeriodStart + 
							mmproductPattern.getProductPattern().deliveryPeriodDuration) + ")");
			return EnergyRequest.Status.INVALID_ENDTIME;
		}
		
		// check price requested (to be in range between min_price and max_price):
		if (request.priceRequested < mmproductPattern.getProductPattern().minPrice) {
			log.info("Requested price (" + request.priceRequested + ") is below minimum price (" + 
					mmproductPattern.getProductPattern().minPrice + ")");
			return EnergyRequest.Status.INVALID_PRICE;
		}

		if (request.priceRequested > mmproductPattern.getProductPattern().maxPrice) {
			log.info("Requested price (" + request.priceRequested + ") is above maximum price (" + 
					mmproductPattern.getProductPattern().maxPrice + ")");
			return EnergyRequest.Status.INVALID_PRICE;
		}
		
		// check requested energy != 0:
		if (request.energyRequested == 0) {
			log.info("Requested energy is 0!");
			return EnergyRequest.Status.INVALID_ENERGY;
		}
		
		// check energyResolutionKWh
		if (mmproductPattern.getProductPattern().energyResolutionKWh != 0.0 &&
				(request.energyRequested % mmproductPattern.getProductPattern().energyResolutionKWh) != 0) {
			log.info("Requested energy (" + request.energyRequested + ") is not a multiple of energy resolution "
					+ "defined in product pattern (" + mmproductPattern.getProductPattern().energyResolutionKWh + ")!");
			return EnergyRequest.Status.INVALID_ENERGY;
		}

		// check whether server is started:
		if (!timebean.isServerStarted()) {
			log.info("Request has been sent before market server was started!");
			return EnergyRequest.Status.INVALID_TOO_EARLY;
		}
		
		// reset energyAccepted
		request.energyAccepted = Float.NaN;
		
		// reset priceAccepted
		request.priceCleared = Float.NaN;
		
		return EnergyRequest.Status.UNHANDLED;
	}
}
