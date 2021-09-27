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
 * 
 * Contains modified sources with
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.unik.ines.soeasy.flex.flex;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.soeasy.common.model.MarketProductPattern;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferOptionType;
import de.soeasy.common.model.flex.offer.FlexOptionISPType;
import de.soeasy.common.utils.Utils;
import de.unik.ines.soeasy.flex.config.UsefConfig;
import de.unik.ines.soeasy.flex.exceptions.FlexBusinessValidationException;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;
import de.unik.ines.soeasy.flex.util.FlexUtils;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.BusinessError;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.validation.CoreBusinessError;


/**
 * @author sascha
 *
 */
@Component
public class FlexOfferValidator {
	
	private static final String MP_ID_PATTERN_STRING = "MP_ID:(.*?)#";

	public enum FlexBusinessError implements BusinessError {
		CONTRACT_ID_INVALID("The given contract ID ({}) is invalid"),
		MARKET_PRODUCT_INVALID("The given market product ID ({}) cannot be found"),
		MIN_ACTIVATION_FACTOR_INVALID("The min activation factor is outside [0,1] (is {})"),
		NEG_SANCTION_PRICE("The sanction price is negative ({})"),
		ISP_START_OUT_OF_RANGE("The ISP's start index ({}) is out of range!"),
		ISP_DURATION_OUT_OF_RANGE("The ISP's index duration ({}) is out of range!"),
		INVALID_PTU_DURATION("The ISP duration ({}) is not the agreed value ({})."),;

		private final String errorMessage;

		FlexBusinessError(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getError() {
			return errorMessage;
		}
	}

	@Autowired
	UsefConfig usefConfig;
	
	@Autowired
	MarketProductRepository mmpRepos;

	@Autowired
	TimeInitializingBean timeb;

	/**
	 * Currently, these criteria are checked: -
	 * {@link FlexOfferValidator#validateCurrency(FlexOffer)} (as in USEF
	 * configuration) - {@link FlexOfferValidator#validateTimezone(FlexOffer)} (as
	 * in USEF configuration) -
	 * {@link FlexOfferValidator#validateFoContractId(FlexOffer)} (matching
	 * MP_ID:(.*?)#) -
	 * {@link FlexOfferValidator#validateIspDuration(Duration, FlexOffer)} (as in
	 * product deliveryPeriodDuration) -
	 * {@link FlexOfferValidator#validateFlexOfferOption(FlexOffer, FlexOfferOptionType)
	 * 
	 * @param flexOffer
	 * @return true if flexOffer is validated, false else
	 * @throws BusinessValidationException
	 */
	public boolean validateFlexOffer(FlexOffer flexOffer) throws BusinessValidationException {
		
		this.validateExpiration(flexOffer);

		// do some extra validation
		this.validateCurrency(flexOffer);
		this.validateTimezone(flexOffer);
		this.validateFoContractId(flexOffer);
		this.validateIspDuration(flexOffer.getIspDuration(), flexOffer);

		for (FlexOfferOptionType offerOption : flexOffer.getOfferOptions()) {
			validateFlexOfferOption(flexOffer, offerOption);
		}

		this.validateDomain(flexOffer);

		return true;
	}

	private void validateExpiration(FlexOffer flexOffer) throws FlexBusinessValidationException {
		if (flexOffer.getExpirationDateTime() != null
				&& flexOffer.getExpirationDateTime().isBefore(DateTimeUtils.currentTimeMillis())) {
			throw new FlexBusinessValidationException(flexOffer, CoreBusinessError.DOCUMENT_EXIRED,
					"FlexOffer", flexOffer.messageID, flexOffer.getExpirationDateTime());
		}
	}
	
    /**
	 * Validates the timezone against the configured timezone (checking
	 * {@link TimeZone#getRawOffset()}).
	 *
	 * @param flexOffer
	 * @throws BusinessValidationException
	 */
	public void validateTimezone(FlexOffer flexOffer) throws FlexBusinessValidationException {
		if (TimeZone.getTimeZone(usefConfig.getProperty(ConfigParam.TIME_ZONE)).getRawOffset() != TimeZone
				.getTimeZone(flexOffer.getTimeZone().getTimeZone()).getRawOffset()) {
			throw new FlexBusinessValidationException(flexOffer, CoreBusinessError.INVALID_TIMEZONE);
        }
    }
	
	/**
	 * Validates contract ID.
	 *
	 * @param flexOffer
	 * @throws BusinessValidationException
	 */
	public void validateFoContractId(FlexOffer flexOffer) throws FlexBusinessValidationException {
			try {
				@SuppressWarnings("unused")
				MarketProductPattern mpp = this.mmpRepos.findById(getProductId(flexOffer)).get();
			} catch (NoSuchElementException e) {
				throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.MARKET_PRODUCT_INVALID,
						getProductId(flexOffer));
			}
	}

	/**
	 * @param flexOffer
	 * @return
	 * @throws FlexBusinessValidationException
	 */
	public static int getProductId(FlexOffer flexOffer) throws FlexBusinessValidationException {
		Pattern pattern = Pattern.compile(MP_ID_PATTERN_STRING, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(flexOffer.getContractID());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		} else {
			throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.CONTRACT_ID_INVALID,
					flexOffer.getContractID());
		}
	}

	/**
	 * Validates the ISP Duration against the configured ISP Duration.
	 *
	 * @param ispDuration
	 * @param flexOffer
	 * @throws BusinessValidationException
	 */
	public void validateIspDuration(Duration ispDuration, FlexOffer flexOffer) throws FlexBusinessValidationException {
		MarketProductPattern mpp = this.mmpRepos.findById(FlexUtils.getMarketProductPatternId(flexOffer)).get();
		if (mpp.deliveryPeriodDuration != ispDuration.getMillis()) {
			throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.INVALID_PTU_DURATION,
					ispDuration.getMillis(), mpp.deliveryPeriodDuration);
		}
	}

	/**
	 * Validates the currency against the configured currency.
	 *
	 * @param flexOffer
	 * @throws BusinessValidationException
	 */
	public void validateCurrency(FlexOffer flexOffer) throws FlexBusinessValidationException {
		if (!usefConfig.getProperty(ConfigParam.CURRENCY).equals(flexOffer.getCurrency().getCurrency())) {
			throw new FlexBusinessValidationException(flexOffer, CoreBusinessError.INVALID_CURRENCY);
		}
	}

	/**
	 * Validates if the domain is the participants own domain.
	 *
	 * @param flexOffer
	 * @throws BusinessValidationException
	 */
	public void validateDomain(FlexOffer flexOffer) throws FlexBusinessValidationException {
		if (!usefConfig.getProperty(ConfigParam.HOST_DOMAIN).equals(flexOffer.getRecipientDomain())) {
			throw new FlexBusinessValidationException(flexOffer, CoreBusinessError.INVALID_DOMAIN,
					flexOffer.getRecipientDomain(),
					usefConfig.getProperty(ConfigParam.HOST_DOMAIN));
		}
	}

	/**
	 * @param flexOffer
	 * @param foo
	 * @return
	 * @throws FlexBusinessValidationException
	 */
	public boolean validateFlexOfferOption(FlexOffer flexOffer, FlexOfferOptionType foo)
			throws FlexBusinessValidationException {
		if (!foo.getIsps().isEmpty()) {
			for (FlexOptionISPType isp : foo.getIsps()) {
				this.validateFlexOfferOptionIsp(flexOffer, isp);
			}
		}
		BigDecimal minActivationFactor = foo.getMinActivationFactor().getValue();
		if (minActivationFactor.doubleValue() < 0 || minActivationFactor.doubleValue() > 1) {
			throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.MIN_ACTIVATION_FACTOR_INVALID,
					minActivationFactor);
		}
		if (foo.sanctionPrice.getAmount().doubleValue() < 0)
			throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.NEG_SANCTION_PRICE,
					foo.sanctionPrice.getAmount());

		return true;
	}
	
	/**
	 * @param flexOffer
	 * @param fooIsp
	 * @return
	 * @throws FlexBusinessValidationException
	 */
	public boolean validateFlexOfferOptionIsp(FlexOffer flexOffer, FlexOptionISPType fooIsp)
			throws FlexBusinessValidationException {
		MarketProductPattern mpp = this.mmpRepos.findById(FlexUtils.getMarketProductPatternId(flexOffer)).get();
		
		// validate being in time
		if (fooIsp.start * flexOffer.ispDuration.getMillis() + 
				flexOffer.getPeriod().getStartMillis() < Utils
						.getMarketClosingTimeForBidStartTime(DateTimeUtils.currentTimeMillis(), mpp.closingTime,
								null)) {
			throw new FlexBusinessValidationException(flexOffer,
					CoreBusinessError.FLEX_OFFER_HAS_PTU_IN_OPERATE_OR_LATER_PHASE,
					flexOffer, flexOffer.getSenderDomain(), fooIsp.getStart());
		}
		
		// validate start index
		if (fooIsp.getStart() < 1
				|| fooIsp.getStart() > Math.ceil(24 * 60 * 60 * 1000 / flexOffer.ispDuration.getMillis())) {
			throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.ISP_START_OUT_OF_RANGE,
					fooIsp.getStart());
		}

		// validate total duration
		if (fooIsp.getDuration() < 1
				|| fooIsp.getDuration() > Math.ceil(24 * 60 * 60 * 1000 / flexOffer.ispDuration.getMillis())) {
			throw new FlexBusinessValidationException(flexOffer, FlexBusinessError.ISP_DURATION_OUT_OF_RANGE,
					fooIsp.getDuration());
		}
		return true;
	}
}
