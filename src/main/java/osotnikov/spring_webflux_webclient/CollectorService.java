package osotnikov.spring_webflux_webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Slf4j
@Service
public class CollectorService {

    private List<String> getDownloadUrls() {
        return Arrays.asList(new String[]{"http://localhost:8080/file1", "http://localhost:8080/file2"});
    }

    private void process() {
        getInParallel(getDownloadUrls());
    }

    private Mono<Data> getData(String downloadUrl) {
        return webClient()
                .get()
                .uri("/data/{downloadUrl}", downloadUrl)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(Data.class);
    }

//    private WebClient webClient() {
//        return WebClient.builder()
//                .baseUrl("http://localhost:8080/")
//
//                .build();
//    }

    // With timeout
    /*private WebClient createWebClient(int timeout) {
        TcpClient tcpClient = TcpClient.newConnection();
        HttpClient httpClient = HttpClient.from(tcpClient)
                .tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout * 1000)

                        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout))));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }*/

    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create(ConnectionProvider
                .builder("connectionProviderName")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.of(1000, ChronoUnit.MILLIS))
                .build());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:8080")
                .build();
    }

    public void getInParallel(List<String> downloadUrls) {
//        Flux.fromIterable(downloadUrls)
//                .parallel()
//                //.map()
//                .flatMap(this::getData)
//                .map((Data d) -> {
//
//                    return d;
//                })
//                .ordered((d1, d2) -> (int) (d1.getId() - d2.getId()))
//                .toStream()
//                .forEach(d -> log.info(d.toString()));
        downloadUrls.forEach(url -> {

            final WebClient client = webClient().mutate().baseUrl(url).build();
            final Flux<DataBuffer> dataBufferFlux = client.mutate()
                    // .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                    .build()
                    .get()
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()

                    .bodyToFlux(DataBuffer.class); // the magic happens here

            final Path path = FileSystems.getDefault().getPath("target/" + url.substring(url.length() - 5));
            DataBufferUtils
                    .write(dataBufferFlux, path, CREATE_NEW)
                    .block(); // only block here if the rest of your code is synchronous
        });

        // broadcast DataBuffer to multiple subscribers with ConnectableFlux then use zip to combine
        // return values from
    }

    private InputStream readAsInputStream(String url) throws IOException {
        PipedOutputStream osPipe = new PipedOutputStream();
        PipedInputStream isPipe = new PipedInputStream(osPipe);

        ClientResponse response = webClient().get().uri(url)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .block();
        final int statusCode = response.statusCode().value();
        // check HTTP status code, can throw exception if needed
        // ....

        Flux<DataBuffer> fluxDataBufferBody = response.body(BodyExtractors.toDataBuffers())
                .doOnError(t -> {
                    log.error("Error reading body.", t);
                    // close pipe to force InputStream to error,
                    // otherwise the returned InputStream will hang forever if an error occurs
                    try {
                        isPipe.close();
                    } catch (IOException ioe) {
                        log.error("Error closing streams", ioe);
                    }
                })
                .doFinally(s -> {
                    try {
                        osPipe.close();
                    } catch (IOException ioe) {
                        log.error("Error closing streams", ioe);
                    }
                });

        DataBufferUtils.write(fluxDataBufferBody, osPipe)

                .subscribe(DataBufferUtils.releaseConsumer());

        return isPipe;
    }

//    Flux<DataBuffer> body = response.getBody();
//  return body.collect(InputStreamCollector::new, (t, dataBuffer)-> t.collectInputStream(dataBuffer.asInputStream))
//            .map(inputStream -> {

//body.reduce(new InputStream() {
//        public int read() { return -1; }
//    }, (s: InputStream, d: DataBuffer) -> new SequenceInputStream(s, d.asInputStream())
//            ).flatMap(inputStream -> /* do something with single InputStream */


//    There's an input stream decorator, java.security.DigestInputStream, so that you can compute the digest while using the input stream as you normally would, instead of having to make an extra pass over the data.
//
//    MessageDigest md = MessageDigest.getInstance("MD5");
//try (InputStream is = Files.newInputStream(Paths.get("file.txt"));
//    DigestInputStream dis = new DigestInputStream(is, md))
//    {
//        /* Read decorated stream (dis) to EOF as normal... */
//    }
//    byte[] digest = md.digest();



}
