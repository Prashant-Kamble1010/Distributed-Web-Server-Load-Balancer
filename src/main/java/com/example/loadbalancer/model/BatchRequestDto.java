package com.example.loadbalancer.model;

import java.util.List;

/**
 * Data Transfer Object for triggering and returning batch request executions.
 */
public class BatchRequestDto {

    private int count;

    public BatchRequestDto() {
        this.count = 1;
    }

    public BatchRequestDto(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public static class Result {
        private final int requestedCount;
        private final int successfulCount;
        private final int failedCount;
        private final List<RequestRecord> records;

        public Result(int requestedCount, int successfulCount, int failedCount, List<RequestRecord> records) {
            this.requestedCount = requestedCount;
            this.successfulCount = successfulCount;
            this.failedCount = failedCount;
            this.records = records;
        }

        public int getRequestedCount() {
            return requestedCount;
        }

        public int getSuccessfulCount() {
            return successfulCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public List<RequestRecord> getRecords() {
            return records;
        }
    }
}
