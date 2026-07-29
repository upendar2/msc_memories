package com.msc.memories.controller;

//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.ui.Model; //  CORRECT SPRING IMPORT
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class AuthController {
	
	
    @GetMapping("/")
    public String showLoginPage() {
        return "login"; // renders login.html
    }

    
 // Single source of truth for /dashboard
    @GetMapping("/user-dashboard")
    public String showUserDashboard() {
        return "user-dashboard";
    }

    // Admin dashboard mapping
    @GetMapping("/admin-dashboard")
    public String showAdminDashboard() {
       
        return "admin-dashboard";
    }
}
