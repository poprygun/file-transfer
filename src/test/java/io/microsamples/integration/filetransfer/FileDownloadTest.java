package io.microsamples.integration.filetransfer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class FileDownloadTest {

    private static final String FILE_URL = "https://liw.logsa.army.mil/etmapp/api/general/search/086623/0/pdf";

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void shouldDownload(){
        File file = restTemplate.execute(FILE_URL, HttpMethod.GET, null, clientHttpResponse -> {
            File ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });

        assertThat(file).isNotNull();
        Assertions.assertThat(file.length()).isGreaterThan(0);
    }
}
