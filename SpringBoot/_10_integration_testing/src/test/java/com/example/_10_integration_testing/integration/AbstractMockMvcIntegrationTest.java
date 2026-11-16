package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.example._10_integration_testing.dto.LoginRequest;
import com.example._10_integration_testing.dto.TokenResponse;

import tools.jackson.databind.ObjectMapper;

/**
 * Lab 10 — base class for every <em>mock-servlet</em> integration test.
 *
 * <p>{@code @SpringBootTest} with the default {@code webEnvironment = MOCK} boots the same complete
 * context as the real-socket base (every bean, the real security chain, H2) but starts <b>no</b>
 * server: requests are dispatched in-process, on the test thread, through a mock servlet container.
 * That is exactly what makes {@code @Transactional} rollback work here (see
 * {@code TransactionalRollbackIsolationTest}) and what makes it useless on a real socket.
 *
 * <p>{@code @AutoConfigureMockMvc} registers a {@link MockMvcTester} bean — the AssertJ-flavoured
 * MockMvc — with Boot's customisations: every servlet {@code Filter} bean in the context is added,
 * which is how the Spring Security chain (and therefore the JWT filter) ends up in front of the
 * controllers. It does <b>not</b> apply Spring Security's test configurer, so {@code @WithMockUser}
 * has no effect on this tester (verified against Boot 4.0.6). We do not need it: we log in for real
 * and send a real Bearer token, exactly like the socket tests.
 *
 * <p>Only the seeded admin is used from this base: the MOCK context has its <em>own</em> H2 database,
 * so a clerk registered by the real-socket suite does not exist here.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractMockMvcIntegrationTest {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin12345";

    private static final Map<String, String> ACCESS_TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Serialises a request DTO with the application's own (Jackson 3) mapper. */
    protected String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    /** Reads {@code $.id} from a JSON response body. */
    protected long idOf(MvcTestResult result) {
        return objectMapper.readTree(bodyOf(result)).get("id").asLong();
    }

    protected String bodyOf(MvcTestResult result) {
        // getContentAsString declares a checked UnsupportedEncodingException; the byte[] route does not
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    /** Logs in through the real endpoint (in-process) and returns the raw access token, cached. */
    protected String loginAs(String username, String password) {
        return ACCESS_TOKENS.computeIfAbsent(username, u -> {
            MvcTestResult result = mvc.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new LoginRequest(u, password)))
                    .exchange();
            assertThat(result).hasStatusOk();
            TokenResponse tokens = objectMapper.readValue(bodyOf(result), TokenResponse.class);
            assertThat(tokens.getTokenType()).isEqualTo("Bearer");
            return tokens.getAccessToken();
        });
    }

    protected String adminBearer() {
        return "Bearer " + loginAs(ADMIN_USERNAME, ADMIN_PASSWORD);
    }
}
