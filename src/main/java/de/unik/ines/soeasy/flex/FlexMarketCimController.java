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
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.soeasy.common.model.api.EndpointsFlex;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandManager;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandValidator;
import de.unik.ines.soeasy.flex.local.LocalFlexDemandManager;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;
import de.unik.ines.soeasy.flex.util.cim.BaselineScheduleMarketDocumentFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Sascha Holzhauer
 *
 */
@RestController()
@Component
@Api(value="FLEX Market CIM Controller")
public class FlexMarketCimController {

	private Log log = LogFactory.getLog(FlexMarketCimController.class);
	private Log elog = LogFactory.getLog("de.unik.ines.soeasy.flex.eventlogger");

	@Autowired
	TimeInitializingBean timebean;
	
	@Autowired
	GridFlexDemandManager gridFlexManager;
	
	@Autowired
	LocalFlexDemandManager localFlexManager;
	
	@Autowired
	ClearingInfoRepository clearingPriceRepos;
	
	@Autowired
	MarketEnergyRequestRepository requestRepos;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	MMarketProductRepository mmProductRepos;
	
	@Autowired
	GridFlexDemandValidator flexDemandValidator;

	@ApiOperation(value = "Receives DSO's flexibility demand as CIM Schedule Market Document.")
	@PostMapping(value=EndpointsFlex.FLEX_DSO_FLEXDEMAND)
	@CrossOrigin
	public @ResponseBody String receiveFlexDemandSchedule(@RequestBody Schedule_MarketDocument flexDemandSmd) {
		log.info("Receive flex demand CIM schedule document...");
		
		// user authentication:
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserAccount userAccount = userRepos.findByName(authentication.getName());
		
		// Check FLEX user matches sender.mrid.mrid!
		flexDemandValidator.validateFlexDemandCim(flexDemandSmd, userAccount);
		log.debug("Validating flex demand CIM schedule document of " + userAccount.getName() + "...");
		
		// TODO store schedule in separate thread
		this.gridFlexManager.storeGridFlexDemandSmd(flexDemandSmd);
		elog.info(System.getProperty("line.separator") + "-> FlexDemand for "
				+ new SimpleDateFormat("dd.MM-HH:mm").format(flexDemandSmd.getTimeInterval().getStart().toDate())
				+ " > " + new SimpleDateFormat("dd.MM-HH:mm").format(flexDemandSmd.getTimeInterval().getEnd().toDate())
				+ " from " + authentication.getName() + " at SimTime "
				+ new SimpleDateFormat("dd.MM-HH:mm").format(new Date(DateTimeUtils.currentTimeMillis())));

		return "Flexibility demand schedule received successfully.";
	}
	
	/**
	 * Return the local flexibility demand relevant for the user's location for the
	 * subsequent DayAhead (0:00h - 24.00h) matching as CIM Schedule document
	 * 
	 * @return local flex demand as CIM Schedule MarketDocument
	 */
	@ApiOperation(value = "Return the local flexibility demand relevant for the user's location "
			+ "for the subsequent DayAhead matching as CIM Schedule document")
	@RequestMapping(value = EndpointsFlex.FLEX_HEMS_FLEXDEMAND_NEXTDAY, 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public @ResponseBody Schedule_MarketDocument getLocalFlexDemand(){
		
		log.info("Send Local Flex Demand CIM schedule document...");
		
		// user authentication:
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserAccount userAccount = userRepos.findByName(authentication.getName());
		
		if (userAccount == null) {
			throw new IllegalStateException("User could not be authenticated!");
		}
		
		return localFlexManager.getLocalFlexDemandSmd(userAccount);
	}
	
	/**
	 * Return the local flexibility demand relevant for the user's location for the
	 * subsequent matching of the given product as CIM Schedule document
	 * 
	 * @return local flex demand as CIM Schedule MarketDocument
	 */
	@ApiOperation(value = "Return the local flexibility demand relevant for the user's location "
			+ "for the subsequent matching of given product (default:3) as CIM Schedule document")
	@RequestMapping(value = EndpointsFlex.FLEX_HEMS_FLEXDEMAND, method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public @ResponseBody Schedule_MarketDocument getLocalFlexDemand(@RequestParam(defaultValue = "3") int productId) {

		// user authentication:
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserAccount userAccount = userRepos.findByName(authentication.getName());

		if (userAccount == null) {
			throw new IllegalStateException("User could not be authenticated!");
		}

		MMarketProductPattern mmpPattern = mmProductRepos.findById(productId).get();
		log.info("Send Local Flex Demand CIM schedule document for Product " + mmpPattern + " to user "
				+ userAccount.getName() + "...");

		Schedule_MarketDocument localFlexDemand = localFlexManager.getLocalFlexDemandSmd(userAccount, mmpPattern, -1);
		elog.info("<- FlexDemand for "
				+ new SimpleDateFormat("dd.MM-HH:mm").format(localFlexDemand.getTimeInterval().getStart().toDate())
				+ " > "
				+ new SimpleDateFormat("dd.MM-HH:mm").format(localFlexDemand.getTimeInterval().getEnd().toDate())
				+ " to " + authentication.getName() + " at SimTime "
				+ new SimpleDateFormat("dd.MM-HH:mm").format(new Date(DateTimeUtils.currentTimeMillis())));

		return localFlexDemand;
	}

	@ApiOperation(value = "Returns clients' schedules as CIM Schedule Market Document")
	@RequestMapping(value="/api/cim/schedule/all", 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public Schedule_MarketDocument scheduleJsonByProductByDeliveryStart(
			@RequestParam("after") long after, @RequestParam("before") long before){
		return BaselineScheduleMarketDocumentFactory.getScheduleDocument(clearingPriceRepos.
				findClearingInfoAfterBefore(after, before), after, before, timebean,
				userRepos.findAllDealingClients(), requestRepos);
	}
	
	@ApiOperation(value = "Receives clients' baseline schedule as CIM Schedule Market Document to verify flexibility")
	@RequestMapping(value = EndpointsFlex.FLEX_HEMS_BASELINE, 
			method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.TEXT_PLAIN_VALUE)
	@CrossOrigin
	public @ResponseBody String receiveBaselineSchedule(@RequestParam Schedule_MarketDocument baselineSchedule) {
		// TODO validate CIM
		
		// TODO store schedule in separate thread
		 
		// TODO send response
		return "Baseline schedule received successfully";
	}
}
