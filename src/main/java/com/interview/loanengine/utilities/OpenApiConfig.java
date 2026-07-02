package com.interview.loanengine.utilities;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Loan Engine API",
        version = "1.0",
        description = "Loan product, loan origination, repayment schedule and payment/prepayment endpoints."))
public class OpenApiConfig {
}
