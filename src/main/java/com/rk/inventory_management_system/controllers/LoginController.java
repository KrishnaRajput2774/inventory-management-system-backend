package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.LoginDto;
import com.rk.inventory_management_system.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class LoginController {


    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginDto loginDto,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {

        log.info("Logging in");
        try {
            String token = authService.login(loginDto.getUserName(), loginDto.getPassword());

            // For local development (different ports)
            ResponseCookie cookie = ResponseCookie.from("token", token)
                    .httpOnly(true)
                    .secure(false)                    // false for local development
                    .path("/")
                    .maxAge(Duration.ofHours(1))
                    .sameSite("Lax")                  // Use "Lax" instead of "None" for local dev
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Return JSON response for better frontend handling
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Login Successful");
            responseBody.put("token", token); // Optional: for debugging

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("logging out");

        String token = Arrays.stream(request.getCookies())
                .filter(cookie -> "token".equals(cookie.getName()))
                .findFirst()
                .map(cookie -> cookie.getValue())
                .orElseThrow(()->new AuthenticationServiceException("Jwt token token not Found in Cookie"));
        log.info("after cookies fetching refresh token");

        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/"); // ADD THIS LINE - it was missing

        response.addCookie(cookie); // Make sure this line is present

        log.info("Ended");
        return ResponseEntity.ok("Logout successfully");
    }

}
