package com.example.sqs.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("web")
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/queues";
    }
}
