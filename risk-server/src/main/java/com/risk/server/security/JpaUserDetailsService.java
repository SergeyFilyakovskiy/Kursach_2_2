package com.risk.server.security;

import com.risk.server.repo.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;

public class JpaUserDetailsService implements UserDetailsService {

    private final AppUserRepository repo;

    public JpaUserDetailsService(AppUserRepository r) { this.repo = r; }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        return repo.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .authorities(new SimpleGrantedAuthority(u.getRole()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
