package com.donggree.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({SecurityConfig.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void auth_엔드포인트는_인증_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk());
    }

    @RestController
    public static class TestController {

        @GetMapping("/auth/test")
        String authTest() {
            return "ok";
        }
    }
}
