/**
 * 
 */
package de.unik.ines.soeasy.flex.exceptions;

/**
 * @author Sascha Holzhauer
 *
 */
public class EntityNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3403958581161306666L;

	public EntityNotFoundException(String message) {
		super(message);
	}
}
