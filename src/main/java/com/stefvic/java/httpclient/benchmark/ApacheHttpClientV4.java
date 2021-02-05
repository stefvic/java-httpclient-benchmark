package com.stefvic.java.httpclient.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import org.apache.http.HttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class ApacheHttpClientV4 implements HttpAgentClient {

    private final CloseableHttpClient client;

    public ApacheHttpClientV4(BenchmarkConfig benchmarkConfig) {
        var concurrency = benchmarkConfig.getConcurrency();
        var soTimeoutMillis = benchmarkConfig.getClientSocketTimeoutMillis();
        var connectTimeoutMillis = benchmarkConfig.getClientConnectTimeoutMillis();
        this.client = HttpClientBuilder.create()
                                       .setMaxConnPerRoute(concurrency + 10)
                                       .setMaxConnTotal(concurrency * 2)
                                       .setDefaultSocketConfig(
                                           SocketConfig.custom()
                                                       .setSoTimeout(soTimeoutMillis)
                                                       .build())
                                       .setDefaultRequestConfig(
                                           RequestConfig.custom()
                                                        .setConnectTimeout(connectTimeoutMillis)
                                                        .build())
                                       .build();
    }

    public static void main(String[] args) {
        BenchmarkConfig benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        BenchmarkRunner.run(new ApacheHttpClientV4(benchmarkConfig), benchmarkConfig);
    }

    private static void addHeaders(HttpMessage httpMessage, HttpClientContext httpClientContext) {
        httpClientContext.getHeaders()
                         .forEach((name, values) -> values.forEach(value -> httpMessage.addHeader(name, value)));
    }

    private static HttpClientResponse toHttpClientResponse(CloseableHttpResponse response) throws IOException {
        var statusCode = response.getStatusLine().getStatusCode();
        var entity = response.getEntity();
        var responseBody = entity == null ? null : EntityUtils.toByteArray(entity);
        return HttpClientResponse.builder()
                                 .statusCode(statusCode)
                                 .responseBody(responseBody)
                                 .build();
    }

    @Override
    public HttpClientResponse get(URI target, HttpClientContext httpClientContext) {
        HttpGet httpGet = new HttpGet(target);
        httpGet.setProtocolVersion(HttpVersion.HTTP_1_1);
        addHeaders(httpGet, httpClientContext);
        try (var response = client.execute(httpGet)) {
            return toHttpClientResponse(response);
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
    }

    @Override
    public HttpClientResponse post(URI target, HttpClientContext httpClientContext) {
        HttpPost httpPost = new HttpPost(target);
        httpPost.setProtocolVersion(HttpVersion.HTTP_1_1);
        addHeaders(httpPost, httpClientContext);
        var entity = new ByteArrayEntity(httpClientContext.getPostBody());
        httpPost.setEntity(entity);
        try (var response = client.execute(httpPost)) {
            return toHttpClientResponse(response);
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
    }
}
