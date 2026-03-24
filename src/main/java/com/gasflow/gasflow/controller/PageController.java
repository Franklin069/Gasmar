package com.gasflow.gasflow.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/pabs")
    public String pabs(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }
        return "pabs";
    }

    @GetMapping("/processo")
    public String processDetail(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }
        return "process-detail";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }
        return "notifications";
    }
}