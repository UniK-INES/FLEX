package de.unik.ines.soeasy.flex.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import de.unik.ines.soeasy.flex.config.UsefConfig;
import energy.usef.core.config.ConfigParam;


@SpringBootTest
@ContextConfiguration(classes = UsefConfig.class)
class UsefConfigTest {

	@Autowired
	UsefConfig usefConfig;

	@Test
	void test() {
		assertEquals("EUR", usefConfig.getProperty(ConfigParam.CURRENCY), "Currency");
	}
}
