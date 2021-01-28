package com.stefvic.java.httpclient.benchmark;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JDKHttpAsyncClientWithFixedThreadPoolTwo extends JDKHttpAsyncClient {

    public JDKHttpAsyncClientWithFixedThreadPoolTwo(BenchmarkConfig benchmarkConfig,
                                                    ExecutorService executor) {
        super(HttpClient.newBuilder()
                        .followRedirects(Redirect.NEVER)
                        .version(Version.HTTP_1_1)
                        .executor(executor)
                        .connectTimeout(Duration.ofMillis(benchmarkConfig.getClientConnectTimeoutMillis())));
    }

    public static void main(String[] args) {
        var executor = Executors.newFixedThreadPool(2);
        BenchmarkConfig benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        BenchmarkRunner.run(new JDKHttpAsyncClientWithFixedThreadPoolTwo(benchmarkConfig, executor), benchmarkConfig);
        executor.shutdown();
    }
}
