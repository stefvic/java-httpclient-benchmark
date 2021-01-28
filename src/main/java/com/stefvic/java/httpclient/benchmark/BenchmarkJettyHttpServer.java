package com.stefvic.java.httpclient.benchmark;

import static com.stefvic.java.httpclient.benchmark.BenchmarkUtils.printJavaOsInfo;
import static com.stefvic.java.httpclient.benchmark.Constants.HTTP_CONTENT_TYPE_HEADER;
import static com.stefvic.java.httpclient.benchmark.Constants.HTTP_CONTENT_TYPE_OCTET_STREAM;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class BenchmarkJettyHttpServer implements AutoCloseable {

    public static final String FIXED_RESOURCE = "/fixed";
    public static final String ECHO_RESOURCE = "/echo";
    public static final String STATS_RESOURCE = "/stats";
    public static final String STATS_RESET_RESOURCE = "/stats/reset";
    private final Server server;
    private final int port;

    public BenchmarkJettyHttpServer(BenchmarkConfig benchmarkConfig) {
        int minThreads = benchmarkConfig.getConcurrency();
        int maxThreads = minThreads * 2; // ++ buffer
        int keepAlive = benchmarkConfig.getServerKeepAliveMillis();

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
        this.server = new Server(threadPool);
        this.port = benchmarkConfig.getPort();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(benchmarkConfig.getPort());
        connector.setIdleTimeout(keepAlive);
        server.addConnector(connector);
        server.setHandler(new BenchmarkHandler(benchmarkConfig));
    }

    public static void main(String[] args) throws Exception {
        var benchmarkConfig = BenchmarkUtils.buildFormSysProperties();
        System.out.println(benchmarkConfig);
        try (var server = new BenchmarkJettyHttpServer(benchmarkConfig)) {
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception ignore) {
                }
            }));

            server.join();
        }
    }

    public void start() throws Exception {
        server.start();
        printJavaOsInfo();
        System.out.println("Benchmark server is listening on port " + port);
    }

    public void stop() throws Exception {
        System.out.println("Stopping benchmark server");
        server.stop();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    @Override
    public void close() throws Exception {
        stop();
    }


    static final class BenchmarkHandler extends AbstractHandler {

        private final BenchmarkConfig benchmarkConfig;
        private final byte[] fixedContentResponse;
        private final LongAdder totalRequestHandled = new LongAdder();
        private final LongAdder fixedRequestHandled = new LongAdder();
        private final LongAdder echoRequestHandled = new LongAdder();

        public BenchmarkHandler(BenchmarkConfig benchmarkConfig) {
            this.benchmarkConfig = benchmarkConfig;
            this.fixedContentResponse = BenchmarkUtils.randomContent(benchmarkConfig.getContentBytesSize());
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
            if (target.equals(STATS_RESOURCE)) {
                stats(response);
                //do not increment total request handle on stats
                return;
            } else if (target.equals(STATS_RESET_RESOURCE)) {
                statsReset(response);
                //do not increment total request handle on stats
                return;
            } else if (target.equals(FIXED_RESOURCE)) {
                // to be used for get fixed content payload response
                fixed(response);
                fixedRequestHandled.increment();
            } else if (target.equals(ECHO_RESOURCE)) {
                // to be used fro post
                echo(request, response);
                echoRequestHandled.increment();
            } else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                try (Writer writer = response.getWriter()) {
                    writer.write("Resource not found: " + target);
                    writer.flush();
                }
            }
            totalRequestHandled.increment();
        }

        private void echo(HttpServletRequest request, HttpServletResponse response) {
            try {
                ByteArrayOutputStream2 buffer = new ByteArrayOutputStream2();
                InputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    IO.copy(inputStream, buffer);
                }
                final byte[] requestContent = buffer.getBuf();
                //final int requestContentCount = buffer.getCount();

                response.setStatus(200);
                //response.setContentLength(requestContentCount);
                response.setHeader(HTTP_CONTENT_TYPE_HEADER, HTTP_CONTENT_TYPE_OCTET_STREAM);

                OutputStream outputStream = response.getOutputStream();
                IO.copy(new ByteArrayInputStream(requestContent), outputStream);
                outputStream.flush();
                response.flushBuffer();
            } catch (IOException ioEx) {
                throw new UncheckedIOException(ioEx);
            }
        }

        private void fixed(final HttpServletResponse response) throws IOException {
            response.setStatus(200);
            //response.setContentLength(benchmarkConfig.getContentBytesSize());
            response.setHeader(HTTP_CONTENT_TYPE_HEADER, HTTP_CONTENT_TYPE_OCTET_STREAM);

            OutputStream outputStream = response.getOutputStream();
            IO.copy(new ByteArrayInputStream(fixedContentResponse), outputStream);
            outputStream.flush();
        }

        private void stats(final HttpServletResponse response) throws IOException {
            response.setStatus(200);
            byte[] stats =
                ("total:" + totalRequestHandled.sum() +
                    ",fixed:" + fixedRequestHandled.sum() +
                    "echo:" + echoRequestHandled.sum())
                    .getBytes(StandardCharsets.UTF_8);

            response.setContentLength(stats.length);
            response.setHeader(HTTP_CONTENT_TYPE_HEADER, HTTP_CONTENT_TYPE_OCTET_STREAM);

            OutputStream outputStream = response.getOutputStream();
            IO.copy(new ByteArrayInputStream(stats), outputStream);
            outputStream.flush();
        }

        private void statsReset(final HttpServletResponse response) throws IOException {
            totalRequestHandled.reset();
            fixedRequestHandled.reset();
            echoRequestHandled.reset();

            response.setStatus(200);
            byte[] stats = "Ok".getBytes(StandardCharsets.UTF_8);

            response.setContentLength(stats.length);
            response.setHeader(HTTP_CONTENT_TYPE_HEADER, HTTP_CONTENT_TYPE_OCTET_STREAM);

            OutputStream outputStream = response.getOutputStream();
            IO.copy(new ByteArrayInputStream(stats), outputStream);
            outputStream.flush();
        }
    }
}
