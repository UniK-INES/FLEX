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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.soeasy.common.model.flex.offer.FlexOffer;
import de.soeasy.common.model.flex.order.FlexOrder;
import de.unik.ines.soeasy.flex.FlexMarketApplication;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

/**
 * @author Sascha Holzhauer
 *
 */
class FlexOrderSerialisationTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testJsonSerialisation() throws JsonGenerationException, JsonMappingException, IOException {
		FlexOffer foffer = FlexTestUtils.buildFlexOffer();
		FlexOrder forder = FlexTestUtils.buildFlexOrderFromFlexOffer(foffer);
		
		 // convert map to JSON file
		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		mapper.writeValue(Paths.get("flexorder.json").toFile(), forder);
	    
		FlexOrder forderStored = mapper.readValue(Paths.get("flexorder.json").toFile(), FlexOrder.class);
		assertEquals("Check BigDecimal", forder.getOrderActivationFactor().getValue(),
				forderStored.getOrderActivationFactor().getValue());
		assertEquals("Deserialised FlexOrder should equals original", forder, forderStored);
	}

}
