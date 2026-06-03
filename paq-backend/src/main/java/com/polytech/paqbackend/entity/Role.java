package com.polytech.paqbackend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static com.polytech.paqbackend.entity.Permission.*;

@RequiredArgsConstructor
public enum Role {

    /**
     * SL — Chef de ligne
     * Explicatif : Créer / Modifier  / Valider
     * Accord     : Créer / Modifier  / Valider
     * Mesure     : Créer
     * Décision   : Créer / Modifier
     * Positif    : Tout
     * Défaut grave : peut notifier SGL
     */
    SL(Set.of(
            USER_READ,
            PAQ_READ, PAQ_CREATE,
            COLLABORATEUR_READ, COLLABORATEUR_CREATE, COLLABORATEUR_UPDATE, COLLABORATEUR_DELETE,
            EXPLICATIF_CREATE, EXPLICATIF_UPDATE,  EXPLICATIF_VALIDATE, EXPLICATIF_READ,
            ACCORD_CREATE, ACCORD_UPDATE,  ACCORD_VALIDATE, ACCORD_READ,
            MESURE_CREATE, MESURE_READ,
            DECISION_CREATE, DECISION_UPDATE, DECISION_READ,
            POSITIF_READ, POSITIF_SEND, POSITIF_ARCHIVE,
            ARCHIVE_READ,
            NOTIFICATION_READ
    )),

    /**
     * SGL — Chef de segment
     * Explicatif :  Valider (obligatoire défaut grave)
     * Mesure     : Modifier / Supprimer / Valider (2e)
     * Décision   : Valider (1re)
     */
    SGL(Set.of(
            USER_READ,
            PAQ_READ,
            COLLABORATEUR_READ,
            EXPLICATIF_UPDATE,
           EXPLICATIF_READ,
            EXPLICATIF_VALIDATE,
            ACCORD_READ,
            MESURE_UPDATE, MESURE_VALIDATE_2, MESURE_READ,
            DECISION_VALIDATE_1, DECISION_READ,
            ARCHIVE_READ,
            NOTIFICATION_READ
    )),

    /**
     * QM_SEGMENT — Qualité Segment
     * Accord  : Valider
     * Mesure  : Valider (1re)
     */
    QM_SEGMENT(Set.of(
            USER_READ, PAQ_READ, COLLABORATEUR_READ,
            EXPLICATIF_READ,
            ACCORD_VALIDATE, ACCORD_READ,
            MESURE_VALIDATE_1, MESURE_READ,
            DECISION_READ, FINAL_READ,
            ARCHIVE_READ, NOTIFICATION_READ
    )),

    /**
     * QM_PLANT — Qualité Plant
     * Décision : Valider (2e)
     */
    QM_PLANT(Set.of(
            USER_READ, PAQ_READ, COLLABORATEUR_READ,
            EXPLICATIF_READ, ACCORD_READ, MESURE_READ,
            DECISION_VALIDATE_2, DECISION_READ,
            ARCHIVE_READ, NOTIFICATION_READ
    )),

    /**
     * HP — Hiérarchie Plant
     * Décision : Valider (1re)
     */
    HP(Set.of(
            USER_READ, PAQ_READ, COLLABORATEUR_READ,
            EXPLICATIF_READ, ACCORD_READ, MESURE_READ,
            DECISION_VALIDATE_1, DECISION_READ,
             ARCHIVE_READ, NOTIFICATION_READ
    )),

    /**
     * RH — Ressources Humaines
     * Final : Créer / Modifier / Supprimer / Valider / Consulter
     */
    RH(Set.of(
            USER_READ, PAQ_READ, COLLABORATEUR_READ,
            EXPLICATIF_READ, ACCORD_READ, MESURE_READ, DECISION_READ,
            FINAL_CREATE, FINAL_UPDATE, FINAL_VALIDATE, FINAL_READ,
            ARCHIVE_READ, NOTIFICATION_READ
    )),

    /**
     * ADMIN — Tous les droits
     */
    ADMIN(Set.of(
            ADMIN_READ, ADMIN_CREATE, ADMIN_UPDATE, ADMIN_DELETE,
            USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE


    ));

    @Getter
    private final Set<Permission> permissions;

    public List<String> getAuthorities() {
        return permissions.stream()
                .map(Permission::getPermission)
                .collect(Collectors.toList());
    }

    public List<SimpleGrantedAuthority> getGrantedAuthorities() {
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermission()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}