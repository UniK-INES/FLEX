package de.unik.ines.soeasy.flex.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.iwes.enavi.cim.schedule51.Schedule_MarketDocument;
import de.iwes.enavi.cim.schedule51.TimeSeries;

@JsonTest
class GridFlexSmdDeserialiserTest {

	/**
	 * Grid FLEX Schedule contains time series of two locations (Tranformer01 and
	 * Transformer02) for 8 intervals of 15 minutes. First 4 intervals are 10 kW,
	 * second 4 intervals are 0 kW for Transformer 1. First 4 intervals are 20 kW,
	 * second 48 intervals are -10 kW for Transformer 2.
	 */
	public static final String FILENAME_JSON_FLEXDEMAND_SMD = "json/GridFlexDemand_Schedule_MarketDocument.json";

	@Autowired
	private ObjectMapper mapper;

	@Test
	void test() {
		Schedule_MarketDocument flexDemandSmd = null;
		try {
			flexDemandSmd = mapper.readValue(
					new File(getClass().getClassLoader().getResource(FILENAME_JSON_FLEXDEMAND_SMD).toURI()),
					Schedule_MarketDocument.class);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertTrue(flexDemandSmd != null);
		Map<String, TimeSeries> timeSeries = new HashMap<>();
		flexDemandSmd.getTimeSeries().forEach(ts -> timeSeries.put(ts.getmRID(), ts));
		assertEquals(timeSeries.get("timeseries_transformer01").getPeriod().getPoints().
				get(0).getQuantity().doubleValue(), 10.0, 0.0001);
	}
}
