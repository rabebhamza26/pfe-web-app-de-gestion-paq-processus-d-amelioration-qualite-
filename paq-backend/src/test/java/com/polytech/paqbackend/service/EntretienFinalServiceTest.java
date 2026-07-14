package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.EntretienFinalDTO;
import com.polytech.paqbackend.entity.*;
import com.polytech.paqbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntretienFinalServiceTest {

    @Mock private EntretienFinalRepository entretienFinalRepository;
    @Mock private PaqRepository paqRepository;
    @Mock private NotificationService notificationService;
    @Mock private CollaboratorRepository collaboratorRepository;

    @InjectMocks
    private EntretienFinalService service;

    private EntretienFinalDTO dto;
    private PaqDossier paq;

    @BeforeEach
    void setUp() {
        paq = new PaqDossier();
        paq.setId(1L);
        paq.setCollaboratorMatricule("M001");
        paq.setNiveau(4);
        paq.setActif(true);
        paq.setArchived(false);

        dto = new EntretienFinalDTO();
        dto.setDecision("Clôture");
        dto.setDateEntretien(LocalDate.now());
        dto.setTypeFaute("Faute D");
        dto.setCommentaireRH("RH comment");
    }

    @Test
    void shouldCreateEntretienFinal() {
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(entretienFinalRepository.save(any(EntretienFinal.class))).thenAnswer(inv -> inv.getArgument(0));

        EntretienFinal result = service.create("M001", dto, "exp@test.com");

        assertThat(result.getDecision()).isEqualTo("Clôture");
        assertThat(paq.getNiveau()).isEqualTo(5);
        assertThat(paq.getStatut()).isEqualTo("CLOTURE");
        verify(notificationService).envoyerNotification(anyString(), anyString(), anyString(), anyString());
    }
}