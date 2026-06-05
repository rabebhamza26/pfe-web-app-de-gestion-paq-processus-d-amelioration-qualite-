package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.EntretienDecisionRequestDTO;
import com.polytech.paqbackend.entity.EntretienDecision;
import com.polytech.paqbackend.service.EntretienDecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entretiens-decision")
public class EntretienDecisionController {

    private static final Logger log = LoggerFactory.getLogger(EntretienDecisionController.class);
    private final EntretienDecisionService service;

    public EntretienDecisionController(EntretienDecisionService service) {
        this.service = service;
    }

    // SL peut créer
    @PostMapping("/{matricule}")
    @PreAuthorize("hasRole('SL')")
    public ResponseEntity<?> create(@PathVariable String matricule, @RequestBody EntretienDecisionRequestDTO dto, Authentication authentication) {
        log.info("CREATE - Matricule: {}, User: {}", matricule, authentication.getName());
        return ResponseEntity.ok(service.createAvecNotification(matricule, dto, authentication.getName()));
    }

    // SL peut modifier
    @PutMapping("/{matricule}/{id}")
    @PreAuthorize("hasRole('SL')")
    public ResponseEntity<?> update(@PathVariable String matricule, @PathVariable Long id, @RequestBody EntretienDecisionRequestDTO dto, Authentication authentication) {
        log.info("UPDATE - Matricule: {}, ID: {}, User: {}", matricule, id, authentication.getName());
        return ResponseEntity.ok(service.updateAvecNotification(id, matricule, dto, authentication.getName()));
    }

    // SL peut supprimer
    @DeleteMapping("/{matricule}/{id}")
    @PreAuthorize("hasRole('SL')")
    public ResponseEntity<Void> delete(@PathVariable String matricule, @PathVariable Long id, @RequestBody(required = false) Map<String, String> body, Authentication authentication) {
        log.info("DELETE - Matricule: {}, ID: {}, User: {}", matricule, id, authentication.getName());
        service.deleteAvecNotification(id, matricule, authentication.getName(), body != null ? body.get("destinataireEmail") : null, matricule);
        return ResponseEntity.noContent().build();
    }

    // ✅ SL valide (envoi email à 2 obligatoires + 1 optionnel)
    @PostMapping("/{matricule}/{id}/valider-sl")
    @PreAuthorize("hasRole('SL')")
    public ResponseEntity<?> validerParSL(@PathVariable String matricule, @PathVariable Long id, @RequestBody EntretienDecisionRequestDTO dto, Authentication authentication) {
        log.info("=== validerParSL ===");
        log.info("Matricule: {}", matricule);
        log.info("ID: {}", id);
        log.info("TypeFaute: {}", dto.getTypeFaute());
        log.info("Decision: {}", dto.getDecision());
        log.info("Justification: {}", dto.getJustification());
        log.info("DateEntretien: {}", dto.getDateEntretien());
        log.info("DestinatairesEmails: {}", dto.getDestinatairesEmails());
        log.info("User: {}", authentication.getName());

        try {
            EntretienDecision result = service.validerParSL(id, matricule, dto, authentication.getName());
            log.info("Validation SL réussie pour l'entretien ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors de la validation SL: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ✅ HP ou SGL valident (1ère validation) - PAS D'EMAIL
    @PostMapping("/{matricule}/{id}/valider1")
    @PreAuthorize("hasAnyRole('HP', 'SGL')")
    public ResponseEntity<?> validerParHPSGL(@PathVariable String matricule, @PathVariable Long id, @RequestBody EntretienDecisionRequestDTO dto, Authentication authentication) {
        log.info("=== validerParHPSGL ===");
        log.info("Matricule: {}, ID: {}, User: {}", matricule, id, authentication.getName());

        try {
            EntretienDecision result = service.validerParHPSGL(id, matricule, dto, authentication.getName());
            log.info("Validation HP/SGL réussie pour l'entretien ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors de la validation HP/SGL: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ✅ QM_PLANT valide (2ème validation) - PAS D'EMAIL
    @PostMapping("/{matricule}/{id}/valider2")
    @PreAuthorize("hasRole('QM_PLANT')")
    public ResponseEntity<?> validerParQMPlant(@PathVariable String matricule, @PathVariable Long id, @RequestBody EntretienDecisionRequestDTO dto, Authentication authentication) {
        log.info("=== validerParQMPlant ===");
        log.info("Matricule: {}, ID: {}, User: {}", matricule, id, authentication.getName());

        try {
            EntretienDecision result = service.validerParQMPlant(id, matricule, dto, authentication.getName());
            log.info("Validation QM_PLANT réussie pour l'entretien ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur lors de la validation QM_PLANT: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Lecture accessible à tous
    @GetMapping("/matricule/{matricule}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EntretienDecision>> getByMatricule(@PathVariable String matricule) {
        log.info("GET - Matricule: {}", matricule);
        return ResponseEntity.ok(service.findByMatricule(matricule));
    }
}