package de.unik.ines.soeasy.flex.offer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.soeasy.common.model.flex.offer.FlexOffer;
import de.unik.ines.soeasy.flex.FlexMarketApplication;
import de.unik.ines.soeasy.flex.exceptions.FlexBusinessValidationException;
import de.unik.ines.soeasy.flex.flex.FlexOfferValidator;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;
import energy.usef.core.exception.BusinessValidationException;

/**
 * 
 * JPA required to validate interval against market product.
 * 
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, JsonbAutoConfiguration.class })
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.flex", "de.unik.ines.soeasy.flex.config",
		"de.unik.ines.soeasy.flex.scheduling" })
@Import({ FlexOfferValidator.class })
public class FlexOfferJsonTest {
	
	FlexOffer flexOffer;

	@Autowired
	FlexOfferValidator foValidator;

	@BeforeEach
	void setup() throws Exception {
		// create FlexOffer JSON file
		this.flexOffer = FlexTestUtils.buildFlexOffer();

		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		
	    // convert map to JSON file
	    mapper.writeValue(Paths.get("flexoffer.json").toFile(), this.flexOffer);
	}

	@Test
	void testDefault() {
		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		
		try {
			FlexOffer readFlexOffer = mapper.readValue(Paths.get("flexoffer.json").toFile(), FlexOffer.class);
			assertEquals(this.flexOffer, readFlexOffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	void testMinimum() {
		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		
		try {
			FlexOffer readFlexOffer = mapper.readValue(Paths.get("flexoffer_sample_minimum.json").toFile(), FlexOffer.class);
			assertTrue(readFlexOffer!=null);
			assertEquals(readFlexOffer.getCongestionPoint().getEntityAddress(), "CongestionPointID");
			assertEquals(readFlexOffer.getOfferOptions().get(1).getIsps().get(2).getPower(), 800); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	void testValidation() throws BusinessValidationException {
		assertTrue(foValidator.validateFlexOffer(flexOffer));
	}

	@Test
	void testInvalidMarketProductId() throws BusinessValidationException {
		flexOffer.contractID = "NoMarketID";
		Exception exception = assertThrows(FlexBusinessValidationException.class, () -> {
			foValidator.validateFlexOffer(flexOffer);
		});
		assertTrue(exception.getMessage().contains("The given contract ID"), "No Market Product ID given");

		flexOffer.contractID = "MP_ID:99#SampleContractID";
		exception = assertThrows(FlexBusinessValidationException.class, () -> {
			foValidator.validateFlexOffer(flexOffer);
		});
		assertTrue(exception.getMessage().contains("The given market product ID"), "Market Product ID to high");
	}
}
