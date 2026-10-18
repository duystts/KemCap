package com.englishplatform.identity.internal.web;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAdminService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/users} — giáo vụ quản lý người dùng (contracts/rest-api.md nhóm identity). */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
class UserAdminController {

  private final UserAdminService service;

  UserAdminController(UserAdminService service) {
    this.service = service;
  }

  record UserDto(
      UUID id,
      String email,
      String fullName,
      String phone,
      Role role,
      UserStatus status,
      LocalDate dateOfBirth,
      Instant lastLoginAt,
      boolean anonymized) {

    static UserDto of(UserAccount u) {
      return new UserDto(
          u.getId(),
          u.getEmail(),
          u.getFullName(),
          u.getPhone(),
          u.getRole(),
          u.getStatus(),
          u.getDateOfBirth(),
          u.getLastLoginAt(),
          u.isAnonymized());
    }
  }

  record CreateUserRequest(
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(max = 150) String fullName,
      @NotNull Role role,
      @Size(max = 20) String phone,
      LocalDate dateOfBirth) {}

  record StatusRequest(@NotNull UserStatus status) {}

  @GetMapping
  List<UserDto> list(
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(required = false) String q) {
    return service.list(role, status, q).stream().map(UserDto::of).toList();
  }

  @GetMapping("/{id}")
  UserDto get(@PathVariable UUID id) {
    return UserDto.of(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  UserDto create(@Valid @RequestBody CreateUserRequest request) {
    return UserDto.of(
        service.create(
            new UserAdminService.NewUser(
                request.email(),
                request.fullName(),
                request.role(),
                request.phone(),
                request.dateOfBirth())));
  }

  @PatchMapping("/{id}/status")
  UserDto changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
    return UserDto.of(service.changeStatus(id, CurrentUser.require().id(), request.status()));
  }

  @PostMapping("/{id}/resend-invite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void resendInvite(@PathVariable UUID id) {
    service.resendInvite(id);
  }
}
