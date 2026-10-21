package com.se201411.project.mooncake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Temporary controller for pages whose feature-specific backend is not implemented yet.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String homepage() {
        return "homepage";
    }

}
