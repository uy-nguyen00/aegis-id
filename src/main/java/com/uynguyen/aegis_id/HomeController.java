package com.uynguyen.aegis_id;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@Tag(name = "Home", description = "Homepage for dev environment")
public class HomeController {

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/")
    public String home() {
        return (
            "Welcome to " +
            appName +
            "<br> <a href=\"/swagger-ui/index.html\">API Docs</a>"
        );
    }
}
