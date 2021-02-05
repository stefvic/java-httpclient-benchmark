package com.stefvic.java.httpclient.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpClientV4 implements HttpAgentClient {

    private final OkHttpClient client;

    public OkHttpClientV4(BenchmarkConfig config) {
        var dispatcher = new Dispatcher();
        var concurrency = config.getConcurrency();
        dispatcher.setMaxRequestsPerHost(concurrency + 10);
        dispatcher.setMaxRequests(concurrency * 2);
        this.client =
            new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(Duration.ofMillis(config.getClientConnectTimeoutMillis()))
                .readTimeout(Duration.ofMillis(config.getClientSocketTimeoutMillis()))
                .build();
    }

    private static void addHeaders(Request.Builder request, HttpClientContext httpClientContext) {
        httpClientContext.getHeaders()
                         .forEach((name, values) -> values.forEach(value -> request.header(name, value)));
    }

    private static HttpClientResponse toHttpClientResponse(Response response) throws IOException {
        return HttpClientResponse.builder()
                                 .statusCode(response.code())
                                 .responseBody(response.body().bytes())
                                 .build();
    }

    public static void main(String[] args) {
        BenchmarkConfig benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        BenchmarkRunner.run(new OkHttpClientV4(benchmarkConfig), benchmarkConfig);
    }

    @Override
    public HttpClientResponse get(URI target, HttpClientContext httpClientContext) {
        try {
            var requestBld = new Request.Builder().url(target.toURL()).get();
            addHeaders(requestBld, httpClientContext);
            var response = client.newCall(requestBld.build()).execute();
            return toHttpClientResponse(response);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
    }

    @Override
    public HttpClientResponse post(URI target, HttpClientContext httpClientContext) {
        try {
            RequestBody body = RequestBody.create(httpClientContext.getPostBody());
            var requestBld = new Request.Builder()
                .url(target.toURL())
                .post(body);
            addHeaders(requestBld, httpClientContext);
            var response = client.newCall(requestBld.build()).execute();
            return toHttpClientResponse(response);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
    }
}
