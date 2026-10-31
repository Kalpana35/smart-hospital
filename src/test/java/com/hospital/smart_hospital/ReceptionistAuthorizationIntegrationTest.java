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
class ReceptionistAuthorizationIntegrationTest {

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
    @WithMockUser(username = "frontdesk", roles = {"RECEPTIONIST"})
    @DisplayName("ROLE_RECEPTIONIST can read patients, beds, and rooms")
    void testReceptionistCanReadOperationalData() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/beds"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/allocations/history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "frontdesk", roles = {"RECEPTIONIST"})
    @DisplayName("ROLE_RECEPTIONIST is strictly forbidden from Admin room/bed mutations")
    void testReceptionistCannotMutateRoomsOrBeds() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomNumber\":\"888\",\"roomType\":\"GENERAL\",\"wardDepartment\":\"General\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedNumber\":\"B-88\",\"roomId\":1,\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/beds/1/status")
                        .param("status", "MAINTENANCE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "frontdesk", roles = {"RECEPTIONIST"})
    @DisplayName("ROLE_RECEPTIONIST is strictly forbidden from viewing hospital analytics")
    void testReceptionistCannotViewAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/hospital"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "frontdesk", roles = {"RECEPTIONIST"})
    @DisplayName("ROLE_RECEPTIONIST is strictly forbidden from staff account administration")
    void testReceptionistCannotManageStaff() throws Exception {
        mockMvc.perform(get("/api/auth/staff"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newdoc\",\"temporaryPassword\":\"secret123\",\"fullName\":\"Dr New\",\"role\":\"ROLE_DOCTOR\"}"))
                .andExpect(status().isForbidden());
    }
}
