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
package de.unik.ines.soeasy.flex.repos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;


/**
 * @author Sascha Holzhauer
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ClearingPriceReposTest {

	@Autowired protected WebApplicationContext wac;
	
	private MockMvc mockMvc;

	
	@Autowired
	private ClearingInfoRepository cpriceRepository;

	@Before
	public void deleteAllBeforeTests() throws Exception {
		cpriceRepository.deleteAll();
	}

	@Test
	@WithMockUser(roles="ADMIN")
	@Ignore // TODO check API
	public void shouldRetrieveEntity() throws Exception {

		mockMvc.perform(post("/cprices/").content(
				"{\"price\": \"9.99\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = "/price/all";
		mockMvc.perform(get(location)).andExpect(status().isOk()).andExpect(
				jsonPath("$.price").value(9.99f));
	}

}
