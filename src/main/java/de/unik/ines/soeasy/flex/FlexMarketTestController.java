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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.unik.ines.soeasy.flex.clearing.simplematch.SimpleMatching;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.repos.RoleRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * API for testing purposes
 * 
 * @author Sascha Holzhauer
 *
 */
@RestController()
@RequestMapping("/api/test")
@Api(value = "Flex Market Test Controller")
public class FlexMarketTestController {

	private Log log = LogFactory.getLog(FlexMarketTestController.class);

	@Value("${de.unik.ines.soeasy.flex.admin.username}")
	private String admin_username;
	
	@Value("${de.unik.ines.soeasy.flex.admin.password}")
	private String admin_password;
	
	
	@Value("${de.unik.ines.soeasy.flex.user.username}")
	private String user_username;
	
	@Value("${de.unik.ines.soeasy.flex.user.password}")
	private String user_password;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	RoleRepository roleRepos;
	
	@Autowired
	FlexOfferRepository fowRepos;
	
	@Autowired
	ClearingInfoRepository clearingPriceRepos;
	
	@Autowired
	MMarketProductRepository mmproductRepos;

	@Autowired
	MarketProductRepository mproductRepos;
	
	@Autowired
	SimpleMatching clearingMethod;
	
	/**
	 * 
	 * @return
	 */
	// @PreAuthorize("#oauth2.hasScope('admin')")
	@ApiOperation(value = "Triggers flex market clearing", response = String.class)
	@RequestMapping(value ="/clear", method = RequestMethod.GET)
	public String clear(@RequestParam(name = "productid", defaultValue = "3") int productId) {

		log.info("Perform manual clearing...");

		// retrieve requests since lastClearingTime:
		// TODO consider only test offers
		List<FlexOfferWrapper> fows = fowRepos.findAllUnmatchedByProductAndIntervalTime(
				new DateTime().plusDays(1).withTimeAtStartOfDay().getMillis(),
				new DateTime().plusDays(2).withTimeAtStartOfDay().getMillis(), productId);
		ClearingInfo cinfo = new ClearingInfo(this.mmproductRepos.findById(productId).get().getProductPattern(),
				1000 * 60 * 5l,
				1000 * 60l);
		this.clearingMethod.clearMarket(null, fows, cinfo);
		return "Market cleared";
	}
}
