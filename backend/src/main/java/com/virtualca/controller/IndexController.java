package com.virtualca.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping(value = {"/", ""})
    public String index() {
        return "redirect:/frontend/index.html";
    }
}

