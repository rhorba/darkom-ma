package com.darkom.property.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GrantManagerRequest(@NotBlank @Email String managerEmail) {}
