package com.polytech.paqbackend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {
    // ── Admin ────────────────────────────────────────────
    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),

    // ── User (lecture commune) ────────────────────────────
    USER_READ("user:read"),
    USER_UPDATE("user:update"),
    USER_CREATE("user:create"),
    USER_DELETE("user:delete"),

    // ── Entretien Explicatif ─────────────────────────────
    EXPLICATIF_CREATE("explicatif:create"),
    EXPLICATIF_UPDATE("explicatif:update"),
    EXPLICATIF_VALIDATE("explicatif:validate"),
    EXPLICATIF_READ("explicatif:read"),

    // ── Entretien d'Accord ───────────────────────────────
    ACCORD_CREATE("accord:create"),
    ACCORD_UPDATE("accord:update"),
    ACCORD_VALIDATE("accord:validate"),
    ACCORD_READ("accord:read"),

    // ── Entretien de Mesure ──────────────────────────────
    MESURE_CREATE("mesure:create"),
    MESURE_UPDATE("mesure:update"),
    MESURE_VALIDATE_1("mesure:validate1"),   // QMSegment
    MESURE_VALIDATE_2("mesure:validate2"),   // SGL
    MESURE_READ("mesure:read"),

    // ── Entretien de Décision ────────────────────────────
    DECISION_CREATE("decision:create"),
    DECISION_UPDATE("decision:update"),
    DECISION_VALIDATE_1("decision:validate1"),  // HP / SGL
    DECISION_VALIDATE_2("decision:validate2"),  // QMPlant
    DECISION_READ("decision:read"),

    // ── Entretien Final ──────────────────────────────────
    FINAL_CREATE("final:create"),
    FINAL_UPDATE("final:update"),
    FINAL_VALIDATE("final:validate"),
    FINAL_READ("final:read"),



    // ── PAQ général ──────────────────────────────────────
    PAQ_READ("paq:read"),
    PAQ_CREATE("paq:create"),

    // ── Collaborateur ─────────────────────────────────────
    COLLABORATEUR_READ("collaborateur:read"),
    COLLABORATEUR_CREATE("collaborateur:create"),
    COLLABORATEUR_UPDATE("collaborateur:update"),
    COLLABORATEUR_DELETE("collaborateur:delete"),

    
    // ── Entretien Positif ─────────────────────────────────
    POSITIF_READ("positif:read"),
    POSITIF_SEND("positif:send"),
    POSITIF_ARCHIVE("positif:archive"),

    // ── Archives ──────────────────────────────────────────
    ARCHIVE_READ("archive:read"),

    // ── Notifications ─────────────────────────────────────
    NOTIFICATION_READ("notification:read");

    @Getter
    private final String permission;
}