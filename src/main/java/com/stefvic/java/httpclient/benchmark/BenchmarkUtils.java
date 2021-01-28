package com.stefvic.java.httpclient.benchmark;

import com.stefvic.java.httpclient.benchmark.BenchmarkConfig.BenchmarkConfigBuilder;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.IntFunction;

final class BenchmarkUtils {

    private BenchmarkUtils() {
        throw new AssertionError("No instance!");
    }

    static void printJavaOsInfo() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        System.out.println("*".repeat(80));
        System.out.printf("VM name: %s \n", runtimeMxBean.getVmName());
        System.out.printf("VM vendor: %s \n", runtimeMxBean.getVmVendor());
        System.out.printf("VM version: %s \n", runtimeMxBean.getVmVersion());
        System.out.printf("JVM args: %s \n", runtimeMxBean.getInputArguments());
        System.out.printf("JVM PID: %d \n", runtimeMxBean.getPid());
        System.out.printf("OS name: %s \n", System.getProperty("os.name"));
        System.out.printf("OS arch: %s \n", System.getProperty("os.arch"));
        System.out.println("*".repeat(80));
    }

    static byte[] randomContent(int contentBytesSize) {
        byte[] randomBytes = new byte[contentBytesSize];
        var posA = "A".charAt(0);
        var posZ = "Z".charAt(0);
        for (int i = 0; i < randomBytes.length; i++) {
            randomBytes[i] = (byte) ThreadLocalRandom.current()
                                                     .nextInt(posA,
                                                              posZ + 1);
        }
        return randomBytes;
    }


    static int secondsToMillis(int seconds) {
        return Math.toIntExact(Duration.ofSeconds(seconds).toMillis());
    }

    static BenchmarkConfig buildFormSysProperties() {
        BenchmarkConfigBuilder builder = BenchmarkConfig.builder();
        setIfPresentIntSysProperty(builder::concurrency, "benchmark.concurrency");
        setIfPresentIntSysProperty(builder::port, "benchmark.server.port");
        setIfPresentIntSysProperty(builder::requests, "benchmark.requests");
        setIfPresentIntSysProperty(builder::contentBytesSize, "benchmark.content.bytes.size");
        setIfPresentIntSysProperty(builder::keepAliveScenario, "benchmark.keep.alive.scenario");
        setIfPresentIntSysProperty(builder::serverKeepAliveMillis, "benchmark.server.keep.alive.millis");
        setIfPresentIntSysProperty(builder::clientSocketTimeoutMillis, "benchmark.client.socket.timeout.millis");
        setIfPresentIntSysProperty(builder::clientConnectTimeoutMillis, "benchmark.client.connect.timeout.millis");

        return builder.build();
    }


    private static void setIfPresentIntSysProperty(IntFunction<BenchmarkConfigBuilder> setter,
                                                   String sysProperty) throws NumberFormatException {
        String sysPropVal = System.getProperty(sysProperty);
        if (sysPropVal == null) {
            return;
        }
        setter.apply(Integer.parseInt(sysPropVal));
    }

    private static void setIfPresentIntSysProperty(Function<Boolean, BenchmarkConfigBuilder> setter,
                                                   String sysProperty) throws NumberFormatException {
        String sysPropVal = System.getProperty(sysProperty);
        if (sysPropVal == null) {
            return;
        }
        setter.apply(Boolean.parseBoolean(sysPropVal));
    }
}
