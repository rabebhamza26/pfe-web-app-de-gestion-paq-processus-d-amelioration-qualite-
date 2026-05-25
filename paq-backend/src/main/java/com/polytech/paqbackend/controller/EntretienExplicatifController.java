package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.EntretienExplicatifDTO;
import com.polytech.paqbackend.entity.EntretienExplicatif;
import com.polytech.paqbackend.service.EntretienExplicatifService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entretiens")
public class EntretienExplicatifController {

    private final EntretienExplicatifService service;

    public EntretienExplicatifController(EntretienExplicatifService service) {
        this.service = service;
    }

    @PostMapping("/{matricule}")
    @PreAuthorize("hasRole('SL')")
    public ResponseEntity<EntretienExplicatif> create(
            @PathVariable String matricule,
            @RequestBody EntretienExplicatifDTO dto,
            Authentication authentication) {
        // Correction: passer les 3 paramètres: matricule, dto, authentication.getName()
        return ResponseEntity.ok(service.create(matricule, dto, authentication.getName()));
    }

    @PutMapping("/{matricule}/{id}")
    @PreAuthorize("hasAnyRole('SL', 'SGL')")
    public ResponseEntity<EntretienExplicatif> update(
            @PathVariable String matricule,
            @PathVariable Long id,
            @RequestBody EntretienExplicatifDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(service.update(id, matricule, dto, authentication.getName()));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('SL', 'SGL')")
    public ResponseEntity<Void> validate(
            @PathVariable Long id,
            Authentication authentication) {
        service.validate(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/matricule/{matricule}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EntretienExplicatif>> getByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(service.findByMatricule(matricule));
    }
}