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
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.Series_Period;
import de.iwes.enavi.cim.schedule51.TimeSeries;
import de.soeasy.common.model.EnergyRequest;
import de.soeasy.common.model.MarketProductPattern;
import de.soeasy.common.model.MeterReading;
import de.soeasy.common.model.flex.ActivationFactorType;
import de.soeasy.common.model.flex.CurrencyAmountType;
import de.soeasy.common.model.flex.EntityAddressType;
import de.soeasy.common.model.flex.TimeZoneNameType;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferOptionType;
import de.soeasy.common.model.flex.offer.FlexOptionISPType;
import de.soeasy.common.model.flex.order.FlexOrder;
import de.unik.ines.soeasy.flex.FlexMarketApplication;
import de.unik.ines.soeasy.flex.grid.GridFlexDemandManager;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.MarketMeterReading;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.MarketMeterReadingRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeTest;

/**
 * @author Sascha Holzhauer
 *
 */
@Component
public class FlexTestUtils {
	protected int maxMRequestId = 0;
	
	public static Log log = LogFactory.getLog(FlexTestUtils.class);

	public static final String FILENAME_JSON_FLEXDEMAND_SMD_EMPTY = "json/GridFlexDemand_Schedule_MarketDocument_Empty.json";

	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	FlexOfferRepository fowRepos;

	@Autowired
	GridFlexDemandManager gridFlexManager;

	@Autowired
	MarketEnergyRequestRepository requestRepos;

	@Autowired
	MarketMeterReadingRepository readingRepos;
	

	/**
	 * Read empty SMD.
	 * 
	 * @return
	 */
	public static Schedule_MarketDocument createFlexDemand() {
		Schedule_MarketDocument flexDemandSmd = null;
		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		try {
			try {
				flexDemandSmd = mapper.readValue(new File(
						FlexTestUtils.class.getClassLoader().getResource(FILENAME_JSON_FLEXDEMAND_SMD_EMPTY).toURI()),
						Schedule_MarketDocument.class);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return flexDemandSmd;
	}

	public static FlexOffer buildFlexOffer() {
		FlexOffer flexOffer = new FlexOffer();

		flexOffer.setTimeStamp(DateTime.now());
		flexOffer.setSenderDomain("localhost.hems");
		flexOffer.setRecipientDomain("localhost.flex-server");
		flexOffer.setMessageID(UUID.randomUUID());
		flexOffer.setConversationID(UUID.randomUUID());

		flexOffer.setIspDuration(new Duration(1000 * 15 * 60));
		flexOffer.setTimeZone(new TimeZoneNameType("Europe/Amsterdam"));
		flexOffer.setPeriod(new Interval(new DateTime().plusDays(1).withTimeAtStartOfDay(), 
				new DateTime().plusDays(2).withTimeAtStartOfDay()));
		flexOffer.setCongestionPoint(new EntityAddressType("CongestionPointID"));

		flexOffer.setContractID("MP_ID:3#SampleContractID");
		flexOffer.setExpirationDateTime(DateTime.now().plusYears(10));

		List<FlexOfferOptionType> offerOptions = new ArrayList<FlexOfferOptionType>();

		FlexOfferOptionType offerOption = new FlexOfferOptionType();
		offerOption.setSanctionPrice(new CurrencyAmountType(new BigDecimal("10.000")));
		offerOption.setPrice(new CurrencyAmountType(new BigDecimal("15.300")));
		offerOption.setOptionReference("OptionA");
		offerOption.setMinActivationFactor(new ActivationFactorType(new BigDecimal("0.00")));
		offerOption.addIsp(new FlexOptionISPType((short) 1, 500));
		offerOption.addIsp(new FlexOptionISPType((short) 2, 1500));
		offerOption.addIsp(new FlexOptionISPType((short) 3, 0));
		offerOptions.add(offerOption);

		offerOption = new FlexOfferOptionType();
		offerOption.setSanctionPrice(new CurrencyAmountType(new BigDecimal("8.000")));
		offerOption.setPrice(new CurrencyAmountType(new BigDecimal("20.200")));
		offerOption.setOptionReference("OptionB");
		offerOption.setMinActivationFactor(new ActivationFactorType(new BigDecimal("0.00")));
		offerOption.addIsp(new FlexOptionISPType((short) 1, 0));
		offerOption.addIsp(new FlexOptionISPType((short) 2, 1500));
		offerOption.addIsp(new FlexOptionISPType((short) 3, 800));
		offerOptions.add(offerOption);

		flexOffer.setOfferOptions(offerOptions);

		return flexOffer;
	}

	public static MarketProductPattern buildFlexMarketProductPattern() {
		MarketProductPattern marketProductPattern = new MarketProductPattern();
		marketProductPattern.productId = 3;
		marketProductPattern.firstDeliveryPeriodStart = 1000 * 60;
		marketProductPattern.deliveryPeriodDuration = 1000 * 60 * 15;
		marketProductPattern.openingTime = "";
		marketProductPattern.closingTime = "-60M";
		marketProductPattern.auctionInterval = "0";
		marketProductPattern.minPrice = -3000.0f;
		marketProductPattern.maxPrice = 3000.0f;
		return marketProductPattern;
	}

	public static FlexOrder buildFlexOrderFromFlexOffer(FlexOffer fo) {
		FlexOrder forder = new FlexOrder();

		forder.setMessageID(UUID.randomUUID());
		forder.setRecipientDomain(fo.getSenderDomain());
		forder.setSenderDomain(fo.getRecipientDomain());
		forder.setTimeStamp(new DateTime(DateTimeUtils.currentTimeMillis()));
		forder.setConversationID(fo.getConversationID());

		forder.setTimeZone(fo.getTimeZone());
		forder.setCongestionPoint(fo.getCongestionPoint());
		forder.setPeriod(fo.getPeriod());
		forder.setIspDuration(fo.getIspDuration());

		forder.setOrderActivationFactor(new ActivationFactorType("1.000"));
		forder.setCurrency(fo.getCurrency());
		forder.setFlexOfferMessageID(fo.getMessageID());
		forder.setOptionReference(fo.getOfferOptions().get(0).getOptionReference());
		forder.setPrice(fo.getOfferOptions().get(0).getPrice());
		forder.setIsps(fo.getOfferOptions().get(0).getIsps());

		return forder;
	}

	/*
	 * Legacy....
	 */

	public void storeNewProcessedRequest(int id, String username, float priceCleared, float energyAccepted, long startTime, long endTime) {
		EnergyRequest request = new EnergyRequest();
		this.maxMRequestId = Math.max(id, maxMRequestId);
		request.priceCleared = priceCleared;
		request.energyAccepted = energyAccepted;
		request.startTime = startTime;
		request.endTime = endTime;
		requestRepos.save(new MarketEnergyRequest(this.maxMRequestId,  userRepos.findByName(username), request));
	}
	
	public void storeMeterReading(int id, String username, float energy, long startTime, long endTime) {
		MeterReading reading = createMeterReading(id, energy, startTime, endTime);
		MarketMeterReading mreading = new MarketMeterReading(userRepos.findByName(username), reading);
		readingRepos.save(mreading);
	}

	public MeterReading createMeterReading(int id, float energy, long startTime, long endTime) {
		MeterReading reading = new MeterReading();
		reading.energyConsumed = energy;
		reading.energyProduced = 0;
		reading.meteringStarttime = startTime;
		reading.meteringEndtime = endTime;
		reading.id = id;
		return reading;
	}

	/**
	 * Generate SMD from JSON file, set delivery interval according to given
	 * parameter, set mrID, and store SMD.
	 * 
	 * @param deliveryInterval
	 */
	public void generateSmd(Interval deliveryInterval) {
		Schedule_MarketDocument flexDemandSmd = null;
		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		try {
			try {
				flexDemandSmd = mapper.readValue(
						new File(FlexTestUtils.class.getClassLoader().getResource(TimeTest.FILENAME_JSON_FLEXDEMAND_SMD)
								.toURI()),
						Schedule_MarketDocument.class);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		TimeSeries ts = flexDemandSmd.getTimeSeries().iterator().next();
		Series_Period sp = new Series_Period();
		sp.setTimeInterval(deliveryInterval);
		ts.setPeriod(sp);
		flexDemandSmd.setTimeSeries(new HashSet<>());
		flexDemandSmd.addTimeSeries(ts);
		flexDemandSmd.setTimeInterval(deliveryInterval);
		flexDemandSmd.setmRID("TestSmdId_" + deliveryInterval.getStartMillis() / 1000);
		log.debug("Generated SMD: " + flexDemandSmd);
		gridFlexManager.storeGridFlexDemandSmd(flexDemandSmd);
	}

	public void generateFo(Interval deliveryInterval, MarketProductPattern mpattern) {
		UserAccount flex1 = userRepos.findByName("flex1");
		FlexOffer fo1 = buildFlexOffer();
		fo1.setPeriod(deliveryInterval);
		FlexOfferWrapper fow1 = new FlexOfferWrapper(fo1, flex1);
		fow1.setProductId(mpattern.productId);
		log.debug("Generated FlexOffer: " + fo1);
		fowRepos.save(fow1);
	}

	public void generateDemandAndOffer(Interval delivery, MarketProductPattern mpattern, int number) {
		Interval currentInterval;
		for (int i = 0; i < number; i++) {
			currentInterval = new Interval(delivery.getStartMillis() + i * mpattern.deliveryPeriodDuration,
					delivery.getEndMillis() + i * mpattern.deliveryPeriodDuration);
			generateSmd(currentInterval);
			generateFo(currentInterval, mpattern);
		}
	}
}
