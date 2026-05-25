// EntretienDaccord.jsx
// Flux : SL valide → email QM_SEGMENT + valide=true
//        QM_SEGMENT valide → valideQM=true + enregistrement dossier PAQ

import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  collaboratorService,
  entretienDaccordService,
  entretienService,
  fauteService,
  userService,
} from "../../services/api";

import "../../styles/paq-dossier.css";
import "../../styles/entretien-explicatif.css";
import "../../styles/entretien-accord.css";
import {
  showErrorAlert,
  showInfoToast,
  showSuccessAlert,
  showSuccessToast,
} from "../../utils/entretienAlerts";

// ─── Modal Email pour sélection multiple (SL uniquement) ─────────────────────────────
function EmailModal({ isOpen, onClose, onConfirm, emailsList, loadingEmails }) {
  const [selectedEmails, setSelectedEmails] = useState([]);

  useEffect(() => {
    if (isOpen) {
      setSelectedEmails([]);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const toggleEmailSelection = (email) => {
    if (selectedEmails.includes(email)) {
      setSelectedEmails(selectedEmails.filter(e => e !== email));
    } else {
      setSelectedEmails([...selectedEmails, email]);
    }
  };

  const toggleAllEmails = () => {
    if (selectedEmails.length === emailsList.length && emailsList.length > 0) {
      setSelectedEmails([]);
    } else {
      setSelectedEmails([...emailsList]);
    }
  };

  const allSelected = emailsList.length > 0 && selectedEmails.length === emailsList.length;

  return (
    <div className="leoni-modal-overlay" onClick={onClose}>
      <div className="leoni-modal" style={{ maxWidth: "600px", maxHeight: "80vh", overflow: "auto" }} onClick={e => e.stopPropagation()}>
        <div className="leoni-modal-header">
          <div>
            <h3>Envoyer la convocation — QM-Segment</h3>
            <p>Sélectionnez les QM-Segment à convoquer pour l'entretien d'accord</p>
          </div>
          <button className="leoni-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="leoni-modal-body">
          <div style={{ marginBottom: 20, border: "1px solid #e2e8f0", borderRadius: "8px", overflow: "hidden" }}>
            <div style={{ 
              padding: "12px", 
              background: "#f8f9fa", 
              borderBottom: "1px solid #e2e8f0",
              display: "flex",
              alignItems: "center",
              gap: "12px"
            }}>
              <input
                type="checkbox"
                checked={allSelected}
                onChange={toggleAllEmails}
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
              <strong style={{ flex: 1 }}>Sélectionner tous les emails ({emailsList.length})</strong>
              <span style={{ fontSize: "12px", color: "#666" }}>
                {selectedEmails.length} sélectionné(s)
              </span>
            </div>
            <div style={{ maxHeight: "300px", overflow: "auto" }}>
              {loadingEmails ? (
                <div style={{ padding: "40px", textAlign: "center", color: "#999" }}>
                  Chargement des emails...
                </div>
              ) : emailsList.length > 0 ? (
                emailsList.map((email, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: "12px",
                      borderBottom: "1px solid #e2e8f0",
                      display: "flex",
                      alignItems: "center",
                      gap: "12px",
                      background: selectedEmails.includes(email) ? "#f0f9ff" : "white",
                      cursor: "pointer"
                    }}
                    onClick={() => toggleEmailSelection(email)}
                  >
                    <input
                      type="checkbox"
                      checked={selectedEmails.includes(email)}
                      onChange={() => toggleEmailSelection(email)}
                      style={{ width: "18px", height: "18px", cursor: "pointer" }}
                    />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 500, color: "#1a202c" }}>{email}</div>
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ padding: "40px", textAlign: "center", color: "#999" }}>
                  Aucun QM-Segment trouvé dans la base de données
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="leoni-modal-footer">
          <button type="button" className="leoni-btn leoni-btn-outline" onClick={onClose}>
            Annuler
          </button>
          <button
            type="button"
            className="leoni-btn leoni-btn-primary"
            onClick={() => onConfirm(selectedEmails)}
            disabled={selectedEmails.length === 0 || loadingEmails}
          >
            Valider & Envoyer ({selectedEmails.length} destinataire(s))
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── État initial formulaire ──────────────────────────────────────────────────
const buildDefaultForm = () => ({
  typeFaute: "",
  dateEntretien: new Date().toISOString().split("T")[0],
  causeFaute: "",
  mesuresProposees: "",
  casca: "", // Champ CASCA optionnel
});

// ─── Composant principal ──────────────────────────────────────────────────────
export default function EntretienDaccord({ niveau = 2 }) {
  const { matricule } = useParams();
  const navigate = useNavigate();

  const [userRole, setUserRole] = useState(null);
  const [collaborator, setCollaborator] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [error, setError] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [resumeN1, setResumeN1] = useState(null);
  const [currentEntretienId, setCurrentEntretienId] = useState(null);
  const [entretiensList, setEntretiensList] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [typeOptions, setTypeOptions] = useState([]);
  const [showDefautModal, setShowDefautModal] = useState(false);
  const [defautTypeInput, setDefautTypeInput] = useState("");
  const [search, setSearch] = useState("");
  const [filteredFautes, setFilteredFautes] = useState([]);

  // Email modal - utilise l'endpoint spécifique QM_SEGMENT
  const [emailsList, setEmailsList] = useState([]);
  const [loadingEmails, setLoadingEmails] = useState(false);
  const [showEmailModal, setShowEmailModal] = useState(false);

  // Deux statuts distincts
  const [valideSL, setValideSL] = useState(false);
  const [valideQM, setValideQM] = useState(false);

  const [formData, setFormData] = useState(buildDefaultForm());

  // ── Rôle ──
  useEffect(() => {
    const userStr = sessionStorage.getItem("user");
    if (userStr) {
      const user = JSON.parse(userStr);
      setUserRole(user.role);
    }
  }, []);

  // ── Emails QM_SEGMENT uniquement ──
 // Dans EntretienDaccord.jsx - Remplacer la fonction loadQMEmails

// ── Emails QM_SEGMENT par périmètre (site/plant du user connecté) ──
const loadQMEmails = async () => {
  try {
    setLoadingEmails(true);
    // Utiliser l'endpoint qui filtre par périmètre
    const response = await userService.getQMEmailsByPerimeter();
    const validEmails = Array.isArray(response?.data) 
      ? response.data.filter(email => email && email.includes('@'))
      : [];
    setEmailsList(validEmails);
    console.log("📧 Emails QM_SEGMENT par périmètre chargés:", validEmails);
  } catch (err) {
    console.error("Erreur chargement emails QM_SEGMENT par périmètre:", err);
    // Fallback: utiliser l'ancien endpoint
    try {
      const fallbackResponse = await userService.getQMEmails();
      const allEmails = Array.isArray(fallbackResponse?.data) ? fallbackResponse.data : [];
      setEmailsList(allEmails);
    } catch (fallbackErr) {
      console.error("Erreur fallback chargement emails:", fallbackErr);
      setEmailsList([]);
    }
  } finally {
    setLoadingEmails(false);
  }
};

  useEffect(() => {
    loadData();
    loadFautes();
    loadQMEmails();
  }, [matricule]);

  useEffect(() => {
    if (matricule) loadDraft();
  }, [matricule]);

  // ── Filtrage fautes ──
  useEffect(() => {
    const q = search.toLowerCase();
    setFilteredFautes(typeOptions.filter(f => f.toLowerCase().includes(q)));
  }, [search, typeOptions]);

  // ── Reset ──
  const resetForm = () => {
    setFormData(buildDefaultForm());
    setCurrentEntretienId(null);
    setValideSL(false);
    setValideQM(false);
    if (matricule) localStorage.removeItem(`entretien-daccord-draft-${matricule}`);
  };

  // ── Fautes ──
  const loadFautes = async () => {
    try {
      const res = await fauteService.getAll();
      setTypeOptions(res.data.map(f => f.nom));
    } catch (err) {
      console.error("Erreur chargement fautes:", err);
    }
  };

  // ── Données principales ──
  const loadData = async () => {
    try {
      setLoading(true);
      const collabRes = await collaboratorService.getById(matricule);
      setCollaborator(collabRes.data);

      try {
        const res = await entretienService.getByMatricule(matricule);
        const liste = Array.isArray(res.data) ? res.data : [];
        if (liste.length > 0) {
          const dernier = liste.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
          setResumeN1({
            typeFaute: dernier.typeFaute || "–",
            dateFaute: dernier.dateFaute,
            causeFaute: dernier.description || "–",
            mesuresCorrectives: dernier.mesuresCorrectives || "–",
            commentaire: dernier.commentaire || "–",
          });
        } else {
          setResumeN1(null);
        }
      } catch {
        setResumeN1(null);
      }

      await loadAllEntretiens();
    } catch (err) {
      setError("Impossible de charger les informations.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // ── Liste entretiens ──
  const loadAllEntretiens = async () => {
    try {
      const res = await entretienDaccordService.getByMatricule(matricule);
      const list = Array.isArray(res.data) ? res.data : [];
      setEntretiensList(list);
      if (list.length > 0) {
        const dernier = list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
        chargerEntretienDansFormulaire(dernier);
        setValideSL(dernier.valide || false);
        setValideQM(dernier.valideQM || false);
      } else {
        resetForm();
      }
    } catch (err) {
      console.warn("Impossible de charger les entretiens d'accord:", err);
    }
  };

  const chargerEntretienDansFormulaire = (entretien) => {
    if (!entretien) return;
    setCurrentEntretienId(entretien.id);
    setFormData({
      typeFaute: entretien.typeFaute || "",
      dateEntretien: entretien.date || new Date().toISOString().split("T")[0],
      causeFaute: entretien.causeFaute || "",
      mesuresProposees: entretien.mesuresProposees || "",
      casca: entretien.casca || "", // Charger CASCA
    });
    if (entretien.typeFaute && !typeOptions.includes(entretien.typeFaute)) {
      setTypeOptions(prev => [...prev, entretien.typeFaute]);
    }
  };

  // ── Brouillon ──
  const loadDraft = () => {
    try {
      const draft = localStorage.getItem(`entretien-daccord-draft-${matricule}`);
      if (!draft) return;
      const parsed = JSON.parse(draft);
      setFormData(prev => ({ ...prev, ...parsed }));
      if (parsed.id) setCurrentEntretienId(parsed.id);
    } catch (err) {
      console.warn("Brouillon non chargeable:", err);
    }
  };

  const handleChange = e => {
    if (!canModify && userRole !== "QM_SEGMENT") {
      showErrorAlert("Permission refusée", "Vous n'avez pas les droits.");
      return;
    }
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // Gestion spécifique pour le champ CASCA (numérique)
  const handleCascaChange = (e) => {
    if (!canModify && userRole !== "QM_SEGMENT") {
      showErrorAlert("Permission refusée", "Vous n'avez pas les droits.");
      return;
    }
    const value = e.target.value;
    if (value === "" || value === "-" || /^-?\d*\.?\d*$/.test(value)) {
      setFormData(prev => ({ ...prev, casca: value }));
    }
  };

  // ── Bouton MODIFIER ──
  const handleModifier = async () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL peuvent modifier un entretien.");
      return;
    }
    if (entretiensList.length === 0) {
      setError("Aucun entretien d'accord existant à modifier.");
      return;
    }
    const dernier = entretiensList.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
    chargerEntretienDansFormulaire(dernier);
    showInfoToast("Dernier entretien chargé pour modification");
  };

  // ── Bouton BROUILLON ──
  const handleEnregistrer = () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL peuvent enregistrer un brouillon.");
      return;
    }
    setSavingDraft(true);
    try {
      const payload = { ...formData, id: currentEntretienId };
      localStorage.setItem(`entretien-daccord-draft-${matricule}`, JSON.stringify(payload));
      setStatusMessage("Brouillon enregistré avec succès.");
      showSuccessToast("Brouillon enregistré");
    } catch {
      showErrorAlert("Brouillon non enregistré", "Impossible d'enregistrer le brouillon.");
    } finally {
      setSavingDraft(false);
    }
  };

  // ── Validation SL : création/modification + email QM ──
  const handleSLValidation = async (destinatairesEmails) => {
    setShowEmailModal(false);
    setSaving(true);
    try {
      const destinataireEmailString = Array.isArray(destinatairesEmails) 
        ? destinatairesEmails.join(",") 
        : destinatairesEmails;

      const entretienData = {
        typeFaute: formData.typeFaute,
        date: formData.dateEntretien,
        causeFaute: formData.causeFaute,
        mesuresProposees: formData.mesuresProposees || "",
        destinataireEmail: destinataireEmailString,
        casca: formData.casca ? parseFloat(formData.casca) : null, // Envoyer CASCA
      };

      if (currentEntretienId) {
        await entretienDaccordService.update(matricule, currentEntretienId, entretienData);
        await entretienDaccordService.validerPremiere(matricule, currentEntretienId, entretienData);
      } else {
        const response = await entretienDaccordService.create(matricule, entretienData);
        if (response?.data?.id) {
          await entretienDaccordService.validerPremiere(matricule, response.data.id, entretienData);
        }
      }

      const nbDestinataires = Array.isArray(destinatairesEmails) ? destinatairesEmails.length : 1;
      setStatusMessage(`Entretien validé. Email de convocation envoyé à ${nbDestinataires} destinataire(s).`);
      await showSuccessAlert("Entretien soumis", `L'email de convocation a été envoyé à ${nbDestinataires} destinataire(s).`);
      localStorage.removeItem(`entretien-daccord-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error(err);
      showErrorAlert("Enregistrement impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  // ── Validation QM_SEGMENT : sans email, enregistrement PAQ ──
  const handleValidationQM = async () => {
    setSaving(true);
    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        date: formData.dateEntretien,
        causeFaute: formData.causeFaute,
        mesuresProposees: formData.mesuresProposees || "",
        casca: formData.casca ? parseFloat(formData.casca) : null, // Envoyer CASCA
      };

      await entretienDaccordService.validerFinale(matricule, currentEntretienId, entretienData);

      setValideQM(true);
      setStatusMessage("Entretien d'accord validé par QM-Segment.");
      await showSuccessAlert(
        "Entretien validé",
        "La validation QM-Segment a été enregistrée. Redirection vers le dossier PAQ..."
      );

      localStorage.removeItem(`entretien-daccord-draft-${matricule}`);
      navigate(`/paq-dossier/${matricule}`);
    } catch (err) {
      console.error(err);
      showErrorAlert("Validation impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  // ── Bouton VALIDER (dispatch selon rôle) ──
  const handleValider = () => {
    setError("");
    setStatusMessage("");

    if (!formData.typeFaute) {
      setError("Le type de faute est obligatoire");
      return;
    }

    if (userRole === "SL") {
      setShowEmailModal(true);
    } else if (userRole === "QM_SEGMENT") {
      handleValidationQM();
    } else {
      showErrorAlert("Permission refusée", "Action non autorisée pour votre rôle.");
    }
  };

  // ── Ajout type faute ──
  const addTypeOption = async () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL peuvent ajouter un type de faute.");
      return;
    }
    const value = defautTypeInput.trim();
    if (!value) return;
    try {
      const res = await fauteService.create({ nom: value });
      const newFaute = res.data;
      setTypeOptions(prev => prev.includes(newFaute.nom) ? prev : [...prev, newFaute.nom]);
      setFormData(prev => ({ ...prev, typeFaute: newFaute.nom }));
      setDefautTypeInput("");
      setShowDefautModal(false);
      showSuccessToast("Faute ajoutée");
    } catch {
      showErrorAlert("Ajout impossible", "Erreur lors de l'ajout du type de faute.");
    }
  };

  // ── Permissions ──
  const canModify = userRole === "SL";
  const isEditable = userRole === "SL";

  const showValiderSL = userRole === "SL" && !valideQM;
  const showValiderQM = userRole === "QM_SEGMENT" && !!currentEntretienId && valideSL && !valideQM;
  const showBrouillon = canModify && !valideQM;
  const showValider = showValiderSL || showValiderQM;

  const getValiderLabel = () => {
    if (saving) return userRole === "QM_SEGMENT" ? "Validation..." : "Envoi...";
    if (userRole === "QM_SEGMENT") return "Valider (QM-Segment)";
    if (currentEntretienId) return "Modifier et valider";
    return "Créer et valider";
  };

  const formatDate = dateStr => {
    if (!dateStr) return "–";
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("fr-FR");
  };

  // ── Rendu ──
  if (loading)
    return (
      <div className="leoni-loading">
        <div className="leoni-spinner"></div>
        <p>Chargement...</p>
      </div>
    );

  if (userRole === "ADMIN")
    return (
      <div className="leoni-shell">
        <div className="leoni-alert leoni-alert-error">
          Accès non autorisé. Les administrateurs ne peuvent pas accéder aux entretiens d'accord.
        </div>
        <button onClick={() => navigate("/dashboard")} className="leoni-btn leoni-btn-primary">
          Retour au tableau de bord
        </button>
      </div>
    );

  return (
    <div className="leoni-shell">

      {/* ── Header ── */}
      <div className="leoni-header">
        <div className="leoni-header-left">
          <button onClick={() => navigate(`/paq-dossier/${matricule}`)} className="leoni-btn-back">
            Retour
          </button>
        </div>

        <div className="leoni-header-title">
          <div className="leoni-logo-bar">
            <div className="leoni-logo-accent"></div>
            <h1>Etape 2 : Entretien d'accord</h1>
          </div>
          {collaborator && (
            <span className="leoni-header-sub">
              {collaborator.name || ""} {collaborator.prenom || ""} — {collaborator.matricule || matricule}
            </span>
          )}
        </div>

        <div className="leoni-header-actions">
          {/* Champ CASCA dans l'en-tête */}
          <div className="leoni-casca-field">
            <label htmlFor="casca" className="leoni-casca-label">
              CASCA
            </label>
            <input
              type="text"
              id="casca"
              name="casca"
              value={formData.casca}
              onChange={handleCascaChange}
              className="leoni-input leoni-input-casca"
              placeholder="Optionnel"
              disabled={valideQM || (!isEditable && userRole !== "QM_SEGMENT")}
              style={{ width: "100px", textAlign: "center" }}
            />
          </div>

          {/* Badges de statut */}
          {userRole === "SL" && !currentEntretienId && !valideQM && (
            <span className="leoni-badge-sl">Mode création (SL)</span>
          )}
          {userRole === "SL" && currentEntretienId && !valideSL && (
            <span className="leoni-badge-sl">Mode modification & validation (SL)</span>
          )}
          {userRole === "SL" && valideSL && !valideQM && (
            <span className="leoni-badge-sl">Soumis au QM-Segment — en attente de validation</span>
          )}
          {userRole === "QM_SEGMENT" && currentEntretienId && valideSL && !valideQM && (
            <span className="leoni-badge-qm">Mode validation finale (QM-Segment)</span>
          )}
          {userRole === "QM_SEGMENT" && (!currentEntretienId || !valideSL) && !valideQM && (
            <span className="leoni-badge-consult">Mode consultation (QM-Segment)</span>
          )}
          {(userRole === "SGL" || (userRole !== "SL" && userRole !== "QM_SEGMENT" && userRole !== "ADMIN")) && (
            <span className="leoni-badge-consult">Mode consultation</span>
          )}
          {valideQM && (
            <span className="leoni-badge-success">Entretien validé par QM-Segment</span>
          )}
        </div>
      </div>

      {statusMessage && <div className="leoni-alert leoni-alert-success">{statusMessage}</div>}
      {error && <div className="leoni-alert leoni-alert-error">{error}</div>}

      <div className="leoni-grid-main">

        {/* ── Colonne gauche ── */}
        <div className="leoni-col-left">
          {collaborator && (
            <div className="leoni-card">
              <div className="leoni-card-header">Informations Collaborateur</div>
              <div className="leoni-card-body">
                <div className="leoni-collab-avatar">
                  {`${collaborator.name?.[0] || ""}${collaborator.prenom?.[0] || ""}`.toUpperCase()}
                </div>
                <div className="leoni-collab-name">
                  {collaborator.name || "–"} {collaborator.prenom || ""}
                </div>
                <div className="leoni-collab-info-grid">
                  <div className="leoni-info-item">
                    <span className="leoni-info-label">Matricule</span>
                    <span className="leoni-info-value leoni-mono">{collaborator.matricule || "–"}</span>
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
                    <span className="leoni-info-value">{collaborator.status || "–"}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="leoni-card">
            <div className="leoni-card-header">Résumé — Entretien explicatif (N1)</div>
            <div className="leoni-card-body">
              {resumeN1 ? (
                <div className="leoni-form-stack">
                  <div className="leoni-form-group">
                    <label>Type faute :</label>
                    <p className="leoni-readonly">{resumeN1.typeFaute || "–"}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Date :</label>
                    <p className="leoni-readonly">{formatDate(resumeN1.dateFaute)}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Cause de faute :</label>
                    <p className="leoni-readonly">{resumeN1.causeFaute || "–"}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Mesures correctives :</label>
                    <p className="leoni-readonly">{resumeN1.mesuresCorrectives || "–"}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Commentaire :</label>
                    <p className="leoni-readonly">{resumeN1.commentaire || "–"}</p>
                  </div>
                </div>
              ) : (
                <p className="leoni-muted">Aucun entretien explicatif trouvé.</p>
              )}
            </div>
          </div>
        </div>

        {/* ── Colonne droite : Formulaire ── */}
        <div className="leoni-col-right">
          <div className="leoni-card">
            <div className="leoni-card-header">Formulaire</div>
            <div className="leoni-card-body">
              <div className="leoni-form-stack">

                {/* Type de faute */}
                <div className="leoni-form-group">
                  <label>Type de faute *</label>
                  <div className="leoni-inline">
                    {isEditable && !valideQM && (
                      <button
                        type="button"
                        className="leoni-btn leoni-btn-warning leoni-btn-sm"
                        onClick={() => setShowDefautModal(true)}
                      >
                        + Ajouter faute
                      </button>
                    )}
                    <div className="leoni-dropdown-container" style={{ flex: 1 }}>
                      <input
                        type="text"
                        className="leoni-input"
                        placeholder="Rechercher ou sélectionner une faute..."
                        value={formData.typeFaute}
                        onChange={e => {
                          setFormData(prev => ({ ...prev, typeFaute: e.target.value }));
                          setSearch(e.target.value);
                          setShowDropdown(true);
                        }}
                        onFocus={() => setShowDropdown(true)}
                        disabled={valideQM || (!isEditable && userRole !== "QM_SEGMENT")}
                      />
                      {showDropdown && !valideQM && (
                        <div className="leoni-dropdown">
                          {filteredFautes.length > 0 ? (
                            filteredFautes.map((f, index) => (
                              <div key={index} className="leoni-dropdown-item" onClick={() => {
                                setFormData(prev => ({ ...prev, typeFaute: f }));
                                setSearch(f);
                                setShowDropdown(false);
                              }}>
                                {f}
                              </div>
                            ))
                          ) : (
                            <div className="leoni-dropdown-empty">Aucun résultat</div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Date */}
                <div className="leoni-form-group">
                  <label>Date entretien *</label>
                  <input
                    type="date"
                    name="dateEntretien"
                    value={formData.dateEntretien}
                    onChange={handleChange}
                    className="leoni-input"
                    disabled={valideQM || (!isEditable && userRole !== "QM_SEGMENT")}
                    required
                  />
                </div>

                {/* Cause */}
                <div className="leoni-form-group">
                  <label>Cause de faute</label>
                  <textarea
                    name="causeFaute"
                    value={formData.causeFaute}
                    onChange={handleChange}
                    className="leoni-textarea"
                    rows="3"
                    disabled={valideQM || (!isEditable && userRole !== "QM_SEGMENT")}
                  />
                </div>

                {/* Mesures */}
                <div className="leoni-form-group">
                  <label>Mesures correctives proposées</label>
                  <textarea
                    name="mesuresProposees"
                    value={formData.mesuresProposees}
                    onChange={handleChange}
                    className="leoni-textarea"
                    rows="3"
                    disabled={valideQM || (!isEditable && userRole !== "QM_SEGMENT")}
                  />
                </div>

                {/* Barre d'actions */}
                <div className="leoni-form-actions">
                  {showBrouillon && (
                    <button
                      type="button"
                      className="leoni-btn leoni-btn-outline-dark"
                      onClick={handleEnregistrer}
                      disabled={savingDraft}
                    >
                      {savingDraft ? "Enregistrement..." : "Brouillon"}
                    </button>
                  )}
                  {showValider && (
                    <button
                      type="button"
                      className="leoni-btn leoni-btn-primary"
                      onClick={handleValider}
                      disabled={saving}
                    >
                      {getValiderLabel()}
                    </button>
                  )}

                  {/* Message consultation QM en attente */}
                  {userRole === "QM_SEGMENT" && currentEntretienId && !valideSL && !valideQM && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      En attente de la soumission par le SL.
                    </div>
                  )}
                  {!currentEntretienId && !canModify && userRole !== "QM_SEGMENT" && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      Aucun entretien d'accord trouvé pour ce collaborateur.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Modal Email */}
      <EmailModal
        isOpen={showEmailModal}
        onClose={() => setShowEmailModal(false)}
        onConfirm={handleSLValidation}
        emailsList={emailsList}
        loadingEmails={loadingEmails}
      />

      {/* Modal Ajout faute */}
      {showDefautModal && (
        <div className="leoni-modal-overlay" onClick={() => setShowDefautModal(false)}>
          <div className="leoni-modal" onClick={e => e.stopPropagation()}>
            <div className="leoni-modal-header">
              <div>
                <h3>Ajouter une faute</h3>
                <p>Enregistrer un nouveau type de faute</p>
              </div>
              <button className="leoni-modal-close" onClick={() => setShowDefautModal(false)}>✕</button>
            </div>
            <div className="leoni-modal-body">
              <div className="leoni-form-group">
                <label>Type de faute</label>
                <input
                  type="text"
                  value={defautTypeInput}
                  onChange={e => setDefautTypeInput(e.target.value)}
                  className="leoni-input"
                  placeholder="Saisir un nouveau type de faute"
                  onKeyDown={e => e.key === "Enter" && addTypeOption()}
                />
              </div>
            </div>
            <div className="leoni-modal-footer">
              <button type="button" className="leoni-btn leoni-btn-outline" onClick={() => setShowDefautModal(false)}>
                Annuler
              </button>
              <button
                type="button"
                className="leoni-btn leoni-btn-warning"
                onClick={addTypeOption}
                disabled={!defautTypeInput.trim()}
              >
                Ajouter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}