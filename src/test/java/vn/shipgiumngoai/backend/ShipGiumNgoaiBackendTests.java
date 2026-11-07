package vn.shipgiumngoai.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.shipgiumngoai.backend.repository.BlacklistAttemptLogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShipGiumNgoaiBackendTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BlacklistAttemptLogRepository blacklistAttemptLogRepository;

    @Test
    void testJwtLoginAndProtectedEndpointsAndBlacklistAutoKick() throws Exception {
        // 1. Test Login with seeded Customer account -> Receive JWT token
        String loginJson = """
                {
                  "phone": "0901111111",
                  "password": "123456"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String jwtToken = loginNode.get("token").asText();
        assertThat(jwtToken).isNotBlank();
        assertThat(loginNode.get("user").get("role").asText()).isEqualTo("CUSTOMER");

        // 2. Test accessing /api/auth/me WITH valid JWT token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // 3. Test accessing /api/orders/my WITHOUT JWT token -> Expect 403 Forbidden
        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isForbidden());

        // 4. Test Blacklist Auto-Kick + Police/Admin Evidence Log when blacklisted CCCD tries to register
        String blacklistedDriverJson = """
                {
                  "phone": "0907776666",
                  "fullName": "Kẻ Xấu Thử Tạo Lại Acc",
                  "password": "123456",
                  "cccdNumber": "079099888777",
                  "cccdFrontPath": "cccd_front_test.jpg",
                  "cccdBackPath": "cccd_back_test.jpg",
                  "vehiclePlate": "59-Z1 999.99",
                  "cashOnHand": 300000
                }
                """;

        mockMvc.perform(post("/api/auth/register-driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blacklistedDriverJson))
                .andExpect(status().isForbidden());

        assertThat(blacklistAttemptLogRepository.count()).isGreaterThanOrEqualTo(1);
    }
}
