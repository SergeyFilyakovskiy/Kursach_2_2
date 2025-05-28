package com.risk.server.config;

import com.risk.server.model.AppUser;
import com.risk.server.repo.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean public PasswordEncoder encoder(){ return new BCryptPasswordEncoder(); }

    /* JPA-база → UserDetails */
    @Bean
    public UserDetailsService uds(AppUserRepository repo){
        return username -> repo.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .authorities(u.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    /* правила доступа */
    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/register","/api/role").permitAll()
                        .requestMatchers("/api/**").authenticated()   // ВСЕ защищённые
                        .anyRequest().permitAll()                    // ← в самом конце
                );
        return http.build();
    }


    /* добавляем дефолтные учётки один раз */
    @Bean
    CommandLineRunner init(AppUserRepository repo, PasswordEncoder enc){
        return args->{
            if(!repo.existsByUsername("admin"))
                repo.save(new AppUser("admin",enc.encode("admin"),"ROLE_ADMIN"));
            if(!repo.existsByUsername("analyst"))
                repo.save(new AppUser("analyst",enc.encode("analyst"),"ROLE_ANALYST"));
            if(!repo.existsByUsername("guest"))
                repo.save(new AppUser("guest",enc.encode("guest"),"ROLE_GUEST"));
        };
    }
}
