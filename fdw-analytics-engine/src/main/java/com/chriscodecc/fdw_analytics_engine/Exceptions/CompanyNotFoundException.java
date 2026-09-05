package com.chriscodecc.fdw_analytics_engine.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="No such Company")   //404
public class CompanyNotFoundException extends RuntimeException{
    
    public CompanyNotFoundException(String message) {
        super(message);
    }
}
