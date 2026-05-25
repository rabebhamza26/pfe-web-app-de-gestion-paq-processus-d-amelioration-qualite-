package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.*;
import com.polytech.paqbackend.entity.*;
import com.polytech.paqbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final PlantRepository plantRepository;
    private final SegmentRepository segmentRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);


    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAllWithAllRelations()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SiteUserDistributionDTO> getUsersDistributionBySite() {
        // Note: Assurez-vous que la requête dans UserRepository est correcte
        // Version alternative si la requête JPQL pose problème:
        List<User> users = userRepository.findAllWithAllRelations();
        Map<Long, SiteUserDistributionDTO> distributionMap = new HashMap<>();

        for (User user : users) {
            for (Site site : user.getSites()) {
                distributionMap.computeIfAbsent(site.getId(), k ->
                                new SiteUserDistributionDTO(site.getId(), site.getName(), 0L))
                        .setUserCount(distributionMap.get(site.getId()).getUserCount() + 1);
            }
        }

        return new ArrayList<>(distributionMap.values())
                .stream()
                .sorted((a, b) -> b.getUserCount().compareTo(a.getUserCount()))
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(Long id) {
        return userRepository.findByIdWithAllRelations(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public UserResponseDto createUser(CreateUserRequest request) {
        User user = new User();
        user.setActive(true);
        user.setEmail(request.getEmail());
        user.setLogin(request.getLogin());
        user.setNomUtilisateur(request.getNomUtilisateur());
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            throw new RuntimeException("Password is required");
        }

        if (request.getSiteIds() != null && !request.getSiteIds().isEmpty()) {
            Set<Site> sites = new HashSet<>(siteRepository.findAllById(request.getSiteIds()));
            user.setSites(sites);
        }

        if (request.getPlantIds() != null && !request.getPlantIds().isEmpty()) {
            Set<Plant> plants = new HashSet<>(plantRepository.findAllById(request.getPlantIds()));
            user.setPlants(plants);
        }

        if (request.getSegmentIds() != null && !request.getSegmentIds().isEmpty()) {
            Set<Segment> segments = new HashSet<>(segmentRepository.findAllById(request.getSegmentIds()));
            user.setSegments(segments);
        }

        return mapToResponseDto(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            User existing = userRepository.findByEmail(request.getEmail());
            if (existing != null && !existing.getId().equals(id)) {
                throw new RuntimeException("Email déjà utilisé");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getNomUtilisateur() != null) {
            user.setNomUtilisateur(request.getNomUtilisateur());
        }

        if (request.getLogin() != null) {
            user.setLogin(request.getLogin());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getSiteIds() != null) {
            Set<Site> sites = new HashSet<>(siteRepository.findAllById(request.getSiteIds()));
            user.setSites(sites);
        }

        if (request.getPlantIds() != null) {
            Set<Plant> plants = new HashSet<>(plantRepository.findAllById(request.getPlantIds()));
            user.setPlants(plants);
        }

        if (request.getSegmentIds() != null) {
            Set<Segment> segments = new HashSet<>(segmentRepository.findAllById(request.getSegmentIds()));
            user.setSegments(segments);
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return mapToResponseDto(userRepository.save(user));
    }

    public List<String> getAllEmails() {
        return userRepository.findAllEmails();
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponseDto toggleActive(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.isActive());
        return mapToResponseDto(userRepository.save(user));
    }

    private UserResponseDto mapToResponseDto(User user) {
        List<String> permissions = user.getRole().getAuthorities();

        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nomUtilisateur(user.getNomUtilisateur())
                .login(user.getLogin())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .permissions(permissions)
                .siteIds(user.getSites().stream().map(Site::getId).collect(Collectors.toList()))
                .siteNames(user.getSites().stream().map(Site::getName).collect(Collectors.toList()))
                .plantIds(user.getPlants().stream().map(Plant::getId).collect(Collectors.toList()))
                .plantNames(user.getPlants().stream().map(Plant::getName).collect(Collectors.toList()))
                .segmentIds(user.getSegments().stream().map(Segment::getId).collect(Collectors.toList()))
                .segmentNames(user.getSegments().stream().map(Segment::getNomSegment).collect(Collectors.toList()))
                .build();
    }





    public List<String> getAllActiveUserEmails() {
        return userRepository.findAllActiveUserEmails();
    }

    // ⭐ NOUVEAU: Récupérer les emails par Site ET Plant
    public List<String> getEmailsBySiteAndPlant(Long siteId, Long plantId) {
        return userRepository.findEmailsBySiteAndPlant(siteId, plantId);
    }

    // Récupérer les emails par Site uniquement
    public List<String> getEmailsBySite(Long siteId) {
        return userRepository.findEmailsBySite(siteId);
    }

    // Récupérer les emails par Plant uniquement
    public List<String> getEmailsByPlant(Long plantId) {
        return userRepository.findEmailsByPlant(plantId);
    }

    // Récupérer les emails par plusieurs Sites et Plants
    public List<String> getEmailsBySitesAndPlants(List<Long> siteIds, List<Long> plantIds) {
        Set<String> allEmails = new HashSet<>();

        if (siteIds != null && !siteIds.isEmpty()) {
            for (Long siteId : siteIds) {
                allEmails.addAll(userRepository.findEmailsBySite(siteId));
            }
        }

        if (plantIds != null && !plantIds.isEmpty()) {
            for (Long plantId : plantIds) {
                allEmails.addAll(userRepository.findEmailsByPlant(plantId));
            }
        }

        return new ArrayList<>(allEmails);
    }

    public List<String> getQMEmails() {
        try {
            // Méthode 1: Utiliser la requête JPQL directe
            List<String> qmEmails = userRepository.findQMEmails();

            if (qmEmails == null || qmEmails.isEmpty()) {
                log.warn("Aucun email QM_SEGMENT trouvé dans la base de données");
                return new ArrayList<>();
            }

            log.info("{} emails QM_SEGMENT trouvés", qmEmails.size());
            return qmEmails;

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails QM_SEGMENT", e);
            return new ArrayList<>();
        }
    }

    public List<String> getQMEmailsByPerimeter(User currentUser) {
        try {
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;

            if (isAdmin) {
                // ADMIN voit tous les QM_SEGMENT
                return userRepository.findQMEmails();
            }

            // Récupérer les sites et plants accessibles par l'utilisateur
            Set<Long> accessibleSiteIds = new HashSet<>();
            Set<Long> accessiblePlantIds = new HashSet<>();

            // Sites directement assignés
            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getId() != null) {
                        accessibleSiteIds.add(site.getId());
                    }
                }
            }

            // Plants directement assignés
            if (currentUser.getPlants() != null) {
                for (Plant plant : currentUser.getPlants()) {
                    if (plant != null && plant.getId() != null) {
                        accessiblePlantIds.add(plant.getId());
                    }
                }
            }

            // Plants via sites
            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getPlants() != null) {
                        for (Plant plant : site.getPlants()) {
                            if (plant != null && plant.getId() != null) {
                                accessiblePlantIds.add(plant.getId());
                            }
                        }
                    }
                }
            }

            Set<String> allEmails = new HashSet<>();

            // Chercher les QM_SEGMENT par sites accessibles
            if (!accessibleSiteIds.isEmpty()) {
                for (Long siteId : accessibleSiteIds) {
                    List<String> siteEmails = userRepository.findQMEmailsBySite(siteId);
                    allEmails.addAll(siteEmails);
                }
            }

            // Chercher les QM_SEGMENT par plants accessibles
            if (!accessiblePlantIds.isEmpty()) {
                for (Long plantId : accessiblePlantIds) {
                    List<String> plantEmails = userRepository.findQMEmailsByPlant(plantId);
                    allEmails.addAll(plantEmails);
                }
            }

            log.info("{} emails QM_SEGMENT trouvés dans le périmètre de {}", allEmails.size(), currentUser.getEmail());
            return new ArrayList<>(allEmails);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails QM_SEGMENT par périmètre", e);
            return new ArrayList<>();
        }
    }

    public List<String> getSGLEmailsByPerimeter(User currentUser) {
        try {
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;

            if (isAdmin) {
                return userRepository.findSGLEmails();
            }

            Set<Long> accessibleSiteIds = new HashSet<>();
            Set<Long> accessiblePlantIds = new HashSet<>();

            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getId() != null) {
                        accessibleSiteIds.add(site.getId());
                    }
                }
            }

            if (currentUser.getPlants() != null) {
                for (Plant plant : currentUser.getPlants()) {
                    if (plant != null && plant.getId() != null) {
                        accessiblePlantIds.add(plant.getId());
                    }
                }
            }

            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getPlants() != null) {
                        for (Plant plant : site.getPlants()) {
                            if (plant != null && plant.getId() != null) {
                                accessiblePlantIds.add(plant.getId());
                            }
                        }
                    }
                }
            }

            Set<String> allEmails = new HashSet<>();

            if (!accessibleSiteIds.isEmpty()) {
                for (Long siteId : accessibleSiteIds) {
                    List<String> siteEmails = userRepository.findSGLEmailsBySite(siteId);
                    allEmails.addAll(siteEmails);
                }
            }

            if (!accessiblePlantIds.isEmpty()) {
                for (Long plantId : accessiblePlantIds) {
                    List<String> plantEmails = userRepository.findSGLEmailsByPlant(plantId);
                    allEmails.addAll(plantEmails);
                }
            }

            log.info("{} emails SGL trouvés dans le périmètre de {}", allEmails.size(), currentUser.getEmail());
            return new ArrayList<>(allEmails);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails SGL par périmètre", e);
            return new ArrayList<>();
        }
    }

// Dans UserService.java - Ajoutez ces méthodes

    public List<String> getHPEmailsByPerimeter(User currentUser) {
        try {
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;

            if (isAdmin) {
                return userRepository.findHPEmails();
            }

            Set<Long> accessibleSiteIds = new HashSet<>();
            Set<Long> accessiblePlantIds = new HashSet<>();

            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getId() != null) {
                        accessibleSiteIds.add(site.getId());
                    }
                }
            }

            if (currentUser.getPlants() != null) {
                for (Plant plant : currentUser.getPlants()) {
                    if (plant != null && plant.getId() != null) {
                        accessiblePlantIds.add(plant.getId());
                    }
                }
            }

            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getPlants() != null) {
                        for (Plant plant : site.getPlants()) {
                            if (plant != null && plant.getId() != null) {
                                accessiblePlantIds.add(plant.getId());
                            }
                        }
                    }
                }
            }

            Set<String> allEmails = new HashSet<>();

            if (!accessibleSiteIds.isEmpty()) {
                for (Long siteId : accessibleSiteIds) {
                    List<String> siteEmails = userRepository.findHPEmailsBySite(siteId);
                    allEmails.addAll(siteEmails);
                }
            }

            if (!accessiblePlantIds.isEmpty()) {
                for (Long plantId : accessiblePlantIds) {
                    List<String> plantEmails = userRepository.findHPEmailsByPlant(plantId);
                    allEmails.addAll(plantEmails);
                }
            }

            log.info("{} emails HP trouvés dans le périmètre de {}", allEmails.size(), currentUser.getEmail());
            return new ArrayList<>(allEmails);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails HP par périmètre", e);
            return new ArrayList<>();
        }
    }

    public List<String> getQMPlantEmailsByPerimeter(User currentUser) {
        try {
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;

            if (isAdmin) {
                return userRepository.findQMPlantEmails();
            }

            Set<Long> accessibleSiteIds = new HashSet<>();
            Set<Long> accessiblePlantIds = new HashSet<>();

            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getId() != null) {
                        accessibleSiteIds.add(site.getId());
                    }
                }
            }

            if (currentUser.getPlants() != null) {
                for (Plant plant : currentUser.getPlants()) {
                    if (plant != null && plant.getId() != null) {
                        accessiblePlantIds.add(plant.getId());
                    }
                }
            }

            if (currentUser.getSites() != null) {
                for (Site site : currentUser.getSites()) {
                    if (site != null && site.getPlants() != null) {
                        for (Plant plant : site.getPlants()) {
                            if (plant != null && plant.getId() != null) {
                                accessiblePlantIds.add(plant.getId());
                            }
                        }
                    }
                }
            }

            Set<String> allEmails = new HashSet<>();

            if (!accessibleSiteIds.isEmpty()) {
                for (Long siteId : accessibleSiteIds) {
                    List<String> siteEmails = userRepository.findQMPlantEmailsBySite(siteId);
                    allEmails.addAll(siteEmails);
                }
            }

            if (!accessiblePlantIds.isEmpty()) {
                for (Long plantId : accessiblePlantIds) {
                    List<String> plantEmails = userRepository.findQMPlantEmailsByPlant(plantId);
                    allEmails.addAll(plantEmails);
                }
            }

            log.info("{} emails QM_PLANT trouvés dans le périmètre de {}", allEmails.size(), currentUser.getEmail());
            return new ArrayList<>(allEmails);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails QM_PLANT par périmètre", e);
            return new ArrayList<>();
        }
    }


}

