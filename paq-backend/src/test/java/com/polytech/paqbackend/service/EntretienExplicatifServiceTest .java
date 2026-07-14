package com.polytech.paqbackend.service;


import com.polytech.paqbackend.controller.PaqController;
import com.polytech.paqbackend.dto.EntretienExplicatifDTO;
import com.polytech.paqbackend.entity.*;
import com.polytech.paqbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntretienExplicatifServiceTest {

    @Mock private EntretienExplicatifRepository entretienRepo;
    @Mock private CollaboratorRepository collaborateurRepo;
    @Mock private PaqRepository paqRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private EntretienExplicatifService service;

    private EntretienExplicatifDTO dto;
    private Collaborator collaborator;
    private PaqDossier paqDossier;

    @BeforeEach
    void setUp() {
        collaborator = new Collaborator();
        collaborator.setMatricule("M001");
        collaborator.setName("Dupont");
        collaborator.setPrenom("Jean");
        collaborator.setSegment("SEG1");

        paqDossier = new PaqDossier();
        paqDossier.setId(1L);
        paqDossier.setCollaboratorMatricule("M001");
        paqDossier.setNiveau(0);
        paqDossier.setActif(true);
        paqDossier.setArchived(false);

        dto = new EntretienExplicatifDTO();
        dto.setTypeFaute("Non-respect");
        dto.setDate(LocalDate.now());
        dto.setDescription("Description");
        dto.setMesuresCorrectives("Mesure");
        dto.setDefautGrave(false);
    }

    @Test
    void shouldCreateEntretienExplicatif() {
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paqDossier));
        when(entretienRepo.save(any(EntretienExplicatif.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paqRepository.save(any(PaqDossier.class))).thenReturn(paqDossier);

        EntretienExplicatif result = service.create("M001", dto, "exp@test.com");

        assertThat(result.getMatricule()).isEqualTo("M001");
        assertThat(result.getTypeFaute()).isEqualTo("Non-respect");
        assertThat(paqDossier.getNiveau()).isEqualTo(1);
        verify(paqRepository).save(paqDossier);
    }

    @Test
    void shouldThrowWhenPaqNotFound() {
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("M001", dto, "exp@test.com"))
                .hasMessageContaining("Aucun dossier PAQ actif trouvé");
    }

    @Test
    void shouldNotifierSGLWhenDefautGrave() {
        dto.setDefautGrave(true);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paqDossier));
        when(entretienRepo.save(any(EntretienExplicatif.class))).thenAnswer(inv -> inv.getArgument(0));
        when(collaborateurRepo.findById("M001")).thenReturn(Optional.of(collaborator));

        User sgl = new User();
        sgl.setLogin("sgluser");
        sgl.setEmail("sgl@test.com");
        sgl.setRole(Role.SGL);
        sgl.setSegments(Set.of(Segment.builder().nomSegment("SEG1").build()));
        when(userRepository.findAll()).thenReturn(List.of(sgl));

        service.create("M001", dto, "exp@test.com");
        verify(emailService).sendEmail(anyString(), eq("sgl@test.com"), anyString(), anyString());
    }

    @Test
    void shouldUpdateEntretien() {
        EntretienExplicatif existing = new EntretienExplicatif();
        existing.setId(1L);
        existing.setDefautGrave(false);
        when(entretienRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(entretienRepo.save(any())).thenReturn(existing);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paqDossier));

        dto.setDefautGrave(true);
        service.update(1L, "M001", dto, "exp@test.com");

        assertThat(existing.isDefautGrave()).isTrue();
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }
}