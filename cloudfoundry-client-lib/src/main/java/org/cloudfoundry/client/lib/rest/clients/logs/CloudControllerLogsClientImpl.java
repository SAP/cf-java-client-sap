package org.cloudfoundry.client.lib.rest.clients.logs;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchFlux;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.adapters.ImmutableRawApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClientHttpRequestFactory;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class CloudControllerLogsClientImpl extends CloudControllerBaseClient implements CloudControllerLogsClient {

    private static final Log LOGGER = LogFactory.getLog(CloudControllerLogsClientImpl.class);

    private CloudControllerApplicationsClient applicationsClient;
    private DopplerClient dopplerClient;
    private RestTemplate restTemplate;

    public CloudControllerLogsClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                         CloudControllerApplicationsClient applicationsClient, DopplerClient dopplerClient,
                                         RestTemplate restTemplate) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
        this.dopplerClient = dopplerClient;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        UUID appGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        return getRecentLogs(appGuid);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        RecentLogsRequest request = RecentLogsRequest.builder()
                                                     .applicationId(applicationGuid.toString())
                                                     .build();
        return fetchFlux(() -> dopplerClient.recentLogs(request),
                         ImmutableRawApplicationLog::of).collectSortedList(Comparator.comparing(ApplicationLog::getTimestamp))
                                                        .block();
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        String stagingFile = info.getStagingFile();
        if (stagingFile != null) {
            CloudControllerRestClientHttpRequestFactory cfRequestFactory = null;
            try {
                Map<String, Object> logsRequest = new HashMap<>();
                logsRequest.put("offset", offset);

                cfRequestFactory = restTemplate.getRequestFactory() instanceof CloudControllerRestClientHttpRequestFactory
                    ? (CloudControllerRestClientHttpRequestFactory) restTemplate.getRequestFactory()
                    : null;
                if (cfRequestFactory != null) {
                    cfRequestFactory.increaseReadTimeoutForStreamedTailedLogs(5 * 60 * 1000);
                }
                return restTemplate.getForObject(stagingFile + "&tail&tail_offset={offset}", String.class, logsRequest);
            } catch (CloudOperationException e) {
                if (e.getStatusCode()
                     .equals(HttpStatus.NOT_FOUND)) {
                    // Content is no longer available
                    return null;
                } else {
                    throw e;
                }
            } catch (ResourceAccessException e) {
                // Likely read timeout, the directory server won't serve
                // the content again
                LOGGER.debug("Caught exception while fetching staging logs. Aborting. Caught:" + e, e);
            } finally {
                if (cfRequestFactory != null) {
                    cfRequestFactory.increaseReadTimeoutForStreamedTailedLogs(-1);
                }
            }
        }
        return null;
    }

}
