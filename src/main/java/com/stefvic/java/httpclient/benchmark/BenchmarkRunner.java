package com.stefvic.java.httpclient.benchmark;

import static com.stefvic.java.httpclient.benchmark.BenchmarkJettyHttpServer.ECHO_RESOURCE;
import static com.stefvic.java.httpclient.benchmark.BenchmarkJettyHttpServer.FIXED_RESOURCE;
import static com.stefvic.java.httpclient.benchmark.BenchmarkJettyHttpServer.STATS_RESET_RESOURCE;
import static com.stefvic.java.httpclient.benchmark.BenchmarkJettyHttpServer.STATS_RESOURCE;
import static com.stefvic.java.httpclient.benchmark.Constants.HTTP_CONNECTION_CLOSE;
import static com.stefvic.java.httpclient.benchmark.Constants.HTTP_CONNECTION_HEADER;
import static com.stefvic.java.httpclient.benchmark.Constants.HTTP_CONTENT_TYPE_HEADER;
import static com.stefvic.java.httpclient.benchmark.Constants.HTTP_CONTENT_TYPE_OCTET_STREAM;
import static java.util.function.Predicate.not;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class BenchmarkRunner {

    private static final String LOCAL_HOST = "http://127.0.0.1";

    private BenchmarkRunner() {
        throw new AssertionError("No instance");
    }

    static void run(HttpAgentClient httpAgentClient, BenchmarkConfig benchmarkConfig) {
        BenchmarkUtils.printJavaOsInfo();
        System.out.println(benchmarkConfig);

        var concurrency = benchmarkConfig.getConcurrency();
        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(concurrency);

            warmup(executor, httpAgentClient, benchmarkConfig);

            benchmark(executor, httpAgentClient, benchmarkConfig);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    private static void benchmark(ExecutorService executor, HttpAgentClient httpAgentClient, BenchmarkConfig benchmarkConfig) {
        var requests = benchmarkConfig.getRequests();
        var concurrency = benchmarkConfig.getConcurrency();
        var postBody = BenchmarkUtils.randomContent(benchmarkConfig.getContentBytesSize());

        printStartBenchmark("GET", requests);
        long start = System.nanoTime();
        var completedGets = sendRequestAndWaitAllForCompletion(executor, requests,
                                                               benchmarkGet(httpAgentClient, benchmarkConfig));
        long end = System.nanoTime();
        printEndBenchmark("GET", requests, concurrency, Duration.ofNanos(end - start));

        printStartBenchmark("POST", requests);
        start = System.nanoTime();
        var completedPosts = sendRequestAndWaitAllForCompletion(executor, requests,
                                                                benchmarkPost(httpAgentClient, benchmarkConfig, postBody));
        end = System.nanoTime();
        printEndBenchmark("POST", requests, concurrency, Duration.ofNanos(end - start));

        benchmarkServerStatsAndReset(httpAgentClient, benchmarkConfig);

        verifyBenchmarkAllRequestSucceed(benchmarkConfig, completedGets, completedPosts);
    }

    private static void verifyBenchmarkAllRequestSucceed(BenchmarkConfig benchmarkConfig,
                                                         List<CompletableFuture<HttpClientResponse>> completedGets,
                                                         List<CompletableFuture<HttpClientResponse>> completedPosts) {
        long expectedContentReceived = benchmarkConfig.getContentBytesSize() * benchmarkConfig.getRequests() * 2;
        long completedGetSum = sumResponseBodyLength(completedGets);
        long completedPostSum = sumResponseBodyLength(completedPosts);
        var totalSum = completedGetSum + completedPostSum;
        if (totalSum < expectedContentReceived) {
            var error = "Benchmark completed exceptionally!!! received bytes '" +
                totalSum + "' are less then expected '" + expectedContentReceived + "'";
            System.out.println(error);
            throw new IllegalStateException(error);
        }

    }

    private static void printStartBenchmark(String httpMethod, int requests) {
        System.out.println("\nStart benchmarking " + httpMethod + " requests: " + requests);
    }

    private static void printEndBenchmark(String httpMethod, int requests, int concurrency, Duration duration) {
        System.out.println(httpMethod + " '" + requests + "' requests completed in: " + duration.toMillis() + " millis");
        System.out.println(httpMethod + " requests per seconds on concurrency '" + concurrency +
                               "' : " + (double) requests / duration.toSeconds());
    }

    private static void warmup(ExecutorService executor,
                               HttpAgentClient httpAgentClient,
                               BenchmarkConfig benchmarkConfig) {
        var concurrency = benchmarkConfig.getConcurrency();
        var requests = benchmarkConfig.getRequests();
        var warmupRequests = Math.max(requests / 10, concurrency);
        var postBody = BenchmarkUtils.randomContent(benchmarkConfig.getContentBytesSize());
        System.out.println("Warmup requests: " + warmupRequests);

        // warmup GET by half of total warmupRequests
        sendRequestAndWaitAllForCompletion(executor, warmupRequests / 2,
                                           benchmarkGet(httpAgentClient, benchmarkConfig));

        // warmup POST by half of total warmupRequests
        sendRequestAndWaitAllForCompletion(executor, warmupRequests / 2,
                                           benchmarkPost(httpAgentClient, benchmarkConfig, postBody));

        benchmarkServerStatsAndReset(httpAgentClient, benchmarkConfig);
    }

    private static void benchmarkServerStatsAndReset(HttpAgentClient httpAgentClient, BenchmarkConfig benchmarkConfig) {
        System.out.println("\nBenchmark server stats: " + benchmarkServerGetStats(httpAgentClient, benchmarkConfig));
        System.out.println("Benchmark server stats reset: " + benchmarkServerStatsReset(httpAgentClient, benchmarkConfig));
    }

    private static List<CompletableFuture<HttpClientResponse>> sendRequestAndWaitAllForCompletion(
        ExecutorService executor,
        int requests,
        Supplier<HttpClientResponse> httpClientResponseSupplier) {

        List<CompletableFuture<HttpClientResponse>> sendFeatures =
            IntStream.range(0, requests)
                     .mapToObj(i -> CompletableFuture.supplyAsync(httpClientResponseSupplier,
                                                                  executor))
                     .collect(Collectors.toList());
        CompletableFuture.allOf(sendFeatures.toArray(CompletableFuture[]::new)).join();
        return sendFeatures;
    }

    private static Supplier<HttpClientResponse> benchmarkGet(HttpAgentClient httpAgentClient, BenchmarkConfig benchmarkConfig) {
        return () -> httpAgentClient.get(URI.create(LOCAL_HOST + ":" + benchmarkConfig.getPort() + FIXED_RESOURCE),
                                         toHttpClientContext(benchmarkConfig));
    }

    private static Supplier<HttpClientResponse> benchmarkPost(HttpAgentClient httpAgentClient,
                                                              BenchmarkConfig benchmarkConfig,
                                                              byte[] body) {
        return () -> httpAgentClient.post(URI.create(LOCAL_HOST + ":" + benchmarkConfig.getPort() + ECHO_RESOURCE),
                                          toHttpClientContext(benchmarkConfig, body));
    }

    private static String benchmarkServerGetStats(HttpAgentClient httpAgentClient, BenchmarkConfig benchmarkConfig) {
        var response = httpAgentClient.get(URI.create(LOCAL_HOST + ":" + benchmarkConfig.getPort() + STATS_RESOURCE),
                                           toHttpClientContext(benchmarkConfig));
        return new String(response.getResponseBody(), StandardCharsets.UTF_8);
    }

    private static String benchmarkServerStatsReset(HttpAgentClient httpAgentClient, BenchmarkConfig benchmarkConfig) {
        var response = httpAgentClient.get(URI.create(LOCAL_HOST + ":" + benchmarkConfig.getPort() + STATS_RESET_RESOURCE),
                                           toHttpClientContext(benchmarkConfig));
        return new String(response.getResponseBody(), StandardCharsets.UTF_8);
    }

    private static HttpClientContext toHttpClientContext(BenchmarkConfig benchmarkConfig, byte[] body) {
        var httpClientContextBuilder = HttpClientContext.builder()
                                                        .connectTimeoutMillis(benchmarkConfig.getClientConnectTimeoutMillis())
                                                        .socketTimeoutMillis(benchmarkConfig.getClientSocketTimeoutMillis());
        List<Entry<String, List<String>>> headers = new ArrayList<>(2);
        if (!benchmarkConfig.isKeepAliveScenario()) {
            headers.add(Map.entry(HTTP_CONNECTION_HEADER, List.of(HTTP_CONNECTION_CLOSE)));
        }
        if (body != null) {
            headers.add(Map.entry(HTTP_CONTENT_TYPE_HEADER, List.of(HTTP_CONTENT_TYPE_OCTET_STREAM)));
            httpClientContextBuilder.postBody(body);
        }
        if (!headers.isEmpty()) {
            httpClientContextBuilder.headers(Map.ofEntries(headers.toArray(new Entry[0])));
        }
        return httpClientContextBuilder.build();
    }

    private static HttpClientContext toHttpClientContext(BenchmarkConfig benchmarkConfig) {
        return toHttpClientContext(benchmarkConfig, null);
    }

    private static long sumResponseBodyLength(List<CompletableFuture<HttpClientResponse>> responses) {
        return responses.stream()
                        .filter(not(CompletableFuture::isCompletedExceptionally))
                        .map(CompletableFuture::join)
                        .map(HttpClientResponse.class::cast)
                        .filter(HttpClientResponse::isSuccess)
                        .mapToLong(HttpClientResponse::responseBodyLength)
                        .sum();
    }

}
