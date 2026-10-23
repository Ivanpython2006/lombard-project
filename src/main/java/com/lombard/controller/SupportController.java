package com.lombard.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SupportController {

    @GetMapping("/support")
    @PreAuthorize("hasRole('CLIENT')")
    public String support() {
        return "support";
    }
}