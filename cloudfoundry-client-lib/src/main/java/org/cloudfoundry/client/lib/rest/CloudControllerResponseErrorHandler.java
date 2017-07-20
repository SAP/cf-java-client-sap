package org.cloudfoundry.client.lib.rest;

import java.io.IOException;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.client.lib.StagingErrorException;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;

public class CloudControllerResponseErrorHandler extends DefaultResponseErrorHandler {

    private static CloudFoundryException getException(ClientHttpResponse response) throws IOException {
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

                int cloudFoundryErrorCode = CloudUtil.parse(Integer.class, map.get("code"));
                String cloudFoundryErrorName = CloudUtil.parse(String.class, map.get("error_code"));

                if (cloudFoundryErrorCode >= 0 && cloudFoundryErrorName != null) {
                    switch (cloudFoundryErrorCode) {
                        case StagingErrorException.ERROR_CODE:
                            return new StagingErrorException(statusCode, statusText, description, cloudFoundryErrorCode,
                                cloudFoundryErrorName);
                        case NotFinishedStagingException.ERROR_CODE:
                            return new NotFinishedStagingException(statusCode, statusText, description, cloudFoundryErrorCode,
                                cloudFoundryErrorName);
                        default:
                            return new CloudFoundryException(statusCode, statusText, description, cloudFoundryErrorCode,
                                cloudFoundryErrorName);
                    }
                }
                return new CloudFoundryException(statusCode, statusText, description);
            } catch (JsonParseException e) {
                // Fall through. Handled below.
            } catch (IOException e) {
                // Fall through. Handled below.
            }
        }
        return new CloudFoundryException(statusCode, statusText);
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
