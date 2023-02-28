package com.sap.cloudfoundry.client.facade.adapters;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.HttpClientResponseWithBody;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.util.AbstractReactorOperations;
import org.cloudfoundry.reactor.util.JsonCodec;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LogCacheClient extends AbstractReactorOperations {

    private final ObjectMapper mapper;

    public LogCacheClient(ConnectionContext connectionContext, String logCacheApi, TokenProvider tokenProvider,
                          Map<String, String> requestTags) {
        super(connectionContext, Mono.just(logCacheApi), tokenProvider, requestTags);
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                        .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    }

    public Flux<ApplicationLogEntity> getRecentLogs(UUID applicationGuid, LocalDateTime offset) {
        return createOperator().flatMapMany(operator -> operator.get()
                                                                .uri(builder -> getEndpoint(builder, applicationGuid.toString(), offset))
                                                                .response()
                                                                .parseBodyToMono(this::parseBody)
                                                                .flatMapIterable(ApplicationLogsResponse::getLogs));
    }

    private UriComponentsBuilder getEndpoint(UriComponentsBuilder builder, String applicationGuid, LocalDateTime offset) {
        var instant = offset.toInstant(ZoneOffset.UTC);
        var secondsInNanos = Duration.ofSeconds(instant.getEpochSecond())
                                     .toNanos();
        return builder.pathSegment("api", "v1", "read", applicationGuid)
                      .queryParam("envelope_types", "LOG")
                      .queryParam("descending", "true")
                      .queryParam("start_time", Long.toString(secondsInNanos + instant.getNano() + 1))
                      .queryParam("limit", "1000");
    }

    private Mono<ApplicationLogsResponse> parseBody(HttpClientResponseWithBody response) {
        return JsonCodec.decode(mapper, response.getBody(), ApplicationLogsResponse.class);
    }
}
