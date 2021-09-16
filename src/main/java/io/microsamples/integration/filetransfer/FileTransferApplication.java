package io.microsamples.integration.filetransfer;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.dsl.Http;

@SpringBootApplication
public class FileTransferApplication {

    @Autowired
    private FileNamesSender sender;

    public static void main(String[] args) {
        SpringApplication.run(FileTransferApplication.class, args);
    }

    @Bean
    public CommandLineRunner CommandLineRunnerBean() {
        return (args) -> {
            sender.send("test.pdf");
        };
    }


    @MessagingGateway
    public interface FileNamesSender {
        @Gateway(requestChannel = "fileNames")
        void send(String insuranceIds);
    }

    @Bean
    public IntegrationFlow flow() {
        return IntegrationFlows.from("fileNames")
                .enrichHeaders(
                        h -> h.headerExpression("fileName", "payload")
                )
                .handle(
                        Http.outboundGateway("https://s2.q4cdn.com/498544986/files/doc_downloads/{file}")
                                .httpMethod(HttpMethod.GET)
                                .uriVariable("file", "payload")
                                .expectedResponseType(byte[].class)
                ).handle(
                        s3Handler()
                )
                .get();
    }

    private S3MessageHandler s3Handler() {
        var handler = new S3MessageHandler(s3Client(), "chachkies");
        handler.setKeyExpression(new SpelExpressionParser().parseExpression("headers.fileName"));
        return handler;
    }

    private AmazonS3 s3Client() {

        var credentials = new BasicAWSCredentials("minioadmin", "minioadmin");
        var clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new EndpointConfiguration("http://localhost:9000", Regions.US_EAST_1.name()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}

