/**
 * 
 */
package de.unik.ines.soeasy.flex.balance;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import de.soeasy.common.model.UserBalance;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.MarketMeterReading;
import de.unik.ines.soeasy.flex.model.MarketUserBalance;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.MarketInformationRepos;
import de.unik.ines.soeasy.flex.repos.MarketMeterReadingRepository;
import de.unik.ines.soeasy.flex.repos.MarketUserBalanceRepos;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;

/**
 * Schedules calculation of balances
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class BalanceManager implements InitializingBean {

	private Log log = LogFactory.getLog(BalanceManager.class);
	
	@Autowired
	@Qualifier("taskScheduler")
	ThreadPoolTaskScheduler taskScheduler;
	
	@Autowired
	MeterReadingManager readingManager;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	MarketUserBalanceRepos balanceRepos;
	
	@Autowired
	MarketEnergyRequestRepository mrequestRepos;
	
	@Autowired
	MarketMeterReadingRepository readingRepos;
	
	@Autowired
	MarketInformationRepos minfoRepos;
	
	@Value("${de.unik.ines.soeasy.flex.balance.schedule:false}")
	protected boolean scheduleBalancing;
	
	protected float fine;
	
	protected float fineMissingReading;
	
	protected boolean active = true;
	
	public BalanceManager() {
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// schedule this manager
		if (scheduleBalancing) {
			this.scheduleBalanceCalculation(readingManager.getFirstDeliveryStart() + readingManager.getMeteringInterval());
		}
		
		this.fine = this.minfoRepos.findById(1).get().finePerUntradedKwh;
		this.fineMissingReading = this.minfoRepos.findById(1).get().fineMissingReading;
	}
	
	public void scheduleBalanceCalculation(long deliveryEndTime) {
	taskScheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if (BalanceManager.this.active) {
					BalanceManager.this.calculateBalances(deliveryEndTime);
					// schedule next balance calculation					
					BalanceManager.this.scheduleBalanceCalculation(deliveryEndTime + readingManager.getMeteringInterval());
				}
			}
		}, new Date(deliveryEndTime));
	}
	
	public void calculateBalances(long deliveryPeriodEndTime) {
		// for each user
		for (UserAccount userAccount : this.userRepos.findAll()) {
			// retrieve all requests which overlap with the accounting period
			calculateBalance(deliveryPeriodEndTime, userAccount);
		}
	}

	public void calculateBalance(long deliveryPeriodEndTime, UserAccount userAccount) {
		float energy = 0;
		float costs = 0;
		log.debug("Start calculation of balance...");
		
		for (MarketEnergyRequest request : this.mrequestRepos.findAllByIntersectionAndUser(deliveryPeriodEndTime - 
				readingManager.getMeteringInterval(), deliveryPeriodEndTime, userAccount)) {
			// calculate and sum energy of the accounting period
			float factor = (request.getRequest().endTime -  request.getRequest().startTime) / readingManager.getMeteringInterval();
			float renergy = request.getRequest().energyAccepted / factor;
			energy += renergy;
			costs += renergy * request.getRequest().priceCleared;
			log.trace("Energy for " + request + " (factor " + factor + "): " + renergy);
		}
		
		// compare with meter reading and calculate fine
		MarketMeterReading mreading = this.readingRepos.findByUserAndTime(userAccount, deliveryPeriodEndTime);
		float reading = Float.NaN;
		if (mreading != null) {
			reading = mreading.getReading().energyConsumed - 
					mreading	.getReading().energyProduced;
		} else { 
			log.debug("No reading available for user "  + userAccount + " for period from " + 
					new SimpleDateFormat("HH:mm:ss").format(new Date(deliveryPeriodEndTime - 
					readingManager.getMeteringInterval())) + " to " + 
					new SimpleDateFormat("HH:mm:ss").format(new Date(deliveryPeriodEndTime)));
		}
		
		UserBalance balance = new UserBalance(deliveryPeriodEndTime - 
				readingManager.getMeteringInterval(), deliveryPeriodEndTime, reading, energy, 
				Float.isNaN(reading) ? this.fineMissingReading : Math.abs(reading-energy) * this.fine,
				costs);
		
		// create and store balancing
		log.debug("Created " + balance + " of user " + userAccount);
		
		this.balanceRepos.save(new MarketUserBalance(userAccount, balance));
	}
	
	public void setInActive() {
		this.active = false;
	}
}
