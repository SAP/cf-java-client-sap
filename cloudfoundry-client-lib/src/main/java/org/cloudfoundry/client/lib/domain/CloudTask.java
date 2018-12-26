package org.cloudfoundry.client.lib.domain;

import java.util.Map;

public class CloudTask extends CloudEntity {

    public enum State {
        PENDING, RUNNING, SUCCEEDED, CANCELING, FAILED;
    }

    public static class Result {

        private String failureReason;

        public Result() {
        }

        public Result(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

    }

    private String command;
    // Used in XSA:
    private Map<String, String> environmentVariables;
    private Integer memory;
    private Integer diskQuota;
    private Result result;
    private State state;

    public CloudTask(Meta meta, String name) {
        super(meta, name);
    }

    public String getCommand() {
        return command;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public Integer getMemory() {
        return memory;
    }

    public Integer getDiskQuota() {
        return diskQuota;
    }

    public Result getResult() {
        return result;
    }

    public State getState() {
        return state;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public void setDiskQuota(Integer diskQuota) {
        this.diskQuota = diskQuota;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public void setState(State state) {
        this.state = state;
    }

}
