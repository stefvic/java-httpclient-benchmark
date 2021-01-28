package com.stefvic.java.httpclient.benchmark;

import java.net.URI;

public interface HttpAgentClient {

    HttpClientResponse get(URI target, HttpClientContext httpClientContext);

    HttpClientResponse post(URI target, HttpClientContext httpClientContext);
}
