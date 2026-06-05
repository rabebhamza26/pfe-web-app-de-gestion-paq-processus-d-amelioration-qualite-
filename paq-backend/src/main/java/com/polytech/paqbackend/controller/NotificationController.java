package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private String resolveLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;

        String principal = authentication.getName();
        if (principal == null || principal.isEmpty() || "anonymousUser".equals(principal)) return null;

        User user = userRepository.findByEmailOrLogin(principal);
        if (user != null && user.getLogin() != null) {
            return user.getLogin();
        }

        return principal;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getNotifications(Authentication authentication) {
        String login = resolveLogin(authentication);
        if (login == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(notificationService.getNotificationsByLogin(login));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Map<String, Object>>> getUnreadNotifications(Authentication authentication) {
        String login = resolveLogin(authentication);
        if (login == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByLogin(login));
    }

    @GetMapping("/count/unread")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication authentication) {
        String login = resolveLogin(authentication);
        long count = login != null ? notificationService.countUnreadByLogin(login) : 0L;
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        String login = resolveLogin(authentication);
        if (login == null) return ResponseEntity.status(401).build();
        notificationService.markAsRead(id, login);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(Authentication authentication) {
        String login = resolveLogin(authentication);
        if (login == null) return ResponseEntity.ok(Map.of("marquees", 0));
        int count = notificationService.markAllAsRead(login);
        return ResponseEntity.ok(Map.of("marquees", count));
    }
}