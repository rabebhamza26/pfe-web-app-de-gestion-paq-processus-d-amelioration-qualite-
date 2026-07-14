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

// ─── Modal Email pour sélection multiple (SL) avec séparation HP, SGL, QM_PLANT ───
function EmailModal({ isOpen, onClose, onConfirm, hpEmails, sglEmails, qmPlantEmails, loadingEmails }) {
  const [selectedEmails, setSelectedEmails] = useState([]);
  const [selectedHpEmails, setSelectedHpEmails] = useState([]);
  const [selectedSglEmails, setSelectedSglEmails] = useState([]);
  const [selectedQmPlantEmails, setSelectedQmPlantEmails] = useState([]);
  const [selectAllHp, setSelectAllHp] = useState(false);
  const [selectAllSgl, setSelectAllSgl] = useState(false);
  const [selectAllQmPlant, setSelectAllQmPlant] = useState(false);

  // Mise à jour de la sélection totale
  useEffect(() => {
    const allSelected = [...selectedHpEmails, ...selectedSglEmails, ...selectedQmPlantEmails];
    setSelectedEmails(allSelected);
  }, [selectedHpEmails, selectedSglEmails, selectedQmPlantEmails]);

  // Mise à jour des boutons "Tout sélectionner"
  useEffect(() => {
    if (hpEmails.length > 0) {
      setSelectAllHp(selectedHpEmails.length === hpEmails.length);
    }
  }, [selectedHpEmails, hpEmails]);

  useEffect(() => {
    if (sglEmails.length > 0) {
      setSelectAllSgl(selectedSglEmails.length === sglEmails.length);
    }
  }, [selectedSglEmails, sglEmails]);

  useEffect(() => {
    if (qmPlantEmails.length > 0) {
      setSelectAllQmPlant(selectedQmPlantEmails.length === qmPlantEmails.length);
    }
  }, [selectedQmPlantEmails, qmPlantEmails]);

  // Réinitialisation quand la modal s'ouvre
  useEffect(() => {
    if (isOpen) {
      setSelectedHpEmails([]);
      setSelectedSglEmails([]);
      setSelectedQmPlantEmails([]);
      setSelectedEmails([]);
      setSelectAllHp(false);
      setSelectAllSgl(false);
      setSelectAllQmPlant(false);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const toggleHpEmail = (email) => {
    setSelectedHpEmails(prev => 
      prev.includes(email) ? prev.filter(e => e !== email) : [...prev, email]
    );
  };

  const toggleSglEmail = (email) => {
    setSelectedSglEmails(prev => 
      prev.includes(email) ? prev.filter(e => e !== email) : [...prev, email]
    );
  };

  const toggleQmPlantEmail = (email) => {
    setSelectedQmPlantEmails(prev => 
      prev.includes(email) ? prev.filter(e => e !== email) : [...prev, email]
    );
  };

  const toggleAllHp = () => {
    if (selectAllHp) {
      setSelectedHpEmails([]);
    } else {
      setSelectedHpEmails([...hpEmails]);
    }
  };

  const toggleAllSgl = () => {
    if (selectAllSgl) {
      setSelectedSglEmails([]);
    } else {
      setSelectedSglEmails([...sglEmails]);
    }
  };

  const toggleAllQmPlant = () => {
    if (selectAllQmPlant) {
      setSelectedQmPlantEmails([]);
    } else {
      setSelectedQmPlantEmails([...qmPlantEmails]);
    }
  };

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
          {loadingEmails ? (
            <div style={{ padding: "40px", textAlign: "center", color: "#999" }}>
              <div className="leoni-spinner" style={{ margin: "0 auto 10px" }}></div>
              Chargement des emails...
            </div>
          ) : (
            <>
              {/* Section HP */}
              <div style={{ marginBottom: 20, border: "1px solid #e2e8f0", borderRadius: "8px", overflow: "hidden" }}>
                <div style={{ 
                  padding: "12px", 
                  background: "#f0f9ff", 
                  borderBottom: "1px solid #e2e8f0",
                  display: "flex",
                  alignItems: "center",
                  gap: "12px"
                }}>
                  <input
                    type="checkbox"
                    checked={selectAllHp}
                    onChange={toggleAllHp}
                    style={{ width: "18px", height: "18px", cursor: "pointer" }}
                    disabled={hpEmails.length === 0}
                  />
                  <strong style={{ flex: 1 }}>HP ({hpEmails.length})</strong>
                  <span style={{ fontSize: "12px", color: "#666" }}>
                    {selectedHpEmails.length} sélectionné(s)
                  </span>
                </div>
                <div style={{ maxHeight: "150px", overflow: "auto" }}>
                  {hpEmails.length > 0 ? (
                    hpEmails.map((email, idx) => (
                      <div
                        key={`hp-${idx}`}
                        style={{
                          padding: "12px",
                          borderBottom: "1px solid #e2e8f0",
                          display: "flex",
                          alignItems: "center",
                          gap: "12px",
                          background: selectedHpEmails.includes(email) ? "#e6f7ff" : "white",
                          cursor: "pointer"
                        }}
                        onClick={() => toggleHpEmail(email)}
                      >
                        <input
                          type="checkbox"
                          checked={selectedHpEmails.includes(email)}
                          onChange={() => toggleHpEmail(email)}
                          style={{ width: "18px", height: "18px", cursor: "pointer" }}
                        />
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 500, color: "#1a202c" }}>{email}</div>
                          <div style={{ fontSize: "12px", color: "#3182ce" }}>HP</div>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div style={{ padding: "20px", textAlign: "center", color: "#999" }}>
                      Aucun HP trouvé dans votre périmètre
                    </div>
                  )}
                </div>
              </div>

              {/* Section SGL */}
              <div style={{ marginBottom: 20, border: "1px solid #e2e8f0", borderRadius: "8px", overflow: "hidden" }}>
                <div style={{ 
                  padding: "12px", 
                  background: "#f0fff4", 
                  borderBottom: "1px solid #e2e8f0",
                  display: "flex",
                  alignItems: "center",
                  gap: "12px"
                }}>
                  <input
                    type="checkbox"
                    checked={selectAllSgl}
                    onChange={toggleAllSgl}
                    style={{ width: "18px", height: "18px", cursor: "pointer" }}
                    disabled={sglEmails.length === 0}
                  />
                  <strong style={{ flex: 1 }}>SGL ({sglEmails.length})</strong>
                  <span style={{ fontSize: "12px", color: "#666" }}>
                    {selectedSglEmails.length} sélectionné(s)
                  </span>
                </div>
                <div style={{ maxHeight: "150px", overflow: "auto" }}>
                  {sglEmails.length > 0 ? (
                    sglEmails.map((email, idx) => (
                      <div
                        key={`sgl-${idx}`}
                        style={{
                          padding: "12px",
                          borderBottom: "1px solid #e2e8f0",
                          display: "flex",
                          alignItems: "center",
                          gap: "12px",
                          background: selectedSglEmails.includes(email) ? "#e6fffa" : "white",
                          cursor: "pointer"
                        }}
                        onClick={() => toggleSglEmail(email)}
                      >
                        <input
                          type="checkbox"
                          checked={selectedSglEmails.includes(email)}
                          onChange={() => toggleSglEmail(email)}
                          style={{ width: "18px", height: "18px", cursor: "pointer" }}
                        />
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 500, color: "#1a202c" }}>{email}</div>
                          <div style={{ fontSize: "12px", color: "#38a169" }}>SGL</div>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div style={{ padding: "20px", textAlign: "center", color: "#999" }}>
                      Aucun SGL trouvé dans votre périmètre
                    </div>
                  )}
                </div>
              </div>

              {/* Section QM_PLANT */}
              <div style={{ marginBottom: 20, border: "1px solid #e2e8f0", borderRadius: "8px", overflow: "hidden" }}>
                <div style={{ 
                  padding: "12px", 
                  background: "#fef3c7", 
                  borderBottom: "1px solid #e2e8f0",
                  display: "flex",
                  alignItems: "center",
                  gap: "12px"
                }}>
                  <input
                    type="checkbox"
                    checked={selectAllQmPlant}
                    onChange={toggleAllQmPlant}
                    style={{ width: "18px", height: "18px", cursor: "pointer" }}
                    disabled={qmPlantEmails.length === 0}
                  />
                  <strong style={{ flex: 1 }}>QM_PLANT ({qmPlantEmails.length})</strong>
                  <span style={{ fontSize: "12px", color: "#666" }}>
                    {selectedQmPlantEmails.length} sélectionné(s)
                  </span>
                </div>
                <div style={{ maxHeight: "150px", overflow: "auto" }}>
                  {qmPlantEmails.length > 0 ? (
                    qmPlantEmails.map((email, idx) => (
                      <div
                        key={`qmplant-${idx}`}
                        style={{
                          padding: "12px",
                          borderBottom: "1px solid #e2e8f0",
                          display: "flex",
                          alignItems: "center",
                          gap: "12px",
                          background: selectedQmPlantEmails.includes(email) ? "#fef3c7" : "white",
                          cursor: "pointer"
                        }}
                        onClick={() => toggleQmPlantEmail(email)}
                      >
                        <input
                          type="checkbox"
                          checked={selectedQmPlantEmails.includes(email)}
                          onChange={() => toggleQmPlantEmail(email)}
                          style={{ width: "18px", height: "18px", cursor: "pointer" }}
                        />
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 500, color: "#1a202c" }}>{email}</div>
                          <div style={{ fontSize: "12px", color: "#d97706" }}>QM_PLANT</div>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div style={{ padding: "20px", textAlign: "center", color: "#999" }}>
                      Aucun QM_PLANT trouvé dans votre périmètre
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
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

// ─── Formulaire vide par défaut ──────────────────────────────────────────────
const buildDefaultForm = () => ({
  typeFaute: "",
  dateEntretien: new Date().toISOString().split("T")[0],
  decision: "",
  justification: "",
  causePrincipale: "",
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

  // Email modal - séparer HP, SGL, QM_PLANT
  const [hpEmails, setHpEmails] = useState([]);
  const [sglEmails, setSglEmails] = useState([]);
  const [qmPlantEmails, setQmPlantEmails] = useState([]);
  const [loadingEmails, setLoadingEmails] = useState(false);
  const [showEmailModal, setShowEmailModal] = useState(false);

  // Trois statuts distincts
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

  // ── Charger les emails HP, SGL, QM_PLANT par périmètre ──
 // Dans EntretienDeDecision.jsx - loadEmailsByPerimeter()
const loadEmailsByPerimeter = async () => {
  try {
    setLoadingEmails(true);
    const response = await userService.getHPSGLQMPlantEmailsByPerimeter();
    
    // 🔍 DÉBOGAGE - Afficher la structure complète
    console.log("=== RÉPONSE COMPLÈTE ===");
    console.log("Response:", response);
    console.log("Response.data:", response.data);
    console.log("Type de response.data:", typeof response.data);
    
    // Si response.data est un tableau, afficher son contenu
    if (Array.isArray(response.data)) {
      console.log("C'est un tableau de", response.data.length, "éléments");
      console.log("Contenu:", response.data);
    } 
    // Si c'est un objet
    else if (typeof response.data === 'object') {
      console.log("Propriétés de response.data:", Object.keys(response.data));
      console.log("response.data.hp:", response.data.hp);
      console.log("response.data.sgl:", response.data.sgl);
      console.log("response.data.qmPlant:", response.data.qmPlant);
    }
    
    setHpEmails(response.data?.hp || []);
    setSglEmails(response.data?.sgl || []);
    setQmPlantEmails(response.data?.qmPlant || []);
    
    console.log("📧 HP emails après set:", response.data?.hp);
    console.log("📧 SGL emails après set:", response.data?.sgl);
    console.log("📧 QM_PLANT emails après set:", response.data?.qmPlant);
  } catch (err) {
    console.error("Erreur chargement emails par périmètre:", err);
    setHpEmails([]);
    setSglEmails([]);
    setQmPlantEmails([]);
  } finally {
    setLoadingEmails(false);
  }
};

  useEffect(() => {
    loadData();
    loadFautes();
    loadEmailsByPerimeter();
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
      causePrincipale: entretien.causePrincipale || "",

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
  const handleSLValidation = async (destinatairesEmails) => {
    setShowEmailModal(false);
    setSaving(true);
    try {
      const entretienData = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        decision: formData.decision,
        justification: formData.justification || "",
        causePrincipale: formData.causePrincipale || "",
        destinatairesEmails: destinatairesEmails,
      };

      let entretienId = currentEntretienId;

      if (currentEntretienId) {
        await entretienDecisionService.update(matricule, currentEntretienId, entretienData);
        entretienId = currentEntretienId;
      } else {
        const response = await entretienDecisionService.create(matricule, entretienData);
        if (response?.data?.id) {
          entretienId = response.data.id;
        } else {
          throw new Error("Aucun ID retourné après création");
        }
      }

      await entretienDecisionService.validerParSL(matricule, entretienId, entretienData);

      const nbDestinataires = destinatairesEmails.length;
      setStatusMessage(`Entretien validé. Email envoyé à ${nbDestinataires} destinataire(s).`);
      await showSuccessAlert("Entretien soumis", `L'email de convocation a été envoyé à ${nbDestinataires} destinataire(s).`);
      localStorage.removeItem(`entretien-decision-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error(err);
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
        causePrincipale: formData.causePrincipale || "",
      };

      await entretienDecisionService.valider1(matricule, currentEntretienId, entretienData);

      setValideHPSGL(true);
      setStatusMessage("Entretien de décision validé (1ère validation HP/SGL).");
      await showSuccessAlert("Validation enregistrée", "La validation HP/SGL a été enregistrée.");

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
        causePrincipale: formData.causePrincipale || "",
      };

      await entretienDecisionService.valider2(matricule, currentEntretienId, entretienData);

      setValideQMPlant(true);
      setStatusMessage("Entretien de décision validé (2ème validation QM_PLANT).");
      await showSuccessAlert("Validation enregistrée", "La validation QM_PLANT a été enregistrée.");

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

  // ── Permissions ──
  const canModify = userRole === "SL";
  const isEditable = userRole === "SL";

  const showValiderSL = userRole === "SL" && !valideQMPlant;
  const showValiderHPSGL = (userRole === "HP" || userRole === "SGL") && !!currentEntretienId && valideSL && !valideHPSGL && !valideQMPlant;
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
        </div>

        <div className="leoni-header-actions">

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
                  <label>Type de faute </label>
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
                  <label>Date entretien </label>
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
                 {/* Cause principale */}
                <div className="leoni-form-group">
                  <label>Cause principale</label>
                  <input
                    type="text"
                    name="causePrincipale"
                    value={formData.causePrincipale}
                    onChange={handleChange}
                    className="leoni-input"
                    disabled={valideQMPlant || (!isEditable && userRole !== "HP" && userRole !== "SGL" && userRole !== "QM_PLANT")}
                    placeholder="Indiquez la cause principale"
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

      {/* Modal Email (SL uniquement) avec séparation HP, SGL, QM_PLANT */}
      <EmailModal
        isOpen={showEmailModal}
        onClose={() => setShowEmailModal(false)}
        onConfirm={handleSLValidation}
        hpEmails={hpEmails}
        sglEmails={sglEmails}
        qmPlantEmails={qmPlantEmails}
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