package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.EntretienDecisionRequestDTO;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntretienDecisionServiceTest {

    @Mock private EntretienDecisionRepository repo;
    @Mock private PaqRepository paqRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private UserService userService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private EntretienDecisionService service;

    private EntretienDecisionRequestDTO dto;
    private PaqDossier paq;

    @BeforeEach
    void setUp() {
        paq = new PaqDossier();
        paq.setId(1L);
        paq.setCollaboratorMatricule("M001");
        paq.setNiveau(3);
        paq.setActif(true);
        paq.setArchived(false);

        dto = new EntretienDecisionRequestDTO();
        dto.setTypeFaute("Faute B");
        dto.setDateEntretien(LocalDate.now());
        dto.setDecision("Licenciement");
        dto.setJustification("Justif");
        dto.setDestinatairesEmails(List.of("hp@test.com", "qmplant@test.com"));
    }

    @Test
    void shouldCreateEntretienDecision() {
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(repo.save(any(EntretienDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        EntretienDecision result = service.create("M001", dto, "exp@test.com");

        assertThat(result.getDecision()).isEqualTo("Licenciement");
        assertThat(result.isValideSL()).isFalse();
        verify(paqRepository).save(paq);
    }

    @Test
    void shouldThrowWhenWrongNiveau() {
        paq.setNiveau(2);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));

        assertThatThrownBy(() -> service.create("M001", dto, "exp@test.com"))
                .hasMessageContaining("niveau 3 requis");
    }

    @Test
    void shouldValiderParSL() {
        EntretienDecision entretien = new EntretienDecision();
        entretien.setId(1L);
        entretien.setValideSL(false);
        when(repo.findById(1L)).thenReturn(Optional.of(entretien));
        when(repo.save(any())).thenReturn(entretien);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(collaboratorRepository.findByMatricule("M001"))
                .thenReturn(Optional.of(new Collaborator("M001", "Dupont", "Jean", LocalDate.now(), "SEG1")));

        service.validerParSL(1L, "M001", dto, "exp@test.com");

        assertThat(entretien.isValideSL()).isTrue();
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }
}