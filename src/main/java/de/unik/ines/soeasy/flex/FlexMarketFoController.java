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
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.soeasy.common.model.api.EndpointsFlex;
import de.soeasy.common.model.flex.AcceptedRejectedType;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferResponse;
import de.soeasy.common.model.flex.order.FlexOrder;
import de.unik.ines.soeasy.flex.exceptions.EntityNotFoundException;
import de.unik.ines.soeasy.flex.exceptions.FlexBusinessValidationException;
import de.unik.ines.soeasy.flex.flex.FlexOfferValidator;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexUtils;
import energy.usef.core.exception.BusinessValidationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Sascha Holzhauer
 *
 */
@RestController()
@Component
@Api(value="FLEX Market FO Controller")
public class FlexMarketFoController {

	protected static String ERROR_MESSAGE_MESSAGEID_NOT_EXISTING = "The MessageID ({}) is not existing!";
	protected static String ERROR_MESSAGE_INVALID_DATE_PATTERN = "Invalid date format ({}) - should be yyyy-MM-dd)";

	private Log log = LogFactory.getLog(FlexMarketFoController.class);
	private Log elog = LogFactory.getLog("de.unik.ines.soeasy.flex.eventlogger");

	/**
	 * If true, also invalid requests are stored to the DB, wrapped by a new {@link MarketEnergyRequest}.
	 */
	@Value("${de.unik.ines.soeasy.flex.db.storeInvalidFlexOffers:false}")
	protected boolean storeInvalidFos;
	

	@Autowired
	FlexOfferRepository foRepos;

	@Autowired
	FlexOrderRepository forderRepos;

	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	FlexOfferValidator foValidator;

	/********************
	 * FLEX OFFER API
	 ********************/

	/**
	 * The {@link FlexOffer} JSON representation is checked at least against these
	 * properties:
	 * 
	 * <ul>
	 * <li><code>ispDuration</code>: Needs to correspond to market product's
	 * <code>deliveryPeriodDuration</code>. Example: <code>PT900S</code></li>
	 * 
	 * <li><code>timeZone</code>: Needs to be a valid TimeZone representation (@see
	 * {@link TimeZone#getTimeZone(String)}) and correspond to FLEX market's USEF
	 * configuration (<code>TIME_ZONE</code>). Example:
	 * <code>Europe/Berlin</code></li>
	 * 
	 * <li><code>period</code>: Needs to correspond to market product's
	 * <code>auctionDeliverySpan</code> for the bidding period. Example:
	 * <code>2021-06-10T00:00:00.000+02:00/2021-06-11T00:00:00.000+02:00</code></li>
	 * 
	 * <li><code>congestionPoint \> entityAddress</code>: The HEMS grid connecting
	 * point. Should correspond to <code>timeSeries > marketEvaluationPoint >
	 * mRID</code> of the FlexDemand Schedule_MarketDocument, extended by the node
	 * id. Example: <code>1X-KS-DIENST1-4_Transformer01_Node01</code></li>
	 * 
	 * <li><code>expirationDateTime</code>: Any date in future possible. Example:
	 * <code>2031-06-09T19:45:07.647+02:00</code></li>
	 * 
	 * <li><code>contractID</code>: Needs to contain to market product ID. Example:
	 * <code>MP_ID:3#SampleContractID</code></li>
	 * 
	 * <li><code>currency</code>: Needs to be a valid currency representation and
	 * correspond to FLEX market's USEF configuration (<code>CURRENCY</code>).
	 * Example: <code>EUR</code></li>
	 * </ul>
	 * 
	 * @param flexOffer
	 * @return {@link FlexOfferResponse}
	 * @throws BusinessValidationException
	 */
	@ApiOperation(value = "Receives HEMS's flexibility offer")
	@PostMapping(value = EndpointsFlex.FLEX_HEMS_FLEXOFFER)
	@CrossOrigin
	public @ResponseBody FlexOfferResponse receiveFlexOffer(@RequestBody FlexOffer flexOffer)
			throws BusinessValidationException {
		
		
		// user authentication:
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserAccount userAccount = userRepos.findByName(authentication.getName());
		log.info("Receive flex offer of " + authentication.getName() + "...");

		elog.info("-> FlexOffer for " + new SimpleDateFormat("dd.MM-HH:mm").format(flexOffer.period.getStart().toDate())
				+ " > " + new SimpleDateFormat("dd.MM-HH:mm").format(flexOffer.period.getEnd().toDate()) + " of "
				+ authentication.getName() + " at SimTime "
				+ new SimpleDateFormat("dd.MM-HH:mm").format(new Date(DateTimeUtils.currentTimeMillis())));

		// store flex offer in separate thread
		if (checkAndStoreRequest(flexOffer, userAccount)) {
			return FlexUtils.getFoResponse(flexOffer, AcceptedRejectedType.Accepted);
		} else {
			return FlexUtils.getFoResponse(flexOffer, AcceptedRejectedType.Rejected);
		}
	}

	/**
	 * @param flexOffer
	 * @param userAccount
	 * @return
	 * @throws BusinessValidationException
	 */
	protected boolean checkAndStoreRequest(FlexOffer flexOffer, UserAccount userAccount)
			throws BusinessValidationException {
		// validate request:
		if (log.isDebugEnabled()) {
			log.debug("Validate " + flexOffer + " of user " + userAccount.getName());
		}
		
		boolean validated = foValidator.validateFlexOffer(flexOffer);
		
		if (validated || this.storeInvalidFos) {
			// to prevent double CIDs for a user
			FlexOfferWrapper fow = new FlexOfferWrapper(flexOffer, userAccount);
			//flexOffer.setMessageID(UUID.randomUUID());
			
			Thread processRequest = new Thread() {
				public void run() {
					FlexMarketFoController.this.foRepos.save(fow);
					log.debug("FlexOffer from " + fow.getFlexOffer().senderDomain + " stored ("
							+ fow.getFlexOffer().messageID + ")");
				}
			};
			processRequest.run();
		}	
		return validated;
	}

	/********************
	 * FLEX ORDER API
	 ********************/
	
	@ApiOperation(value = "Sends FlexOrder for given flexibility offer message ID")
	@GetMapping(value = EndpointsFlex.FLEX_HEMS_FLEXORDER_ID)
	@CrossOrigin
	public @ResponseBody FlexOrder sendFlexOrderId(@RequestParam("foMessageId") String foId)
			throws BusinessValidationException {
		Optional<FlexOrder> forder = this.forderRepos.findByFlexOfferId(UUID.fromString(foId));
		if (!forder.isPresent()) {
			log.warn(ERROR_MESSAGE_MESSAGEID_NOT_EXISTING.replaceFirst("\\{\\}", foId));
			throw new EntityNotFoundException(ERROR_MESSAGE_MESSAGEID_NOT_EXISTING.replaceFirst("\\{\\}", foId));
		} else {
			log.info(
					"Send flex order to " + forder.get().recipientDomain + " for FlexOffer message ID " + foId + "...");
		}
		return forder.get();
	}

	@ApiOperation(value = "Sends all FlexOrder for the given date")
	@GetMapping(value = EndpointsFlex.FLEX_HEMS_FLEXORDER_DATE)
	@CrossOrigin
	public @ResponseBody List<FlexOrder> sendFlexOrderDate(@RequestParam("date") String dateString)
			throws BusinessValidationException {
		log.info("Send all flex orders for " + dateString + "...");

		try {
			DateTime date = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(dateString);

			List<FlexOrder> forders = this.forderRepos.findFordersByTime(date.withTimeAtStartOfDay().getMillis(),
					date.plusDays(1).withTimeAtStartOfDay().getMillis());

			return forders;

		} catch (IllegalArgumentException ex) {
			log.warn(ERROR_MESSAGE_INVALID_DATE_PATTERN.replaceFirst("\\{\\}", dateString));
			throw new IllegalArgumentException(ERROR_MESSAGE_INVALID_DATE_PATTERN.replaceFirst("\\{\\}", dateString));
		}
	}

	/********************
	 * UTILITY
	 ********************/

	/**
	 * 
	 * @param ex
	 * @return response entity containing {@link FlexOfferResponse}.
	 */
	@ExceptionHandler(FlexBusinessValidationException.class)
	public ResponseEntity<Object> handleBusinessValidationException(FlexBusinessValidationException ex) {
		return new ResponseEntity<>(FlexUtils.getFoResponse(ex.getFlexOffer(), ex), HttpStatus.BAD_REQUEST);
	}
}
