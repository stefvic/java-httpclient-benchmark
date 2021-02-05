package com.stefvic.java.httpclient.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class JDKHttpClientSupport {

    private JDKHttpClientSupport() {
        throw new AssertionError("No instance");
    }

    static void addHeaders(HttpRequest.Builder httpRequestBld, Map<String, List<String>> headers) {
        headers.forEach((name, values) -> values.forEach(value -> httpRequestBld.header(name, value)));
    }

    static HttpRequest.Builder commonBuilder(URI uri, HttpClientContext clientContext) {
        var httpRequestBld = HttpRequest.newBuilder()
                                        .timeout(Duration.ofMillis(clientContext.getSocketTimeoutMillis()))
                                        .uri(uri);
        addHeaders(httpRequestBld, clientContext.getHeaders());
        return httpRequestBld;
    }

    static HttpClientResponse toResponse(HttpResponse<byte[]> response) {
        return HttpClientResponse.builder()
                                 .statusCode(response.statusCode())
                                 .responseBody(response.body())
                                 .build();
    }


    static HttpResponse<byte[]> sendRequest(HttpClient client, HttpRequest httpRequest) {
        try {
            return client.send(httpRequest,
                               BodyHandlers.ofByteArray());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        } catch (InterruptedException interruptedEx) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedEx);
        }
    }

    static HttpResponse<byte[]> sendAsyncRequest(HttpClient client,
                                                 HttpClientContext httpClientContext,
                                                 HttpRequest httpRequest) {
        try {
            return client.sendAsync(httpRequest,
                                    BodyHandlers.ofByteArray())
                         .get(waitAsyncResponseTime(httpClientContext),
                              TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException interruptedEx) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedEx);
        }
    }

    private static long waitAsyncResponseTime(HttpClientContext httpClientContext) {
        return httpClientContext.getSocketTimeoutMillis() +
            httpClientContext.getConnectTimeoutMillis() +
            10000; //buffer
    }

}
