package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollaboratorServiceTest {

    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private PaqRepository paqRepository;

    @InjectMocks
    private CollaboratorService collaboratorService;

    @Test
    void shouldCreateCollaborator() {
        Collaborator collaborator = new Collaborator("M002", "Martin", "Paul", LocalDate.now(), "SEG2");
        when(collaboratorRepository.save(any(Collaborator.class))).thenReturn(collaborator);

        Collaborator saved = collaboratorService.create(collaborator);

        assertThat(saved.getMatricule()).isEqualTo("M002");
        assertThat(saved.isActif()).isTrue();
        assertThat(saved.isDepart()).isFalse();
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    void shouldUpdateCollaborator() {
        Collaborator existing = new Collaborator("M002", "Martin", "Paul", LocalDate.now(), "SEG2");
        when(collaboratorRepository.findById("M002")).thenReturn(Optional.of(existing));
        when(collaboratorRepository.save(any(Collaborator.class))).thenReturn(existing);

        Collaborator updateData = new Collaborator();
        updateData.setName("Martin Updated");
        updateData.setSegment("SEG3");

        Collaborator updated = collaboratorService.update("M002", updateData);

        assertThat(updated.getName()).isEqualTo("Martin Updated");
        assertThat(updated.getSegment()).isEqualTo("SEG3");
    }

    @Test
    void shouldDeleteCollaboratorAndArchivePaq() {
        PaqDossier paq = new PaqDossier("M002");
        paq.setId(1L);
        paq.setActif(true);
        when(collaboratorRepository.existsById("M002")).thenReturn(true);
        when(paqRepository.findFirstByCollaboratorMatriculeAndActifTrue("M002")).thenReturn(Optional.of(paq));

        collaboratorService.delete("M002");

        assertThat(paq.isArchived()).isTrue();
        assertThat(paq.isActif()).isFalse();
        verify(collaboratorRepository).deleteById("M002");
        verify(paqRepository).save(paq);
    }
}