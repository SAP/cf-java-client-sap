package org.cloudfoundry.client.lib.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.TestUtil;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.rest.clients.util.UriInfoUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CloudControllerRestClientImplTest {

    // @formatter:off
    public static Stream<Arguments> testExtractUriInfo() {
        return Stream.of(
                // Select matching domain
                Arguments.of("domain-1.json", null),
                // Test with port and user
                Arguments.of("domain-2.json", null),
                // Test with route path
                Arguments.of("domain-3.json", null),
                // Test with invalid uri, should throw an exception
                Arguments.of("domain-4.json", CloudOperationException.class),
                // Uri equals the domain -> no host
                Arguments.of("domain-5.json", null),
                // Uri equals the domain (with path)
                Arguments.of("domain-6.json", null),
                // Test with domain which does not exist, should throw exception
                Arguments.of("domain-7.json", CloudOperationException.class)
        );
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    public void testExtractUriInfo(String fileName, Class<? extends RuntimeException> expectedException) throws Throwable {
        String fileContent = TestUtil.readFileContent(fileName, getClass());
        Input input = TestUtil.fromJson(fileContent, Input.class);
        Map<String, String> uriInfo = new HashMap<>();
        Map<String, UUID> domainsAsMap = input.getDomainsAsMap();

        executeWithErrorHandling(() -> UriInfoUtil.extractUriInfo(domainsAsMap, input.getUri(), uriInfo), expectedException,
                                 input.getErrorMessage());

        validateUriInfo(uriInfo, domainsAsMap, input.getExpectedDomain(), input.getExpectedHost(), input.getExpectedPath());
    }

    private void executeWithErrorHandling(Executable executable, Class<? extends RuntimeException> expectedException,
                                          String exceptionMessage)
        throws Throwable {
        if (expectedException != null) {
            RuntimeException runtimeException = Assertions.assertThrows(expectedException, executable);
            Assertions.assertEquals(exceptionMessage, runtimeException.getMessage());
            return;
        }
        executable.execute();
    }

    private void validateUriInfo(Map<String, String> uriInfo, Map<String, UUID> domainsAsMap, String expectedDomain, String expectedHost,
                                 String expectedPath) {
        Assertions.assertEquals(domainsAsMap.get(expectedDomain), domainsAsMap.get(uriInfo.get("domainName")));
        Assertions.assertEquals(expectedHost, uriInfo.get("host"));
        Assertions.assertEquals(expectedPath, uriInfo.get("path"));
    }

    private static class Input {

        private String uri;
        private List<CloudDomain> domains;
        private String expectedPath;
        private String expectedHost;
        private String expectedDomain;
        private String errorMessage;

        public String getUri() {
            return uri;
        }

        public List<CloudDomain> getDomains() {
            return domains;
        }

        public String getExpectedPath() {
            return expectedPath;
        }

        public String getExpectedHost() {
            return expectedHost;
        }

        public String getExpectedDomain() {
            return expectedDomain;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Map<String, UUID> getDomainsAsMap() {
            return getDomains().stream()
                               .collect(Collectors.toMap(CloudDomain::getName, domain -> domain.getMetadata()
                                                                                               .getGuid()));
        }

    }
}
