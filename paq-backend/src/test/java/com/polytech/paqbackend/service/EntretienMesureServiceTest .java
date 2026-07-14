package com.polytech.paqbackend.service;



import com.polytech.paqbackend.dto.EntretienMesureRequestDTO;
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
class EntretienMesureServiceTest {

    @Mock private EntretienMesureRepository repo;
    @Mock private PaqRepository paqRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private UserService userService;

    @InjectMocks
    private EntretienMesureService service;

    private EntretienMesureRequestDTO dto;
    private PaqDossier paq;

    @BeforeEach
    void setUp() {
        paq = new PaqDossier();
        paq.setId(1L);
        paq.setCollaboratorMatricule("M001");
        paq.setNiveau(2);
        paq.setActif(true);
        paq.setArchived(false);

        dto = new EntretienMesureRequestDTO();
        dto.setTypeFaute("Faute A");
        dto.setCausesPrincipales("Cause");
        dto.setConvention("Conv");
        dto.setPlanAction("Plan");
        dto.setDateEntretien(LocalDate.now());
        dto.setDestinatairesEmails(List.of("qm@test.com"));
    }

    @Test
    void shouldCreateEntretienMesure() {
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(repo.save(any(EntretienMesure.class))).thenAnswer(inv -> inv.getArgument(0));

        EntretienMesure result = service.create("M001", dto, "exp@test.com");

        assertThat(result.getTypeFaute()).isEqualTo("Faute A");
        assertThat(result.isValideSL()).isFalse();
        verify(paqRepository).save(paq);
    }

    @Test
    void shouldThrowWhenWrongNiveau() {
        paq.setNiveau(1);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));

        assertThatThrownBy(() -> service.create("M001", dto, "exp@test.com"))
                .hasMessageContaining("niveau 2 requis");
    }

    @Test
    void shouldValiderPremiere() {
        EntretienMesure entretien = new EntretienMesure();
        entretien.setId(1L);
        entretien.setValideSL(false);
        when(repo.findById(1L)).thenReturn(Optional.of(entretien));
        when(repo.save(any())).thenReturn(entretien);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(collaboratorRepository.findByMatricule("M001"))
                .thenReturn(Optional.of(new Collaborator("M001", "Dupont", "Jean", LocalDate.now(), "SEG1")));

        service.validerPremiere(1L, "M001", dto, "exp@test.com");

        assertThat(entretien.isValideSL()).isTrue();
        verify(emailService).sendEmail(anyString(), eq("qm@test.com"), anyString(), anyString());
    }
}
