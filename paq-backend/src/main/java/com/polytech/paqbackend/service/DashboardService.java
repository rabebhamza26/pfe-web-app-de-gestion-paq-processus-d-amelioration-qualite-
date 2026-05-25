package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.*;
import com.polytech.paqbackend.entity.*;
import com.polytech.paqbackend.repository.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PaqRepository paqRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final EntretienExplicatifRepository entretienExplicatifRepository;
    private final EntretienDaccordRepository entretienDaccordRepository;
    private final EntretienMesureRepository entretienMesureRepository;
    private final EntretienDecisionRepository entretienDecisionRepository;
    private final EntretienFinalRepository entretienFinalRepository;

    public DashboardService(PaqRepository paqRepository,
                            CollaboratorRepository collaboratorRepository,
                            UserRepository userRepository,
                            EntretienExplicatifRepository entretienExplicatifRepository,
                            EntretienDaccordRepository entretienDaccordRepository,
                            EntretienMesureRepository entretienMesureRepository,
                            EntretienDecisionRepository entretienDecisionRepository,
                            EntretienFinalRepository entretienFinalRepository) {
        this.paqRepository = paqRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
        this.entretienExplicatifRepository = entretienExplicatifRepository;
        this.entretienDaccordRepository = entretienDaccordRepository;
        this.entretienMesureRepository = entretienMesureRepository;
        this.entretienDecisionRepository = entretienDecisionRepository;
        this.entretienFinalRepository = entretienFinalRepository;
    }

    public DashboardStatsDTO getStats(Long siteId, Long plantId) {
        DashboardStatsDTO dto = new DashboardStatsDTO();
        List<CollaborateurDTO> filteredDTOs = getFilteredCollaboratorDTOs(siteId, plantId);
        List<Collaborator> filteredCollabs = getFilteredCollaborators(siteId, plantId);
        List<PaqDossier> filteredPaqs = getFilteredPaqs(siteId, plantId);

        dto.setTotalCollaborateurs(filteredCollabs.size());
        dto.setTotalPaqs(filteredPaqs.size());

        long paqActifs = filteredPaqs.stream()
                .filter(p -> !"CLOTURE".equals(p.getStatut()) && !"ARCHIVE".equals(p.getStatut()))
                .count();
        dto.setPaqEnCours(paqActifs);

        Map<Integer, Long> paqParNiveau = filteredPaqs.stream()
                .collect(Collectors.groupingBy(PaqDossier::getNiveau, Collectors.counting()));
        dto.setPaqParNiveau(paqParNiveau);

        List<CollaborateurDTO> sansFaute = filteredDTOs.stream()
                .filter(c -> c.getNiveau() == 0 || "POSITIF".equals(c.getStatut()))
                .toList();
        dto.setSansFaute(sansFaute);

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long inactiveUsers = userRepository.countByActiveFalse();
        dto.setTotalUsers(totalUsers);
        dto.setActiveUsers(activeUsers);
        dto.setInactiveUsers(inactiveUsers);

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        dto.setNewUsersThisMonth(userRepository.countByCreatedAtAfter(startOfMonth));

        long newPaqsThisMonth = filteredPaqs.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfMonth))
                .count();
        dto.setNewPaqsThisMonth(newPaqsThisMonth);

        long activeCollaborators = filteredCollabs.stream()
                .filter(c -> c.isActif() && !c.isDepart())
                .count();
        dto.setActiveCollaborators(activeCollaborators);

        List<Object[]> roleCountsResult = userRepository.countUsersByRole();
        Map<String, Long> roleCounts = new HashMap<>();
        for (Object[] result : roleCountsResult) {
            roleCounts.put(result[0].toString(), (Long) result[1]);
        }
        dto.setRoleCounts(roleCounts);

        return dto;
    }

    public EntretiensTotalsDTO getEntretiensTotals(Long siteId, Long plantId) {
        EntretiensTotalsDTO dto = new EntretiensTotalsDTO();

        try {
            List<String> matricules = getFilteredMatricules(siteId, plantId);

            if (matricules == null || matricules.isEmpty()) {
                return getDefaultTotals();
            }

            dto.setExplicatif(entretienExplicatifRepository.countByMatriculeIn(matricules));
            dto.setAccord(entretienDaccordRepository.countByMatriculeIn(matricules));
            dto.setMesure(entretienMesureRepository.countByMatriculeIn(matricules));
            dto.setDecision(entretienDecisionRepository.countByMatriculeIn(matricules));
            dto.setFinal(entretienFinalRepository.countByMatriculeIn(matricules));
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des totaux des entretiens: " + e.getMessage());
            return getDefaultTotals();
        }

        return dto;
    }

    private EntretiensTotalsDTO getDefaultTotals() {
        EntretiensTotalsDTO dto = new EntretiensTotalsDTO();
        dto.setExplicatif(0);
        dto.setAccord(0);
        dto.setMesure(0);
        dto.setDecision(0);
        dto.setFinal(0);
        return dto;
    }

    /**
     * Récupère l'évolution des entretiens sur les 6 derniers mois
     * CORRIGÉ : Gère correctement les différents noms de colonnes de date
     */
    public List<EntretienEvolutionDTO> getEntretiensEvolution(Long siteId, Long plantId) {
        try {
            List<String> matricules = getFilteredMatricules(siteId, plantId);

            List<EntretienEvolutionDTO> result = new ArrayList<>();

            // Obtenir la date actuelle
            LocalDate now = LocalDate.now();

            // Calculer les 6 derniers mois complets (en commençant par le plus ancien)
            for (int i = 5; i >= 0; i--) {
                LocalDate targetDate = now.minusMonths(i);
                LocalDate startDate = targetDate.withDayOfMonth(1);
                LocalDate endDate = targetDate.withDayOfMonth(targetDate.lengthOfMonth());

                // Formater le mois (Oct, Nov, Déc, Jan, Fév, Mar, Avr, Mai, etc.)
                String periode = formatMonthFrench(targetDate);

                long count = calculateTotalEntretiensForPeriod(matricules, startDate, endDate);

                System.out.println("Mois: " + periode + " (" + startDate + " à " + endDate + ") = " + count + " entretiens");

                result.add(new EntretienEvolutionDTO(periode, count));
            }

            return result;

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            return getDefaultEvolutionData();
        }
    }

    private String formatMonthFrench(LocalDate date) {
        // Map pour avoir les abréviations françaises standard
        Map<Integer, String> monthMap = Map.ofEntries(
                Map.entry(1, "Jan"),
                Map.entry(2, "Fév"),
                Map.entry(3, "Mar"),
                Map.entry(4, "Avr"),
                Map.entry(5, "Mai"),
                Map.entry(6, "Juin"),
                Map.entry(7, "Juil"),
                Map.entry(8, "Aoû"),
                Map.entry(9, "Sep"),
                Map.entry(10, "Oct"),
                Map.entry(11, "Nov"),
                Map.entry(12, "Déc")
        );
        return monthMap.get(date.getMonthValue());
    }

    private List<EntretienEvolutionDTO> getDefaultEvolutionData() {
        List<EntretienEvolutionDTO> result = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate targetDate = now.minusMonths(i);
            String periode = formatMonthFrench(targetDate);
            result.add(new EntretienEvolutionDTO(periode, 0));
        }
        return result;
    }
    /**
     * Calcule le total des entretiens pour une période donnée
     * CORRIGÉ : Utilise le bon nom de colonne pour chaque type d'entretien
     */
    private long calculateTotalEntretiensForPeriod(List<String> matricules, LocalDate startDate, LocalDate endDate) {
        long total = 0;

        System.out.println("Calcul pour période: " + startDate + " à " + endDate);
        System.out.println("Nombre de matricules: " + (matricules != null ? matricules.size() : 0));

        try {
            long explicatif = entretienExplicatifRepository.countByMatriculeInAndDateBetween(matricules, startDate, endDate);
            System.out.println("Entretiens explicatifs: " + explicatif);
            total += explicatif;
        } catch (Exception e) {
            System.err.println("Erreur EntretienExplicatif: " + e.getMessage());
        }

        try {
            long accord = entretienDaccordRepository.countByMatriculeInAndDateBetween(matricules, startDate, endDate);
            System.out.println("Entretiens d'accord: " + accord);
            total += accord;
        } catch (Exception e) {
            System.err.println("Erreur EntretienDaccord: " + e.getMessage());
        }

        try {
            long mesure = entretienMesureRepository.countByMatriculeInAndDateBetween(matricules, startDate, endDate);
            System.out.println("Entretiens mesure: " + mesure);
            total += mesure;
        } catch (Exception e) {
            System.err.println("Erreur EntretienMesure: " + e.getMessage());
        }

        try {
            long decision = entretienDecisionRepository.countByMatriculeInAndDateBetween(matricules, startDate, endDate);
            System.out.println("Entretiens decision: " + decision);
            total += decision;
        } catch (Exception e) {
            System.err.println("Erreur EntretienDecision: " + e.getMessage());
        }

        try {
            long finalEnt = entretienFinalRepository.countByMatriculeInAndDateBetween(matricules, startDate, endDate);
            System.out.println("Entretiens finaux: " + finalEnt);
            total += finalEnt;
        } catch (Exception e) {
            System.err.println("Erreur EntretienFinal: " + e.getMessage());
        }

        System.out.println("Total pour la période: " + total);
        return total;
    }
    /**
     * Données mock pour le développement et les tests
     */

    private List<String> getFilteredMatricules(Long siteId, Long plantId) {
        try {
            List<CollaborateurDTO> dtos = getFilteredCollaboratorDTOs(siteId, plantId);
            if (dtos == null || dtos.isEmpty()) {
                return new ArrayList<>();
            }
            return dtos.stream()
                    .map(CollaborateurDTO::getMatricule)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur getFilteredMatricules: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            String username = auth.getName();
            User user = userRepository.findByEmail(username);
            if (user == null) {
                user = userRepository.findByLogin(username);
            }
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> getAccessibleSegments(User user) {
        Set<String> segments = new HashSet<>();
        if (user == null) return segments;
        if (user.getSegments() != null) {
            user.getSegments().forEach(s -> {
                if (s != null && s.getNomSegment() != null) segments.add(s.getNomSegment());
            });
        }
        if (user.getPlants() != null) {
            user.getPlants().forEach(p -> {
                if (p != null && p.getSegments() != null) {
                    p.getSegments().forEach(s -> {
                        if (s != null && s.getNomSegment() != null) segments.add(s.getNomSegment());
                    });
                }
            });
        }
        if (user.getSites() != null) {
            user.getSites().forEach(site -> {
                if (site != null && site.getPlants() != null) {
                    site.getPlants().forEach(p -> {
                        if (p != null && p.getSegments() != null) {
                            p.getSegments().forEach(s -> {
                                if (s != null && s.getNomSegment() != null) segments.add(s.getNomSegment());
                            });
                        }
                    });
                }
            });
        }
        return segments;
    }

    private List<CollaborateurDTO> getFilteredCollaboratorDTOs(Long siteId, Long plantId) {
        try {
            User currentUser = getCurrentUser();
            boolean isAdmin = currentUser != null && currentUser.getRole() != null && "ADMIN".equals(currentUser.getRole().name());

            if (isAdmin) {
                if (plantId != null) {
                    return collaboratorRepository.getCollaboratorsByPlants(List.of(plantId));
                } else if (siteId != null) {
                    return collaboratorRepository.getCollaboratorsBySites(List.of(siteId));
                }
                return collaboratorRepository.getAllWithPaq();
            }

            if (currentUser != null) {
                if (plantId != null && isPlantAllowed(currentUser, plantId)) {
                    return collaboratorRepository.getCollaboratorsByPlants(List.of(plantId));
                }
                if (siteId != null && isSiteAllowed(currentUser, siteId)) {
                    return collaboratorRepository.getCollaboratorsBySites(List.of(siteId));
                }
                Set<String> accessibleSegments = getAccessibleSegments(currentUser);
                if (!accessibleSegments.isEmpty()) {
                    return collaboratorRepository.getCollaboratorsBySegments(new ArrayList<>(accessibleSegments));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur getFilteredCollaboratorDTOs: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private boolean isPlantAllowed(User user, Long plantId) {
        return user.getPlants() != null && user.getPlants().stream().anyMatch(p -> p.getId().equals(plantId));
    }

    private boolean isSiteAllowed(User user, Long siteId) {
        return user.getSites() != null && user.getSites().stream().anyMatch(s -> s.getId().equals(siteId));
    }

    private List<Collaborator> getFilteredCollaborators(Long siteId, Long plantId) {
        List<CollaborateurDTO> dtos = getFilteredCollaboratorDTOs(siteId, plantId);
        Set<String> matricules = dtos.stream().map(CollaborateurDTO::getMatricule).collect(Collectors.toSet());
        if (matricules.isEmpty()) return new ArrayList<>();
        return collaboratorRepository.findByDepartFalseAndArchivedFalse().stream()
                .filter(c -> matricules.contains(c.getMatricule()))
                .toList();
    }

    private List<PaqDossier> getFilteredPaqs(Long siteId, Long plantId) {
        List<CollaborateurDTO> dtos = getFilteredCollaboratorDTOs(siteId, plantId);
        Set<String> matricules = dtos.stream().map(CollaborateurDTO::getMatricule).collect(Collectors.toSet());
        if (matricules.isEmpty()) return new ArrayList<>();
        return paqRepository.findAll().stream()
                .filter(p -> !p.isArchived() && matricules.contains(p.getCollaboratorMatricule()))
                .toList();
    }
}