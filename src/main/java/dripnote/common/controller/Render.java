package dripnote.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Render {
    @GetMapping("/main")
    public String main() {
        return "index";
    }

    @GetMapping("/bean")
    public String bean() {
        return "bean";
    }

    @GetMapping("/bean_detail")
    public String bean_detail() {
        return "bean_detail";
    }

    @GetMapping("/lesson")
    public String lesson() {
        return "lesson";
    }

    @GetMapping("/lesson_detail")
    public String lesson_detail() {
        return "lesson_detail";
    }

    @GetMapping("/lesson_reservation")
    public String lesson_reservation() {
        return "lesson_reservation";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "mypage";
    }

    @GetMapping("/signin")
    public String signin() {
        return "signin";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }
}
