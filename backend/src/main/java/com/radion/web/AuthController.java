package com.radion.web;

import com.radion.domain.models.User;
import com.radion.dto.UserSyncRequest;
import com.radion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncUser(@RequestBody UserSyncRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .avatarUrl(request.getAvatarUrl())
                            .build();
                    return userRepository.save(newUser);
                });

        return ResponseEntity.ok(Map.of("id", user.getId().toString()));
    }
}
