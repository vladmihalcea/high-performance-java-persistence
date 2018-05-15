package com.sorinc.test.domain;

public class AAAException extends RuntimeException{

    public AAAException(){
        super();
    }

    public AAAException(final String message) {
        super(message);
    }

    public AAAException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AAAException(final Throwable cause) {
        super(cause);
    }

}
