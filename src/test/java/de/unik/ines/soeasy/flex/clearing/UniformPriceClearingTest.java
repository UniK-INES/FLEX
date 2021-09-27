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
package de.unik.ines.soeasy.flex.clearing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;

import de.soeasy.common.model.MarketProductPattern;
import de.unik.ines.soeasy.flex.clearing.uniform.UniformPriceClearing;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;

/**
 * @author Sascha Holzhauer
 * 
 *         TODO adapt to FlexOffer
 * 
 *         Cut information with respect to energy on x axis
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {
		"de.unik.ines.soeasy.flex.repos", "de.unik.ines.soeasy.flex.model",
		"de.unik.ines.soeasy.flex.clearing", "de.unik.ines.soeasy.flex.scheduling" })
public class UniformPriceClearingTest {
	
	MarketProductPattern marketProductPattern = new MarketProductPattern();
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	MarketProductRepository mproductRepos;
	
	@Autowired
	MMarketProductRepository mmproductRepos;
	
	@Autowired
	MarketEnergyRequestRepository requestRepos;
	
	@Autowired
	UniformPriceClearing clearingMethod;
	
	@Autowired
	ClearingInfoRepository cInfoRepos;
	
	protected int maxMRequestId = 0;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		marketProductPattern = new MarketProductPattern();
		marketProductPattern.productId = 2;
		marketProductPattern.firstDeliveryPeriodStart = 1000*60;
		marketProductPattern.deliveryPeriodDuration = 1000*60*5;
		marketProductPattern.openingTime = "-1M";
		marketProductPattern.closingTime = "-1M";
		marketProductPattern.auctionInterval = "0";
		marketProductPattern.minPrice = -3000.0f;
		marketProductPattern.maxPrice = 3000.0f;
		marketProductPattern = mproductRepos.save(marketProductPattern);
	}

	@AfterEach
	public void cleanUp() throws Exception {
		cInfoRepos.deleteAll();
		mmproductRepos.deleteAll();
		mproductRepos.delete(marketProductPattern);
	}
	
//	private FlexOfferWrapper getRequest(float price, float energy, int id) {
//		FlexOffer fo = FlexTestUtils.buildFlexOffer();
//		this.maxMRequestId = Math.max(id, maxMRequestId);
//		fo.getOfferOptions().get(0).setPrice(new CurrencyAmountType(new BigDecimal(price)));
//		fo.energyRequested = energy;
//		fo.startTime = 1000 * 60;
//		fo.endTime = 1000 * 60 * 6;
//		return new MarketEnergyRequest(id, userRepos.findByName("enavi"), fo);
//	}
//	
//	@SuppressWarnings("unused")
//	private MarketEnergyRequest getRequest(float price, float energy) {
//		this.maxMRequestId++;
//		return this.getRequest(price, energy, this.maxMRequestId);
//	}
//	
//
//	protected void assertUpdatingRequest(long id, float clearedPrice, float acceptedEnergy,
//			EnergyRequest.Status status) {
//		assertEquals(status, requestRepos.findById(id).get().getStatus());
//		assertEquals(acceptedEnergy, requestRepos.findById(id).get().getRequest().energyAccepted, 0.001);
//		assertEquals(clearedPrice, requestRepos.findById(id).get().getRequest().priceCleared, 0.001);
//	}
//	
//	/*******************************************
//	 * NORMAL CASES
//	 ******************************************/
//	
//	/**
//	 * Demand is vertically cut by supply.
//	 * Supply point after intersection is below demand point at intersection.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutV_SnextBelowDcurr() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 4000.0f, 11));
//		requests.add(getRequest(0.12f, 4000.0f, 12)); // different to test B here
//		requests.add(getRequest(0.09f, 3000.0f, 13));
//
//		requests.add(getRequest(0.10f, -3000.0f, 14));
//		requests.add(getRequest(0.13f, -7000.0f, 15));
//		requests.add(getRequest(0.15f, -2500.0f, 16));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.13f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(11, 0.13f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(12, 0.13f, 0f, Status.DECLINED);
//		assertUpdatingRequest(13, 0.13f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(14, 0.13f, -3000f, Status.ACCEPTED);
//		assertUpdatingRequest(15, 0.13f, -1000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(16, 0.13f, 0f, Status.DECLINED);
//	}
//
//	/**
//	 * Demand is vertically cut by supply.
//	 * Supply point after intersection is left to next demand point after intersection.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutV_SnextLeftDnext() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 4000.0f, 101));
//		requests.add(getRequest(0.12f, 4000.0f, 102)); // different to test B here
//		requests.add(getRequest(0.09f, 3000.0f, 103));
//
//		requests.add(getRequest(0.10f, -3000.0f, 104));
//		requests.add(getRequest(0.13f, -3000.0f, 105));
//		requests.add(getRequest(0.15f, -6500.0f, 106));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.13f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(101, 0.13f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(102, 0.13f, 0f, Status.DECLINED);
//		assertUpdatingRequest(103, 0.13f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(104, 0.13f, -3000f, Status.ACCEPTED);
//		assertUpdatingRequest(105, 0.13f, -1000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(106, 0.13f, 0f, Status.DECLINED);		
//	}
//	
//	/**
//	 * Demand is vertically cut by supply.
//	 * Supply point after intersection is left to next demand point after intersection.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutV_SaboveDbeforeCut() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 4000.0f, 201));
//		requests.add(getRequest(0.12f, 4000.0f, 202)); // different to test B here
//		requests.add(getRequest(0.09f, 3000.0f, 203));
//
//		requests.add(getRequest(0.10f, -5000.0f, 204));
//		requests.add(getRequest(0.11f, -1000.0f, 205));
//		requests.add(getRequest(0.15f, -6500.0f, 206));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.12f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(6000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(201, 0.12f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(202, 0.12f, 2000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(203, 0.12f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(204, 0.12f, -5000f, Status.ACCEPTED);
//		assertUpdatingRequest(205, 0.12f, -1000f, Status.ACCEPTED);
//		assertUpdatingRequest(206, 0.12f, 0f, Status.DECLINED);	
//	}
//	
//	
//	/**
//	 * Demand is vertically cut by supply.
//	 * Supply point after intersection is left to next demand point after intersection.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutV_SnextAboveDcurr() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.14f, 4000.0f, 301));
//		requests.add(getRequest(0.12f, 4000.0f, 302)); // different to test B here
//		requests.add(getRequest(0.09f, 3000.0f, 303));
//
//		requests.add(getRequest(0.10f, -3000.0f, 304));
//		requests.add(getRequest(0.13f, -3000.0f, 305));
//		requests.add(getRequest(0.15f, -6500.0f, 306));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.13f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(301, 0.13f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(302, 0.13f, 0f, Status.DECLINED);
//		assertUpdatingRequest(303, 0.13f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(304, 0.13f, -3000f, Status.ACCEPTED);
//		assertUpdatingRequest(305, 0.13f, -1000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(306, 0.13f, 0f, Status.DECLINED);	
//	}
//	
//	/**
//	 * Demand is horizontally cut by supply.
//	 * Test method for {@link de.unik.ines.soeasy.flex.util.PriceEnergyFunction#getIntersectionPrice(de.unik.ines.soeasy.flex.util.PriceEnergyFunction)}.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutH_SnextBelowDcurr() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 4000.0f, 401));
//		requests.add(getRequest(0.12f, 9000.0f, 402));
//		requests.add(getRequest(0.09f, 3000.0f, 403));
//		
//		requests.add(getRequest(0.10f, -7000.0f, 404));
//		requests.add(getRequest(0.13f, -3000.0f, 405));
//		requests.add(getRequest(0.15f, -2500.0f, 406));
//		
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.12f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(7000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(401, 0.12f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(402, 0.12f, 3000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(403, 0.12f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(404, 0.12f, -7000f, Status.ACCEPTED);
//		assertUpdatingRequest(405, 0.12f, -0f, Status.DECLINED);
//		assertUpdatingRequest(406, 0.12f, 0f, Status.DECLINED);	
//	}
//	
//	/**
//	 * Next demand point after intersection is left to next supply point after intersection.
//	 * Demand is cut horizontally by supply.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutH_DnextLeftSnext() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 4000.0f, 501));
//		requests.add(getRequest(0.12f, 4000.0f, 502)); // different to test B here
//		requests.add(getRequest(0.09f, 3000.0f, 503));
//
//		requests.add(getRequest(0.10f, -7000.0f, 504));
//		requests.add(getRequest(0.13f, -3000.0f, 505));
//		requests.add(getRequest(0.15f, -2500.0f, 506));
//
//		requestRepos.saveAll(requests);
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.12f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(7000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(501, 0.12f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(502, 0.12f, 3000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(503, 0.12f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(504, 0.12f, -7000f, Status.ACCEPTED);
//		assertUpdatingRequest(505, 0.12f, -0f, Status.DECLINED);
//		assertUpdatingRequest(506, 0.12f, 0f, Status.DECLINED);	
//	}
//	
//
//	/**
//	 * Next demand point after intersection is left to next supply point after intersection.
//	 * Demand is cut horizontally by supply.
//	 * Demand and supply is split in equal parts compared to previous text.
//	 */
//	@Test
//	public void testGetIntersectionPrice_DcutH_DnextLeftSnext_SplitRequests() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 4000.0f, 601));
//		requests.add(getRequest(0.12f, 2000.0f, 602));
//		requests.add(getRequest(0.12f, 1000.0f, 603));
//		requests.add(getRequest(0.12f, 1000.0f, 604));
//		requests.add(getRequest(0.09f, 3000.0f, 605));
//
//		requests.add(getRequest(0.10f, -7000.0f, 606));
//		requests.add(getRequest(0.13f, -1000.0f, 607));
//		requests.add(getRequest(0.13f,  -500.0f, 608));
//		requests.add(getRequest(0.13f, -1500.0f, 609));
//		requests.add(getRequest(0.15f, -2500.0f, 610));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.12f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(7000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(601, 0.12f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(602, 0.12f, 2000f, Status.ACCEPTED);
//		assertUpdatingRequest(603, 0.12f, 1000f, Status.ACCEPTED);
//		assertUpdatingRequest(604, 0.12f, 0f, Status.DECLINED);
//		assertUpdatingRequest(605, 0.12f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(606, 0.12f, -7000f, Status.ACCEPTED);
//		assertUpdatingRequest(607, 0.12f, -0f, Status.DECLINED);
//		assertUpdatingRequest(608, 0.12f, 0f, Status.DECLINED);
//		assertUpdatingRequest(609, 0.12f, -0f, Status.DECLINED);
//		assertUpdatingRequest(610, 0.12f, 0f, Status.DECLINED);	
//	}
//	
//	/**
//	 * Supply and Demand match at intersection.
//	 * 
//	 * Test method for {@link de.unik.ines.soeasy.flex.util.PriceEnergyFunction#getIntersectionPrice(de.unik.ines.soeasy.flex.util.PriceEnergyFunction)}.
//	 */
//	@Test
//	public void testGetIntersectionPrice_SDmatch() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.17f, 3.0f, 701));
//		requests.add(getRequest(0.14f, 2.0f, 702));
//		requests.add(getRequest(0.13f, 1.0f, 703));
//		requests.add(getRequest(0.11f, 2.0f, 704));
//
//		requests.add(getRequest(0.0f, -2.0f, 705));
//		requests.add(getRequest(0.13f, -1.0f, 706));
//		requests.add(getRequest(0.14f, -1.0f, 707));
//		requests.add(getRequest(0.16f, -2.0f, 708));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.14f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(701, 0.14f, 3f, Status.ACCEPTED);
//		assertUpdatingRequest(702, 0.14f, 1f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(703, 0.14f, 0f, Status.DECLINED);
//		assertUpdatingRequest(704, 0.14f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(705, 0.14f, -2f, Status.ACCEPTED);
//		assertUpdatingRequest(706, 0.14f, -1f, Status.ACCEPTED);
//		assertUpdatingRequest(707, 0.14f, -1f, Status.ACCEPTED);
//		assertUpdatingRequest(708, 0.14f, 0f, Status.DECLINED);	
//	}
//	
//	
//	/*******************************************
//	 * EXTREME CASES
//	 ******************************************/
//	
//	/**
//	 * Extreme case: supply completely under demand (highest offer determines).
//	 */
//	@Test
//	public void testGetIntersectionPriceExtreme_SupplyUnderDemand() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.21f, 4000.0f, 801));
//		requests.add(getRequest(0.19f, 4000.0f, 802));
//		requests.add(getRequest(0.19f, 3000.0f, 803));
//		
//		requests.add(getRequest(0.10f, -7000.0f, 804));
//		requests.add(getRequest(0.13f, -2000.0f, 805));
//		requests.add(getRequest(0.15f, -2500.0f, 806));
//		
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.15f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(11000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(801, 0.15f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(802, 0.15f, 4000f, Status.ACCEPTED);
//		assertUpdatingRequest(803, 0.15f, 3000f, Status.ACCEPTED);
//		
//		assertUpdatingRequest(804, 0.15f, -7000f, Status.ACCEPTED);
//		assertUpdatingRequest(805, 0.15f, -2000f, Status.ACCEPTED);
//		assertUpdatingRequest(806, 0.15f, -2000f, Status.PARTLY_ACCEPTED);	
//	}
//	
//	/**
//	 * Extreme case: supply completely over demand (no trade).
//	 */
//	@Test
//	public void testGetIntersectionPriceExtreme_SupplyOverDemand() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.11f, 4000.0f, 901));
//		requests.add(getRequest(0.10f, 4000.0f, 902));
//		requests.add(getRequest(0.09f, 3000.0f, 903));
//		
//		requests.add(getRequest(0.13f, -7000.0f, 904));
//		requests.add(getRequest(0.14f, -2000.0f, 905));
//		requests.add(getRequest(0.15f, -2500.0f, 906));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(Float.NaN, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(0.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(901, Float.NaN, 0f, Status.DECLINED);
//		assertUpdatingRequest(902, Float.NaN, 0f, Status.DECLINED);
//		assertUpdatingRequest(903, Float.NaN, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(904, Float.NaN, 0f, Status.DECLINED);
//		assertUpdatingRequest(905, Float.NaN, 0f, Status.DECLINED);
//		assertUpdatingRequest(906, Float.NaN, 0f, Status.DECLINED);
//	}
//	
//	/**
//	 * Extreme case: no supply.
//	 */
//	@Test
//	public void testGetIntersectionPriceExtreme_OnlyDemand() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.21f, 4000.0f, 1001));
//		requests.add(getRequest(0.19f, 4000.0f, 1002));
//		requests.add(getRequest(0.19f, 3000.0f, 1003));
//		
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(Float.NaN, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(0.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1001, Float.NaN, 0f, Status.DECLINED);
//		assertUpdatingRequest(1002, Float.NaN, 0f, Status.DECLINED);
//		assertUpdatingRequest(1003, Float.NaN, 0f, Status.DECLINED);
//	}
//	
//	/**
//	 * Extreme case: supply prices completely under demand but not enough supplied.
//	 */
//	@Test
//	public void testGetIntersectionPriceExtreme_InsufficientSupplyUnderDemand() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 3000.0f, 1101));
//		
//		requests.add(getRequest(0.13f, -2000.0f, 1102));
//		
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.19f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(2000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1101, 0.19f, 2000f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(1102, 0.19f, -2000f, Status.ACCEPTED);
//	
//	}
//
//	/**
//	 * Extreme case: supply prices completely under demand prices, supplied energy equals demand.
//	 */
//	@Test
//	public void testGetIntersectionPriceExtreme_SupplyUnderDemandEnergyEqual() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.19f, 3000.0f, 1201));
//		
//		requests.add(getRequest(0.13f, -3000.0f, 1202));
//		
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.19f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(3000.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1201, 0.19f, 3000f, Status.ACCEPTED);
//		assertUpdatingRequest(1202, 0.19f, -3000f, Status.ACCEPTED);
//	
//	}
//	
//	@Test
//	public void testGetIntersectionPrice_SingleDemand() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.14f, 4.0f, 1601));
//
//		requests.add(getRequest(0.0f, -2.0f, 1605));
//		requests.add(getRequest(0.13f, -1.0f, 1606));
//		requests.add(getRequest(0.14f, -1.0f, 1607));
//		requests.add(getRequest(0.16f, -2.0f, 1608));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.14f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1601, 0.14f, 4f, Status.ACCEPTED);
//		
//		assertUpdatingRequest(1605, 0.14f, -2f, Status.ACCEPTED);
//		assertUpdatingRequest(1606, 0.14f, -1f, Status.ACCEPTED);
//		assertUpdatingRequest(1607, 0.14f, -1f, Status.ACCEPTED);
//		assertUpdatingRequest(1608, 0.14f, 0f, Status.DECLINED);	
//	}
//	
//	@Test
//	public void testGetIntersectionPrice_SingleDemandSmall() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.14f, 2.5f, 1611));
//
//		requests.add(getRequest(0.0f, -2.0f, 1615));
//		requests.add(getRequest(0.13f, -1.0f, 1616));
//		requests.add(getRequest(0.14f, -1.0f, 1617));
//		requests.add(getRequest(0.16f, -2.0f, 1618));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.13f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(2.5f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1611, 0.13f, 2.5f, Status.ACCEPTED);
//		
//		assertUpdatingRequest(1615, 0.13f, -2f, Status.ACCEPTED);
//		assertUpdatingRequest(1616, 0.13f, -0.5f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(1617, 0.13f, 0f, Status.DECLINED);
//		assertUpdatingRequest(1618, 0.13f, 0f, Status.DECLINED);	
//	}
//	
//	@Test
//	public void testGetIntersectionPrice_SingleDemandAbove() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.17f, 3.0f, 1701));
//
//		requests.add(getRequest(0.0f, -2.0f, 1705));
//		requests.add(getRequest(0.13f, -1.0f, 1706));
//		requests.add(getRequest(0.14f, -1.0f, 1707));
//		requests.add(getRequest(0.16f, -2.0f, 1708));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.13f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(3.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1701, 0.13f, 3f, Status.ACCEPTED);
//		
//		assertUpdatingRequest(1705, 0.13f, -2f, Status.ACCEPTED);
//		assertUpdatingRequest(1706, 0.13f, -1f, Status.ACCEPTED);
//		assertUpdatingRequest(1707, 0.13f, 0f, Status.DECLINED);
//		assertUpdatingRequest(1708, 0.13f, 0f, Status.DECLINED);	
//	}
//	@Test
//	public void testGetIntersectionPrice_SingleSupply() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.17f, 3.0f, 1801));
//		requests.add(getRequest(0.14f, 2.0f, 1802));
//		requests.add(getRequest(0.13f, 1.0f, 1803));
//		requests.add(getRequest(0.11f, 2.0f, 1804));
//
//		requests.add(getRequest(0.13f, -5.5f, 1805));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.13f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(5.5f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1801, 0.13f, 3f, Status.ACCEPTED);
//		assertUpdatingRequest(1802, 0.13f, 2f, Status.ACCEPTED);
//		assertUpdatingRequest(1803, 0.13f, 0.5f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(1804, 0.13f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(1805, 0.13f, -5.5f, Status.ACCEPTED);
//	}
//	
//	@Test
//	public void testGetIntersectionPrice_SingleSupplySmall() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.17f, 3.0f, 1811));
//		requests.add(getRequest(0.14f, 2.0f, 1812));
//		requests.add(getRequest(0.13f, 1.0f, 1813));
//		requests.add(getRequest(0.11f, 2.0f, 1814));
//
//		requests.add(getRequest(0.13f, -4.0f, 1815));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.14f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1811, 0.14f, 3f, Status.ACCEPTED);
//		assertUpdatingRequest(1812, 0.14f, 1f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(1813, 0.14f, 0f, Status.DECLINED);
//		assertUpdatingRequest(1814, 0.14f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(1815, 0.14f, -4f, Status.ACCEPTED);
//	}
//	
//	@Test
//	public void testGetIntersectionPrice_SingleSupplySingleDemand() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//
//		requests.add(getRequest(0.14f, 132.2797f, 1821));
//
//		requests.add(getRequest(0.14f, -198.41954f, 1822));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.14f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(132.2797f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1821, 0.14f, 132.2797f, Status.ACCEPTED);
//		assertUpdatingRequest(1822, 0.14f, -132.2797f, Status.PARTLY_ACCEPTED);
//	}
//	
//	
//	@Test
//	public void testGetIntersectionPrice_SingleSupplyBelow() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.17f, 3.0f, 1901));
//		requests.add(getRequest(0.14f, 2.0f, 1902));
//		requests.add(getRequest(0.13f, 1.0f, 1903));
//		requests.add(getRequest(0.11f, 2.0f, 1904));
//
//		requests.add(getRequest(0.09f, -4.0f, 1905));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.14f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4.0f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(1901, 0.14f, 3f, Status.ACCEPTED);
//		assertUpdatingRequest(1902, 0.14f, 1f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(1903, 0.14f, 0f, Status.DECLINED);
//		assertUpdatingRequest(1904, 0.14f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(1905, 0.14f, -4f, Status.ACCEPTED);
//	}
//	
//	@Test
//	public void testGetIntersectionPrice_SpecialA() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.7f, 8.97852f, 2001));
//
//		requests.add(getRequest(0.8f, -7.92222f, 2005));
//		requests.add(getRequest(0.4f, -4.75333f, 2006));
//		requests.add(getRequest(1.2f, -7.13f, 2007));
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.7f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(4.75333f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(2001, 0.7f, 4.75333f, Status.PARTLY_ACCEPTED);
//		
//		assertUpdatingRequest(2005, 0.7f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2006, 0.7f, -4.75333f, Status.ACCEPTED);
//		assertUpdatingRequest(2007, 0.7f, 0f, Status.DECLINED);
//	}
//	
//	@Test
//	public void test04_22() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.07f, -0.85274667f, 2101));
//		requests.add(getRequest(0.18f, -2.5889068f, 2102));
//		requests.add(getRequest(0.18f, -3.42374f, 2103));
//		requests.add(getRequest(0.19f, -2.8980978f, 2104));
//		requests.add(getRequest(0.09f, -5.697067f, 2105));
//		requests.add(getRequest(0.08f, 13.96591f, 2106));
//		requests.add(getRequest(0.09f, -8.545565f, 2107));
//		
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.08f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(0.85274667f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(2101, 0.08f, -0.85274667f, Status.ACCEPTED);
//		assertUpdatingRequest(2102, 0.08f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2103, 0.08f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2104, 0.08f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2105, 0.08f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2106, 0.08f, 0.85274667f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(2107, 0.08f, 0f, Status.DECLINED);
//	}
//	
//	@Test
//	public void test_23() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.18f, -0.09244444f, 2111));
//		requests.add(getRequest(0.09f, -0.04332f, 2112));
//		
//		requests.add(getRequest(0.16f, 0.10120889f, 2113));
//		requests.add(getRequest(0.08f, 139.96591f, 2114));
//		requests.add(getRequest(0.11f, 473.79984f, 2115));
//		requests.add(getRequest(0.10f, 2.4287288f, 2116));
//		
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.16f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(0.04332f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(2111, 0.16f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2112, 0.16f, -0.04332f, Status.ACCEPTED);
//		
//		assertUpdatingRequest(2113, 0.16f, 0.04332f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(2114, 0.16f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2115, 0.16f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2116, 0.16f, 0f, Status.DECLINED);
//	}
//	
//	@Test
//	public void test_24() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.14f, -198.41954f, 2121));
//		requests.add(getRequest(0.16f, -145.82182f, 2122));
//		requests.add(getRequest(0.15f, -328.75208f, 2123));
//		
//		requests.add(getRequest(0.19f, 4.5478754f, 2124));
//		requests.add(getRequest(0.14f, 56.151894f, 2125));		
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.14f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(60.699768f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(2121, 0.14f, -60.699768f, Status.PARTLY_ACCEPTED);
//		assertUpdatingRequest(2122, 0.14f, 0f, Status.DECLINED);
//		assertUpdatingRequest(2123, 0.14f, 0f, Status.DECLINED);
//		
//		assertUpdatingRequest(2124, 0.14f, 4.5478754f, Status.ACCEPTED);
//		assertUpdatingRequest(2125, 0.14f, 56.151894f, Status.ACCEPTED);
//	}
//	
//	@Test
//	public void test_partlyAccepted() {
//		List<MarketEnergyRequest> requests = new ArrayList<>();
//		
//		requests.add(getRequest(0.18f, 2.31776f, 2131));
//		requests.add(getRequest(0.17f, 3.06457f, 2132));
//		requests.add(getRequest(0.15f, 1.97251f, 2133));
//		requests.add(getRequest(0.13f, 2.60953f, 2134));
//		requests.add(getRequest(0.13f, 0.756628f, 2135));
//
//		requests.add(getRequest(0.101169f, -4.34572f, 2136));
//		requests.add(getRequest(0.111169f, -2.89715f, 2137));
//		requests.add(getRequest(0.121169f, -21.7286f, 2138));		
//
//		requestRepos.saveAll(requests);	
//		ClearingInfo cinfo = new ClearingInfo(this.marketProductPattern, 1000*60*5l, 1000*60l);
//		this.clearingMethod.clearMarket(requests, cinfo);
//
//		assertEquals(0.121169f, cinfo.getPriceCleared(), 0.0001f);
//		assertEquals(10.721f, cinfo.getEnergyCleared(), 0.0001f);
//		
//		// check updating of requests:
//		assertUpdatingRequest(2131, 0.121169f, 2.31776f, Status.ACCEPTED);
//		assertUpdatingRequest(2132, 0.121169f, 3.06457f, Status.ACCEPTED);
//		assertUpdatingRequest(2133, 0.121169f, 1.97251f, Status.ACCEPTED);
//		assertUpdatingRequest(2134, 0.121169f, 2.60953f, Status.ACCEPTED);
//		assertUpdatingRequest(2135, 0.121169f, 0.756628f, Status.ACCEPTED);
//		
//		assertUpdatingRequest(2136, 0.121169f, -4.34572f, Status.ACCEPTED);
//		assertUpdatingRequest(2137, 0.121169f, -2.89715f, Status.ACCEPTED);
//		assertUpdatingRequest(2138, 0.121169f, -3.47813f, Status.PARTLY_ACCEPTED);
//	}
}
