package com.iflytek.skillhub.compat;

import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.auth.device.DeviceAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WellKnownControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NamespaceMemberRepository namespaceMemberRepository;

    @MockitoBean
    private DeviceAuthService deviceAuthService;

    @Test
    void clawhubConfig_returns_apiBase() throws Exception {
        mockMvc.perform(get("/.well-known/clawhub.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiBase").value("/api/v1"))
                .andExpect(jsonPath("$.capabilities[0]").value("skill-suite-v1"));
    }

    @Test
    void clawhubConfig_includes_forwarded_prefix() throws Exception {
        mockMvc.perform(get("/.well-known/clawhub.json").header("X-Forwarded-Prefix", "/skillhub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiBase").value("/skillhub/api/v1"));
    }
}
