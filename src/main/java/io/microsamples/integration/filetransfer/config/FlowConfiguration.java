package io.microsamples.integration.filetransfer.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.dsl.Http;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
@Slf4j
public class FlowConfiguration {

    private static final String TM_URL = "https://liw.logsa.army.mil/etmapp/api/general/search/{file}/0/pdf";

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
        customRequestFactory.setHttpClient(httpClient);
        return builder
                .errorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return !response.getStatusCode().is2xxSuccessful();
                    }

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                        log.info("👀 Error processing pin ----------> {}, {}", response.getStatusCode().value(), response.getStatusText());
                    }
                })
                .requestFactory(() -> customRequestFactory).build();
    }

    @Bean
    public IntegrationFlow flow(RestTemplate restTemplate
            , @Value("${s3.url:http://localhost:9000}") String s3Url
            , @Value("${s3.user:minioadmin}") String s3User
            , @Value("${s3.password:minioadmin}") String s3Pwd
            , @Value("${s3.bucket:chachkies}") String bucket
    ) {
        return IntegrationFlows.from("fileNames")
                .enrichHeaders(
                        h -> h.headerExpression("fileName", "payload")
                ).handle(
                        Http.outboundGateway(TM_URL, restTemplate)
                                .httpMethod(HttpMethod.GET)
                                .uriVariable("file", "payload")
                                .expectedResponseType(byte[].class)
                ).handle(
                        s3Handler(bucket, s3Url, s3User, s3Pwd)
                )
                .get();
    }

    private S3MessageHandler s3Handler(String bucket, String s3Url, String user, String pwd) {
        var handler = new S3MessageHandler(s3Client(s3Url, user, pwd), bucket);
        handler.setKeyExpression(new SpelExpressionParser().parseExpression("headers.fileName"));
        return handler;
    }

    private AmazonS3 s3Client(String s3Url, String s3User, String pwd) {

        var credentials = new BasicAWSCredentials(s3User, pwd);
        var clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Url, Regions.US_EAST_1.name()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}
