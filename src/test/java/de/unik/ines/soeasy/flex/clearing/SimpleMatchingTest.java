package de.unik.ines.soeasy.flex.clearing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;

import de.soeasy.common.model.flex.offer.FlexOffer;
import de.unik.ines.soeasy.flex.clearing.simplematch.SimpleMatching;
import de.unik.ines.soeasy.flex.model.ClearingInfo;
import de.unik.ines.soeasy.flex.model.FlexOfferWrapper;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.util.FlexTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "de.unik.ines.soeasy.flex.model", "de.unik.ines.soeasy.flex.scheduling",
		"de.unik.ines.soeasy.flex.clearing", "de.unik.ines.soeasy.flex.repos",
		"de.unik.ines.soeasy.flex.clearing.simplematch" })
class SimpleMatchingTest {

	@Autowired
	FlexOfferRepository fowRepos;

	@Autowired
	FlexOrderRepository forRepos;

	@Autowired
	SimpleMatching clearingMethod;

	@Autowired
	UserAccountRepository userRepos;

	@Autowired
	MMarketProductRepository mProductRepos;

	FlexOffer fo1, fo2, fo3;
	FlexOfferWrapper fow1, fow2, fow3;

	@BeforeEach
	void setUp() throws Exception {
		UserAccount flex1 = userRepos.findByName("flex1");
		fo1 = FlexTestUtils.buildFlexOffer();
		fow1 = new FlexOfferWrapper(fo1, flex1);
		fowRepos.save(fow1);

		fo2 = FlexTestUtils.buildFlexOffer();
		fow2 = new FlexOfferWrapper(fo2, flex1);
		fowRepos.save(fow2);

		fo3 = FlexTestUtils.buildFlexOffer();
		fow3 = new FlexOfferWrapper(fo3, flex1);
		fowRepos.save(fow3);
	}

	@AfterEach
	void tearDown() throws Exception {
		fowRepos.deleteAll();
	}

	@Test
	void test() {
		ClearingInfo cinfo = new ClearingInfo(this.mProductRepos.findById(3).get().getProductPattern(), 1000 * 60 * 5l,
				1000 * 60l);
		List<FlexOfferWrapper> fows = fowRepos.findAllUnmatchedByProductAndIntervalTime(
				new DateTime().plusDays(1).withTimeAtStartOfDay().getMillis(),
				new DateTime().plusDays(2).withTimeAtStartOfDay().getMillis(), 3);
		this.clearingMethod.clearMarket(null, fows, cinfo);
		AtomicInteger count = new AtomicInteger(0);
		forRepos.findAll().forEach(action -> count.incrementAndGet());

		assertEquals(3, count.intValue());

		assertTrue(forRepos.findByFlexOfferId(fo1.messageID).isPresent());
		assertTrue(forRepos.findByFlexOfferId(fo2.messageID).isPresent());
		assertTrue(forRepos.findByFlexOfferId(fo3.messageID).isPresent());
	}
}
