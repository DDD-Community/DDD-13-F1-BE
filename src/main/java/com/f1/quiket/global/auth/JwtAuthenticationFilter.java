package com.f1.quiket.global.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, e.getErrorCode());
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private void authenticate(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return;
        }

        String publicId = jwtTokenProvider.getSubject(token);
        User user = userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_TOKEN));
        if (user.isLocked()) {
            throw new CustomException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        UserPrincipal principal = UserPrincipal.from(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        List<String> publicPaths = List.of(
                "/api/v1/auth/signup",
                "/api/v1/auth/emails/availability",
                "/api/v1/auth/email-verifications",
                "/api/v1/auth/email-verifications/confirm",
                "/api/v1/auth/login",
                "/api/v1/auth/password-reset",
                "/api/v1/auth/token/refresh",
                "/api/v1/auth/oauth/kakao",
                "/api-docs",
                "/swagger-ui",
                "/v3/api-docs"
        );
        return publicPaths.stream().anyMatch(path::startsWith);
    }
}
