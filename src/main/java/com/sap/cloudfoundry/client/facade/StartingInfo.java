package com.sap.cloudfoundry.client.facade;

/**
 * Starting info contains values from response headers when an application is first started. One of the possible header values may be the
 * location of the staging log when starting an application.
 *
 */
public class StartingInfo {

    private String stagingFile;

    // Required by Jackson.
    protected StartingInfo() {
    }

    public StartingInfo(String stagingFile) {
        this.stagingFile = stagingFile;
    }

    /**
     * @return URL value of the file location for the staging log, or null if not available.
     */
    public String getStagingFile() {
        return stagingFile;
    }

}
