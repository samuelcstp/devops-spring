package com.example.restservice;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class GreetingControllerTests {

  @Autowired
  private RestTestClient restTestClient;

  @Test
  public void noParamGreetingShouldReturnDefaultMessage() throws Exception {

    this.restTestClient.get().uri("/greeting")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isEqualTo("Hello, World!");
  }

  @Test
  public void paramGreetingShouldReturnTailoredMessage() throws Exception {

    this.restTestClient.get()
        .uri(uri -> uri.path("/greeting").queryParam("name", "Spring Community").build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isEqualTo("Hello, Spring Community!");
  }

  @Test
  public void blankNameShouldBeRejected() throws Exception {

    this.restTestClient.get()
        .uri(uri -> uri.path("/greeting").queryParam("name", " ").build())
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  public void errorEndpointShouldReturn500() throws Exception {

    this.restTestClient.get().uri("/greeting/error")
        .exchange()
        .expectStatus().isEqualTo(500);
  }

  @Test
  public void slowEndpointShouldReturn200() throws Exception {

    this.restTestClient.get()
        .uri(uri -> uri.path("/greeting/slow").queryParam("delay", "100").build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isEqualTo("Hello, Slow World!");
  }

  @Test
  public void prometheusEndpointShouldExposeJvmAndHttpMetrics() throws Exception {

    // Dispara uma requisição para garantir que a métrica HTTP seja registrada
    this.restTestClient.get().uri("/greeting").exchange().expectStatus().isOk();

    this.restTestClient.get().uri("/actuator/prometheus")
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(body -> {
          assertThat(body).contains("jvm_memory_used_bytes");
          assertThat(body).contains("process_cpu_usage");
          assertThat(body).contains("http_server_requests_seconds");
        });
  }

}
