package com.stefvic.java.httpclient.benchmark;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JDKHttpClient implements HttpAgentClient {

    private final HttpClient httpClient;

    public JDKHttpClient(BenchmarkConfig benchmarkConfig) {
        // The default JDK pool looks  good enough
//        static final long KEEP_ALIVE = Utils.getIntegerNetProperty(
//            "jdk.httpclient.keepalive.timeout", 1200); // seconds
//        static final long MAX_POOL_SIZE = Utils.getIntegerNetProperty(
//            "jdk.httpclient.connectionPoolSize", 0); //unbounded
        this.httpClient = HttpClient.newBuilder()
                                    .followRedirects(Redirect.NEVER)
                                    .version(Version.HTTP_1_1)
                                    .connectTimeout(Duration.ofMillis(benchmarkConfig.getClientConnectTimeoutMillis()))
                                    .build();

    }

    public static void main(String[] args) {
        BenchmarkConfig benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        BenchmarkRunner.run(new JDKHttpClient(benchmarkConfig), benchmarkConfig);
    }

    @Override
    public HttpClientResponse get(URI target, HttpClientContext httpClientContext) {
        HttpRequest.Builder httpRequestBld =
            JDKHttpClientSupport.commonBuilder(target, httpClientContext)
                                .GET();

        HttpResponse<byte[]> response = JDKHttpClientSupport.sendRequest(httpClient,
                                                                         httpRequestBld.build());

        return JDKHttpClientSupport.toResponse(response);
    }

    @Override
    public HttpClientResponse post(URI target, HttpClientContext httpClientContext) {
        HttpRequest.Builder httpRequestBld =
            JDKHttpClientSupport.commonBuilder(target, httpClientContext)
                                .POST(HttpRequest.BodyPublishers.ofByteArray(httpClientContext.getPostBody()));

        HttpResponse<byte[]> response = JDKHttpClientSupport.sendRequest(httpClient,
                                                                         httpRequestBld.build());
        return JDKHttpClientSupport.toResponse(response);
    }
}
