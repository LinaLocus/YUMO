package com.voicenotes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AppUserDetailsService uds;

    public JwtAuthFilter(JwtService jwt, AppUserDetailsService uds) {
        this.jwt = jwt;
        this.uds = uds;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE 异步重派发时仍需重新认证（从 ?token= 读取），否则授权链视为匿名而拒绝
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // SSE/EventSource 无法设 header，允许 ?token= 传递
            String q = req.getParameter("token");
            if (q != null && !q.isBlank()) token = q;
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwt.extractUsername(token);
                if (jwt.isValid(token, username)) {
                    UserDetails ud = uds.loadUserByUsername(username);
                    var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // 无效 token：保持未认证，后续由 security 返回 401
            }
        }
        chain.doFilter(req, res);
    }
}
