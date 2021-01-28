package com.stefvic.java.httpclient.benchmark;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class HttpClientResponse {


    private final int statusCode;
    private final byte[] responseBody;

    public int responseBodyLength() {
        return responseBody == null ? 0 : responseBody.length;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
