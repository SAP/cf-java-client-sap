package com.sap.cloudfoundry.client.facade.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.CloudException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.Messages;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.logcache.v1.*;
import org.cloudfoundry.reactor.logcache.v1.ReactorLogCacheClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

//TODO run stress tests and observe direct memory with ReactorLogCacheClient
public class LogCacheClient {

    private static final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                                 .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    private final String logCacheApi;
    private final OAuthClient oAuthClient;
    private final Map<String, String> requestTags;

//    private final org.cloudfoundry.logcache.v1.LogCacheClient delegate;

    public LogCacheClient(String logCacheApi, OAuthClient oAuthClient, Map<String, String> requestTags) {
        this.logCacheApi = logCacheApi;
        this.oAuthClient = oAuthClient;
        this.requestTags = requestTags;
    }

    private ApplicationLog mapToAppLog(Envelope envelope) {
        var tags = envelope.getTags();
        var log = envelope.getLog();
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(envelope.getSourceId())
                                      .message(log.getPayloadAsText())
                                      .timestamp(fromLogTimestamp(envelope.getTimestamp()))
                                      .messageType(fromLogMessageType0(log.getType()))
                                      .sourceName(tags.get("source_type"))
                                      .build();
    }

    private static LocalDateTime fromLogTimestamp(long timestampNanos) {
        Duration duration = Duration.ofNanos(timestampNanos);
        Instant instant = Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    private static ApplicationLog.MessageType fromLogMessageType0(LogType logType) {
        return logType == LogType.OUT ? ApplicationLog.MessageType.STDOUT : ApplicationLog.MessageType.STDERR;
    }

    private Throwable throwOriginalError(RetryBackoffSpec retrySpec, Retry.RetrySignal signal) {
        return signal.failure();
    }

    public List<ApplicationLog> getRecentLogs0(UUID appGuid, LocalDateTime offset) {
        org.cloudfoundry.logcache.v1.LogCacheClient client = ReactorLogCacheClient.builder()
                .requestTags(requestTags)
                .tokenProvider(oAuthClient.getTokenProvider())
                .root(Mono.just(logCacheApi))
//                .connectionContext(<>)
                .build();

        var instant = offset.toInstant(ZoneOffset.UTC);
        var secondsInNanos = Duration.ofSeconds(instant.getEpochSecond())
                                     .toNanos();

        var request = ReadRequest.builder()
                                 .sourceId(appGuid.toString())
                                 .envelopeType(EnvelopeType.LOG)
                                 .startTime(secondsInNanos + instant.getNano() + 1)
                                 .limit(1000)
                                 .descending(true)
                                 .build();

        return client.read(request)
                     .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3))
                                     .onRetryExhaustedThrow(this::throwOriginalError))
                     .map(ReadResponse::getEnvelopes)
                     .flatMapIterable(EnvelopeBatch::getBatch)
                     .map(this::mapToAppLog)
                     .collectSortedList()
                     .block();
    }

    public List<ApplicationLog> getRecentLogs(UUID applicationGuid, LocalDateTime offset) {
        //We create a new client every time to not keep a live reference to the object for the lifetime of the
        // LogCacheClient. A SelectorManager thread is spawned for each HttpClient and if the client is a class field
        // it was observed that the references are kept for the lifetime of the JVM and the threads exceeded 2.1K.
        // (2.1K SelectorManager threads -> 2.1K HttpClients)
        var client = HttpClient.newBuilder()
                               .executor(Executors.newSingleThreadExecutor())
                               .followRedirects(HttpClient.Redirect.NORMAL)
                               .connectTimeout(Duration.ofMinutes(30))
                               .build();
        HttpRequest request = buildGetLogsRequest(applicationGuid, offset);

        //TODO add resilience
        HttpResponse<InputStream> response = sendRequest(client, request);

        if (response.statusCode() / 100 != 2) {
            var status = HttpStatus.valueOf(response.statusCode());
            throw new CloudOperationException(status, status.getReasonPhrase(), parseBodyToString(response.body()));
        }
        return parseBody(response.body()).getLogs()
                                         .stream()
                                         .map(this::mapToAppLog)
                                         .sorted()
                                         .collect(Collectors.toList());
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

    private HttpResponse<InputStream> sendRequest(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new CloudException(e.getMessage(), e);
        }
    }

    private String parseBodyToString(InputStream is) {
        try (InputStream wrapped = is) {
            return IOUtils.toString(wrapped, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CloudException(String.format(Messages.CANT_READ_APP_LOGS_RESPONSE, e.getMessage()), e);
        }
    }

    private ApplicationLogsResponse parseBody(InputStream is) {
        try (InputStream wrapped = is) {
            return mapper.readValue(wrapped, ApplicationLogsResponse.class);
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

    private static String decodeLogPayload(String base64Encoded) {
        var result = Base64.getDecoder()
                .decode(base64Encoded.getBytes(StandardCharsets.UTF_8));
        return new String(result, StandardCharsets.UTF_8);
    }

    private static ApplicationLog.MessageType fromLogMessageType(String messageType) {
        return "OUT".equals(messageType) ? ApplicationLog.MessageType.STDOUT : ApplicationLog.MessageType.STDERR;
    }
}
