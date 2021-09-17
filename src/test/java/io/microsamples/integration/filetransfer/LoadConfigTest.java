package io.microsamples.integration.filetransfer;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class LoadConfigTest {

    @Test
    void readPins() throws IOException {
        final File configData = new ClassPathResource("config/load-config.json").getFile();
        final DocumentContext parse = JsonPath.parse(configData);
        List<String> read = parse.read("$..pin");
        assertThat(read.size()).isGreaterThan(0);
    }
}
