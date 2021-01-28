package com.stefvic.java.httpclient.benchmark;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class HttpClientContext {

    private final long socketTimeoutMillis;
    private final long connectTimeoutMillis;
    private final byte[] postBody;
    @Default
    private final Map<String, List<String>> headers = Map.of();
}
