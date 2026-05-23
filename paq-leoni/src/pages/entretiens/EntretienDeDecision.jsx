// EntretienDeDecision.jsx
// Flux : SL valide → email HP, SGL, QM_PLANT + valideSL=true
//        HP/SGL valide (1ère) → valideHPSGL=true
//        QM_PLANT valide (2ème) → valideQMPlant=true + passage niveau PAQ à 4

import React, { useEffect, useState } from "react";
import {
  collaboratorService,
  entretienMesureService, 
  entretienDaccordService,
  entretienService, 
  entretienDecisionService,
  fauteService,
  userService,
} from "../../services/api";
import { useNavigate, useParams } from "react-router-dom";
import "../../styles/entretien-decision.css";
import "../../styles/paq-dossier.css";
import { showErrorAlert, showInfoToast, showSuccessAlert, showSuccessToast } from "../../utils/entretienAlerts";

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
            <p>Sélectionnez les destinataires pour l'entretien de décision</p>
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
                        {user.nomUtilisateur || user.email.split('@')[0]} • Rôle: <strong>{user.role || "Utilisateur"}</strong>
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
  decision: "",
  justification: "",
});

// ─── Composant principal ──────────────────────────────────────────────────────
export default function EntretienDeDecision() {
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
  const [resumeN2, setResumeN2] = useState(null);
  const [resumeN3, setResumeN3] = useState(null);
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

  // Trois statuts distincts (comme entretien de mesure)
  const [valideSL, setValideSL] = useState(false);
  const [valideHPSGL, setValideHPSGL] = useState(false);
  const [valideQMPlant, setValideQMPlant] = useState(false);

  const [formData, setFormData] = useState(buildDefaultForm());

  // ── Rôle ──
  useEffect(() => {
    const userStr = sessionStorage.getItem("user");
    if (userStr) {
      const user = JSON.parse(userStr);
      setUserRole(user.role);
    }
  }, []);

  const loadUsers = async () => {
    try {
      setLoadingUsers(true);
      const response = await userService.getAllEmails();
      console.log("Emails récupérés:", response.data);
      
      if (response.data && Array.isArray(response.data) && response.data.length > 0) {
        const emailList = response.data.filter(email => email && email.includes('@')).map(email => ({
          email: email,
          nomUtilisateur: email.split('@')[0] || email,
          role: "UTILISATEUR"
        }));
        setUsersList(emailList);
      } else {
        setUsersList([]);
      }
    } catch (err) {
      console.error("Erreur chargement emails:", err);
      setUsersList([]);
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
    setValideHPSGL(false);
    setValideQMPlant(false);
    if (matricule) localStorage.removeItem(`entretien-decision-draft-${matricule}`);
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

      try {
        const res = await entretienMesureService.getByMatricule(matricule);
        const liste = Array.isArray(res.data) ? res.data : [];
        if (liste.length > 0) {
          const dernier = liste.sort((a, b) => new Date(b.dateCreation) - new Date(a.dateCreation))[0];
          setResumeN3({
            typeFaute: dernier.typeFaute || "–",
            date: dernier.dateEntretien,
            planAction: dernier.planAction || "–",
          });
        } else {
          setResumeN3(null);
        }
      } catch {
        setResumeN3(null);
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
      const res = await entretienDecisionService.getByMatricule(matricule);
      const list = Array.isArray(res.data) ? res.data : [];
      setEntretiensList(list);
      if (list.length > 0) {
        const dernier = list.sort((a, b) => new Date(b.dateCreation) - new Date(a.dateCreation))[0];
        chargerEntretienDansFormulaire(dernier);
        setValideSL(dernier.valideSL || false);
        setValideHPSGL(dernier.valideHPSGL || false);
        setValideQMPlant(dernier.valideQMPlant || false);
        console.log("Statuts - valideSL:", dernier.valideSL, "valideHPSGL:", dernier.valideHPSGL, "valideQMPlant:", dernier.valideQMPlant);
      } else {
        resetForm();
      }
    } catch (err) {
      console.warn("Impossible de charger les entretiens de décision:", err);
    }
  };

  const chargerEntretienDansFormulaire = (entretien) => {
    if (!entretien) return;
    setCurrentEntretienId(entretien.id);
    setFormData({
      typeFaute: entretien.typeFaute || "",
      dateEntretien: entretien.dateEntretien || new Date().toISOString().split("T")[0],
      decision: entretien.decision || "",
      justification: entretien.justification || "",
    });
    if (entretien.typeFaute && !typeOptions.includes(entretien.typeFaute)) {
      setTypeOptions(prev => [...prev, entretien.typeFaute]);
    }
  };

  // ── Brouillon ──
  const loadDraft = () => {
    try {
      const draft = localStorage.getItem(`entretien-decision-draft-${matricule}`);
      if (!draft) return;
      const parsed = JSON.parse(draft);
      setFormData(prev => ({ ...prev, ...parsed }));
      if (parsed.id) setCurrentEntretienId(parsed.id);
    } catch (err) {
      console.warn("Brouillon non chargeable:", err);
    }
  };

  const handleChange = e => {
    if (!canModify && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT") {
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
      setError("Aucun entretien de décision existant à modifier.");
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
      localStorage.setItem(`entretien-decision-draft-${matricule}`, JSON.stringify(payload));
      setStatusMessage("Brouillon enregistré avec succès.");
      showSuccessToast("Brouillon enregistré");
    } catch {
      showErrorAlert("Brouillon non enregistré", "Impossible d'enregistrer le brouillon.");
    } finally {
      setSavingDraft(false);
    }
  };

  // ── Validation SL : création/modification + email aux destinataires ──
  // ── Validation SL : création/modification + email aux destinataires ──
const handleSLValidation = async (destinatairesEmails) => {
  console.log("=== handleSLValidation START ===");
  console.log("Destinataires emails:", destinatairesEmails);
  console.log("Current entretien ID:", currentEntretienId);
  
  setShowEmailModal(false);
  setSaving(true);
  try {
    const entretienData = {
      typeFaute: formData.typeFaute,
      dateEntretien: formData.dateEntretien,
      decision: formData.decision,
      justification: formData.justification || "",
      destinatairesEmails: destinatairesEmails,  // ✅ Inclure dans les données
      messageOptionnel: "Convocation à l'entretien de décision"  // Optionnel
    };

    console.log("Données à envoyer:", entretienData);

    let entretienId = currentEntretienId;

    if (currentEntretienId) {
      // ✅ Mise à jour
      console.log("Mode UPDATE pour ID:", currentEntretienId);
      await entretienDecisionService.update(matricule, currentEntretienId, entretienData);
      console.log("Update effectué");
      entretienId = currentEntretienId;
    } else {
      // ✅ Création
      console.log("Mode CREATE");
      const response = await entretienDecisionService.create(matricule, entretienData);
      console.log("Création réponse:", response);
      if (response?.data?.id) {
        entretienId = response.data.id;
        console.log("Entretien créé avec ID:", entretienId);
      } else {
        throw new Error("Aucun ID retourné après création");
      }
    }

    // ✅ Appel de la validation SL avec TOUTES les données (y compris destinatairesEmails)
    console.log("Appel validerParSL avec ID:", entretienId);
    await entretienDecisionService.validerParSL(matricule, entretienId, entretienData);
    console.log("Validation SL effectuée");

    const nbDestinataires = destinatairesEmails.length;
    setStatusMessage(`Entretien validé. Email envoyé à ${nbDestinataires} destinataire(s).`);
    await showSuccessAlert("Entretien soumis", `L'email de convocation a été envoyé à ${nbDestinataires} destinataire(s).`);
    localStorage.removeItem(`entretien-decision-draft-${matricule}`);
    await loadAllEntretiens();
    setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
  } catch (err) {
    console.error("❌ Erreur dans handleSLValidation:", err);
    console.error("Détails erreur:", err.response?.data);
    showErrorAlert("Enregistrement impossible", err.response?.data?.message || err.message);
  } finally {
    setSaving(false);
  }
};

  // ── Validation HP/SGL (1ère validation) ──
  const handleValidationHPSGL = async () => {
    setSaving(true);
    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        decision: formData.decision,
        justification: formData.justification || "",
      };

      await entretienDecisionService.valider1(matricule, currentEntretienId, entretienData);

      setValideHPSGL(true);
      setStatusMessage("Entretien de décision validé (1ère validation HP/SGL).");
      await showSuccessAlert(
        "Validation enregistrée",
        "La validation HP/SGL a été enregistrée."
      );

      localStorage.removeItem(`entretien-decision-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error(err);
      showErrorAlert("Validation impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  // ── Validation QM_PLANT (2ème validation) ──
  const handleValidationQMPlant = async () => {
    setSaving(true);
    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        decision: formData.decision,
        justification: formData.justification || "",
      };

      await entretienDecisionService.valider2(matricule, currentEntretienId, entretienData);

      setValideQMPlant(true);
      setStatusMessage("Entretien de décision validé (2ème validation QM_PLANT).");
      await showSuccessAlert(
        "Validation enregistrée",
        "La validation QM_PLANT a été enregistrée. Passage au niveau supérieur."
      );

      localStorage.removeItem(`entretien-decision-draft-${matricule}`);
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
      return;
    }
    if (!formData.decision) {
      setError("La décision est obligatoire");
      return;
    }

    if (userRole === "SL") {
      setShowEmailModal(true);
    } else if (userRole === "HP" || userRole === "SGL") {
      handleValidationHPSGL();
    } else if (userRole === "QM_PLANT") {
      handleValidationQMPlant();
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

  // ── Permissions ──────────────────────────────────────────────────────────────
  const canModify = userRole === "SL";
  const isEditable = userRole === "SL";

  // SL peut valider si QM_PLANT n'a pas encore validé
  const showValiderSL = userRole === "SL" && !valideQMPlant;

  // HP/SGL peut valider si SL a soumis ET que HP/SGL n'a pas encore validé
  const showValiderHPSGL = (userRole === "HP" || userRole === "SGL") && !!currentEntretienId && valideSL && !valideHPSGL && !valideQMPlant;

  // QM_PLANT peut valider si SL a soumis ET HP/SGL a validé ET QM_PLANT pas encore validé
  const showValiderQMPlant = userRole === "QM_PLANT" && !!currentEntretienId && valideSL && valideHPSGL && !valideQMPlant;

  const showModifier = canModify && !!currentEntretienId && !valideQMPlant;
  const showBrouillon = canModify && !valideQMPlant;
  const showValider = showValiderSL || showValiderHPSGL || showValiderQMPlant;

  const getValiderLabel = () => {
    if (saving) return "Enregistrement...";
    if (userRole === "QM_PLANT") return "Valider (2ème validation)";
    if (userRole === "HP" || userRole === "SGL") return "Valider (1ère validation)";
    if (currentEntretienId) return "Modifier et valider";
    return "Créer et valider";
  };

  const formatDate = dateStr => {
    if (!dateStr) return "–";
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("fr-FR");
  };

  // ── Rendu ─────────────────────────────────────────────────────────────────────
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
            <h1>Etape 4 : Entretien de Décision</h1>
          </div>
          {collaborator && (
            <span className="leoni-header-sub">
              {collaborator.name || ""} {collaborator.prenom || ""} — {collaborator.matricule || matricule}
            </span>
          )}

          {/* Badges de statut */}
          {userRole === "SL" && !currentEntretienId && !valideQMPlant && (
            <span className="leoni-badge-sl">Mode création (SL)</span>
          )}
          {userRole === "SL" && currentEntretienId && !valideSL && (
            <span className="leoni-badge-sl">Mode modification & validation (SL)</span>
          )}
          {userRole === "SL" && valideSL && !valideHPSGL && !valideQMPlant && (
            <span className="leoni-badge-sl">Soumis — en attente validation HP/SGL</span>
          )}
          {userRole === "SL" && valideHPSGL && !valideQMPlant && (
            <span className="leoni-badge-sl">Validé HP/SGL — en attente validation QM_PLANT</span>
          )}
          {userRole === "HP" && currentEntretienId && valideSL && !valideHPSGL && !valideQMPlant && (
            <span className="leoni-badge-hp">1ère validation (HP)</span>
          )}
          {userRole === "SGL" && currentEntretienId && valideSL && !valideHPSGL && !valideQMPlant && (
            <span className="leoni-badge-sgl">1ère validation (SGL)</span>
          )}
          {userRole === "QM_PLANT" && currentEntretienId && valideSL && valideHPSGL && !valideQMPlant && (
            <span className="leoni-badge-qm-plant">2ème validation (QM_PLANT)</span>
          )}
          {(userRole === "HP" || userRole === "SGL" || userRole === "QM_PLANT") && (!currentEntretienId || !valideSL) && (
            <span className="leoni-badge-consult">Mode consultation</span>
          )}
          {valideQMPlant && (
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
            <div className="leoni-card-header">Résumé — Entretien de mesure (N3)</div>
            <div className="leoni-card-body">
              {resumeN3 ? (
                <div className="leoni-form-stack">
                  <div className="leoni-form-group">
                    <label>Type faute :</label>
                    <p className="leoni-readonly">{resumeN3.typeFaute || "–"}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Date :</label>
                    <p className="leoni-readonly">{formatDate(resumeN3.date)}</p>
                  </div>
                  <div className="leoni-form-group">
                    <label>Plan d'action :</label>
                    <p className="leoni-readonly">{resumeN3.planAction || "–"}</p>
                  </div>
                </div>
              ) : (
                <p className="leoni-muted">Aucun entretien de mesure trouvé.</p>
              )}
            </div>
          </div>

          {/* Statut des validations */}
          {currentEntretienId && (
            <div className="leoni-card">
              <div className="leoni-card-header">Statut des validations</div>
              <div className="leoni-card-body">
                <div className="leoni-info-item">
                  <span className="leoni-info-label">SL</span>
                  <span className={`leoni-info-value ${valideSL ? 'text-success' : 'text-warning'}`}>
                    {valideSL ? "✅ Soumis" : "⏳ En attente"}
                  </span>
                </div>
                <div className="leoni-info-item">
                  <span className="leoni-info-label">HP/SGL (1ère)</span>
                  <span className={`leoni-info-value ${valideHPSGL ? 'text-success' : 'text-warning'}`}>
                    {valideHPSGL ? "✅ Validé" : valideSL ? "⏳ En attente" : "🔒 Bloqué"}
                  </span>
                </div>
                <div className="leoni-info-item">
                  <span className="leoni-info-label">QM_PLANT (2ème)</span>
                  <span className={`leoni-info-value ${valideQMPlant ? 'text-success' : 'text-warning'}`}>
                    {valideQMPlant ? "✅ Validé" : valideHPSGL ? "⏳ En attente" : "🔒 Bloqué"}
                  </span>
                </div>
              </div>
            </div>
          )}
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
                    {isEditable && !valideQMPlant && (
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
                        disabled={valideQMPlant || (!isEditable && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT")}
                      />
                      {showDropdown && !valideQMPlant && (
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
                    disabled={valideQMPlant || (!isEditable && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT")}
                    required
                  />
                </div>

                {/* Décision */}
                <div className="leoni-form-group">
                  <label>Décision </label>
                  <select
                    name="decision"
                    value={formData.decision}
                    onChange={handleChange}
                    className="leoni-input"
                    disabled={valideQMPlant || (!isEditable && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT")}
                    required
                  >
                    <option value="">— Sélectionnez une décision —</option>
                    <option value="Avertissement">Avertissement</option>
                    <option value="Formation">Formation</option>
                    <option value="Mutation">Mutation</option>
                    <option value="Suspension">Suspension</option>
                    <option value="Licenciement">Licenciement</option>
                  </select>
                </div>

                {/* Justification */}
                <div className="leoni-form-group">
                  <label>Justification</label>
                  <textarea
                    name="justification"
                    value={formData.justification}
                    onChange={handleChange}
                    className="leoni-textarea"
                    rows="3"
                    disabled={valideQMPlant || (!isEditable && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT")}
                    placeholder="Motivez la décision prise lors de cet entretien..."
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
                  {(userRole === "HP" || userRole === "SGL") && currentEntretienId && !valideSL && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      En attente de la soumission par le SL.
                    </div>
                  )}
                  {(userRole === "HP" || userRole === "SGL") && currentEntretienId && valideSL && !valideHPSGL && !valideQMPlant && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      1ère validation disponible.
                    </div>
                  )}
                  {userRole === "QM_PLANT" && currentEntretienId && valideSL && valideHPSGL && !valideQMPlant && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      2ème validation disponible.
                    </div>
                  )}
                  {userRole === "QM_PLANT" && currentEntretienId && (!valideSL || !valideHPSGL) && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      En attente des validations précédentes.
                    </div>
                  )}
                  {!currentEntretienId && !canModify && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT" && (
                    <div className="leoni-muted" style={{ padding: "8px 0" }}>
                      Aucun entretien de décision trouvé pour ce collaborateur.
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