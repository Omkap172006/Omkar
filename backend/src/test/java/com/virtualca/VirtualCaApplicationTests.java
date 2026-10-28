package com.virtualca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VirtualCaApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {}

    @Test
    @DisplayName("Root URL redirects to frontend index.html")
    void testRootRedirect() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/frontend/index.html"));
    }

    @Test
    @DisplayName("Admin login succeeds with admin secret key")
    void testAdminLogin() throws Exception {
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin_key\": \"admin2026\", \"email\": \"admin@virtualca.internal\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.activeAccount.role").value("Admin"));
    }

    @Test
    @DisplayName("User login fails for non-existent email")
    void testUserLoginNotFound() throws Exception {
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"nonexistent@nobody.com\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("not_found"));
    }

    @Test
    @DisplayName("Register business entity creates organization and derives state from GSTIN")
    void testRegister() throws Exception {
        String payload = """
            {
              "name": "Integration Test Corp",
              "email": "test@integration.in",
              "company": "Integration Test Corp Ltd",
              "gstin": "29TEST1234F1Z9",
              "role": "Owner"
            }
            """;

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.user.stateCode").value("29"))
            .andExpect(jsonPath("$.user.stateName").value("Karnataka"));
    }

    @Test
    @DisplayName("Get users returns tenant directory for Admin")
    void testGetUsersAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("X-Admin-Key", "admin2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("admin"))
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.accounts", notNullValue()));
    }

    @Test
    @DisplayName("Get metrics calculates revenue, expenses, bank balance, and profit")
    void testGetMetrics() throws Exception {
        mockMvc.perform(get("/api/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revenue", notNullValue()))
            .andExpect(jsonPath("$.expenses", notNullValue()))
            .andExpect(jsonPath("$.bank", notNullValue()))
            .andExpect(jsonPath("$.profit", notNullValue()))
            .andExpect(jsonPath("$.gstPayable", notNullValue()));
    }

    @Test
    @DisplayName("Post transaction records a balanced double-entry voucher")
    void testCreateTransaction() throws Exception {
        String payload = """
            {
              "type": "SALES",
              "party": "Test Client Pvt Ltd",
              "amount": 50000,
              "rate": 18,
              "inter_state": false,
              "bank": "HDFC Bank"
            }
            """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.transaction.voucher_number", notNullValue()))
            .andExpect(jsonPath("$.transaction.taxable").value(50000.0))
            .andExpect(jsonPath("$.transaction.tax").value(9000.0))
            .andExpect(jsonPath("$.transaction.amount").value(59000.0))
            .andExpect(jsonPath("$.metrics", notNullValue()));
    }

    @Test
    @DisplayName("Get compliance events returns statutory tax calendar")
    void testGetCompliance() throws Exception {
        mockMvc.perform(get("/api/compliance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Get AI insights returns automated recommendations")
    void testGetInsights() throws Exception {
        mockMvc.perform(get("/api/insights"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.insights", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Database status endpoint is strictly forbidden without Admin authorization")
    void testDbStatusForbidden() throws Exception {
        mockMvc.perform(get("/api/db-status"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.isAdmin").value(false))
            .andExpect(jsonPath("$.connected").value(false));
    }

    @Test
    @DisplayName("Database status endpoint succeeds with X-Admin-Key")
    void testDbStatusAuthorized() throws Exception {
        mockMvc.perform(get("/api/db-status")
                .header("X-Admin-Key", "admin2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.connected").value(true))
            .andExpect(jsonPath("$.table_counts", notNullValue()));
    }
}

