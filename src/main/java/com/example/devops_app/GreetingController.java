package com.example.restservice;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GreetingController {

  private static final Logger log = LoggerFactory.getLogger(GreetingController.class);
  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(defaultValue = "World") String name) {
    log.debug("Recebida requisição de saudação para name={}", name);
    if (name.isBlank()) {
      log.error("Saudação recusada porque o nome está em branco");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
    }
    Greeting greeting = new Greeting(counter.incrementAndGet(), template.formatted(name));
    log.info("Saudação #{} gerada para {}", greeting.id(), name);
    return greeting;
  }

  /** Sempre lança 500 — útil para popular a série 5xx nos dashboards do Grafana. */
  @GetMapping("/greeting/error")
  public Greeting greetingError() {
    log.error("Erro simulado disparado em /greeting/error");
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "erro simulado para demonstração");
  }

  /**
   * Responde com delay configurável (padrão 500 ms, máximo 5000 ms).
   * Útil para demonstrar o painel de tempo de resposta no Grafana.
   */
  @GetMapping("/greeting/slow")
  public Greeting greetingSlow(@RequestParam(defaultValue = "500") long delay) throws InterruptedException {
    long safeDelay = Math.min(delay, 5000);
    log.info("Requisição lenta recebida — aguardando {} ms", safeDelay);
    Thread.sleep(safeDelay);
    Greeting greeting = new Greeting(counter.incrementAndGet(), template.formatted("Slow World"));
    log.info("Saudação lenta #{} concluída após {} ms", greeting.id(), safeDelay);
    return greeting;
  }
}
