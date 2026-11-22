// AuthService.java
package com.medslot.platform.identity;

import com.medslot.platform.common.BusinessConflictException;
import com.medslot.platform.identity.security.AuthenticatedUser;
import com.medslot.platform.identity.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDto registerPatient(RegisterRequestDto request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessConflictException("Email is already registered: " + normalizedEmail);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.PATIENT);

        User saved = userRepository.save(user);
        AuthenticatedUser principal = AuthenticatedUser.from(saved);
        return AuthResponseDto.bearer(jwtTokenProvider.generateToken(principal), principal);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        return AuthResponseDto.bearer(jwtTokenProvider.generateToken(principal), principal);
    }
}