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

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

/**
 * @author Sascha Holzhauer
 */

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlexTestUtils.class)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.flex", "de.unik.ines.soeasy.flex.config",
		"de.unik.ines.soeasy.flex.scheduling", "de.unik.ines.soeasy.flex.grid" })
class FlexOfferWrapperDbTest {

	FlexOfferWrapper flexOffer;

	@Autowired
	FlexOfferRepository foRepos;

	@Autowired
	EntityManager entityManager;

	@Autowired
	UserAccountRepository userRepos;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		UserAccount flex1 = userRepos.findByName("flex1");
		this.flexOffer = new FlexOfferWrapper(FlexTestUtils.buildFlexOffer(), flex1);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		foRepos.deleteAll();
	}


	@Test
	void testFoStorage() {
		assertEquals(this.flexOffer, this.foRepos.save(this.flexOffer));
	}

	@Test
	@Transactional(propagation = Propagation.SUPPORTS)
	void testFoStorageUniqueUuid() {
		assertEquals(this.flexOffer, this.foRepos.save(this.flexOffer));
	}

	@Test
	void testFoRetrival() {
		this.foRepos.save(this.flexOffer);
		assertEquals(this.flexOffer,
				this.foRepos.findById(this.flexOffer.getFlexOffer().getMessageID().toString()).get());
	}
}
