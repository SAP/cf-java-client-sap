package com.sap.cloudfoundry.client.facade.adapters;

import com.sap.cloudfoundry.client.facade.CloudException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.Messages;
import com.sap.cloudfoundry.client.facade.domain.ReadinessHealthCheck;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.CloudUtil;
import com.sap.cloudfoundry.client.facade.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Map;

public class ReadinessHealthCheckClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadinessHealthCheckClient.class);
    private final OAuthClient oAuthClient;
    private final URI controllerApi;

    public ReadinessHealthCheckClient(OAuthClient oAuthClient, URL api) {
        this.oAuthClient = oAuthClient;
        this.controllerApi = getControllerUriFromUrl(api);
    }

    public void updateApplicationReadinessHealthCheck(String processId, Map<String, ReadinessHealthCheck> readinessHealthCheck) {
        CloudUtil.executeWithRetry(() -> executeRequest(processId, readinessHealthCheck));
    }

    private String executeRequest(String processId, Map<String, ReadinessHealthCheck> readinessHealthCheck) {
        try {
            LOGGER.info(MessageFormat.format(Messages.TRYING_TO_UPDATE_APPLICATION_PROCESS_WITH_ID, processId));
            HttpRequest request = buildGetLogsRequest(processId, readinessHealthCheck);
            var response = CloudFoundryClientFactory.HTTP_CLIENT.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                var status = HttpStatus.valueOf(response.statusCode());
                throw new CloudOperationException(status, status.getReasonPhrase(), parseBodyToString(response.body()));
            }
            return parseBodyToString(response.body());
        } catch (IOException | InterruptedException e) {
            throw new CloudException(e.getMessage(), e);
        }
    }

    private URI getControllerUriFromUrl(URL controllerUrl) {
        try {
            return controllerUrl.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private HttpRequest buildGetLogsRequest(String processId, Map<String, ReadinessHealthCheck> readinessHealthCheck) {
        var requestBuilder = HttpRequest.newBuilder()
                                        .method(HttpMethod.PATCH.name(),
                                                HttpRequest.BodyPublishers.ofString(JsonUtil.convertToJson(readinessHealthCheck)))
                                        .uri(buildUpdateApplicationReadinessHealthCheck(processId))
                                        .timeout(Duration.ofMinutes(5))
                                        .header(HttpHeaders.AUTHORIZATION, oAuthClient.getAuthorizationHeaderValue())
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return requestBuilder.build();
    }

    private String parseBodyToString(InputStream is) {
        try (InputStream wrapped = is) {
            return IOUtils.toString(wrapped, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CloudException(e);
        }
    }

    private URI buildUpdateApplicationReadinessHealthCheck(String processId) {
        return UriComponentsBuilder.fromUri(controllerApi)
                                   .pathSegment("v3", "processes", processId)
                                   .build()
                                   .toUri();
    }
}
