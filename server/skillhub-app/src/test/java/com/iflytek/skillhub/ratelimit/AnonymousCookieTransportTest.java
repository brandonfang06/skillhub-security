package com.iflytek.skillhub.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.iflytek.skillhub.config.DownloadRateLimitProperties;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnonymousCookieTransportTest {
    @TempDir Path directory;

    @Test
    void servletEmitsHostOnlyHttpOnlySameSiteCookieOverRealHttp() throws Exception {
        var properties = new DownloadRateLimitProperties();
        properties.setAnonymousCookieSecret(UUID.randomUUID().toString());
        var service = new AnonymousDownloadIdentityService(properties, new ClientIpResolver());
        var server = new Tomcat();
        server.setBaseDir(directory.resolve("server").toString());
        server.setPort(0);
        server.getConnector().setProperty("address", "127.0.0.1");
        var context = server.addContext("", Files.createDirectory(directory.resolve("web")).toString());
        Tomcat.addServlet(context, "identity", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) {
                service.resolve(request, response);
            }
        });
        context.addServletMappingDecoded("/", "identity");
        try {
            server.start();
            try (var client = HttpClient.newHttpClient()) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                        + server.getConnector().getLocalPort() + "/")).timeout(Duration.ofSeconds(10)).build();
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie ->
                        assertThat(cookie).contains("HttpOnly", "SameSite=Lax", "Path=/", "Max-Age=2592000")
                                .doesNotContain("Domain="));
                assertThat(response.headers().firstValue("Location")).isEmpty();
            }
        } finally {
            server.stop();
            server.destroy();
        }
    }
}
