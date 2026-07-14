package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.CreateUserRequest;
import com.polytech.paqbackend.dto.UpdateUserRequest;
import com.polytech.paqbackend.dto.UserResponseDto;
import com.polytech.paqbackend.entity.*;
import com.polytech.paqbackend.repository.PlantRepository;
import com.polytech.paqbackend.repository.SegmentRepository;
import com.polytech.paqbackend.repository.SiteRepository;
import com.polytech.paqbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private PlantRepository plantRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private Site site;
    private Plant plant;
    private Segment segment;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Création des entités sans builder (constructeur par défaut + setters)
        site = new Site();
        site.setId(1L);
        site.setName("Site A");

        plant = new Plant();
        plant.setId(2L);
        plant.setName("Plant A");

        segment = new Segment();
        segment.setId(3L);
        segment.setNomSegment("Segment A");

        // Utilisation du builder de User (si disponible)
        user = User.builder()
                .id(100L)
                .email("test@example.com")
                .login("testLogin")
                .nomUtilisateur("Test User")
                .role(Role.SL)
                .active(true)
                .sites(Collections.singleton(site))
                .plants(Collections.singleton(plant))
                .segments(Collections.singleton(segment))
                .build();

        createRequest = new CreateUserRequest();
        createRequest.setEmail("new@example.com");
        createRequest.setLogin("newLogin");
        createRequest.setNomUtilisateur("New User");
        createRequest.setRole(Role.ADMIN);
        createRequest.setPassword("password123");
        createRequest.setSiteIds(List.of(1L));
        createRequest.setPlantIds(List.of(2L));
        createRequest.setSegmentIds(List.of(3L));

        updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@example.com");
        updateRequest.setNomUtilisateur("Updated User");
        updateRequest.setLogin("updatedLogin");
        updateRequest.setRole(Role.SL);
        updateRequest.setActive(true);
    }

    // ─── TESTS CRUD ─────────────────────────────────────────────────────────────

    @Test
    void shouldGetAllUsers() {
        when(userRepository.findAllWithAllRelations()).thenReturn(List.of(user));

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findAllWithAllRelations();
    }

    @Test
    void shouldGetUserById() {
        when(userRepository.findByIdWithAllRelations(100L)).thenReturn(Optional.of(user));

        UserResponseDto result = userService.getUserById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getSiteIds()).contains(1L);
        verify(userRepository).findByIdWithAllRelations(100L);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByIdWithAllRelations(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldCreateUser() {
        // ✅ Utiliser List.of() pour retourner un Iterable<Site> (List<Site>) qui convient à findAllById
        when(siteRepository.findAllById(any())).thenReturn(List.of(site));
        when(plantRepository.findAllById(any())).thenReturn(List.of(plant));
        when(segmentRepository.findAllById(any())).thenReturn(List.of(segment));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(200L);
            return u;
        });

        UserResponseDto result = userService.createUser(createRequest);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenPasswordMissing() {
        createRequest.setPassword(null);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .hasMessageContaining("Password is required");
    }

    @Test
    void shouldUpdateUser() {
        when(userRepository.findByIdWithAllRelations(100L)).thenReturn(Optional.of(user));
        // ✅ Utiliser List.of() pour les mocks findAllById
        when(siteRepository.findAllById(any())).thenReturn(List.of(site));
        when(plantRepository.findAllById(any())).thenReturn(List.of(plant));
        when(segmentRepository.findAllById(any())).thenReturn(List.of(segment));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDto result = userService.updateUser(100L, updateRequest);

        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenEmailAlreadyUsed() {
        User existing = User.builder()
                .id(200L)
                .email("updated@example.com")
                .build();
        when(userRepository.findByIdWithAllRelations(100L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(existing);

        assertThatThrownBy(() -> userService.updateUser(100L, updateRequest))
                .hasMessageContaining("Email déjà utilisé");
    }

    @Test
    void shouldToggleActive() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDto result = userService.toggleActive(100L);

        assertThat(result.isActive()).isFalse(); // initialement true
        verify(userRepository).save(user);
    }

    @Test
    void shouldDeleteUser() {
        when(userRepository.existsById(100L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(100L);

        userService.deleteUser(100L);

        verify(userRepository).deleteById(100L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentUser() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .hasMessageContaining("User not found");
    }

    // ─── TESTS PERIMETER METHODS ──────────────────────────────────────────────

    @Test
    void shouldGetQMEmails() {
        List<String> expected = List.of("qm1@test.com", "qm2@test.com");
        when(userRepository.findQMEmails()).thenReturn(expected);

        List<String> result = userService.getQMEmails();

        assertThat(result).isEqualTo(expected);
        verify(userRepository).findQMEmails();
    }

    @Test
    void shouldGetQMEmailsByPerimeterForAdmin() {
        User admin = User.builder().role(Role.ADMIN).build();
        List<String> expected = List.of("admin@test.com");
        when(userRepository.findQMEmails()).thenReturn(expected);

        List<String> result = userService.getQMEmailsByPerimeter(admin);

        assertThat(result).isEqualTo(expected);
        verify(userRepository).findQMEmails();
    }

    @Test
    void shouldGetQMEmailsByPerimeterForNonAdmin() {
        User sl = User.builder()
                .role(Role.SL)
                .email("sl@test.com")
                .sites(Collections.singleton(site))
                .plants(Collections.singleton(plant))
                .build();

        List<String> siteEmails = List.of("site@test.com");
        List<String> plantEmails = List.of("plant@test.com");
        when(userRepository.findQMEmailsBySite(1L)).thenReturn(siteEmails);
        when(userRepository.findQMEmailsByPlant(2L)).thenReturn(plantEmails);

        List<String> result = userService.getQMEmailsByPerimeter(sl);

        assertThat(result).containsExactlyInAnyOrder("site@test.com", "plant@test.com");
        verify(userRepository).findQMEmailsBySite(1L);
        verify(userRepository).findQMEmailsByPlant(2L);
    }
}