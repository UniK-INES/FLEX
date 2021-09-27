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
package de.unik.ines.soeasy.flex.util;


import org.joda.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import de.unik.ines.soeasy.flex.exceptions.RestResponseEntityExceptionHandler;

/**
 * @see RestResponseEntityExceptionHandler
 * 
 * @author Sascha Holzhauer
 *
 */
public class FlexApiError {

	private HttpStatus status;
	
	@DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime timestamp;

	private String message;
	private String debugMessage;

	private FlexApiError() {
		timestamp = LocalDateTime.now();
	}

	/**
	 * @param status
	 */
	FlexApiError(HttpStatus status) {
		this();
		this.status = status;
	}

	/**
	 * @param status
	 * @param ex
	 */
	FlexApiError(HttpStatus status, Throwable ex) {
		this();
		this.status = status;
		this.message = "Unexpected error";
		this.debugMessage = ex.getLocalizedMessage();
	}

	/**
	 * @param status
	 * @param message
	 * @param ex
	 */
	public FlexApiError(HttpStatus status, String message, Throwable ex) {
		this();
		this.status = status;
		this.message = message;
		this.debugMessage = ex.getLocalizedMessage();
	}
	
	/**
	 * @return
	 */
	public HttpStatus getStatus() {
		return this.status;
	}
	
	/**
	 * @return
	 */
	public String getMessage() {
		return this.message;
	}
	
	/**
	 * @return
	 */
	public String getDebugMessage() {
		return this.debugMessage;
	}
}