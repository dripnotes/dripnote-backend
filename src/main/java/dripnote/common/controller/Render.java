package dripnote.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Render {
    @GetMapping("/signin")
    public String signin() {
        return "signin";
    }

}