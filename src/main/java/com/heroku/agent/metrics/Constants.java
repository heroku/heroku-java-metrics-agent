package com.heroku.agent.metrics;

public final class Constants {
    public static final long METRICS_REPORTING_INTERVAL_MS = 5 * 1000;
    public static final int REPORTER_MAX_RETRIES = 3;
    public static final long REPORTER_RETRY_DELAY_MS = 2 * 1000;

    public static final String BUFFER_POOL_NAME_DIRECT = "direct";
    public static final String BUFFER_POOL_NAME_MAPPED = "mapped";

    private Constants() {}
}
