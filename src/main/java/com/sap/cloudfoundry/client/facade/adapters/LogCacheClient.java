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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.sap.cloudfoundry.client.facade.CloudException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.Messages;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

public class LogCacheClient {

    private static final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                                 .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    private final HttpClient client;
    private final String logCacheApi;
    private final OAuthClient oAuthClient;
    private final Map<String, String> requestTags;

    public LogCacheClient(String logCacheApi, OAuthClient oAuthClient, Map<String, String> requestTags) {
        this.client = HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofMinutes(30))
                                .build();
        this.logCacheApi = logCacheApi;
        this.oAuthClient = oAuthClient;
        this.requestTags = requestTags;
    }

    public Flux<ApplicationLogEntity> getRecentLogs(UUID applicationGuid, LocalDateTime offset) {
        HttpRequest request = buildGetLogsRequest(applicationGuid, offset);

        HttpResponse<InputStream> response = sendRequest(request);

        if (response.statusCode() / 100 != 2) {
            var status = HttpStatus.valueOf(response.statusCode());
            throw new CloudOperationException(status, status.getReasonPhrase(), parseBodyToString(response.body()));
        }
        return Flux.fromIterable(parseBody(response.body()).getLogs());
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

    private HttpResponse<InputStream> sendRequest(HttpRequest request) {
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
}
