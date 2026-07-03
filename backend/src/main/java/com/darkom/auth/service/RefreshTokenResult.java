package com.darkom.auth.service;

import com.darkom.auth.dto.AuthResponse;

public record RefreshTokenResult(AuthResponse authResponse, String rawRefreshToken) {}
