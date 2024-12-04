package osotnikov.spring_webflux_webclient;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@EnableWireMock
class CollectorServiceWmTest {

    @Value("${wiremock.server.baseUrl}")
    private String wiremockUrl;

    private CollectorService collectorService = new CollectorService();

    @BeforeEach
    void setUp() {

    }

    @Test
    public void test() {
        stubFor(get("/ping")
            .willReturn(aResponse().withStatus(200)));

        RestAssured
            .when()
            .get(this.wiremockUrl + "/ping")
            .then()
            .statusCode(200);
    }

    @Test
    public void testDownload() throws IOException {
        for (int i = 1 ; i <= 5 ; i++) {
//            stubFor(get(this.wiremockUrl + "/" + i + ".jpg")
//                    .willReturn(aResponse().withStatus(200)
//                            .withHeader("Content-Type", "application/binary")
//                            .withBodyFile(i + ".jpg")));

            InputStream is = RestAssured
                    .when()
                    .get(this.wiremockUrl + "/" + i + ".jpg")
                    .then()
                    .statusCode(200)
                    .extract().asInputStream();

            File targetFile = new File("src/main/resources/" + i + ".cp.jpg" );

            FileUtils.copyInputStreamToFile(is, targetFile);
        }
    }

    @Test
    public void testDownload2() throws IOException {

        List<String> urls = new LinkedList<>();
        String url;
        for (int i = 1 ; i <= 5 ; i++) {
            url = wiremockUrl + "/" + i + ".jpg";
            urls.add(url);
//            stubFor(get(url)
//                    .willReturn(aResponse().withStatus(200)
//                            .withHeader("Content-Type", "application/binary")
//                            .withBodyFile(i + ".jpg")));
        }

        collectorService.getInParallel(urls);

//        File targetFile = new File("src/main/resources/" + i + ".cp.jpg" );
//
//        FileUtils.copyInputStreamToFile(is, targetFile);
    }

}