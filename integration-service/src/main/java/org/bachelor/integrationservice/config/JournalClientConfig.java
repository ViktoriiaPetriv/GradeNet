package org.bachelor.integrationservice.config;

import org.bachelor.integrationservice.service.journal.HttpJournalClient;
import org.bachelor.integrationservice.service.journal.JournalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class JournalClientConfig {

    @Bean
    public JournalClient journalClient(
            RestClient restClient,
            @Value("${journal.base-url}") String baseUrl) {
        return new HttpJournalClient(restClient, baseUrl);
    }
}
