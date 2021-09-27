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
package de.unik.ines.soeasy.flex.order;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.soeasy.common.model.api.EndpointsFlex;
import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.order.FlexOrder;
import de.unik.ines.soeasy.flex.FlexMarketApplication;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexApiError;
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
 * REST API tests
 * 
 * JPA tests
 * 
 * @author Sascha Holzhauer Invalid date format
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class FlexOrderTest {

	static final String SERVER_NAME = "https://localhost";
	static final String USERNAME = "flex1";
	static final String PASSWORD = "flex!";

	private static RequestSpecification spec;

	@Autowired
	private Environment env;

	@Autowired
	FlexOfferRepository foRepos;

	@Autowired
	FlexOrderRepository forderRepos;

	@Autowired
	UserAccountRepository userRepos;

	FlexOffer flexOffer1, flexOffer2, flexOffer3;
	FlexOrder flexOrder1, flexOrder2, flexOrder3;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		UserAccount flex1 = userRepos.findByName("flex1");
		// store FOffers
		this.flexOffer1 = FlexTestUtils.buildFlexOffer();
		this.flexOffer1.setPeriod(new Interval(new DateTime().plusDays(2).withTimeAtStartOfDay(),
				new DateTime().plusDays(3).withTimeAtStartOfDay()));
		foRepos.save(new FlexOfferWrapper(flexOffer1, flex1));

		this.flexOffer2 = FlexTestUtils.buildFlexOffer();
		foRepos.save(new FlexOfferWrapper(flexOffer2, flex1));

		this.flexOffer3 = FlexTestUtils.buildFlexOffer();
		foRepos.save(new FlexOfferWrapper(flexOffer3, flex1));

		// generate FOrders
		this.flexOrder1 = FlexTestUtils.buildFlexOrderFromFlexOffer(flexOffer1);
		this.forderRepos.save(flexOrder1);

		this.flexOrder2 = FlexTestUtils.buildFlexOrderFromFlexOffer(flexOffer2);
		this.forderRepos.save(flexOrder2);

		this.flexOrder3 = FlexTestUtils.buildFlexOrderFromFlexOffer(flexOffer3);
		this.forderRepos.save(flexOrder3);


		// setup rest-assured:
		PreemptiveBasicAuthScheme authenticationScheme = new PreemptiveBasicAuthScheme();
		authenticationScheme.setUserName(USERNAME);
		authenticationScheme.setPassword(PASSWORD);

		RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
				new ObjectMapperConfig().jackson2ObjectMapperFactory(new Jackson2ObjectMapperFactory() {
					@Override
					public ObjectMapper create(Type aClass, String s) {
						return FlexMarketApplication.getObjectMapper();
					}
				}));

		spec = new RequestSpecBuilder().setContentType(ContentType.JSON)
				.setBaseUri(SERVER_NAME + ":" + env.getProperty("local.server.port")).setAuth(authenticationScheme)
				.setRelaxedHTTPSValidation().addFilter(new ResponseLoggingFilter())
				.addFilter(new RequestLoggingFilter()).build();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		this.forderRepos.deleteAll();
	}

	@Test
	void testValidSingle() {
		FlexOrder result = given().spec(spec).param("foMessageId", this.flexOffer1.getMessageID()).when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_ID).then().statusCode(HttpStatus.SC_OK).extract().body()
				.as(FlexOrder.class);
		assertEquals("Valid FlexOrder request", this.flexOrder1, result);
	}

	@Test
	void testUniqueMessageId() {
		FlexOrder flexOrder2 = FlexTestUtils.buildFlexOrderFromFlexOffer(this.flexOffer1);
		Exception exception = assertThrows(DataIntegrityViolationException.class, () -> {
			this.forderRepos.save(flexOrder2);
		});

		String expectedMessage = "could not execute statement";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testInvalidMessageIDSingle() {
		FlexApiError result = given().spec(spec).param("foMessageId", "InvalidMessageId").when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_ID).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract()
				.body().as(FlexApiError.class);
		assertEquals("Invalid FlexOrder request (non-existing messageID)",
				"Invalid UUID string: InvalidMessageId", result.getDebugMessage());
	}

	@Test
	void testMissingMessageIDSingle() {
		String invalidMessageId = UUID.randomUUID().toString();
		FlexApiError result = given().spec(spec).param("foMessageId", invalidMessageId).when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_ID).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract().body()
				.as(FlexApiError.class);
		assertEquals("Invalid FlexOrder request (non-existing messageID)",
				"The MessageID (" + invalidMessageId + ") is not existing!", result.getDebugMessage());
	}

	@Test
	void testValidDate() {
		

		FlexOrder[] fos = given().spec(spec)
				.param("date", DateTimeFormat.forPattern("yyyy-MM-dd").print((new DateTime()).plusDays(1))).when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_DATE).then().statusCode(HttpStatus.SC_OK).extract().body()
				.as(FlexOrder[].class);
		List<FlexOrder> result = Arrays.asList(fos);

		// BigDecimal deserialization does not work properly here:
		// List<FlexOrder> result = given().spec(spec)
		// .param("date", DateTimeFormat.forPattern("yyyy-MM-dd").print((new
		// DateTime()).plusDays(1))).when()
		// .get(EndpointsFlex.FLEX_HEMS_FLEXORDER_DATE).then().statusCode(HttpStatus.SC_OK).extract().body()
		// .jsonPath().getList(".", FlexOrder.class);

		assertEquals("Valid FlexOrder request", Set.of(this.flexOrder3, this.flexOrder2),
				new HashSet<FlexOrder>(result));
	}

	@Test
	void testEmptyListTooLateDate() {
		List<?> result = given().spec(spec)
				.param("date", DateTimeFormat.forPattern("yyyy-MM-dd").print((new DateTime()).minusDays(1))).when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_DATE).then().statusCode(HttpStatus.SC_OK).extract()
				.body()
				.as(List.class);
		assertEquals("Valid FlexOrder request", 0, result.size());
	}

	@Test
	void testEmptyListTooEarlyDate() {
		List<?> result = given().spec(spec)
				.param("date", DateTimeFormat.forPattern("yyyy-MM-dd").print((new DateTime()).plusDays(3))).when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_DATE).then().statusCode(HttpStatus.SC_OK).extract()
				.body()
				.as(List.class);
		assertEquals("Valid FlexOrder request", 0, result.size());
	}

	@Test
	void testInvalidFormatDate() {
		FlexApiError result = given().spec(spec)
				.param("date", DateTimeFormat.forPattern("dd-MM-yyyy").print((new DateTime()).plusDays(1))).when()
				.get(EndpointsFlex.FLEX_HEMS_FLEXORDER_DATE).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract()
				.body().as(FlexApiError.class);
		assertEquals("nvalid FlexOrder request (invalid date format)",
				"Invalid date format (" + DateTimeFormat.forPattern("dd-MM-yyyy").print((new DateTime()).plusDays(1))
						+ ") - should be yyyy-MM-dd)",
				result.getDebugMessage());
	}
}
