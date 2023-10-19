package com.sap.cloudfoundry.client.facade.adapters;

import static com.sap.cloudfoundry.client.facade.adapters.CloudFoundryClientFactory.WEB_CLIENT;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponentsBuilder;

import com.sap.cloudfoundry.client.facade.Messages;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.CloudUtil;

import reactor.core.publisher.Mono;

public class LogCacheClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCacheClient.class);

    private final String logCacheApi;
    private final OAuthClient oAuthClient;
    private final Map<String, String> requestTags;

    public LogCacheClient(String logCacheApi, OAuthClient oAuthClient, Map<String, String> requestTags) {
        this.logCacheApi = logCacheApi;
        this.oAuthClient = oAuthClient;
        this.requestTags = requestTags;
    }

    public List<ApplicationLog> getRecentLogs(UUID applicationGuid, LocalDateTime offset) {
        ApplicationLogsResponse applicationLogsResponse = CloudUtil.executeWithRetry(() -> executeApplicationLogsRequest(applicationGuid,
                                                                                                                         offset));
        return applicationLogsResponse.getLogs()
                                      .stream()
                                      .map(this::mapToAppLog)
                                      // we use a linked list so that the log messages can be a LIFO sequence
                                      // that way, we avoid unnecessary sorting and copying to and from another collection/array
                                      .collect(LinkedList::new, LinkedList::addFirst, LinkedList::addAll);
    }

    private ApplicationLogsResponse executeApplicationLogsRequest(UUID applicationGuid, LocalDateTime offset) {
        LOGGER.info(Messages.CALLING_LOG_CACHE_ENDPOINT_TO_GET_APP_LOGS);
        ApplicationLogsResponse applicationLogsResponse = executeApplicationLogsRequest(buildGetLogsUrl(applicationGuid, offset));
        LOGGER.info(Messages.APP_LOGS_WERE_FETCHED_SUCCESSFULLY);
        return applicationLogsResponse;
    }

    private URI buildGetLogsUrl(UUID applicationGuid, LocalDateTime offset) {
        var instant = offset.toInstant(ZoneOffset.UTC);
        var secondsInNanos = Duration.ofSeconds(instant.getEpochSecond())
                                     .toNanos();
        return UriComponentsBuilder.fromHttpUrl(logCacheApi)
                                   .pathSegment("api", "v1", "read", applicationGuid.toString())
                                   .queryParam("envelope_types", "LOG")
                                   .queryParam("descending", "true")
                                   .queryParam("limit", "1000")
                                   .queryParam("start_time", Long.toString(secondsInNanos + instant.getNano() + 1))
                                   .build()
                                   .toUri();
    }

    private ApplicationLogsResponse executeApplicationLogsRequest(URI logsUrl) {
        return WEB_CLIENT.get()
                         .uri(logsUrl)
                         .headers(httpHeaders -> httpHeaders.addAll(getAdditionalRequestHeaders(requestTags)))
                         .header(HttpHeaders.AUTHORIZATION, oAuthClient.getAuthorizationHeaderValue())
                         .exchangeToMono(this::handleClientResponse)
                         .block();
    }

    private LinkedMultiValueMap<String, String> getAdditionalRequestHeaders(Map<String, String> requestTags) {
        LinkedMultiValueMap<String, String> additionalHeaders = new LinkedMultiValueMap<>();
        requestTags.forEach(additionalHeaders::add);
        return additionalHeaders;
    }

    private Mono<ApplicationLogsResponse> handleClientResponse(ClientResponse clientResponse) {
        if (clientResponse.statusCode()
                          .is2xxSuccessful()) {
            return clientResponse.bodyToMono(ApplicationLogsResponse.class);
        }
        return clientResponse.createException()
                             .flatMap(Mono::error);
    }

    private ApplicationLog mapToAppLog(ApplicationLogEntity log) {
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(log.getSourceId())
                                      .message(decodeLogPayload(log.getLogBody()
                                                                   .getMessage()))
                                      .timestamp(fromLogTimestamp(log.getTimestampInNanoseconds()))
                                      .messageType(fromLogMessageType(log.getLogBody()
                                                                         .getMessageType()))
                                      .sourceName(log.getTags()
                                                     .get("source_type"))
                                      .build();
    }

    private String decodeLogPayload(String base64Encoded) {
        var result = Base64.getDecoder()
                           .decode(base64Encoded.getBytes(StandardCharsets.UTF_8));
        return new String(result, StandardCharsets.UTF_8);
    }

    private LocalDateTime fromLogTimestamp(long timestampNanos) {
        Duration duration = Duration.ofNanos(timestampNanos);
        Instant instant = Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    private ApplicationLog.MessageType fromLogMessageType(String messageType) {
        return "OUT".equals(messageType) ? ApplicationLog.MessageType.STDOUT : ApplicationLog.MessageType.STDERR;
    }
}
