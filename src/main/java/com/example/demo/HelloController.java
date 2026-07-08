package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from the Jenkins -> Docker -> AWS EC2 pipeline demo!";
    }

    @GetMapping("/version")
    public String version() {
        return "v1.0.0";
    }
}
