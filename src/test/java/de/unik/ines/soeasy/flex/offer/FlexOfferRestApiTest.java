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
package de.unik.ines.soeasy.flex.offer;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;

import org.apache.http.HttpStatus;
import org.joda.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.soeasy.common.model.MarketProductPattern;
import de.soeasy.common.model.api.EndpointsFlex;
import de.soeasy.common.model.flex.AcceptedRejectedType;
import de.soeasy.common.model.flex.ISO4217CurrencyType;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.offer.FlexOfferResponse;
import de.unik.ines.soeasy.flex.FlexMarketApplication;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;
import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.specification.RequestSpecification;

/**
 * @author Sascha Holzhauer
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class FlexOfferRestApiTest {

	private static RequestSpecification spec;

	@Autowired
	private Environment env;

	@Autowired
	protected MarketProductRepository mppRepos;

	static final String SERVER_NAME = "https://localhost";
	static final String USERNAME = "flex1";
	static final String PASSWORD = "flex!";

	protected MarketProductPattern mpp;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		this.mpp = FlexTestUtils.buildFlexMarketProductPattern();
		this.mppRepos.save(mpp);

		// setup rest-assured:
		PreemptiveBasicAuthScheme authenticationScheme = new PreemptiveBasicAuthScheme();
		authenticationScheme.setUserName(USERNAME);
		authenticationScheme.setPassword(PASSWORD);

		RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
				new Jackson2ObjectMapperFactory() {
				        @Override
					public ObjectMapper create(Type aClass, String s) {
						return FlexMarketApplication.getObjectMapper();
				        }
				    }
				));
		
		spec = new RequestSpecBuilder().setContentType(ContentType.JSON)
				.setBaseUri(SERVER_NAME + ":" + env.getProperty("local.server.port"))
				.setAuth(authenticationScheme)
				.setRelaxedHTTPSValidation()
				.addFilter(new ResponseLoggingFilter())
				.addFilter(new RequestLoggingFilter()).build();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}
	
	@Test
	void testValidFo() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		AcceptedRejectedType result = given().spec(spec).body(fo).when().post(EndpointsFlex.FLEX_HEMS_FLEXOFFER).then()
				.statusCode(HttpStatus.SC_OK).extract().as(FlexOfferResponse.class).getResult();
		assertEquals("Success message", AcceptedRejectedType.Accepted, result);
	}

	@Test
	void testInvalidCurrency() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		fo.setCurrency(new ISO4217CurrencyType("ECU"));
		checkBadRequest(fo, "Wrong currency", "The Currency is not the agreed value.");
	}

	@Test
	void testInvalidDurationFo() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		fo.setIspDuration(Duration.standardMinutes(5));
		checkBadRequest(fo, "Wrong ISP duration (FO).", "The ISP duration (300000) is not the agreed value (900000).");
	}

	@Test
	void testInvalidDurationMpp() {
		this.mpp.deliveryPeriodDuration = 600000;
		this.mppRepos.save(mpp);
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		checkBadRequest(fo, "Wrong ISP duration (MPP).",
				"The ISP duration (900000) is not the agreed value (600000).");
	}

	@Test
	void testIspStartIndexTooLow() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		fo.getOfferOptions().get(0).getIsps().get(0).setStart((short) 0);
		checkBadRequest(fo, "ISP start index too low.", "The ISP's start index (0) is out of range!");
	}

	@Test
	void testIspStartIndexTooHigh() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		fo.getOfferOptions().get(0).getIsps().get(0).setStart((short) 97);
		checkBadRequest(fo, "ISP start index too high.", "The ISP's start index (97) is out of range!");
	}

	@Test
	void testIspDurationIndexTooHigh() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		fo.getOfferOptions().get(0).getIsps().get(0).setDuration((short) 97);
		checkBadRequest(fo, "ISP duration index too high", "The ISP's index duration (97) is out of range!");
	}

	@Test
	void testIspDurationIndexTooLow() {
		FlexOffer fo = FlexTestUtils.buildFlexOffer();
		fo.getOfferOptions().get(0).getIsps().get(0).setDuration((short) 0);
		checkBadRequest(fo, "ISP duration index too low.", "The ISP's index duration (0) is out of range!");
	}

	/**
	 * @param fo
	 */
	private void checkBadRequest(FlexOffer fo, String explanation, String errorMessage) {
		FlexOfferResponse result = given().spec(spec).body(fo).when().post(EndpointsFlex.FLEX_HEMS_FLEXOFFER).then()
				.statusCode(HttpStatus.SC_BAD_REQUEST).extract().body().as(FlexOfferResponse.class);
		assertEquals(explanation, errorMessage, result.getRejectionReason());
	}
}
