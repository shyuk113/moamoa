package com.moamoa.config;

import com.moamoa.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
  @Bean
  org.springframework.security.core.userdetails.UserDetailsService userDetailsService(
      com.moamoa.repository.user.UserRepository users) {
    return email -> {
      var user =
          users
              .findByEmail(email)
              .orElseThrow(
                  () ->
                      new org.springframework.security.core.userdetails.UsernameNotFoundException(
                          "Unknown account"));
      return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
          .password(user.getPassword())
          .roles("USER")
          .build();
    };
  }

  @Bean
  org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter>
      jwtRegistration(JwtAuthenticationFilter jwt) {
    var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(jwt);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
    return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/contests",
                        "/contests/**",
                        "/auth/**",
                        "/api/auth/**",
                        "/api/contests/**",
                        "/css/**",
                        "/js/**",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    (req, res, ex) -> {
                      if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(401);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                      } else res.sendRedirect("/auth/login");
                    }))
        // JWT is restored per request, after session management, so asset requests do not
        // look like fresh logins and clear the page's CSRF cookie.
        .addFilterBefore(jwt, ExceptionTranslationFilter.class)
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())
        .logout(l -> l.disable())
        .build();
  }
}
