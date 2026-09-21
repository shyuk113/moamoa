package com.moamoa.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthViewController {
  @GetMapping("/auth/login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("/auth/signup")
  public String signup() {
    return "auth/signup";
  }

  @GetMapping("/auth/forgot-password")
  public String forgot() {
    return "auth/forgot-password";
  }

  @GetMapping("/auth/reset-password")
  public String reset(@RequestParam(defaultValue = "") String token, Model model) {
    model.addAttribute("token", token);
    return "auth/reset-password";
  }
}
