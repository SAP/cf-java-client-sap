package org.cloudfoundry.client.lib.rest.clients.util;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.http.HttpStatus;

public class UriInfoUtil {

    private static final String DEFAULT_HOST_DOMAIN_SEPARATOR = "\\.";
    private static final String DEFAULT_PATH_SEPARATOR = "/";

    public static void extractUriInfo(Map<String, UUID> existingDomains, String uri, Map<String, String> uriInfo) {
        URI newUri = URI.create(uri);
        String host = newUri.getScheme() != null ? newUri.getHost() : newUri.getPath();
        String[] hostAndDomain = host.split(DEFAULT_HOST_DOMAIN_SEPARATOR, 2);
        if (hostAndDomain.length != 2) {
            throw new CloudOperationException(HttpStatus.BAD_REQUEST,
                                              HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                              "Invalid URI " + uri + " -- host or domain is not specified");
        }
        String hostName = hostAndDomain[0];
        int indexOfPathSeparator = hostAndDomain[1].indexOf(DEFAULT_PATH_SEPARATOR);
        String domain = hostAndDomain[1];
        String path = "";
        if (indexOfPathSeparator > 0) {
            domain = hostAndDomain[1].substring(0, indexOfPathSeparator);
            path = hostAndDomain[1].substring(indexOfPathSeparator);
        }

        extractDomainInfo(existingDomains, uriInfo, domain, hostName, path);

        if (uriInfo.get("domainName") == null) {
            domain = host.split(DEFAULT_PATH_SEPARATOR)[0];
            extractDomainInfo(existingDomains, uriInfo, domain, "", path);
        }
        if (uriInfo.get("domainName") == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                              HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              "Domain not found for URI " + uri);
        }
    }

    public static void extractDomainInfo(Map<String, UUID> existingDomains, Map<String, String> uriInfo, String domain, String hostName,
                                         String path) {
        for (String existingDomain : existingDomains.keySet()) {
            if (domain.equals(existingDomain)) {
                uriInfo.put("domainName", existingDomain);
                uriInfo.put("host", hostName);
                uriInfo.put("path", path);
            }
        }
    }
}
