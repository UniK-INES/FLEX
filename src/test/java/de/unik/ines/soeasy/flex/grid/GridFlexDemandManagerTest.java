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
package de.unik.ines.soeasy.flex.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.unik.ines.soeasy.flex.FlexMarketApplication;
import de.unik.ines.soeasy.flex.repos.GridFlexDemandSmdRepos;

/**
 * @author Sascha Holzhauer
 *
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, JsonbAutoConfiguration.class })
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.grid", "de.unik.ines.soeasy.flex.balance",
		"de.unik.ines.soeasy.flex.scheduling" })
class GridFlexDemandManagerTest {

	@Autowired
	GridFlexDemandManager gridFlexManager;

	@Autowired
	protected GridFlexDemandSmdRepos gridflexrepos;

	public static final String FILENAME_JSON_FLEXDEMAND_SMD = "json/GridFlexDemand_Schedule_MarketDocument.json";
	public static final String ID_JSON_FLEXDEMAND_SMD = "TestSmdId";

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void test() {
		Schedule_MarketDocument flexDemandSmd = null;
		ObjectMapper mapper = FlexMarketApplication.getObjectMapper();
		try {
			try {
				flexDemandSmd = mapper.readValue(
						new File(getClass().getClassLoader().getResource(FILENAME_JSON_FLEXDEMAND_SMD).toURI()),
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
		gridFlexManager.storeGridFlexDemandSmd(flexDemandSmd);

		assertTrue(gridflexrepos.existsById(ID_JSON_FLEXDEMAND_SMD));
		Schedule_MarketDocument smd = gridflexrepos.findById(ID_JSON_FLEXDEMAND_SMD).get();
		assertTrue(smd.equals(flexDemandSmd));

		AtomicInteger count = new AtomicInteger(0);
		gridflexrepos.findAll().forEach(action -> count.incrementAndGet());
		assertEquals(1, count.intValue());
	}
}
