package io.microsamples.integration.filetransfer;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.dsl.Http;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SpringBootApplication
public class FileTransferApplication {

    private static final String TM_URL = "https://liw.logsa.army.mil/etmapp/api/general/search/{file}/0/pdf";

    @Value("${s3.user:minioadmin}")
    private String s3User;

    @Value("${s3.password:minioadmin}")
    private String s3Password;

    @Value("${s3.url:http://localhost:9000}")
    private String s3Url;

    @Value("${s3.bucket:chachkies}")
    private String bucket;

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(FileTransferApplication.class, args);

        List<String> fileNames = fileNames();

        final FileNamesSender sender = ctx.getBean(FileNamesSender.class);
        fileNames.forEach(
                fileName -> {
                    try {
                        System.out.println("👀 processing " + fileName);
                        Thread.sleep(2000);
                        sender.send(fileName);
                        System.out.println("👀 uploaded " + fileName);
                    } catch (Throwable e) {
//                        Absent Files rase 500 Http response with exceptions instead of 404
                    }
                }
        );

        ctx.close();
    }

    private static List<String> fileNames() {
        try {
            final File configData = new ClassPathResource("config/load-config.json").getFile();
            final DocumentContext parse = JsonPath.parse(configData);
            return parse.read("$..pin");
        } catch (IOException e) {
            throw new RuntimeException("Error loading config json.");
        }
    }

    @MessagingGateway
    public interface FileNamesSender {
        @Gateway(requestChannel = "fileNames")
        void send(String insuranceIds);
    }

    @Bean
    public IntegrationFlow flow(RestTemplate restTemplate) {
        return IntegrationFlows.from("fileNames")
                .enrichHeaders(
                        h -> h.headerExpression("fileName", "payload")
                ).handle(
                        Http.outboundGateway(TM_URL, restTemplate)

                                .httpMethod(HttpMethod.GET)
                                .uriVariable("file", "payload")
                                .expectedResponseType(byte[].class)
                ).handle(
                        s3Handler()
                )
                .get();
    }

    private S3MessageHandler s3Handler() {
        var handler = new S3MessageHandler(s3Client(), bucket);
        handler.setKeyExpression(new SpelExpressionParser().parseExpression("headers.fileName"));
        return handler;
    }

    private AmazonS3 s3Client() {

        var credentials = new BasicAWSCredentials(s3User, s3Password);
        var clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new EndpointConfiguration(s3Url, Regions.US_EAST_1.name()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}

