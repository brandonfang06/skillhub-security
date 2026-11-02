package com.iflytek.skillhub.ratelimit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.iflytek.skillhub.config.DownloadRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.Cookie;

class AnonymousDownloadIdentityServiceTest {

    @Test
    void transportSecurityUsesTheFrameworkRequestRatherThanReadingForwardedHeadersAgain() {
        var properties = new DownloadRateLimitProperties();
        properties.setAnonymousCookieSecret(java.util.UUID.randomUUID().toString());
        var service = new AnonymousDownloadIdentityService(properties, new ClientIpResolver());
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-Proto", "https");
        var response = new MockHttpServletResponse();

        service.resolve(request, response);

        assertThat(response.getHeader("Set-Cookie")).doesNotContain("Secure");
        request.setSecure(true);
        var secureResponse = new MockHttpServletResponse();
        service.resolve(request, secureResponse);
        assertThat(secureResponse.getHeader("Set-Cookie")).contains("Secure", "HttpOnly");
        assertThat(secureResponse.getCookie(properties.getAnonymousCookieName()).getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(secureResponse.getHeader("Location")).isNull();
    }

    @Test
    void anonymousIdentityIssuesHostOnlySignedCookieWithoutRedirecting() {
        var properties = new DownloadRateLimitProperties();
        properties.setAnonymousCookieSecret(java.util.UUID.randomUUID().toString());
        var service = new AnonymousDownloadIdentityService(properties, new ClientIpResolver());
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("Host", "attacker.example");
        request.addHeader("X-Forwarded-Host", "attacker.example");
        var response = new MockHttpServletResponse();

        var identity = service.resolve(request, response);

        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        String cookie = response.getHeader("Set-Cookie");
        assertThat(cookie).contains("HttpOnly", "Path=/")
                .doesNotContain("Domain=", "attacker.example");
        assertThat(response.getCookie(properties.getAnonymousCookieName()).getAttribute("SameSite")).isEqualTo("Lax");
        String value = cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
        request.setCookies(new Cookie(properties.getAnonymousCookieName(), value));
        var reused = new MockHttpServletResponse();
        assertThat(service.resolve(request, reused)).isEqualTo(identity);
        assertThat(reused.getHeader("Set-Cookie")).isNull();

        String tampered = "v1.changed." + value.substring(value.lastIndexOf('.') + 1);
        request.setCookies(new Cookie(properties.getAnonymousCookieName(), tampered));
        var replaced = new MockHttpServletResponse();
        assertThat(service.resolve(request, replaced).cookieHash()).isNotEqualTo(identity.cookieHash());
        assertThat(replaced.getHeader("Set-Cookie")).isNotNull();
    }

    @Test
    void validateAnonymousCookieSecretRejectsReleaseExamplePlaceholder() {
        DownloadRateLimitProperties properties = new DownloadRateLimitProperties();
        properties.setAnonymousCookieSecret("replace-with-random-download-secret-32-bytes");
        AnonymousDownloadIdentityService service = new AnonymousDownloadIdentityService(properties, new ClientIpResolver());

        assertThatThrownBy(service::validateAnonymousCookieSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not use the default placeholder");
    }
}
