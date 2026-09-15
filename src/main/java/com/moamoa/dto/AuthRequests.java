package com.moamoa.dto;

import jakarta.validation.constraints.*;

public final class AuthRequests {
  private AuthRequests() {}

  public record Signup(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 10, max = 64) String password,
      @NotBlank @Size(max = 40) String nickname) {}

  public record Login(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 64) String password) {}

  public record Forgot(@NotBlank @Email @Size(max = 254) String email) {}

  public record Reset(
      @NotBlank @Size(max = 100) String token,
      @NotBlank @Size(min = 10, max = 64) String password) {}
}
