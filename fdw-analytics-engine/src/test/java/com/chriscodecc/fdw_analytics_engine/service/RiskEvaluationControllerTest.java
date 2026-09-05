package com.chriscodecc.fdw_analytics_engine.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chriscodecc.fdw_analytics_engine.config.SecurityConfig;
import com.chriscodecc.fdw_analytics_engine.controller.RiskEvaluationController;
import com.chriscodecc.fdw_analytics_engine.dto.RiskEvaluationResponse;
import com.chriscodecc.fdw_analytics_engine.security.ApiKeyAuthenticationFilter;

@Import({SecurityConfig.class, ApiKeyAuthenticationFilter.class})
@TestPropertySource(properties = "app.api.key=testKEy123")
@WebMvcTest(RiskEvaluationController.class)
public class RiskEvaluationControllerTest {

    @Autowired 
    private MockMvc mockMvc;
    @MockitoBean 
    private RiskEvaluationService riskEvaluationService;

    @Test
    void shouldRejectAccessWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/riskEvaluation/risklevel")
                .param("companySymbol", "DAX")) 
            .andExpect(status().isForbidden()); 
    }

    @Test
    void shouldRejectAccessWhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/riskEvaluation/risklevel")
                .param("companySymbol", "DAX")
                .header("API_KEY", "WRONG_KEY"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAccessWhenApiKeyIsValid() throws Exception {
        RiskEvaluationResponse mockResponse = new RiskEvaluationResponse();
        when(riskEvaluationService.culateOverAllRiskLevel("DAX")).thenReturn(mockResponse);
        mockMvc.perform(get("/api/v1/riskEvaluation/risklevel")
                .param("companySymbol", "DAX")
                .header("API_KEY", "testKEy123"))
            .andExpect(status().isOk()); 
    }
}
