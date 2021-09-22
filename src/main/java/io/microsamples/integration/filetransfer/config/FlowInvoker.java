package io.microsamples.integration.filetransfer.config;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FlowInvoker {
    private FlowInvokerConfiguration.FileNamesSender sender;
    private MeterRegistry meterRegistry;
    private Counter counter;

    FlowInvoker(FlowInvokerConfiguration.FileNamesSender sender, MeterRegistry meterRegistry) {
        this.sender = sender;
        this.meterRegistry = meterRegistry;

        counter = Counter.builder("uploaded.tm")
                .tag("type", "pdf")
                .description("The number of PDF files uploaded to S3.")
                .register(meterRegistry);
    }

    @Async("processExecutor")
    public void invoke() {
        List<String> fileNames = fileNames();

        fileNames.forEach(
                fileName -> {
                    try {
                        System.out.println("👀 processing " + fileName);
                        counter.increment(-counter.count()); //reset the counter
                        Thread.sleep(1000);
                        sender.send(fileName);
                        System.out.println("👀 uploaded " + fileName);
                        counter.increment();
                    } catch (Throwable e) {
                        System.out.println(e.getMessage());
                    }
                }
        );
    }

    private static List<String> fileNames() {
        try {
            final File configData = new ClassPathResource("config/load-config.json").getFile();
            final DocumentContext parse = JsonPath.parse(configData);
            return parse.read("$.[?(@['disCode'] == 'A')].pin");
        } catch (IOException e) {
            throw new RuntimeException("Error loading config json.");
        }
    }
}
