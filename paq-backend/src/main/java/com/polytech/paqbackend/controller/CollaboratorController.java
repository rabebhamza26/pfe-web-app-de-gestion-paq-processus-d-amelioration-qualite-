package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.CollaborateurDTO;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.service.CollaboratorService;
import com.polytech.paqbackend.entity.Collaborator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collaborators")
public class CollaboratorController {

    @Autowired private CollaboratorRepository repo;
    @Autowired private CollaboratorService collaboratorService;
    @Autowired private UserRepository userRepository;

    // =========================================================
    // Récupère l'utilisateur connecté avec TOUTES ses relations
    // (segments, plants, sites) via une jointure fetch dédiée.
    // =========================================================
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            String username = auth.getName();
            if (username == null || username.isEmpty()) return null;

            // Utilise la requête avec fetch join pour éviter le lazy-loading
            User user = userRepository.findByEmailOrLoginWithPerimeter(username);
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // Retourne les IDs de sites accessibles pour le user.
    // =========================================================
    private Set<Long> getAccessibleSiteIds(User user) {
        Set<Long> siteIds = new HashSet<>();
        if (user == null) return siteIds;
        if (user.getSites() != null) {
            user.getSites().forEach(site -> {
                if (site != null && site.getId() != null) siteIds.add(site.getId());
            });
        }
        return siteIds;
    }

    // =========================================================
    // Retourne les IDs de plants accessibles pour le user.
    // Inclut les plants directs + plants via les sites du user.
    // =========================================================
    private Set<Long> getAccessiblePlantIds(User user) {
        Set<Long> plantIds = new HashSet<>();
        if (user == null) return plantIds;

        // Plants directement assignés
        if (user.getPlants() != null) {
            user.getPlants().forEach(plant -> {
                if (plant != null && plant.getId() != null) plantIds.add(plant.getId());
            });
        }

        // Plants via sites assignés
        if (user.getSites() != null) {
            user.getSites().forEach(site -> {
                if (site != null && site.getPlants() != null) {
                    site.getPlants().forEach(plant -> {
                        if (plant != null && plant.getId() != null) plantIds.add(plant.getId());
                    });
                }
            });
        }
        return plantIds;
    }

    // =========================================================
    // Retourne les noms de segments accessibles pour le user.
    // segments directs → plants directs → plants via sites
    // =========================================================
    private Set<String> getAccessibleSegments(User user) {
        Set<String> segments = new HashSet<>();
        if (user == null) return segments;

        // 1. Segments directement assignés
        if (user.getSegments() != null) {
            user.getSegments().forEach(seg -> {
                if (seg != null && seg.getNomSegment() != null)
                    segments.add(seg.getNomSegment());
            });
        }

        // 2. Segments via plants directs
        if (user.getPlants() != null) {
            user.getPlants().forEach(plant -> {
                if (plant != null && plant.getSegments() != null) {
                    plant.getSegments().forEach(seg -> {
                        if (seg != null && seg.getNomSegment() != null)
                            segments.add(seg.getNomSegment());
                    });
                }
            });
        }

        // 3. Segments via sites → plants → segments
        if (user.getSites() != null) {
            user.getSites().forEach(site -> {
                if (site != null && site.getPlants() != null) {
                    site.getPlants().forEach(plant -> {
                        if (plant != null && plant.getSegments() != null) {
                            plant.getSegments().forEach(seg -> {
                                if (seg != null && seg.getNomSegment() != null)
                                    segments.add(seg.getNomSegment());
                            });
                        }
                    });
                }
            });
        }
        return segments;
    }


    private List<String> resolveSegmentFilter(User user, Long siteId, Long plantId) {
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole() != null ? user.getRole().name() : "");

        // Segments demandés par le filtre frontend (site ou plant)
        Set<String> filterSegments = null;

        if (plantId != null) {
            // Récupère les segments du plant demandé
            List<String> segs = repo.findSegmentNamesByPlantId(plantId);
            filterSegments = new HashSet<>(segs);
        } else if (siteId != null) {
            // Récupère les segments du site demandé
            List<String> segs = repo.findSegmentNamesBySiteId(siteId);
            filterSegments = new HashSet<>(segs);
        }

        if (isAdmin) {
            // ADMIN : uniquement le filtre frontend (pas de restriction périmètre)
            if (filterSegments != null) return new ArrayList<>(filterSegments);
            return null; // null = tout afficher
        }

        // Non-ADMIN : périmètre du user
        Set<String> userSegments = getAccessibleSegments(user);

        if (userSegments.isEmpty()) {
            // User sans périmètre → rien
            return Collections.emptyList();
        }

        if (filterSegments != null) {
            // Intersection périmètre user ∩ filtre
            filterSegments.retainAll(userSegments);
            return new ArrayList<>(filterSegments);
        }

        // Pas de filtre frontend → périmètre complet du user
        return new ArrayList<>(userSegments);
    }

    // =========================================================
    // GET /api/collaborators/view?siteId=X&plantId=Y
    // Retourne les collaborateurs selon le périmètre du user
    // ET le site/plant sélectionné dans l'interface.
    // =========================================================
    @GetMapping("/view")
    @PreAuthorize("hasAuthority('collaborateur:read')")
    public ResponseEntity<List<CollaborateurDTO>> getAll(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long plantId) {
        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                System.err.println("[CollaboratorController] Utilisateur non trouvé dans le SecurityContext");
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<String> segmentFilter = resolveSegmentFilter(currentUser, siteId, plantId);

            List<CollaborateurDTO> collaborators;

            if (segmentFilter == null) {
                // ADMIN sans filtre : tout afficher
                collaborators = repo.getAllWithPaq();
                System.out.println("[CollaboratorController] ADMIN (sans filtre) : " + collaborators.size() + " collaborateurs");
            } else if (segmentFilter.isEmpty()) {
                System.out.println("[CollaboratorController] Aucun segment accessible → liste vide");
                collaborators = new ArrayList<>();
            } else {
                collaborators = repo.getCollaboratorsBySegments(segmentFilter);
                System.out.println("[CollaboratorController] " + collaborators.size()
                        + " collaborateurs pour segments=" + segmentFilter);
            }

            return ResponseEntity.ok(collaborators);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================
    // GET /api/collaborators/{matricule}
    // =========================================================
    @GetMapping("/{matricule}")
    @PreAuthorize("hasAuthority('collaborateur:read')")
    public ResponseEntity<?> getCollaborator(@PathVariable String matricule) {
        try {
            User currentUser = getCurrentUser();

            Optional<Collaborator> col = repo.findById(matricule);
            if (col.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            if (currentUser != null && "ADMIN".equals(currentUser.getRole().name())) {
                return ResponseEntity.ok(col.get());
            }

            if (currentUser != null) {
                Set<String> accessibleSegments = getAccessibleSegments(currentUser);
                String collabSegment = col.get().getSegment();
                if (collabSegment != null && accessibleSegments.contains(collabSegment)) {
                    return ResponseEntity.ok(col.get());
                } else {
                    System.out.println("[CollaboratorController] Accès refusé : segment '"
                            + collabSegment + "' non dans " + accessibleSegments);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Accès non autorisé à ce collaborateur"));
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur : " + e.getMessage()));
        }
    }

    // =========================================================
    // GET /api/collaborators/by-segment/{segment}
    // =========================================================
    @GetMapping("/by-segment/{segment}")
    @PreAuthorize("hasAuthority('collaborateur:read')")
    public ResponseEntity<List<CollaborateurDTO>> getBySegment(@PathVariable String segment) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return ResponseEntity.ok(new ArrayList<>());

            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";

            if (!"ADMIN".equals(role)) {
                Set<String> accessibleSegments = getAccessibleSegments(currentUser);
                if (!accessibleSegments.contains(segment)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            return ResponseEntity.ok(repo.getCollaboratorsBySegment(segment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================
    // GET /api/collaborators/by-sites
    // =========================================================
    @GetMapping("/by-sites")
    @PreAuthorize("hasAuthority('collaborateur:read')")
    public ResponseEntity<List<CollaborateurDTO>> getBySites() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return ResponseEntity.ok(new ArrayList<>());

            if ("ADMIN".equals(currentUser.getRole().name())) {
                return ResponseEntity.ok(repo.getAllWithPaq());
            }

            Set<Long> siteIds = getAccessibleSiteIds(currentUser);
            if (siteIds.isEmpty()) return ResponseEntity.ok(new ArrayList<>());
            return ResponseEntity.ok(repo.getCollaboratorsBySites(new ArrayList<>(siteIds)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================
    // GET /api/collaborators/by-plants
    // =========================================================
    @GetMapping("/by-plants")
    @PreAuthorize("hasAuthority('collaborateur:read')")
    public ResponseEntity<List<CollaborateurDTO>> getByPlants() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return ResponseEntity.ok(new ArrayList<>());

            if ("ADMIN".equals(currentUser.getRole().name())) {
                return ResponseEntity.ok(repo.getAllWithPaq());
            }

            Set<Long> plantIds = getAccessiblePlantIds(currentUser);
            if (plantIds.isEmpty()) return ResponseEntity.ok(new ArrayList<>());
            return ResponseEntity.ok(repo.getCollaboratorsByPlants(new ArrayList<>(plantIds)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================
    // POST /api/collaborators — Création
    // =========================================================
    @PostMapping
    @PreAuthorize("hasAuthority('collaborateur:create')")
    public ResponseEntity<?> create(@RequestBody Collaborator c) {
        try {
            if (c.getMatricule() == null || c.getMatricule().isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Le matricule est obligatoire"));
            if (repo.existsByMatricule(c.getMatricule()))
                return ResponseEntity.badRequest().body(Map.of("message", "Matricule déjà existant"));
            if (c.getName() == null || c.getName().isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Le nom est obligatoire"));
            if (c.getHireDate() == null)
                return ResponseEntity.badRequest().body(Map.of("message", "La date d'embauche est obligatoire"));

            User currentUser = getCurrentUser();
            if (currentUser != null && !"ADMIN".equals(currentUser.getRole().name())) {
                Set<String> accessibleSegments = getAccessibleSegments(currentUser);
                if (c.getSegment() != null && !accessibleSegments.contains(c.getSegment())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Vous ne pouvez pas créer un collaborateur dans ce segment"));
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(collaboratorService.create(c));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur : " + e.getMessage()));
        }
    }

    // =========================================================
    // PUT /api/collaborators/{matricule} — Modification
    // =========================================================
    @PutMapping("/{matricule}")
    @PreAuthorize("hasAuthority('collaborateur:update')")
    public ResponseEntity<?> update(@PathVariable String matricule, @RequestBody Collaborator c) {
        try {
            if (!repo.existsById(matricule))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Collaborateur non trouvé"));

            User currentUser = getCurrentUser();
            if (currentUser != null && !"ADMIN".equals(currentUser.getRole().name())) {
                Optional<Collaborator> existing = repo.findById(matricule);
                if (existing.isPresent()) {
                    Set<String> accessibleSegments = getAccessibleSegments(currentUser);
                    String collabSegment = existing.get().getSegment();
                    if (collabSegment != null && !accessibleSegments.contains(collabSegment)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Accès non autorisé à ce collaborateur"));
                    }
                }
            }

            return ResponseEntity.ok(collaboratorService.update(matricule, c));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur : " + e.getMessage()));
        }
    }

    // =========================================================
    // DELETE /api/collaborators/{matricule} — Suppression
    // =========================================================
    @DeleteMapping("/{matricule}")
    @PreAuthorize("hasAuthority('collaborateur:delete')")
    public ResponseEntity<?> delete(@PathVariable String matricule) {
        try {
            if (!repo.existsById(matricule))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Collaborateur non trouvé"));

            User currentUser = getCurrentUser();
            if (currentUser != null && !"ADMIN".equals(currentUser.getRole().name())) {
                Optional<Collaborator> existing = repo.findById(matricule);
                if (existing.isPresent()) {
                    Set<String> accessibleSegments = getAccessibleSegments(currentUser);
                    String collabSegment = existing.get().getSegment();
                    if (collabSegment != null && !accessibleSegments.contains(collabSegment)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Accès non autorisé à ce collaborateur"));
                    }
                }
            }

            repo.deleteById(matricule);
            return ResponseEntity.ok(Map.of("message", "Supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur : " + e.getMessage()));
        }
    }
}