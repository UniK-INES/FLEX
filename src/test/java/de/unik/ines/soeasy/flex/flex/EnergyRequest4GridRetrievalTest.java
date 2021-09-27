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
package de.unik.ines.soeasy.flex.flex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;

import de.soeasy.common.model.EnergyRequest;
import de.soeasy.common.model.EnergyRequest.Status;
import de.unik.ines.soeasy.flex.model.GridData;
import de.unik.ines.soeasy.flex.model.MarketEnergyRequest;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.repos", "de.unik.ines.soeasy.flex.model" })
public class EnergyRequest4GridRetrievalTest {

	@Autowired
	MarketEnergyRequestRepository requestRepos;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	FlexOfferRepository flexOfferRepos;

	GridData gridData;
	
	static final String LOCATION_1 = "Transformer01";
	static final String LOCATION_2 = "Transformer02";
	
	protected int maxMRequestId = 0;
	
	@BeforeEach
	public void setUp() throws Exception {
		List<MarketEnergyRequest> requests = new ArrayList<>();

		UserAccount user1 = new UserAccount("User_" + LOCATION_1, LOCATION_1);
		UserAccount user2 = new UserAccount("User_" + LOCATION_2, LOCATION_2);
		
		flexOfferRepos.deleteAll();
		requestRepos.deleteAll();
		userRepos.deleteAll();
		userRepos.save(user1);
		userRepos.save(user2);		
		
		requests.add(getRequest(0.19f, 4000.0f, 1, 1000*60, 1000*60*6, EnergyRequest.Status.ACCEPTED, user1));
		requests.add(getRequest(0.12f, 4000.0f, 2, 1000*60, 1000*60*6, EnergyRequest.Status.PARTLY_ACCEPTED, user1));
		requests.add(getRequest(0.09f, 3000.0f, 3, 1000*60, 1000*60*6, EnergyRequest.Status.DECLINED, user1));
		requests.add(getRequest(0.09f, 3000.0f, 4, 1000*60, 1000*60*6, EnergyRequest.Status.UNHANDLED, user1));
		requests.add(getRequest(0.09f, 3000.0f, 5, 1000*60, 1000*60*6, EnergyRequest.Status.INVALID, user1));
		requests.add(getRequest(0.19f, 4000.0f, 6, 1000*60*10, 1000*60*16, EnergyRequest.Status.DECLINED, user1));
		requests.add(getRequest(0.19f, 4000.0f, 7, 1000*60*4, 1000*60*10, EnergyRequest.Status.DECLINED, user1));
		requests.add(getRequest(0.19f, 4000.0f, 8, 1000*60, 1000*60*6, EnergyRequest.Status.DECLINED, user2));
		requests.add(getRequest(0.19f, 4000.0f, 9, 1000*60, 1000*60*10, EnergyRequest.Status.DECLINED, user1));
		
		requests.add(getRequest(0.10f, -3000.0f, 10, 1000*60, 1000*60*6, EnergyRequest.Status.ACCEPTED, user1));
		requests.add(getRequest(0.13f, -7000.0f, 11, 1000*60, 1000*60*6, EnergyRequest.Status.PARTLY_ACCEPTED, user1));
		requests.add(getRequest(0.15f, -2500.0f, 12, 1000*60, 1000*60*6, EnergyRequest.Status.DECLINED, user1));
		requests.add(getRequest(0.09f, -3000.0f, 13, 1000*60, 1000*60*6, EnergyRequest.Status.UNHANDLED, user1));
		requests.add(getRequest(0.09f, -3000.0f, 14, 1000*60, 1000*60*6, EnergyRequest.Status.INVALID, user1));
		requests.add(getRequest(0.10f, -3000.0f, 15, 1000*60*10, 1000*60*16, EnergyRequest.Status.DECLINED, user1));
		requests.add(getRequest(0.10f, -3000.0f, 16, 1000*60*4, 1000*60*10, EnergyRequest.Status.DECLINED, user1));
		requests.add(getRequest(0.10f, -3000.0f, 17, 1000*60, 1000*60*6, EnergyRequest.Status.DECLINED, user2));
		requests.add(getRequest(0.10f, -3000.0f, 18, 1000*60, 1000*60*10, EnergyRequest.Status.DECLINED, user1));
		
		requestRepos.saveAll(requests);
	}
	
	private MarketEnergyRequest getRequest(float price, float energy, int id, 
			long starttime, long endtime, Status status, UserAccount user) {
		EnergyRequest request = new EnergyRequest();
		this.maxMRequestId = Math.max(id, maxMRequestId);
		request.priceRequested = price;
		request.energyRequested = energy;
		request.startTime = starttime;
		request.endTime = endtime;
		request.status = status.getId();
		return new MarketEnergyRequest(id, user, request);
	}

	@AfterEach
	public void tearDown() throws Exception {
		requestRepos.deleteAll();
		flexOfferRepos.deleteAll();
		userRepos.deleteAll();
	}

	@Test
	public void testNotAcceptedLoad() {
		List<MarketEnergyRequest> retrievedRequests =  requestRepos.findNotAcceptedLoadByIntersectionAndLocation(
				1000*60, 1000*60*6, LOCATION_1);
		
		assertFalse(retrievedRequests.contains(requestRepos.findById(1l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(2l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(3l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(4l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(5l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(6l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(7l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(8l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(9l).get()));
		
		assertFalse(retrievedRequests.contains(requestRepos.findById(10l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(11l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(12l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(13l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(14l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(15l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(16l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(17l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(18l).get()));
	}

	@Test
	public void testNotAcceptedGeneration() {
		List<MarketEnergyRequest> retrievedRequests =  requestRepos.findNotAcceptedGenerationByIntersectionAndLocation(
				1000*60, 1000*60*6, LOCATION_1);
		
		assertFalse(retrievedRequests.contains(requestRepos.findById(1l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(2l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(3l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(4l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(5l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(6l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(7l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(8l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(9l).get()));
		
		assertFalse(retrievedRequests.contains(requestRepos.findById(10l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(11l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(12l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(13l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(14l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(15l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(16l).get()));
		assertFalse(retrievedRequests.contains(requestRepos.findById(17l).get()));
		assertTrue(retrievedRequests.contains(requestRepos.findById(18l).get()));
	}
}
