package com.stefvic.java.httpclient.benchmark;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaders;
import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

public class ReactorNettyClient implements HttpAgentClient {

    private final HttpClient client;

    public ReactorNettyClient(BenchmarkConfig benchmarkConfig) {
        this.client = HttpClient.create(ConnectionProvider.builder("http")
                                                          .maxConnections(benchmarkConfig.getConcurrency())
                                                          .build())
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, benchmarkConfig.getClientConnectTimeoutMillis())
                                .responseTimeout(Duration.ofMillis(benchmarkConfig.getClientSocketTimeoutMillis()));
    }

    private static Consumer<HttpHeaders> addHeaders(HttpClientContext httpClientContext) {
        return httpHeaders -> httpClientContext.getHeaders()
                                               .forEach((name, values) -> values.forEach(
                                                   value -> httpHeaders.add(name, value)));
    }

    public static void main(String[] args) {
        BenchmarkConfig benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        BenchmarkRunner.run(new ReactorNettyClient(benchmarkConfig), benchmarkConfig);
    }


    @Override
    public HttpClientResponse get(URI target, HttpClientContext httpClientContext) {
        return client.headers(addHeaders(httpClientContext))
                     .get()
                     .uri(target)
                     .responseSingle((r, bytes) ->
                                         bytes.asByteArray()
                                              .map(t -> HttpClientResponse.builder()
                                                                          .statusCode(r.status().code())
                                                                          .responseBody(t)
                                                                          .build()))
                     .block();
    }

    @Override
    public HttpClientResponse post(URI target, HttpClientContext httpClientContext) {
        return client.headers(addHeaders(httpClientContext))
                     .post()
                     .uri(target)
                     .send(Flux.just(Unpooled.wrappedBuffer(httpClientContext.getPostBody())))
                     .responseSingle((r, bytes) ->
                                         bytes.asByteArray()
                                              .map(t -> HttpClientResponse.builder()
                                                                          .statusCode(r.status().code())
                                                                          .responseBody(t)
                                                                          .build()))
                     .block();
    }
}
