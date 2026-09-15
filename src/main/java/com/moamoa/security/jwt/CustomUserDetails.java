package com.moamoa.security.jwt;

public record CustomUserDetails(Long id, String email, String nickname, int tokenVersion) {}
