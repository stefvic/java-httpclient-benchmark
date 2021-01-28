package com.stefvic.java.httpclient.benchmark;

import static com.stefvic.java.httpclient.benchmark.BenchmarkUtils.secondsToMillis;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public final class BenchmarkConfig {

    @Default
    private final int concurrency = 10;
    @Default
    private final int requests = 100_000;
    @Default
    private final int port = 8989;
    @Default
    private final int contentBytesSize = 10_000;
    @Default
    private final boolean keepAliveScenario = true;
    @Default
    private final int serverKeepAliveMillis = secondsToMillis(60);
    @Default
    private final int clientSocketTimeoutMillis = secondsToMillis(60);
    @Default
    private final int clientConnectTimeoutMillis = secondsToMillis(10);

}
