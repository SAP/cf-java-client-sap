package org.cloudfoundry.client.lib.rest;

import java.io.IOException;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;

public class CloudControllerResponseErrorHandler extends DefaultResponseErrorHandler {

    private static CloudOperationException getException(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        String statusText = response.getStatusText();

        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

        if (response.getBody() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.readValue(response.getBody(), Map.class);
                String description = CloudUtil.parse(String.class, map.get("description"));
                if (description != null) {
                    description = description.trim();
                }
                return new CloudOperationException(statusCode, statusText, description);
            } catch (JsonParseException e) {
                // Fall through. Handled below.
            } catch (IOException e) {
                // Fall through. Handled below.
            }
        }
        return new CloudOperationException(statusCode, statusText);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        switch (statusCode.series()) {
            case CLIENT_ERROR:
            case SERVER_ERROR:
                throw getException(response);
            default:
                throw new RestClientException("Unknown status code [" + statusCode + "]");
        }
    }
}
