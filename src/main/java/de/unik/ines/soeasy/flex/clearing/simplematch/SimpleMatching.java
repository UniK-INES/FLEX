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
package de.unik.ines.soeasy.flex.clearing.simplematch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.soeasy.common.model.flex.ActivationFactorType;
import de.soeasy.common.model.flex.CurrencyAmountType;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferOptionType;
import de.soeasy.common.model.flex.offer.FlexOptionISPType;
import de.soeasy.common.model.flex.order.FlexOrder;
import de.unik.ines.soeasy.flex.clearing.ClearingMethod;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;

/**
 * Simple Matching that accepts all unaccepted flex offers. Does not consider
 * flex demand.
 * 
 * @author Sascha Holzhauer
 *
 */
@Component
public class SimpleMatching implements ClearingMethod {

	private Log log = LogFactory.getLog(SimpleMatching.class);

	@Autowired
	FlexOrderRepository forderRepos;

	/**
	 * Accept all given flex offers which have not been accepted before (e.g. there
	 * is no according message id in flex orders.
	 * 
	 * @see de.unik.ines.soeasy.flex.clearing.ClearingMethod#clearMarket(List, List,
	 *      ClearingInfo)
	 */
	@Override
	public void clearMarket(List<Schedule_MarketDocument> demands, List<FlexOfferWrapper> offers, ClearingInfo cInfo) {
		log.info("Simple matching in operation...");
		for (FlexOfferWrapper fow : offers) {
			log.debug("Handle " + fow.getFlexOffer());
			if (!this.forderRepos.findByFlexOfferId(fow.getFlexOffer().getMessageID()).isPresent()) {
				FlexOrder fo = buildFlexOrderFromFlexOffer(fow.getFlexOffer(),
						fow.getFlexOffer().getOfferOptions().get(0));
				this.forderRepos.save(fo);
			}
		}
	}

	public static FlexOrder buildFlexOrderFromFlexOffer(FlexOffer fo, FlexOfferOptionType offerOption) {
		return buildFlexOrderFromFlexOffer(fo, offerOption, 1.0);
	}

	public static FlexOrder buildFlexOrderFromFlexOffer(FlexOffer fo, FlexOfferOptionType offerOption,
			double activationFactor) {
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

		forder.setOrderActivationFactor(new ActivationFactorType(new BigDecimal(activationFactor)));
		forder.setCurrency(fo.getCurrency());
		forder.setFlexOfferMessageID(fo.getMessageID());
		forder.setOptionReference(offerOption.getOptionReference());
		forder.setPrice(new CurrencyAmountType(
				new BigDecimal(offerOption.getPrice().getAmount().doubleValue() * activationFactor)));

		List<FlexOptionISPType> isps = new ArrayList<>();
		for (FlexOptionISPType offerIsp : offerOption.getIsps()) {
			FlexOptionISPType orderIsp = offerIsp.getCopy();
			isps.add(orderIsp);
		}
		forder.setIsps(isps);

		return forder;
	}
}
