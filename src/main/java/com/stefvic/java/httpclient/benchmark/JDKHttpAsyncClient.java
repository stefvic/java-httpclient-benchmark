package com.stefvic.java.httpclient.benchmark;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.time.Duration;

public class JDKHttpAsyncClient implements HttpAgentClient {

    private final HttpClient httpClient;

    public JDKHttpAsyncClient(BenchmarkConfig benchmarkConfig) {
        this(HttpClient.newBuilder()
                       .followRedirects(Redirect.NEVER)
                       .version(Version.HTTP_1_1)
                       .connectTimeout(Duration.ofMillis(benchmarkConfig.getClientConnectTimeoutMillis())));
    }

    public JDKHttpAsyncClient(HttpClient.Builder builder) {
        this.httpClient = builder.build();
    }

    public static void main(String[] args) {
        BenchmarkConfig benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        BenchmarkRunner.run(new JDKHttpAsyncClient(benchmarkConfig), benchmarkConfig);
    }

    @Override
    public HttpClientResponse get(URI target, HttpClientContext httpClientContext) {
        HttpRequest.Builder httpRequestBld =
            JDKHttpClientSupport.commonBuilder(target, httpClientContext)
                                .GET();
        var response = JDKHttpClientSupport.sendAsyncRequest(httpClient,
                                                             httpClientContext,
                                                             httpRequestBld.build());
        return JDKHttpClientSupport.toResponse(response);
    }

    @Override
    public HttpClientResponse post(URI target, HttpClientContext httpClientContext) {
        HttpRequest.Builder httpRequestBld =
            JDKHttpClientSupport.commonBuilder(target, httpClientContext)
                                .POST(HttpRequest.BodyPublishers.ofByteArray(httpClientContext.getPostBody()));

        var response = JDKHttpClientSupport.sendAsyncRequest(httpClient,
                                                             httpClientContext,
                                                             httpRequestBld.build());

        return JDKHttpClientSupport.toResponse(response);
    }
}
