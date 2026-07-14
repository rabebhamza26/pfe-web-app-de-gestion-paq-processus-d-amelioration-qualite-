package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.EntretienDaccordRequestDTO;
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
class EntretienDaccordServiceTest {

    @Mock private EntretienDaccordRepository repo;
    @Mock private PaqRepository paqRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private EntretienDaccordService service;

    private EntretienDaccordRequestDTO dto;
    private PaqDossier paq;

    @BeforeEach
    void setUp() {
        paq = new PaqDossier();
        paq.setId(1L);
        paq.setCollaboratorMatricule("M001");
        paq.setNiveau(1);
        paq.setActif(true);
        paq.setArchived(false);

        dto = new EntretienDaccordRequestDTO();
        dto.setDate(LocalDate.now());
        dto.setTypeFaute("Faute C");
        dto.setCauseFaute("Cause");
        dto.setMesuresProposees("Mesure");
        dto.setDestinataireEmail("qm@test.com");
    }

    @Test
    void shouldCreateEntretienDaccord() {
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(repo.save(any(EntretienDaccord.class))).thenAnswer(inv -> inv.getArgument(0));

        EntretienDaccord result = service.create("M001", dto, "exp@test.com");

        assertThat(result.getTypeFaute()).isEqualTo("Faute C");
        assertThat(paq.getNiveau()).isEqualTo(2);
        verify(paqRepository).save(paq);
    }

    @Test
    void shouldValiderPremiere() {
        EntretienDaccord entretien = new EntretienDaccord();
        entretien.setId(1L);
        entretien.setValide(false);
        when(repo.findById(1L)).thenReturn(Optional.of(entretien));
        when(repo.save(any())).thenReturn(entretien);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse("M001"))
                .thenReturn(Optional.of(paq));
        when(collaboratorRepository.findByMatricule("M001"))
                .thenReturn(Optional.of(new Collaborator("M001", "Dupont", "Jean", LocalDate.now(), "SEG1")));

        service.validerPremiere(1L, "M001", dto, "exp@test.com");

        assertThat(entretien.getValide()).isTrue();
        verify(emailService).sendEmail(anyString(), eq("qm@test.com"), anyString(), anyString());
    }
}