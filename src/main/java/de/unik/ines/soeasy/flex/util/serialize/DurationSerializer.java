/**
 * 
 */
package de.unik.ines.soeasy.flex.util.serialize;

import java.io.IOException;

import org.joda.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author sascha
 *
 */
public class DurationSerializer extends JsonSerializer<Duration> {

	@Override
	public void serialize(Duration value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeString(value.toString());
	}
}
