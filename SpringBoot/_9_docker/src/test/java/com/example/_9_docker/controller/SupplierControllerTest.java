package com.example._9_docker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._9_docker.dto.SupplierRequestDto;
import com.example._9_docker.dto.SupplierResponseDto;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.security.SecurityConfig;
import com.example._9_docker.service.SupplierService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(SupplierController.class)
@Import(SecurityConfig.class)
@DisplayName("SupplierController")
class SupplierControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private SupplierService supplierService;
    @MockitoBean private UserDetailsService userDetailsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private byte[] json(Object o) throws Exception {
        return objectMapper.writeValueAsBytes(o);
    }

    private SupplierResponseDto acme() {
        return new SupplierResponseDto(5L, "Acme Metals", "sales@acme.io", "+1 555 0100");
    }

    @Test
    @DisplayName("GET 401 when anonymous")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/suppliers")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 201 + Location as ADMIN")
    void createAsAdmin() throws Exception {
        given(supplierService.create(any(SupplierRequestDto.class))).willReturn(acme());

        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SupplierRequestDto("Acme Metals", "sales@acme.io", "+1 555 0100"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/suppliers/5")))
                .andExpect(jsonPath("$.email").value("sales@acme.io"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("POST 403 as CLERK")
    void createAsClerkForbidden() throws Exception {
        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SupplierRequestDto("Acme Metals", null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 400 when the email is not an address")
    void createBadEmail() throws Exception {
        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SupplierRequestDto("Acme Metals", "not-an-email", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email must be a valid address"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /{id} 200 as CLERK")
    void getById() throws Exception {
        given(supplierService.getById(5L)).willReturn(acme());

        mockMvc.perform(get("/api/suppliers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Metals"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE 204 as ADMIN")
    void deleteAsAdmin() throws Exception {
        mockMvc.perform(delete("/api/suppliers/5")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE 409 when products still reference the supplier")
    void deleteInUse() throws Exception {
        willThrow(new ResourceInUseException("Supplier", 5L, "products")).given(supplierService).delete(5L);

        mockMvc.perform(delete("/api/suppliers/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }
}
