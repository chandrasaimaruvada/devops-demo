package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelloControllerTest {

    @LocalServerPort
    private int port;

    @Test
    void helloEndpointReturnsGreeting() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        String url = "http://localhost:" + port + "/hello";
        String response = restTemplate.getForObject(url, String.class);
        assertThat(response).contains("Hello from the Jenkins");
    }

    @Test
    void versionEndpointReturnsVersion() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        String url = "http://localhost:" + port + "/version";
        String response = restTemplate.getForObject(url, String.class);
        assertThat(response).isEqualTo("v1.0.0");
    }
}
