package com.sap.cloudfoundry.client.facade.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloudfoundry.client.facade.CloudException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.Messages;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.CloudUtil;

public class LogCacheClient {

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                                 .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);

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
        HttpResponse<InputStream> response = CloudUtil.executeWithRetry(() -> executeRequest(applicationGuid, offset));

        return parseBody(response.body()).getLogs()
                                         .stream()
                                         .map(this::mapToAppLog)
                                         // we use a linked list so that the log messages can be a LIFO sequence
                                         // that way, we avoid unnecessary sorting and copying to and from another collection/array
                                         .collect(LinkedList::new, LinkedList::addFirst, LinkedList::addAll);
    }

    private HttpResponse<InputStream> executeRequest(UUID applicationGuid, LocalDateTime offset) {
        try {
            HttpRequest request = buildGetLogsRequest(applicationGuid, offset);
            LOGGER.info(Messages.CALLING_LOG_CACHE_ENDPOINT_TO_GET_APP_LOGS);
            var response = CloudFoundryClientFactory.HTTP_CLIENT.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                var status = HttpStatus.valueOf(response.statusCode());
                throw new CloudOperationException(status, status.getReasonPhrase(), parseBodyToString(response.body()));
            }
            LOGGER.info(Messages.APP_LOGS_WERE_FETCHED_SUCCESSFULLY);
            return response;
        } catch (IOException | InterruptedException e) {
            throw new CloudException(e.getMessage(), e);
        }
    }

    private HttpRequest buildGetLogsRequest(UUID applicationGuid, LocalDateTime offset) {
        var requestBuilder = HttpRequest.newBuilder()
                                        .GET()
                                        .uri(buildGetLogsUrl(applicationGuid, offset))
                                        .timeout(Duration.ofMinutes(5))
                                        .header(HttpHeaders.AUTHORIZATION, oAuthClient.getAuthorizationHeaderValue());
        requestTags.forEach(requestBuilder::header);
        return requestBuilder.build();
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

    private String parseBodyToString(InputStream is) {
        try (InputStream wrapped = is) {
            return IOUtils.toString(wrapped, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CloudException(String.format(Messages.CANT_READ_APP_LOGS_RESPONSE, e.getMessage()), e);
        }
    }

    private ApplicationLogsResponse parseBody(InputStream is) {
        LOGGER.info(Messages.STARTED_READING_LOG_RESPONSE_INPUT_STREAM);
        try (InputStream wrapped = is) {
            var appLogsResponse = MAPPER.readValue(wrapped, ApplicationLogsResponse.class);
            LOGGER.info(Messages.ENDED_READING_LOG_RESPONSE_INPUT_STREAM);
            return appLogsResponse;
        } catch (IOException e) {
            throw new CloudException(String.format(Messages.CANT_DESERIALIZE_APP_LOGS_RESPONSE, e.getMessage()), e);
        }
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
