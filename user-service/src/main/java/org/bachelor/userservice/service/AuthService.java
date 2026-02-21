package org.bachelor.userservice.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.exception.NotFoundException;
import org.bachelor.userservice.mapper.UserMapper;
import org.bachelor.userservice.model.dto.AuthResponseDTO;
import org.bachelor.userservice.model.dto.LoginRequestDTO;
import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.repository.UserRepository;
import org.bachelor.userservice.security.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    public AuthResponseDTO login(LoginRequestDTO request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        addRefreshCookie(response, refreshToken);

        return new AuthResponseDTO(accessToken, userMapper.toDto(user));
    }

    public AuthResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = jwtService.extractClaims(refreshToken).getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        addRefreshCookie(response, newRefreshToken);

        return new AuthResponseDTO(newAccessToken, userMapper.toDto(user));
    }

//    public void logout(HttpServletResponse response) {
//        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
//        cookie.setHttpOnly(true);
//        cookie.setSecure(jwtProperties.isCookieSecure());
//        cookie.setPath("/");
//        cookie.setMaxAge(0);
//        response.addCookie(cookie);
//    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("No refresh token");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new IllegalArgumentException("No refresh token"));
    }

//    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
//        Cookie cookie = new Cookie(REFRESH_COOKIE, refreshToken);
//        cookie.setHttpOnly(true);
//        cookie.setSecure(jwtProperties.isCookieSecure());
//        cookie.setPath("/");
//        cookie.setMaxAge((int) (jwtProperties.getRefreshExpiration() / 1000));
//        response.addCookie(cookie);
//    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(jwtProperties.getRefreshExpiration() / 1000)
                .sameSite("Lax") // важливо для localhost
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}