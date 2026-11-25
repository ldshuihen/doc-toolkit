package com.toolkit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ToolControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthMissingJobAndShutdownGuard() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.desktop").value(false));

        mvc.perform(get("/api/jobs/non-existent-id"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/shutdown"))
                .andExpect(status().isForbidden());
    }
}
