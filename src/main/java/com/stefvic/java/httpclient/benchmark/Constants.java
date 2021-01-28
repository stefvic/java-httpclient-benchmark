package com.stefvic.java.httpclient.benchmark;

final class Constants {

    static final String HTTP_CONNECTION_HEADER = "Connection";
    static final String HTTP_CONNECTION_CLOSE = "Close";

    static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    static final String HTTP_CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private Constants() {
        throw new AssertionError("No instance");
    }
}
