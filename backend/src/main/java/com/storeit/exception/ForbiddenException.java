package com.storeit.exception;

/**
 * Raised when an authenticated user tries to access a resource they neither
 * own nor have been granted access to.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
