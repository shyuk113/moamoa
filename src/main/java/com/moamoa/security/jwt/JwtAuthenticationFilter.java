package com.moamoa.security.jwt;

import com.moamoa.repository.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider tokens;
  private final UserRepository users;

  public JwtAuthenticationFilter(JwtTokenProvider tokens, UserRepository users) {
    this.tokens = tokens;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getCookies() != null)
      for (var cookie : request.getCookies())
        if (cookie.getName().equals("ACCESS_TOKEN")) {
          try {
            var claims = tokens.verify(cookie.getValue());
            var user = users.findById(Long.parseLong(claims.getSubject()));
            if (user.isPresent()
                && user.get().getTokenVersion() == claims.get("version", Integer.class)) {
              var u = user.get();
              var principal =
                  new CustomUserDetails(
                      u.getId(), u.getEmail(), u.getNickname(), u.getTokenVersion());
              SecurityContextHolder.getContext()
                  .setAuthentication(
                      new UsernamePasswordAuthenticationToken(
                          principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            }
          } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
          }
          break;
        }
    chain.doFilter(request, response);
  }
}
