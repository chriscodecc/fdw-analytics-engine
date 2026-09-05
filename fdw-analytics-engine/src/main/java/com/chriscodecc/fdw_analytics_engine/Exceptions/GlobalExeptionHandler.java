package com.chriscodecc.fdw_analytics_engine.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExeptionHandler {
    
    @ExceptionHandler(CompanyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCompanyNotFound(CompanyNotFoundException ex, Model model){
        model.addAttribute("errorMessage", ex.getMessage());
        return "404.html";
    }
}
