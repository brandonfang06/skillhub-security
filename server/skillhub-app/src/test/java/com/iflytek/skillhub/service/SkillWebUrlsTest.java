package com.iflytek.skillhub.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SkillWebUrlsTest {
    @Test
    void preservesDistinctUnicodeDeploymentPathsWithoutCanonicalizingThem() {
        assertThat(new SkillWebUrls("https://skills.example/cafe\u0301/").detail("team", "demo"))
                .isEqualTo("https://skills.example/cafe%CC%81/space/team/demo");
        assertThat(new SkillWebUrls("https://skills.example/caf\u00e9/").detail("team", "demo"))
                .isEqualTo("https://skills.example/caf%C3%A9/space/team/demo");
        assertThat(new SkillWebUrls("https://skills.example/\u212a/%2F/").detail("team", "demo"))
                .isEqualTo("https://skills.example/%E2%84%AA/%2F/space/team/demo");
    }

    @Test
    void buildsEncodedSegmentsWithConfiguredDeploymentPrefix() {
        var urls = new SkillWebUrls(" https://skills.example/team/ ");
        assertThat(urls.detail("团队 /", "a+b#?%"))
                .isEqualTo("https://skills.example/team/space/%E5%9B%A2%E9%98%9F%20%2F/a%2Bb%23%3F%25");
    }

    @Test
    void acceptsAsciiMixedCaseHostsAndPreservesExistingEscapesAndUnicodeSegments() {
        assertThat(new SkillWebUrls("HTTPS://Skills.Example:8443/%E5%9C%98/\ud83d\ude80/")
                .detail("cafe\u0301", "\u212a/\ud83d\ude80"))
                .isEqualTo("HTTPS://Skills.Example:8443/%E5%9C%98/%F0%9F%9A%80/space/cafe%CC%81/%E2%84%AA%2F%F0%9F%9A%80");
        assertThat(new SkillWebUrls("http://[::1]:8080/prefix/").detail("team", "demo"))
                .isEqualTo("http://[::1]:8080/prefix/space/team/demo");
    }

    @Test
    void rejectsUnicodeAuthorityAmbiguityAndMalformedUtf16Configuration() {
        for (String base : new String[] {"https://\u212a.example", "https://sk\u0131lls.example",
                "https://skills\uff0eexample", "https://%73kills.example", "h\u0131ttps://skills.example",
                "https://skills.example/\ud800", "https://skills.example/\udc00"}) {
            assertThat(new SkillWebUrls(base).detail("team", "demo")).as(base).isNull();
        }
    }

    @Test
    void absentOrUnsafeConfigurationNeverInventsAPublicAddress() {
        for (String base : new String[] {null, "", "  ", "//evil.example", "javascript:alert(1)",
                "https://user:password@example.com", "https://example.com?q=1", "https://example.com#x",
                "https://example.com\\evil", "https://example.com:99999", "https://example.com/\npath"}) {
            assertThat(new SkillWebUrls(base).detail("team", "demo")).isNull();
        }
    }
}
