package osotnikov.spring_webflux_webclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@org.springframework.context.annotation.Configuration
public class Configuration {

    //@Value("${wiremock.server.baseUrl}")
    private String wiremockUrl;

    //@Bean
    public WebClient webClientBuilder() {
        HttpClient httpClient = HttpClient.create(ConnectionProvider
                .builder("connectionProviderName")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.of(1000, ChronoUnit.MILLIS))
                .build());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:8080")
                .build();
    }
}
