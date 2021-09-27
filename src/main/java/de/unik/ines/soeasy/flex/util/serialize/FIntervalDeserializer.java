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
package de.unik.ines.soeasy.flex.util.serialize;

import java.io.IOException;

import org.joda.time.Interval;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import de.iwes.enavi.cim.schedule51.FInterval;

/**
 * @author Sascha Holzhauer
 *
 */
@JsonComponent
public class FIntervalDeserializer extends JsonDeserializer<FInterval> {

	@Override
	public FInterval deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		String intervalStr = p.getValueAsString();
		try {
			FInterval interval = new FInterval(Interval.parse(intervalStr));
			return interval;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
