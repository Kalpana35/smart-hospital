package com.hospital.smart_hospital;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DoctorAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "dr.smith", roles = {"DOCTOR"})
    @DisplayName("ROLE_DOCTOR can read patients, beds, and rooms")
    void testDoctorCanReadClinicalData() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/beds"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dr.smith", roles = {"DOCTOR"})
    @DisplayName("ROLE_DOCTOR is strictly forbidden from Admin room/bed mutations")
    void testDoctorCannotMutateRoomsOrBeds() throws Exception {
        // Doctor cannot create a room -> 403 Forbidden
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomNumber\":\"505\",\"roomType\":\"ICU\",\"wardDepartment\":\"Surgery\"}"))
                .andExpect(status().isForbidden());

        // Doctor cannot create a bed -> 403 Forbidden
        mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedNumber\":\"B-99\",\"roomId\":1,\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isForbidden());

        // Doctor cannot arbitrarily override bed status via admin patch -> 403 Forbidden
        mockMvc.perform(patch("/api/beds/1/status")
                        .param("status", "MAINTENANCE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to clinical APIs is rejected with 401")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }
}

