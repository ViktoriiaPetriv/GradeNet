package org.bachelor.integrationservice.config;

import tools.jackson.databind.ObjectMapper;
import org.bachelor.integrationservice.service.journal.HttpJournalClient;
import org.bachelor.integrationservice.service.journal.JournalClient;
import org.bachelor.integrationservice.service.journal.MockJournalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class JournalClientConfig {

    @Bean
    @ConditionalOnProperty(name = "journal.client.mode", havingValue = "mock", matchIfMissing = true)
    public JournalClient mockJournalClient(ObjectMapper objectMapper) {
        return new MockJournalClient(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "journal.client.mode", havingValue = "http")
    public JournalClient httpJournalClient(
            RestClient restClient,
            @Value("${journal.base-url}") String baseUrl) {
        return new HttpJournalClient(restClient, baseUrl);
    }
}
