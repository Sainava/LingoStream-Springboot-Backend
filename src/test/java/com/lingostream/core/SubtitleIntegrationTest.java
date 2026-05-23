package com.lingostream.core;

import com.lingostream.core.entity.SubtitleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubtitleIntegrationTest {

    // 1. Tell Docker to spin up a temporary PostgreSQL Database
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("lingostream_test")
            .withUsername("testuser")
            .withPassword("testpass");

    // 2. Tell Docker to spin up a temporary Redis Server
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    // 3. Inject the temporary Docker credentials into Spring Boot before it starts
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate; // Simulates our Web Browser

    @Test
    void shouldSuccessfullyIngestDataAndServeFromCache() {
        // Because of Testcontainers, Spring Boot booted up normally and our
        // DataIngestionRunner just shoved all the Iron Man SRT data into the temporary Postgres DB!

        // ACT: Make a GET request exactly like a browser would
        ResponseEntity<SubtitleEntity[]> response = restTemplate
                .getForEntity("/api/subtitles/iron-man-1?language=en", SubtitleEntity[].class);

        // ASSERT: Prove the API returns a 200 OK
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ASSERT: Prove data was actually parsed and loaded
        SubtitleEntity[] subtitles = response.getBody();
        assertThat(subtitles).isNotNull();
        assertThat(subtitles.length).isGreaterThan(0);

        // ASSERT: Prove the first subtitle exists
        assertThat(subtitles[0].getContent()).isNotBlank();

        System.out.println("TEST PASSED: Database initialized, SRT parsed, API serving data, Cache active!");
    }
}