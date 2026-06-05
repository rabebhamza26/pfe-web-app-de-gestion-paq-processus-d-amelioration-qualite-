import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { collaboratorService, paqService } from "../../services/api";
import "../../styles/paq-dossier.css";

import { usePermissions } from "../../hooks/usePermissions";
import { showConfirmAlert, showErrorAlert, showSuccessAlert } from "../../utils/entretienAlerts";

export default function PaqDossier() {
  const { matricule } = useParams();
  const navigate      = useNavigate();

  const [collaborator,  setCollaborator]  = useState(null);
  const [currentPaq,    setCurrentPaq]    = useState(null);
  const [historique,    setHistorique]    = useState([]);
  const [loading,       setLoading]       = useState(true);
  const [error,         setError]         = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const { canNotifyDefautGrave } = usePermissions();

  useEffect(() => { loadData(); }, [matricule]);

  // ── Chargement données ────────────────────────────────────────────────────
  const loadData = async () => {
    try {
      setLoading(true);
      setError("");

      // 1. Collaborateur
      const collabRes = await collaboratorService.getById(matricule);
      setCollaborator(collabRes.data);

      // 2. PAQ actif uniquement (période en cours, 6 mois)
      try {
        const paqsRes = await paqService.getAllByMatricule(matricule);
        const paqs    = paqsRes.data || [];

        // On prend UNIQUEMENT le PAQ actif (actif=true, archived=false)
        const active = paqs.find(p => p.actif === true && p.archived === false) || null;
        setCurrentPaq(active);

        // Historique du PAQ actif uniquement
        if (active?.historique) {
          try { 
            const parsedHist = JSON.parse(active.historique);
            setHistorique(parsedHist);
          }
          catch { setHistorique([]); }
        } else {
          setHistorique([]);
        }
      } catch (err) {
        if (err.response?.status === 404) {
          setCurrentPaq(null);
          setHistorique([]);
        } else throw err;
      }
    } catch (err) {
      setError("Impossible de charger les données");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // ── Créer PAQ ─────────────────────────────────────────────────────────────
const createPaq = async () => {
    try {
        setLoading(true);
        const res = await paqService.create(matricule);
        if (res.data) {
            // Sauvegarder la date de création dans localStorage
            localStorage.setItem(`last_paq_${matricule}`, new Date().toISOString());
            
            setCurrentPaq(res.data);
            if (res.data.historique) setHistorique(JSON.parse(res.data.historique));
            showSuccess("Dossier PAQ créé avec succès !");
        }
    } catch (err) {
        showError(err.response?.data?.message || "Erreur lors de la création");
    } finally {
        setLoading(false);
    }
};

  // ── Archiver PAQ ───────────────────────────────────────────────────────────
  const archiverPaq = async () => {
    const result = await showConfirmAlert({
      title: "Archiver ce dossier ?",
      text: "Le dossier sortira de la periode active.",
      confirmButtonText: "Archiver",
    });
    if (!result.isConfirmed) return;

    try {
      setLoading(true);
      await paqService.archive(matricule);
      setCurrentPaq(null);
      setHistorique([]);
      showSuccess("Dossier archive avec succes");
    } catch (err) {
      showError(err.response?.data?.message || "Erreur lors de l'archivage");
    } finally {
      setLoading(false);
    }
  };

  // ── Helpers messages ──────────────────────────────────────────────────────
  const showSuccess = (msg) => {
    setActionMessage(msg);
    setTimeout(() => setActionMessage(""), 4000);
    showSuccessAlert("Operation reussie", msg);
  };
  const showError   = (msg) => {
    setError(msg);
    setTimeout(() => setError(""), 4000);
    showErrorAlert("Operation impossible", msg);
  };

  // ── Logique boutons ───────────────────────────────────────────────────────

  /** PAQ clôturé ou archivé → lecture seule */
  const isBlocked = currentPaq?.statut === "CLOTURE" || currentPaq?.archived;

  /** true si la date de fin du PAQ est dépassée */
  const isPaqExpire = () =>
    !!currentPaq?.dateFin && new Date(currentPaq.dateFin) <= new Date();

  /**
   * RÈGLE PRINCIPALE :
   * Le bouton "Déclarer Faute" a été SUPPRIMÉ.
   * Le processus commence directement par l'entretien explicatif.
   */
  const peutVoirEntretiens = () =>
    !!currentPaq && !isBlocked;

  /** Prochain entretien selon le niveau actuel */
const getProchainEntretien = () => {
  if (!peutVoirEntretiens()) return null;
  const map = {
      0: { nom: "Etape 1: Entretien Explicatif",  route: `/entretien-explicatif/${matricule}` },
    1: { nom: "Etape 2: Entretien d'Accord",     route: `/entretien-daccord/${matricule}` },
    2: { nom: "Etape 3: Entretien de Mesure",    route: `/entretien-de-mesure/${matricule}` },  // ← niveau 2 = entretien de mesure
    3: { nom: "Etape 4: Entretien de Décision",  route: `/entretien-de-decision/${matricule}` },
    4: { nom: "Etape 5: Entretien Final",        route: `/entretien-final/${matricule}` },
  };
  
  console.log("=== DEBUG: Niveau PAQ =", currentPaq?.niveau);
  console.log("=== DEBUG: Prochain entretien =", map[currentPaq?.niveau]);
  
  return map[currentPaq?.niveau] || null;
};

  /** Peut créer un PAQ ? (pas de PAQ actif + ancienneté OK) */
  const peutCreerPaq = () => {
    if (currentPaq) return false;
    if (!collaborator?.hireDate) return false;
    const limit = new Date(collaborator.hireDate);
    limit.setMonth(limit.getMonth() + 6);
    return new Date() >= limit;
  };

  // Vérifier si le collaborateur a eu un entretien positif dans l'historique
  const aEuEntretienPositif = () => {
    return historique.some(h => {
      const action = (h.action || "").toLowerCase();
      return action.includes("entretien positif") || action.includes("positif");
    });
  };

  // Vérifier si le collaborateur est éligible à l'entretien positif (6 mois sans faute)
  const estEligibleEntretienPositif = () => {
    if (!currentPaq) return false;
    // Vérifier si une faute a été enregistrée dans ce PAQ
    const aDesFautes = historique.some(h => {
      const action = (h.action || "").toLowerCase();
      return action.includes("faute enregistrée");
    });
    // Si pas de faute et pas déjà d'entretien positif, et que le PAQ a plus de 6 mois
    return !aDesFautes && !aEuEntretienPositif() && currentPaq.dateCreation && 
           new Date(currentPaq.dateCreation) <= new Date(Date.now() - 180 * 24 * 60 * 60 * 1000);
  };

  // ── Progression période ───────────────────────────────────────────────────
  const getPaqProgress = () => {
    if (!currentPaq?.dateCreation || !currentPaq?.dateFin) return 0;
    const start   = new Date(currentPaq.dateCreation);
    const end     = new Date(currentPaq.dateFin);
    const elapsed = new Date() - start;
    return Math.min(100, Math.max(0, Math.round((elapsed / (end - start)) * 100)));
  };

  // ── Formatage ─────────────────────────────────────────────────────────────
  const formatDate = (d) => {
    if (!d) return "–";
    try {
      return new Date(d).toLocaleDateString("fr-FR", {
        day: "2-digit", month: "2-digit", year: "numeric",
      });
    } catch { return d; }
  };

  const formatHistoriqueDetail = (detail, date) => {
    if (!detail) return detail;
    let s = String(detail)
      .replace(/\s*-\s*Niveau\s+(descendant|ascendant)\s+de\s+(\d+)\s+à\s+(\d+)\s*/gi,
        (m, _d, from, to) => (from === to ? " - " : m))
      .replace(/\s*-\s*-\s*/g, " - ")
      .replace(/^\s*-\s*/g, "")
      .replace(/\s*-\s*$/g, "")
      .replace(/\s{2,}/g, " ")
      .trim();
    return s.replace(/\b(\d{4})-(\d{2})-(\d{2})\b/g, (_, y, m, d) => `${d}/${m}/${y}`);
  };

  const getBadgeClass = (action = "") => {
    const a = action.toLowerCase();
    if (a.includes("entretien"))  return "leoni-badge leoni-badge-blue";
    if (a.includes("faute"))      return "leoni-badge leoni-badge-red";
    if (a.includes("création"))   return "leoni-badge leoni-badge-green";
    if (a.includes("archiv"))     return "leoni-badge leoni-badge-gray";
    if (a.includes("positif"))    return "leoni-badge leoni-badge-success";
    return "leoni-badge leoni-badge-gray";
  };

  const handleHistoriqueClick = (action) => {
    if (!action) return;
    const a = action.toLowerCase();

    // Chaque action ouvre la page de l'entretien correspondant
    if (a.includes("explicatif"))                               navigate(`/entretien-explicatif/${matricule}`);
    else if (a.includes("accord"))                              navigate(`/entretien-daccord/${matricule}`);
    else if (a.includes("mesure"))                              navigate(`/entretien-de-mesure/${matricule}`);
    else if (a.includes("décision") || a.includes("decision")) navigate(`/entretien-de-decision/${matricule}`);
    else if (a.includes("final"))                               navigate(`/entretien-final/${matricule}`);
    else if (a.includes("positif"))                             navigate(`/entretien-positif/${matricule}`);
  };

  // ── Rendu ─────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="leoni-loading">
        <div className="leoni-spinner"></div>
        <p>Chargement du dossier...</p>
      </div>
    );
  }

  const prochainEntretien = getProchainEntretien();

  return (
    <div className="leoni-shell">

      {/* ── HEADER ─────────────────────────────────────────────────────────── */}
      <div className="leoni-header">

        <div className="leoni-header-left">
          <button onClick={() => navigate("/collaborateurs")} className="leoni-btn-back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M19 12H5M5 12l7 7M5 12l7-7" stroke="currentColor" strokeWidth="2"
                strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Retour
          </button>
        </div>

        <div className="leoni-header-title">
          <div className="leoni-logo-bar">
            <div className="leoni-logo-accent"></div>
            <h1>Dossier PAQ</h1>
          </div>
          {collaborator && (
            <span className="leoni-header-sub">
              {collaborator.name} {collaborator.prenom} — {collaborator.matricule}
            </span>
          )}
        </div>

        <div className="leoni-header-actions">

          {/* Créer PAQ — uniquement si pas de PAQ actif et 6 mois atteints */}
          {!currentPaq && peutCreerPaq() && (
            <button onClick={createPaq} className="leoni-btn leoni-btn-success">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
                <path d="M12 8v8M8 12h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
              </svg>
              Créer PAQ
            </button>
          )}

          {currentPaq && !isBlocked && (
            <>
              {/* ── ÉTAPE : Passer à l'entretien (après création du PAQ) ── */}
              {prochainEntretien && (
                <button
                  onClick={() => navigate(prochainEntretien.route)}
                  className="leoni-btn leoni-btn-primary"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M8 12h8M13 8l4 4-4 4" stroke="currentColor" strokeWidth="2"
                      strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  {prochainEntretien.nom}
                </button>
              )}

              {/* ── Archiver manuellement (période expirée) ── */}
              {isPaqExpire() && (
                <button onClick={archiverPaq} className="leoni-btn leoni-btn-outline">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <polyline points="21 8 21 21 3 21 3 8" stroke="currentColor" strokeWidth="2"/>
                    <rect x="1" y="3" width="22" height="5" rx="1" stroke="currentColor" strokeWidth="2"/>
                  </svg>
                  Archiver
                </button>
              )}
            </>
          )}
        </div>
      </div>

      {/* ── ALERTES ────────────────────────────────────────────────────────── */}
      {actionMessage && (
        <div className="leoni-alert leoni-alert-success">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2.5"
              strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          {actionMessage}
          <button className="leoni-alert-close" onClick={() => setActionMessage("")}>✕</button>
        </div>
      )}
      {error && (
        <div className="leoni-alert leoni-alert-error">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
            <path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
          </svg>
          {error}
          <button className="leoni-alert-close" onClick={() => setError("")}>✕</button>
        </div>
      )}
      {isBlocked && (
        <div className="leoni-alert leoni-alert-warning">
          Ce dossier est <strong>{currentPaq?.statut === "CLOTURE" ? "clôturé" : "archivé"}</strong> — mode lecture seule
        </div>
      )}
      {currentPaq && !isBlocked && isPaqExpire() && (
        <div className="leoni-alert leoni-alert-warning">
          La période de 6 mois est <strong>écoulée</strong>. Ce dossier sera archivé automatiquement.
          Vous pouvez l'archiver manuellement dès maintenant.
        </div>
      )}

      {estEligibleEntretienPositif() && (
        <div className="leoni-alert leoni-alert-success">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2.5"/>
          </svg>
          ✨ Félicitations ! Aucune faute depuis 6 mois. Un entretien positif peut être réalisé.
          <button 
            onClick={() => navigate(`/entretien-positif/${matricule}`)}
            className="leoni-btn leoni-btn-success ms-2"
            style={{ marginLeft: "12px" }}
          >
            Réaliser l'entretien positif
          </button>
        </div>
      )}

     
      {/* ── CONTENU PRINCIPAL ──────────────────────────────────────────────── */}
      <div className="leoni-grid-main">

        {/* ── COLONNE GAUCHE ────────────────────────────────────────────────── */}
        <div className="leoni-col-left">

          {/* Fiche collaborateur */}
          {collaborator && (
          
            <div className="leoni-card">
              <div className="leoni-card-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke="currentColor" strokeWidth="2"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" strokeWidth="2"/>
                </svg>
                Informations Collaborateur
              </div>
              <div className="leoni-card-body">
                <div className="leoni-collab-avatar">
                  {`${collaborator.name?.[0] || ""}${collaborator.prenom?.[0] || ""}`.toUpperCase()}
                </div>
                <div className="leoni-collab-name">{collaborator.name} {collaborator.prenom}</div>
                <div className="leoni-collab-info-grid">
                  <div className="leoni-info-item">
                    <span className="leoni-info-label">Matricule</span>
                    <span className="leoni-info-value leoni-mono">{collaborator.matricule}</span>
                  </div>
                  <div className="leoni-info-item">
                    <span className="leoni-info-label">Segment</span>
                    <span className="leoni-info-value">{collaborator.segment || "–"}</span>
                  </div>
                  <div className="leoni-info-item">
                    <span className="leoni-info-label">Date d'embauche</span>
                    <span className="leoni-info-value leoni-mono">{formatDate(collaborator.hireDate)}</span>
                  </div>
                  <div className="leoni-info-item">
                    <span className="leoni-info-label">Statut</span>
                    <span className={`leoni-badge ${collaborator.status === "ACTIF" ? "leoni-badge-green" : "leoni-badge-gray"}`}>
                      {collaborator.status}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Niveau PAQ */}
          {currentPaq && (
            <div className="leoni-card leoni-card-niveau">
              <div className="leoni-card-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M18 20V10M12 20V4M6 20v-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
                Niveau PAQ Actuel
                {currentPaq.statut === "CRITIQUE" && (
                  <span className="leoni-badge leoni-badge-red ms-auto">CRITIQUE</span>
                )}
              </div>
              <div className="leoni-card-body leoni-niveau-body">

                {/* Cercle de progression */}
                <div className="leoni-niveau-circle">
                  <svg viewBox="0 0 80 80" className="leoni-niveau-svg">
                    <circle cx="40" cy="40" r="34" fill="none" stroke="#e2e8f0" strokeWidth="6"/>
                    <circle cx="40" cy="40" r="34" fill="none"
                      stroke={currentPaq.niveau === 0
                        ? "#f59e0b"
                        : currentPaq.statut === "CRITIQUE"
                          ? "#ef4444"
                          : "#005baa"}
                      strokeWidth="6"
                      strokeDasharray={`${(currentPaq.niveau / 5) * 213.6} 213.6`}
                      strokeDashoffset="53.4"
                      strokeLinecap="round"/>
                  </svg>
                  <div className="leoni-niveau-num">{currentPaq.niveau}<span>/5</span></div>
                </div>

                {/* Étapes */}
                <div className="leoni-niveau-steps">
                  {[1,2,3,4,5].map(n => (
                    <div key={n} className={`leoni-step ${n <= currentPaq.niveau ? "done" : n === currentPaq.niveau + 1 ? "next" : ""}`}>
                      <div className="leoni-step-dot">{n <= currentPaq.niveau ? "✓" : n}</div>
                      <div className="leoni-step-label">N{n}</div>
                    </div>
                  ))}
                </div>

                {/* Description état */}
                {/* Description état */}
<div className="leoni-niveau-desc">
  {currentPaq.niveau === 0 && (
    <span> En attente de la réalisation de l'entretien explicatif</span>
  )}
  {currentPaq.niveau === 1 && " Entretien explicatif réalisé"}
  {currentPaq.niveau === 2 && " Entretien d'accord réalisé - Passer à l'entretien de mesure"}
  {currentPaq.niveau === 3 && " Entretien de mesure réalisé"}
  {currentPaq.niveau === 4 && " Entretien de décision réalisé"}
  {currentPaq.niveau === 5 && " Entretien Final réalisé"}
  {aEuEntretienPositif() && " Entretien positif réalisé - Félicitations !"}
  {estEligibleEntretienPositif() && !aEuEntretienPositif() && " Éligible à l'entretien positif !"}
</div>

                {/* Barre de progression période */}
                <div className="leoni-periode-bar">
                  <div className="leoni-periode-labels">
                    <span className="leoni-mono">{formatDate(currentPaq.dateCreation)}</span>
                    <span className="leoni-mono">{formatDate(currentPaq.dateFin)}</span>
                  </div>
                  <div className="leoni-progress-track">
                    <div className="leoni-progress-fill" style={{ width: `${getPaqProgress()}%` }}></div>
                  </div>
                  <div className="leoni-periode-pct">{getPaqProgress()}% de la période écoulée</div>
                </div>
              </div>
            </div>
          )}

          {/* Pas de PAQ actif */}
          {!currentPaq && collaborator && (
            <div className="leoni-card">
              <div className="leoni-card-body" style={{ textAlign: "center", padding: "32px 16px" }}>
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" style={{ color: "#cbd5e1", marginBottom: 12 }}>
                  <polyline points="21 8 21 21 3 21 3 8" stroke="currentColor" strokeWidth="1.5"/>
                  <rect x="1" y="3" width="22" height="5" rx="1" stroke="currentColor" strokeWidth="1.5"/>
                </svg>
                <p style={{ color: "#64748b", fontWeight: 500 }}>Aucun dossier PAQ en cours</p>
                {peutCreerPaq() ? (
                  <button onClick={createPaq} className="leoni-btn leoni-btn-success" style={{ marginTop: 12 }}>
                    Créer un dossier PAQ
                  </button>
                ) : (
                  <p className="leoni-info-notice" style={{ marginTop: 12 }}>
                    Disponible après 6 mois d'ancienneté
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* ── COLONNE DROITE ─────────────────────────────────────────────────── */}
        <div className="leoni-col-right">

          {/* ── HISTORIQUE CHRONOLOGIQUE ── */}
          <div className="leoni-card">
            <div className="leoni-card-header">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
                <polyline points="12 6 12 12 16 14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
              </svg>
              Historique des Événements
              {currentPaq && (
                <span className="leoni-badge leoni-badge-blue ms-auto" style={{ fontSize: "11px" }}>
                  Période en cours
                </span>
              )}
              {historique.length > 0 && (
                <span className="leoni-badge leoni-badge-blue ms-2">{historique.length}</span>
              )}
            </div>
            <div className="leoni-card-body p-0">
              {!currentPaq ? (
                <div className="leoni-empty">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="1.5"/>
                    <path d="M12 8v4M12 16h.01" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                  <p>Aucun dossier PAQ actif</p>
                  <small style={{ color: "#94a3b8" }}>
                    Les archives des dossiers précédents sont consultables dans la section Archivage.
                  </small>
                </div>
              ) : historique.length === 0 ? (
                <div className="leoni-empty">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="1.5"/>
                    <path d="M12 8v4M12 16h.01" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                  <p>Aucun événement enregistré</p>
                  <small style={{ color: "#94a3b8" }}>
                    Commencez par lancer l'entretien explicatif.
                  </small>
                </div>
              ) : (
                <div className="leoni-timeline">
                  {[...historique]
                    .filter(h => {
                      const action = (h.action || "").toLowerCase();
                      return !action.includes("creation suite entretien positif");
                    })
                    .sort((a, b) => new Date(a.date) - new Date(b.date))
                    .map((h, index, array) => {
                      const isEntretien = (h.action || "").toLowerCase().includes("entretien");
                      const isPositif = (h.action || "").toLowerCase().includes("positif");
                      const isFaute = (h.action || "").toLowerCase().includes("faute");
                      const isCreation = (h.action || "").toLowerCase().includes("création");
                      
                      let icon = null;
                      if (isEntretien || isPositif) {
                        icon = (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                            <path d="M12 8v4l3 3M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z" stroke="currentColor" strokeWidth="2"/>
                          </svg>
                        );
                      } else if (isFaute) {
                        icon = (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                            <path d="M12 9v4M12 17h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" stroke="currentColor" strokeWidth="2"/>
                          </svg>
                        );
                      } else if (isCreation) {
                        icon = (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                            <polyline points="21 8 21 21 3 21 3 8" stroke="currentColor" strokeWidth="2"/>
                            <rect x="1" y="3" width="22" height="5" rx="1" stroke="currentColor" strokeWidth="2"/>
                          </svg>
                        );
                      } else {
                        icon = (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
                            <path d="M12 8v4M12 16h.01" stroke="currentColor" strokeWidth="2"/>
                          </svg>
                        );
                      }

                      return (
                        <div key={index} className="leoni-timeline-item">
                          {index < array.length - 1 && <div className="leoni-timeline-line"></div>}
                          
                          <div className={`leoni-timeline-dot ${isEntretien ? 'entretien' : isPositif ? 'positif' : isFaute ? 'faute' : 'default'}`}>
                            {icon}
                          </div>
                          
                          <div className="leoni-timeline-content">
                            <div className="leoni-timeline-header">
                              <span className="leoni-timeline-date leoni-mono">
                                {formatDate(h.date)}
                              </span>
                              {(isEntretien || isPositif) ? (
                                <button
                                  onClick={() => handleHistoriqueClick(h.action)}
                                  className={`leoni-timeline-badge ${getBadgeClass(h.action)}`}
                                >
                                  {h.action}
                                </button>
                              ) : (
                                <span className={`leoni-timeline-badge ${getBadgeClass(h.action)}`}>
                                  {h.action}
                                </span>
                              )}
                            </div>
                            <div className="leoni-timeline-detail">
                              {formatHistoriqueDetail(h.detail, h.date)}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}