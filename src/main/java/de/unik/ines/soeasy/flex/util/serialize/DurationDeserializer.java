/**
 * 
 */
package de.unik.ines.soeasy.flex.util.serialize;

import java.io.IOException;

import org.joda.time.Duration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author Sascha Holzhauer
 *
 */
public class DurationDeserializer extends JsonDeserializer<Duration> {

	@Override
	public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String dateTimeStr = p.getValueAsString();
		try {
			Duration duration = Duration.parse(dateTimeStr);
			return duration;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
