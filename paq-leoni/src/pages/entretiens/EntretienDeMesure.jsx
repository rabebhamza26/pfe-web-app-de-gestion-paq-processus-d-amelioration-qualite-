// EntretienDeMesure.jsx
// Flux : SL valide → email QM_SEGMENT + SGL + valideSL=true
//        QM_SEGMENT valide (1ère) → valideQM=true
//        SGL valide (2ème) → valideSGL=true + passage niveau PAQ à 3

import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  entretienMesureService,
  fauteService,
  collaboratorService,
  entretienDaccordService,
  userService,
} from "../../services/api";
import "../../styles/entretien-mesure.css";
import "../../styles/paq-dossier.css";
import {
  showErrorAlert,
  showInfoToast,
  showSuccessAlert,
  showSuccessToast,
} from "../../utils/entretienAlerts";

// ─── Modal Email pour sélection multiple (SL) ─────────────────────────────
function EmailModal({ isOpen, onClose, onConfirm, usersList, loadingUsers }) {
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
    if (selectedEmails.length === usersList.length && usersList.length > 0) {
      setSelectedEmails([]);
    } else {
      setSelectedEmails(usersList.map(u => u.email));
    }
  };

  const allSelected = usersList.length > 0 && selectedEmails.length === usersList.length;

  return (
    <div className="leoni-modal-overlay" onClick={onClose}>
      <div className="leoni-modal" style={{ maxWidth: "650px", maxHeight: "80vh", overflow: "auto" }} onClick={e => e.stopPropagation()}>
        <div className="leoni-modal-header">
          <div>
            <h3>Envoyer la convocation</h3>
            <p>Sélectionnez les destinataires pour l'entretien de mesure</p>
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
              <strong style={{ flex: 1 }}>Sélectionner tous les {usersList.length} utilisateur(s)</strong>
              <span style={{ fontSize: "12px", color: "#666" }}>
                {selectedEmails.length} sélectionné(s)
              </span>
            </div>
            <div style={{ maxHeight: "300px", overflow: "auto" }}>
              {usersList.length > 0 ? (
                usersList.map((user, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: "12px",
                      borderBottom: "1px solid #e2e8f0",
                      display: "flex",
                      alignItems: "center",
                      gap: "12px",
                      background: selectedEmails.includes(user.email) ? "#f0f9ff" : "white",
                      cursor: "pointer"
                    }}
                    onClick={() => toggleEmailSelection(user.email)}
                  >
                    <input
                      type="checkbox"
                      checked={selectedEmails.includes(user.email)}
                      onChange={() => toggleEmailSelection(user.email)}
                      style={{ width: "18px", height: "18px", cursor: "pointer" }}
                    />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 500, color: "#1a202c" }}>{user.email}</div>
                      <div style={{ fontSize: "12px", color: "#718096" }}>
                        {user.nomUtilisateur || user.email.split('@')[0]}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ padding: "40px", textAlign: "center", color: "#999" }}>
                  {loadingUsers ? "Chargement des emails..." : "Aucun email trouvé dans la base de données"}
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
            disabled={selectedEmails.length === 0}
          >
            Valider & Envoyer ({selectedEmails.length} destinataire(s))
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Formulaire vide par défaut ──────────────────────────────────────────────
const buildDefaultForm = () => ({
  typeFaute: "",
  dateEntretien: new Date().toISOString().split("T")[0],
  causesPrincipales: "",
  convention: "",
  planAction: "",
});

// ─── Composant principal ──────────────────────────────────────────────────────
export default function EntretienDeMesure() {
  const { matricule } = useParams();
  const navigate = useNavigate();

  const [userRole, setUserRole] = useState(null);
  const [userEmail, setUserEmail] = useState(null);
  const [collaborator, setCollaborator] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [error, setError] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [resumeN2, setResumeN2] = useState(null);
  const [currentEntretienId, setCurrentEntretienId] = useState(null);
  const [entretiensList, setEntretiensList] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [typeOptions, setTypeOptions] = useState([]);
  const [showDefautModal, setShowDefautModal] = useState(false);
  const [defautTypeInput, setDefautTypeInput] = useState("");
  const [search, setSearch] = useState("");
  const [filteredFautes, setFilteredFautes] = useState([]);

  // Email modal
  const [usersList, setUsersList] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [showEmailModal, setShowEmailModal] = useState(false);

  // Trois statuts distincts
  const [valideSL, setValideSL] = useState(false);
  const [valideQM, setValideQM] = useState(false);
  const [valideSGL, setValideSGL] = useState(false);

  const [formData, setFormData] = useState(buildDefaultForm());

  // ── Rôle ──
  useEffect(() => {
    const userStr = sessionStorage.getItem("user");
    if (userStr) {
      const user = JSON.parse(userStr);
      setUserRole(user.role);
      setUserEmail(user.email || user.username);
    }
  }, []);

  const loadUsers = async () => {
    try {
      setLoadingUsers(true);
      const response = await userService.getAllEmails();
      console.log("Emails récupérés:", response.data);
      
      if (response.data && Array.isArray(response.data) && response.data.length > 0) {
        const emailList = response.data.map(email => ({
          email: email,
          nomUtilisateur: email.split('@')[0] || email,
          role: "UTILISATEUR"
        }));
        setUsersList(emailList);
      } else {
        try {
          const usersResponse = await userService.getAllUsersWithEmails();
          if (usersResponse.data && Array.isArray(usersResponse.data)) {
            setUsersList(usersResponse.data);
          } else {
            setUsersList([]);
          }
        } catch (fallbackErr) {
          console.error("Fallback erreur:", fallbackErr);
          setUsersList([]);
        }
      }
    } catch (err) {
      console.error("Erreur chargement emails:", err);
      try {
        const fallbackResponse = await userService.getAllUsersWithEmails();
        if (fallbackResponse.data && Array.isArray(fallbackResponse.data)) {
          setUsersList(fallbackResponse.data);
        } else {
          setUsersList([]);
        }
      } catch (finalErr) {
        setUsersList([]);
      }
    } finally {
      setLoadingUsers(false);
    }
  };

  useEffect(() => {
    loadData();
    loadFautes();
    loadUsers();
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
    setValideSGL(false);
    if (matricule) localStorage.removeItem(`entretien-mesure-draft-${matricule}`);
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
        const res = await entretienDaccordService.getByMatricule(matricule);
        const liste = Array.isArray(res.data) ? res.data : [];
        if (liste.length > 0) {
          const dernier = liste.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
          setResumeN2({
            typeFaute: dernier.typeFaute || "–",
            date: dernier.date,
            causeFaute: dernier.causeFaute || "–",
            mesuresProposees: dernier.mesuresProposees || "–",
          });
        } else {
          setResumeN2(null);
        }
      } catch {
        setResumeN2(null);
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
      const res = await entretienMesureService.getByMatricule(matricule);
      const list = Array.isArray(res.data) ? res.data : [];
      setEntretiensList(list);
      if (list.length > 0) {
        const dernier = list.sort((a, b) => new Date(b.dateCreation) - new Date(a.dateCreation))[0];
        chargerEntretienDansFormulaire(dernier);
        setValideSL(dernier.valideSL || false);
        setValideQM(dernier.valideQM || false);
        setValideSGL(dernier.valideSGL || false);
        console.log("Statuts chargés - valideSL:", dernier.valideSL);
      } else {
        resetForm();
      }
    } catch (err) {
      console.warn("Impossible de charger les entretiens de mesure:", err);
    }
  };

  const chargerEntretienDansFormulaire = (entretien) => {
    if (!entretien) return;
    setCurrentEntretienId(entretien.id);
    setFormData({
      typeFaute: entretien.typeFaute || "",
      dateEntretien: entretien.dateEntretien || new Date().toISOString().split("T")[0],
      causesPrincipales: entretien.causesPrincipales || "",
      convention: entretien.convention || "",
      planAction: entretien.planAction || "",
    });
    if (entretien.typeFaute && !typeOptions.includes(entretien.typeFaute)) {
      setTypeOptions(prev => [...prev, entretien.typeFaute]);
    }
  };

  // ── Brouillon ──
  const loadDraft = () => {
    try {
      const draft = localStorage.getItem(`entretien-mesure-draft-${matricule}`);
      if (!draft) return;
      const parsed = JSON.parse(draft);
      setFormData(prev => ({ ...prev, ...parsed }));
      if (parsed.id) setCurrentEntretienId(parsed.id);
    } catch (err) {
      console.warn("Brouillon non chargeable:", err);
    }
  };

  const handleChange = e => {
    if (!canModify && userRole !== "QM_SEGMENT" && userRole !== "SGL") {
      showErrorAlert("Permission refusée", "Vous n'avez pas les droits.");
      return;
    }
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // ── Bouton MODIFIER (SL uniquement) ──
  const handleModifier = async () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL peuvent modifier un entretien.");
      return;
    }
    if (entretiensList.length === 0) {
      setError("Aucun entretien de mesure existant à modifier.");
      return;
    }
    const dernier = entretiensList.sort((a, b) => new Date(b.dateCreation) - new Date(a.dateCreation))[0];
    chargerEntretienDansFormulaire(dernier);
    showInfoToast("Dernier entretien chargé pour modification");
  };

  // ── Bouton BROUILLON (SL uniquement) ──
  const handleEnregistrer = () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL peuvent enregistrer un brouillon.");
      return;
    }
    setSavingDraft(true);
    try {
      const payload = { ...formData, id: currentEntretienId };
      localStorage.setItem(`entretien-mesure-draft-${matricule}`, JSON.stringify(payload));
      setStatusMessage("Brouillon enregistré avec succès.");
      showSuccessToast("Brouillon enregistré");
    } catch {
      showErrorAlert("Brouillon non enregistré", "Impossible d'enregistrer le brouillon.");
    } finally {
      setSavingDraft(false);
    }
  };

  // ── Validation des dates ──
  const validateDates = () => {
    if (!formData.dateEntretien) {
      showErrorAlert("Date manquante", "Veuillez sélectionner une date d'entretien.");
      return false;
    }
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const entretienDate = new Date(formData.dateEntretien);
    entretienDate.setHours(0, 0, 0, 0);
    
    if (entretienDate < today) {
      showErrorAlert("Date invalide", "La date d'entretien ne peut pas être dans le passé.");
      return false;
    }
    
    return true;
  };

  // ── Validation SL : création/modification + email (CORRIGÉE) ──
  const handleSLValidation = async (destinatairesEmails) => {
    setShowEmailModal(false);
    setSaving(true);
    setError("");
    setStatusMessage("");

    // Vérifications requises
    if (!formData.typeFaute) {
      setError("Le type de faute est obligatoire");
      setSaving(false);
      return;
    }
    if (!formData.dateEntretien) {
      setError("La date d'entretien est obligatoire");
      setSaving(false);
      return;
    }
    if (!validateDates()) {
      setSaving(false);
      return;
    }

    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        causesPrincipales: formData.causesPrincipales || "",
        convention: formData.convention || "",
        planAction: formData.planAction || "",
        destinatairesEmails: destinatairesEmails,
        expediteurEmail: userEmail || "sl@leoni.com"
      };

      let response;
      if (currentEntretienId) {
        // Mise à jour existante
        response = await entretienMesureService.update(matricule, currentEntretienId, entretienData);
        // Appel à l'API de validation qui déclenche l'envoi d'emails
        await entretienMesureService.validerPremiere(matricule, currentEntretienId, entretienData);
      } else {
        // Création
        response = await entretienMesureService.create(matricule, entretienData);
        if (response?.data?.id) {
          await entretienMesureService.validerPremiere(matricule, response.data.id, entretienData);
        }
      }

      const nbDestinataires = destinatairesEmails.length;
      setStatusMessage(`Entretien validé. Email envoyé à ${nbDestinataires} destinataire(s).`);
      await showSuccessAlert(
        "Entretien soumis", 
        `L'entretien de mesure a été créé/validé et un email de convocation a été envoyé à ${nbDestinataires} destinataire(s).`
      );
      
      localStorage.removeItem(`entretien-mesure-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error("Erreur handleSLValidation:", err);
      const errorMsg = err.response?.data?.message || err.message || "Erreur lors de l'envoi";
      setError(errorMsg);
      showErrorAlert("Enregistrement impossible", errorMsg);
    } finally {
      setSaving(false);
    }
  };

  // ── Validation QM_SEGMENT (1ère validation) ──
  const handleValidationQM = async () => {
    if (!validateDates()) return;
    setSaving(true);
    setError("");
    setStatusMessage("");

    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        causesPrincipales: formData.causesPrincipales || "",
        convention: formData.convention || "",
        planAction: formData.planAction || "",
        expediteurEmail: userEmail || "qm@leoni.com"
      };

      await entretienMesureService.valider1(matricule, currentEntretienId, entretienData);

      setValideQM(true);
      setStatusMessage("Entretien de mesure validé (1ère validation QM-Segment).");
      await showSuccessAlert(
        "Validation enregistrée",
        "La validation QM-Segment a été enregistrée."
      );

      localStorage.removeItem(`entretien-mesure-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error(err);
      showErrorAlert("Validation impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  // ── Validation SGL (2ème validation) ──
  const handleValidationSGL = async () => {
    if (!validateDates()) return;
    setSaving(true);
    setError("");
    setStatusMessage("");

    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        causesPrincipales: formData.causesPrincipales || "",
        convention: formData.convention || "",
        planAction: formData.planAction || "",
        expediteurEmail: userEmail || "sgl@leoni.com"
      };

      await entretienMesureService.valider2(matricule, currentEntretienId, entretienData);

      setValideSGL(true);
      setStatusMessage("Entretien de mesure validé (2ème validation SGL).");
      await showSuccessAlert(
        "Validation enregistrée",
        "La validation SGL a été enregistrée et le PAQ passe au niveau 3."
      );

      localStorage.removeItem(`entretien-mesure-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
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
      showErrorAlert("Champ requis", "Le type de faute est obligatoire");
      return;
    }
    if (!formData.dateEntretien) {
      setError("La date d'entretien est obligatoire");
      showErrorAlert("Champ requis", "La date d'entretien est obligatoire");
      return;
    }

    if (!validateDates()) return;

    if (userRole === "SL") {
      setShowEmailModal(true);
    } else if (userRole === "QM_SEGMENT") {
      handleValidationQM();
    } else if (userRole === "SGL") {
      handleValidationSGL();
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

  // SL peut valider si SGL n'a pas encore validé
  const showValiderSL = userRole === "SL" && !valideSGL;

  // QM peut valider si SL a soumis ET que QM n'a pas encore validé ET SGL pas encore validé
  const showValiderQM = userRole === "QM_SEGMENT" && !!currentEntretienId && valideSL && !valideQM && !valideSGL;

  // SGL peut valider si SL a soumis ET QM a validé ET SGL pas encore validé
  const showValiderSGL = userRole === "SGL" && !!currentEntretienId && valideSL && valideQM && !valideSGL;

  const showModifier = canModify && !!currentEntretienId && !valideSGL;
  const showBrouillon = canModify && !valideSGL;
  const showValider = showValiderSL || showValiderQM || showValiderSGL;

  const getValiderLabel = () => {
    if (saving) return "Enregistrement...";
    if (userRole === "SGL") return "Valider (2ème validation)";
    if (userRole === "QM_SEGMENT") return "Valider (1ère validation)";
    if (currentEntretienId) return "Modifier et valider";
    return "Créer et valider";
  };

  const formatDate = dateStr => {
    if (!dateStr) return "–";
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("fr-FR");
  };

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
          Accès non autorisé.
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
            <h1>Etape 3 : Entretien de Mesure</h1>
          </div>
          {collaborator && (
            <span className="leoni-header-sub">
              {collaborator.name || ""} {collaborator.prenom || ""} — {collaborator.matricule || matricule}
            </span>
          )}

          {/* Badges de statut */}
          {userRole === "SL" && !currentEntretienId && !valideSGL && (
            <span className="leoni-badge-sl">Mode création (SL)</span>
          )}
          {userRole === "SL" && currentEntretienId && !valideSL && (
            <span className="leoni-badge-sl">Mode modification & validation (SL)</span>
          )}
          {userRole === "SL" && valideSL && !valideQM && !valideSGL && (
            <span className="leoni-badge-sl">Soumis — en attente validation QM</span>
          )}
          {userRole === "SL" && valideQM && !valideSGL && (
            <span className="leoni-badge-sl">Validé QM — en attente validation SGL</span>
          )}
          {userRole === "QM_SEGMENT" && currentEntretienId && valideSL && !valideQM && !valideSGL && (
            <span className="leoni-badge-qm">1ère validation (QM-Segment)</span>
          )}
          {userRole === "SGL" && currentEntretienId && valideSL && valideQM && !valideSGL && (
            <span className="leoni-badge-sgl">2ème validation (SGL)</span>
          )}
          {(userRole === "QM_SEGMENT" && (!currentEntretienId || !valideSL)) && (
            <span className="leoni-badge-consult">Mode consultation</span>
          )}
          {valideSGL && (
            <span className="leoni-badge-success">Entretien validé</span>
          )}
        </div>

        <div className="leoni-header-actions" />
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
            <div className="leoni-card-header">Résumé — Entretien d'accord (N2)</div>
            <div className="leoni-card-body">
              {resumeN2 ? (
                <div className="leoni-form-stack">
                  <div className="leoni-form-group">
                    <label>Type faute :</label>
                    <p className="leoni-readonly">{resumeN2.typeFaute || "–"}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Date :</label>
                    <p className="leoni-readonly">{formatDate(resumeN2.date)}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Cause de faute :</label>
                    <p className="leoni-readonly">{resumeN2.causeFaute || "–"}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Mesures proposées :</label>
                    <p className="leoni-readonly">{resumeN2.mesuresProposees || "–"}</p>
                  </div>
                </div>
              ) : (
                <p className="leoni-muted">Aucun entretien d'accord trouvé.</p>
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
                    {isEditable && !valideSGL && (
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
                        disabled={valideSGL || (!isEditable && userRole !== "QM_SEGMENT" && userRole !== "SGL")}
                      />
                      {showDropdown && !valideSGL && (
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

                {/* Date entretien */}
                <div className="leoni-row2">
                  <div className="leoni-form-group" style={{ flex: 1 }}>
                    <label>Date entretien *</label>
                    <input
                      type="date"
                      name="dateEntretien"
                      value={formData.dateEntretien}
                      onChange={handleChange}
                      className="leoni-input"
                      disabled={valideSGL || (!isEditable && userRole !== "QM_SEGMENT" && userRole !== "SGL")}
                      required
                    />
                  </div>
                </div>

                {/* Causes principales */}
                <div className="leoni-form-group">
                  <label>Causes principales</label>
                  <textarea
                    name="causesPrincipales"
                    value={formData.causesPrincipales}
                    onChange={handleChange}
                    className="leoni-textarea"
                    rows="3"
                    disabled={valideSGL || (!isEditable && userRole !== "QM_SEGMENT" && userRole !== "SGL")}
                    placeholder="Décrivez les causes profondes identifiées..."
                  />
                </div>

                {/* Convention */}
                <div className="leoni-form-group">
                  <label>Convention établie</label>
                  <textarea
                    name="convention"
                    value={formData.convention}
                    onChange={handleChange}
                    className="leoni-textarea"
                    rows="3"
                    disabled={valideSGL || (!isEditable && userRole !== "QM_SEGMENT" && userRole !== "SGL")}
                    placeholder="Convention établie lors de l'entretien..."
                  />
                </div>

                {/* Plan d'action */}
                <div className="leoni-form-group">
                  <label>Plan d'action correctif</label>
                  <textarea
                    name="planAction"
                    value={formData.planAction}
                    onChange={handleChange}
                    className="leoni-textarea"
                    rows="3"
                    disabled={valideSGL || (!isEditable && userRole !== "QM_SEGMENT" && userRole !== "SGL")}
                    placeholder="Actions correctives à mettre en place..."
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

                  {/* Messages consultation */}
                  {userRole === "QM_SEGMENT" && currentEntretienId && !valideSL && !valideSGL && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      En attente de la soumission par le SL.
                    </div>
                  )}
                  {userRole === "QM_SEGMENT" && currentEntretienId && valideSL && !valideQM && !valideSGL && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      1ère validation disponible.
                    </div>
                  )}
                  {userRole === "SGL" && currentEntretienId && valideSL && valideQM && !valideSGL && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      2ème validation disponible.
                    </div>
                  )}
                  {userRole === "SGL" && currentEntretienId && (!valideSL || !valideQM) && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      En attente des validations précédentes.
                    </div>
                  )}
                  {!currentEntretienId && !canModify && userRole !== "QM_SEGMENT" && userRole !== "SGL" && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      Aucun entretien de mesure trouvé pour ce collaborateur.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Modal Email (SL uniquement) */}
      <EmailModal
        isOpen={showEmailModal}
        onClose={() => setShowEmailModal(false)}
        onConfirm={handleSLValidation}
        usersList={usersList}
        loadingUsers={loadingUsers}
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