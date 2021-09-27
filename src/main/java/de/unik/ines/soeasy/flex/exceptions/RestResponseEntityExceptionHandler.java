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
package de.unik.ines.soeasy.flex.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import de.unik.ines.soeasy.flex.util.FlexApiError;
import energy.usef.core.exception.BusinessValidationException;

/**
 * Translates Java exceptions to {@link FlexApiError} and build response entity.
 * 
 * @author Sascha Holzhauer
 *
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Handle {@link EntityNotFoundException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(BusinessValidationException.class)
	public ResponseEntity<Object> handleBusinessValidationException(BusinessValidationException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		return buildResponseEntity(apiError);
	}

	/**
	 * Handle {@link EntityNotFoundException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.BAD_REQUEST, "Entity not found in database!", ex);
		return buildResponseEntity(apiError);
	}

	/**
	 * Handle {@link IllegalArgumentException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.BAD_REQUEST, "Illegal argument given!", ex);
		return buildResponseEntity(apiError);
	}

	/**
	 * Handle {@link DuplicateUserException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(DuplicateUserException.class)
	public ResponseEntity<Object> handleDuplicateUser(DuplicateUserException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.BAD_REQUEST,
				"Duplicate username (" + ex.getUsername() + ")!", ex);
		return buildResponseEntity(apiError);
	}

	/**
	 * Handle {@link DuplicateRequestException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(DuplicateRequestException.class)
	public ResponseEntity<Object> handleDuplicateRequest(DuplicateRequestException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.CONFLICT, "Duplicate request (" + ex.getRequestId() + ")!",
				ex);
		return buildResponseEntity(apiError);
	}

	/**
	 * Handle {@link NoMatchingDemandDataException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(NoMatchingDemandDataException.class)
	public ResponseEntity<Object> handleDuplicateRequest(NoMatchingDemandDataException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.NO_CONTENT,
				"No matching demand data (" + ex.getMessage() + ")!", ex);
		return buildResponseEntity(apiError);
	}

	/**
	 * Handle {@link DuplicateRequestException}s.
	 * 
	 * @param ex
	 * @return response entity
	 */
	@ExceptionHandler(ServerNotStartedException.class)
	public ResponseEntity<Object> handleServerNotStartedException(ServerNotStartedException ex) {
		FlexApiError apiError = new FlexApiError(HttpStatus.NO_CONTENT, "Server not started (" + ex.getMessage() + ")!",
				ex);
		return buildResponseEntity(apiError);
	}

	private ResponseEntity<Object> buildResponseEntity(FlexApiError apiError) {
		return new ResponseEntity<>(apiError, apiError.getStatus());
	}
}
