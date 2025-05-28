package com.risk.server.api;

import com.risk.server.model.AppUser;
import com.risk.server.repo.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final AppUserRepository repo;
    private final PasswordEncoder enc;

    public UserController(AppUserRepository r, PasswordEncoder e){
        this.repo = r; this.enc = e;
    }

    public record RegisterDTO(String username,String password,String role){}

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDTO dto){
        if (repo.existsByUsername(dto.username()))
            return ResponseEntity.status(409).body("EXISTS");

        String role = "ROLE_" +
                (dto.role()==null? "GUEST" : dto.role().toUpperCase());
        repo.save(new AppUser(dto.username(), enc.encode(dto.password()), role));
        return ResponseEntity.ok("OK");
    }
}

