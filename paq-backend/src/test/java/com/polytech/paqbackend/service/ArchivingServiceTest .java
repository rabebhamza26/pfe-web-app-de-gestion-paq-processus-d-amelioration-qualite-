package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.Archive;
import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.ArchiveRepository;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivingServiceTest {

    @Mock private PaqRepository paqRepository;
    @Mock private ArchiveRepository archiveRepository;
    @Mock private CollaboratorRepository collaboratorRepository;

    @InjectMocks
    private ArchivingService archivingService;

    @BeforeEach
    void setUp() {
        // Simule aujourd'hui pour contrôler les dates
        archivingService.setSimulatedToday(LocalDate.of(2025, 6, 15));
    }

    @Test
    void shouldArchiveExpiredPaqs() {
        PaqDossier paq = new PaqDossier("M003");
        paq.setId(1L);
        paq.setDateCreation(LocalDate.of(2024, 12, 15));
        paq.setDateFin(LocalDate.of(2025, 6, 15));
        paq.setActif(true);
        paq.setArchived(false);

        when(paqRepository.findByActifTrueAndArchivedFalse()).thenReturn(List.of(paq));
        when(collaboratorRepository.findByMatricule("M003")).thenReturn(Optional.of(new Collaborator("M003", "Durand", "Marie", LocalDate.now(), "SEG4")));
        when(archiveRepository.existsByPaqDossierId(1L)).thenReturn(false);

        archivingService.archiveExpiredPaqs();

        verify(paqRepository).save(paq);
        verify(archiveRepository).save(any(Archive.class));
        assertThat(paq.isArchived()).isTrue();
        assertThat(paq.isActif()).isFalse();
    }

    @Test
    void shouldNotArchivePaqWithFutureDateFin() {
        PaqDossier paq = new PaqDossier("M004");
        paq.setId(2L);
        paq.setDateCreation(LocalDate.of(2025, 1, 1));
        paq.setDateFin(LocalDate.of(2025, 12, 31)); // future
        paq.setActif(true);
        paq.setArchived(false);

        when(paqRepository.findByActifTrueAndArchivedFalse()).thenReturn(List.of(paq));

        archivingService.archiveExpiredPaqs();

        verify(paqRepository, never()).save(paq);
        verify(archiveRepository, never()).save(any(Archive.class));
    }

    @Test
    void shouldHandleAlreadyArchivedPaq() {
        PaqDossier paq = new PaqDossier("M005");
        paq.setId(3L);
        paq.setDateCreation(LocalDate.of(2024, 12, 15));
        paq.setDateFin(LocalDate.of(2025, 6, 15));
        paq.setActif(false); // déjà archivé
        paq.setArchived(true);

        when(paqRepository.findByActifTrueAndArchivedFalse()).thenReturn(List.of()); // aucun

        archivingService.archiveExpiredPaqs();

        verify(paqRepository, never()).save(any());
        verify(archiveRepository, never()).save(any());
    }

    @Test
    void shouldSkipIfArchiveAlreadyExists() {
        PaqDossier paq = new PaqDossier("M006");
        paq.setId(4L);
        paq.setDateCreation(LocalDate.of(2024, 12, 15));
        paq.setDateFin(LocalDate.of(2025, 6, 15));
        paq.setActif(true);
        paq.setArchived(false);

        when(paqRepository.findByActifTrueAndArchivedFalse()).thenReturn(List.of(paq));
        when(archiveRepository.existsByPaqDossierId(4L)).thenReturn(true);

        archivingService.archiveExpiredPaqs();

        verify(paqRepository, never()).save(paq);
        verify(archiveRepository, never()).save(any(Archive.class));
    }
}