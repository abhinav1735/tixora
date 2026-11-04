
package com.tixora.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayConfig {

    @Bean
    public RestClient razorpayRestClient(
            RazorpayProperties properties
    ) {
        return RestClient.builder()
                .baseUrl("https://api.razorpay.com/v1")
                .defaultHeaders(headers ->
                        headers.setBasicAuth(
                                properties.keyId(),
                                properties.keySecret()
                        )
                )
                .build();
    }
}
