package com.fleetmatch.user.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public String me(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return "Logged in user: " + userDetails.getUsername();
    }
}