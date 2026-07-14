package com.yusuf.audittool.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoPageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/demo/index.html";
    }
}
