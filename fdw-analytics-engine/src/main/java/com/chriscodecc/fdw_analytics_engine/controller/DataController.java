package com.chriscodecc.fdw_analytics_engine.controller;

import javax.xml.crypto.Data;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chriscodecc.fdw_analytics_engine.dto.CompanyDataDTO;
import com.chriscodecc.fdw_analytics_engine.model.FactPrices;
import com.chriscodecc.fdw_analytics_engine.repository.FactPricesRepository;
import com.chriscodecc.fdw_analytics_engine.service.DataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/api/v1/data")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService){
        this.dataService = dataService;
    }

   /**
     * REST endpoint to retrieve historical company data as a DTO.
     * 
     * @param companySymbol the unique ticker symbol of the company (e.g., "DAX")
     * @return ResponseEntity containing the compiled company data DTO
     */
    @GetMapping("/companyDataJson")
    public ResponseEntity<CompanyDataDTO> provideCompanyData(@RequestParam String companySymbol) {
        return ResponseEntity.ok(dataService.provideCompanyData(companySymbol));
    }
    

    
}
