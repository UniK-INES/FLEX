/**
 * 
 */
package de.unik.ines.soeasy.flex.balance;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.soeasy.common.model.MeterReading;
import de.soeasy.common.utils.Utils;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.util.MathUtils;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class MeterReadingManager implements InitializingBean {

	private static Log log = LogFactory.getLog(MeterReadingManager.class);

	@Autowired
	protected MMarketProductRepository productRepos;
	
	protected long meteringInterval = 0;
	
	protected long firstDelivery = Long.MAX_VALUE;
	
	/**
	 * In milliseconds before delivery period start (negative).
	 */
	protected long latestClosing = -Long.MAX_VALUE;
	
	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void initProperties() throws Exception {
		firstDelivery = Long.MAX_VALUE;
		latestClosing = -Long.MAX_VALUE;
		
		// retrieve all products:
		Set<Long> deliveryPeriods = new HashSet<>();
		for (MMarketProductPattern ppattern : productRepos.findAll()) {
			deliveryPeriods.add(ppattern.getProductPattern().deliveryPeriodDuration);
			this.firstDelivery = Math.min(this.firstDelivery, ppattern.getProductPattern().firstDeliveryPeriodStart);
			this.latestClosing = Math.max(this.latestClosing, Utils.getDurationFromDurationString(
					ppattern.getProductPattern().closingTime).toMillis());
		}
		
		log.info("Latest Closing is " + this.latestClosing + "(" + new SimpleDateFormat("HH:mm:ss").format(new Date(this.latestClosing)) + ")");
		
		// determine GCD (greatest common divisor):
		this.meteringInterval = MathUtils.gcd(deliveryPeriods.stream().mapToLong(l -> l).toArray());
	}

	public long getMeteringInterval() {
		if (this.meteringInterval == 0) {
			throw new IllegalStateException("Meter reading interval has not been determined!");
		}
		return this.meteringInterval;
	}
	
	public long getFirstDeliveryStart() {
		return this.firstDelivery;
	}
	
	/**
	 * @return the latestClosing
	 */
	public long getLatestClosing() {
		return latestClosing;
	}

	public boolean validate(MeterReading reading) {
		log.debug("Validating " + reading);
		
		// check metering start time:
		if ((reading.meteringStarttime - productRepos.getFirstRecord().getProductPattern().firstDeliveryPeriodStart) % 
				this.getMeteringInterval() != 0) {
			log.debug("Meter reading's starttime does not define a valid metering interval!");
			return false;
		}
		// check metering end time:
		if ((reading.meteringEndtime - productRepos.getFirstRecord().getProductPattern().firstDeliveryPeriodStart) % 
				this.getMeteringInterval() != 0) {
			log.debug("Meter reading's endtime does not define a valid metering interval!");
			return false;
		}

		return true;
	}
	
	public static MeterReading setUnhandeled(MeterReading reading) {
		reading.status = MeterReading.Status.UNHANDLED.getId();
		return reading;
	}
	
	public static MeterReading setInvalid(MeterReading reading) {
		reading.status = MeterReading.Status.INVALID.getId();
		return reading;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initProperties();
	}
}
