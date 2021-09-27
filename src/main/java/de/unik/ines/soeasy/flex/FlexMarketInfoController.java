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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.soeasy.common.model.MarketInformation;
import de.soeasy.common.model.MeterReading;
import de.soeasy.common.model.TimeInformation;
import de.soeasy.common.model.UserBalance;
import de.soeasy.common.model.api.Endpoints;
import de.unik.ines.soeasy.flex.balance.BalanceManager;
import de.unik.ines.soeasy.flex.balance.MeterReadingManager;
import de.unik.ines.soeasy.flex.clearing.MarketProductClearer;
import de.unik.ines.soeasy.flex.clearing.uniform.UniformPriceClearing;
import de.unik.ines.soeasy.flex.exceptions.EntityNotFoundException;
import de.unik.ines.soeasy.flex.exceptions.ServerNotStartedException;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.MarketMeterReading;
import de.unik.ines.soeasy.flex.model.MarketUserBalance;
import de.unik.ines.soeasy.flex.model.MarketVersionInformation;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.MarketInformationRepos;
import de.unik.ines.soeasy.flex.repos.MarketMeterReadingRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketUserBalanceRepos;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;
import de.unik.ines.soeasy.flex.util.InitialDataProvisionBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Sascha Holzhauer
 *
 */
@RestController()
@Component
@Api(value = "FLEX Info Controller")
public class FlexMarketInfoController {
		
	private Log log = LogFactory.getLog(FlexMarketInfoController.class);

	
	@Value("${de.unik.ines.soeasy.flex.metering.skipStorage:false}")
	protected boolean skipMetering;
	
	@Value("${de.unik.ines.soeasy.flex.metering.validation.skip:false}")
	protected boolean skipMeteringValidation;
	
	@Value("${de.unik.ines.soeasy.flex.balance.schedule:false}")
	protected boolean scheduleBalancing;

	/**
	 * If true, also invalid requests are stored to the DB, wrapped by a new {@link MarketEnergyRequest}.
	 */
	@Value("${de.unik.ines.soeasy.flex.db.storeInvalidRequests:false}")
	protected boolean storeInvalidRequests;

	/**
	 * If true, invalid requests are (copied and) renamed before storing to the DB. Is only relevant when
	 * <code>de.unik.ines.soeasy.flex.db.storeInvalidRequests is <code>true</code>. Makes only sense when clients would send multiple requests
	 * with identical CID.
	 */
	@Value("${de.unik.ines.soeasy.flex.db.renameInvalidRequests:false}")
	protected boolean renameInvalidRequests;
	
	@Value("${de.unik.ines.soeasy.flex.requests.suppressExceptions:false}")
	protected boolean suppressExceptions;
	
	@Autowired
	UniformPriceClearing clearing;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	MarketEnergyRequestRepository mRequestRepos;
	
	@Autowired
	ClearingInfoRepository clearingPriceRepos;
	
	@Autowired
	MarketInformationRepos minfoRepos;
	
	@Autowired
	MMarketProductRepository mmProductRepos;
	
	@Autowired
	MarketProductRepository mProductRepos;
	
	@Autowired
	MarketProductClearer mpClearer;
	
	@Autowired
	TimeInitializingBean timebean;
	
	@Autowired
	InitialDataProvisionBean dataProvisionBean;

	@Autowired
	MeterReadingManager readingManager;
	
	@Autowired
	BalanceManager balanceManager;
	
	@Autowired MarketMeterReadingRepository meteringRepos;
	
	@Autowired MarketUserBalanceRepos balanceRepos;
	
	public FlexMarketInfoController() {
	}
	
	/*****************************
	 * Information Interface
	 *****************************/
	
	@RequestMapping(value=Endpoints.MARKET_TIME, method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin(origins = "*") // required by web controller (angular2)
	public TimeInformation getTimeInformation(){
		if (log.isDebugEnabled()) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			log.debug("Send time information to " + authentication.getName() + "...");
		} else {
			log.info("Send time information...");
		}
		return this.timebean.getTimeInformation(mProductRepos, readingManager);
	}

	@RequestMapping(value=Endpoints.MARKETPRODUCTS, method = RequestMethod.GET, 
			produces = "application/json; charset=UTF-8")
	@CrossOrigin(origins = "*")
	public MarketInformation getProductInformation(){
		// returning non-NULL indicated the server is running to the OGEMA client
		if (!timebean.isServerStarted()) {
			throw new ServerNotStartedException("The FlexMarket-server has not been started!");
		}
		MarketInformation minfo = minfoRepos.getLastRecord();
		minfo.tradedProducts = new ArrayList<>();
		this.mProductRepos.findAll().forEach(minfo.tradedProducts::add);
		return minfo;
	}
	
	@RequestMapping(value = Endpoints.SERVER_STATUS, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	@CrossOrigin(origins = "*") // required by web controller (angular2)
	public String getServerStatus() {
		log.info("Send server status...");
		return this.timebean.isServerStarted() ? "active" : "inactive";
	}

	@RequestMapping(value="/api/version", method = RequestMethod.GET, 
			produces = "application/json; charset=UTF-8")
	@CrossOrigin(origins = "*")
	public MarketVersionInformation getmarketImplementationVersion(){
		Properties buildProperties = dataProvisionBean.getBuildProperties();
		return new MarketVersionInformation(buildProperties.getProperty("flex.server.implementation.version") + " ("
				+ buildProperties.getProperty("flex.server.implementation.timestamp") + ")");
	}
	
	
	/*****************************
	 * Clearing Price Interfaces
	 *****************************/
	
	@RequestMapping(value="/api/prices/all", method = RequestMethod.GET, 
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public List<ClearingInfo> priceJsonAll(){
		return clearingPriceRepos.findAll();
	}
	
	@ApiOperation(value = "Returns all clearing prices between the given times")
	@RequestMapping(value="/api/prices/between", 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public List<ClearingInfo> priceJsonFromTo(@RequestParam("after") long after, @RequestParam("before") long before){
		return clearingPriceRepos.findClearingInfoAfterBefore(after, before);
	}
	
	@ApiOperation(value = "Returns all clearing prices of the given product between the given times")
	@RequestMapping(value="/api/prices/byproduct/byclearing", 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public List<ClearingInfo> priceJsonByProductByClearingTime(@RequestParam("product") int productId,
			@RequestParam("after") long after, @RequestParam("before") long before){
		return clearingPriceRepos.findClearingInfoByProductByClearingTime(productId, after, before);
	}
	
	@ApiOperation(value = "Returns all clearing prices of the given product between the given times")
	@RequestMapping(value="/api/prices/byproduct/bydelivery", 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public List<ClearingInfo> priceJsonByProductByDeliveryStart(@RequestParam("product") int productId,
			@RequestParam("after") long after, @RequestParam("before") long before){
		return clearingPriceRepos.findClearingInfoByProductByDeliveryStart(productId, after, before);
	}
	
	/*****************************
	 * Metering / User Balance
	 *****************************/
	
	@ApiOperation(value = "Submit meter readings")
	@RequestMapping(value = Endpoints.METERING_SUBMIT, method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String receiveMeterings(@RequestBody List<MeterReading> meterReadings) {
		if (!this.skipMetering) {
			if (!this.skipMeteringValidation) {
				for (MeterReading meterReading : meterReadings) {
					if (!readingManager.validate(meterReading)) {
						return "Invalid meter reading detected.";
					}
				}
			}
		
			Thread t = new Thread(new Runnable() {
			    public void run() {
					Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
					UserAccount userAccount = userRepos.findByName(authentication.getName());
					long deliveryPeriodEndTime = Long.MIN_VALUE;
					List<MarketMeterReading> readings2store = new ArrayList<>();
					for (MeterReading meterReading : meterReadings) {
						meterReading = MeterReadingManager.setUnhandeled(meterReading);
						
						// check whether request with user and its custom ID already exists:
						// MarketMeterReading mreading = meteringRepos.findByCustomId(userAccount.getUserId(), request.cid);
							
						MarketMeterReading mreading = new MarketMeterReading(userAccount, meterReading);
						deliveryPeriodEndTime = Math.max(deliveryPeriodEndTime, meterReading.meteringEndtime);
						readings2store.add(mreading);
					}
					
					log.debug("Store energy requests of account " + userAccount.getName() + "...");
					meteringRepos.saveAll(readings2store);
					balanceManager.calculateBalance(deliveryPeriodEndTime, userAccount);
			    }
			});
			t.run();
			return "Submission of meter readings was successful.";
		} else {
			return "Meter readings submitted, but processing is skipped.";
		}
	}
	
	@ApiOperation(value = "Returns user balance for the requested period")
	@RequestMapping(value=Endpoints.BALANCE, 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public @ResponseBody UserBalance getSingleUserBalance(@RequestParam("starttime") long starttime,
			@RequestParam("endtime") long endtime) {
		if (scheduleBalancing) {
			// user authentication:
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserAccount userAccount = userRepos.findByName(authentication.getName());
			MarketUserBalance balance = this.balanceRepos.getSingleBalance(userAccount, starttime, endtime);
			if (balance == null) {
				throw new EntityNotFoundException("The requested user balance between " + 
						new SimpleDateFormat("HH:mm:ss").format(new Date(starttime)) + " and " +
						new SimpleDateFormat("HH:mm:ss").format(new Date(endtime)) +
						"> is not available for user " + userAccount + "!");
			}
			return balance.getUserBalance();
		} else {
			throw new EntityNotFoundException("Balancing is currently not performed by the market server!");
		}
	}
	
	@ApiOperation(value = "Returns user balances for the requested period")
	@RequestMapping(value="/api/balances", 
			method = RequestMethod.GET,
			produces = "application/json; charset=UTF-8")
	@CrossOrigin
	public @ResponseBody List<UserBalance> getUserBalances(@RequestParam("starttime") long starttime,
			@RequestParam("endtime") long endtime) {
		if (this.scheduleBalancing) {
			// user authentication:
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserAccount userAccount = userRepos.findByName(authentication.getName());
			List<MarketUserBalance> balances = this.balanceRepos.getBalances(userAccount, starttime, endtime);
			if (balances.isEmpty()) {
				throw new EntityNotFoundException("There are no user balances between " + 
						new SimpleDateFormat("HH:mm:ss").format(new Date(starttime)) + " and " +
						new SimpleDateFormat("HH:mm:ss").format(new Date(endtime)) +
						"> available for user " + userAccount + "!");
			}
			List<UserBalance> userBalances = new ArrayList<>(balances.size());
			balances.forEach(balance -> userBalances.add(balance.getUserBalance()));
			return userBalances;
		} else {
			throw new EntityNotFoundException("Balancing is currently not performed by the market server!");
		}
	}
}
