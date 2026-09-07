package com.sunmoon.platform.transport.http;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// "/" has no natural mapping in a REST-only service — redirect it
// somewhere useful instead of showing Spring Boot's default Whitelabel
// 404. Standalone Jetty's ROOT/index.html landing page did this job
// before the Docker switch; see Debian-Setting's docs/docker.md for the
// nginx-based unified landing page across all three services.
@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
