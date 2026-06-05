// EntretienFinalController.java (corrigé)
package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.EntretienFinalDTO;
import com.polytech.paqbackend.service.EntretienFinalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/entretien-final")
public class EntretienFinalController {

    private final EntretienFinalService service;

    public EntretienFinalController(EntretienFinalService service) {
        this.service = service;
    }

    // RH peut créer
    @PostMapping("/{matricule}")
    @PreAuthorize("hasAuthority('final:create') and hasRole('RH')")
    public ResponseEntity<?> create(@PathVariable String matricule, @RequestBody EntretienFinalDTO dto, Authentication authentication) {
        return ResponseEntity.ok(service.create(matricule, dto, authentication.getName()));
    }

    // RH peut modifier
    @PutMapping("/{matricule}/{id}")
    @PreAuthorize("hasAuthority('final:update') and hasRole('RH')")
    public ResponseEntity<?> update(@PathVariable String matricule, @PathVariable Long id, @RequestBody EntretienFinalDTO dto, Authentication authentication) {
        return ResponseEntity.ok(service.updateWithPaqUpdate(id, matricule, dto, authentication.getName()));
    }

    // RH peut supprimer
    @DeleteMapping("/{matricule}/{id}")
    @PreAuthorize("hasAuthority('final:delete') and hasRole('RH')")
    public ResponseEntity<?> delete(@PathVariable String matricule, @PathVariable Long id, @RequestBody(required = false) Map<String, String> body, Authentication authentication) {
        String nomCollaborateur = body != null ? body.get("nomCollaborateur") : null;
        service.deleteAvecNotification(id, matricule, authentication.getName(), null, nomCollaborateur);
        return ResponseEntity.noContent().build();
    }

    // RH peut valider
    @PostMapping("/{matricule}/{id}/valider")
    @PreAuthorize("hasAuthority('final:validate') and hasRole('RH')")
    public ResponseEntity<?> valider(@PathVariable String matricule, @PathVariable Long id, @RequestBody EntretienFinalDTO dto, Authentication authentication) {
        return ResponseEntity.ok(service.updateWithPaqUpdate(id, matricule, dto, authentication.getName()));
    }

    // Lecture accessible à tous
    @GetMapping("/{matricule}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(service.getByMatricule(matricule));
    }
}