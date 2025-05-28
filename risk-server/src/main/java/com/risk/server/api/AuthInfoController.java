package com.risk.server.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthInfoController {

    /** Возвращает: {"role":"ROLE_ADMIN"} */
    @GetMapping("/role")
    public Map<String,String> role(Authentication auth){
        String r = auth.getAuthorities().stream()              // здесь stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_GUEST");
        return Map.of("role", r);                              // Map.of — JDK 9+
    }
}
