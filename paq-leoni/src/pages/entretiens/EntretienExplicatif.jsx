import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { collaboratorService, entretienService } from "../../services/api";
import { fauteService } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import { usePermissions } from "../../hooks/usePermissions";
import { showErrorAlert, showInfoToast, showSuccessAlert, showSuccessToast } from "../../utils/entretienAlerts";

import "../../styles/paq-dossier.css";
import "../../styles/entretien-explicatif.css";

const buildDefaultForm = () => ({
  typeFaute: "",
  dateFaute: new Date().toISOString().split("T")[0],
  description: "",
  mesuresCorrectives: "",
  commentaire: "",
  defautGrave: false,
});

export default function EntretienExplicatif({ niveau = 1 }) {
  const { matricule } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isSL, isSGL } = usePermissions();

  const [collaborator, setCollaborator] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const [error, setError] = useState("");
  const [currentEntretienId, setCurrentEntretienId] = useState(null);
  const [entretiensList, setEntretiensList] = useState([]);
  const [isValidated, setIsValidated] = useState(false);
  
  const [showDefautModal, setShowDefautModal] = useState(false);
  const [defautTypeInput, setDefautTypeInput] = useState("");
  const [typeOptions, setTypeOptions] = useState([]);

  const [formData, setFormData] = useState(buildDefaultForm());

  const [search, setSearch] = useState("");
  const [filteredFautes, setFilteredFautes] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);

  // Vérifier les permissions
  const canModify = isSL || isSGL;
  const canValidate = isSL || isSGL;

  useEffect(() => {
    const q = search.toLowerCase();
    const filtered = typeOptions.filter(f => f.toLowerCase().includes(q));
    setFilteredFautes(filtered);
  }, [search, typeOptions]);

  useEffect(() => {
    loadFautes();
  }, []);

  useEffect(() => {
    loadCollaborator();
    loadAllEntretiens();
  }, [matricule]);

  const resetForm = () => {
    setFormData(buildDefaultForm());
    setCurrentEntretienId(null);
    setIsValidated(false);
    if (matricule) {
      localStorage.removeItem(`entretien-explicatif-draft-${matricule}`);
    }
  };

  const loadCollaborator = async () => {
    try {
      setLoading(true);
      const res = await collaboratorService.getById(matricule);
      setCollaborator(res.data);
    } catch (err) {
      setError("Impossible de charger les informations du collaborateur");
    } finally {
      setLoading(false);
    }
  };

  const loadFautes = async () => {
    try {
      const res = await fauteService.getAll();
      const fautes = res.data || [];
      setTypeOptions(fautes.map((f) => f.nom));
    } catch (err) {
      console.error("Erreur chargement fautes", err);
    }
  };

  const loadAllEntretiens = async () => {
    try {
      const res = await entretienService.getByMatricule(matricule);
      const list = Array.isArray(res.data) ? res.data : [];
      setEntretiensList(list);
      
      if (list.length > 0) {
        const dernier = list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
        chargerEntretienDansFormulaire(dernier);
        setIsValidated(dernier.valide || false);
      }
    } catch (err) {
      console.warn("Impossible de charger les entretiens:", err);
    }
  };

  const chargerEntretienDansFormulaire = (entretien) => {
    if (!entretien) return;
    
    setCurrentEntretienId(entretien.id);
    setFormData({
      typeFaute: entretien.typeFaute || "",
      dateFaute: entretien.dateFaute || new Date().toISOString().split("T")[0],
      description: entretien.description || "",
      mesuresCorrectives: entretien.mesuresCorrectives || "",
      commentaire: entretien.commentaire || "",
      defautGrave: entretien.defautGrave || false,
    });
    
    if (entretien.typeFaute && !typeOptions.includes(entretien.typeFaute)) {
      setTypeOptions(prev => [...prev, entretien.typeFaute]);
    }
    
    setStatusMessage("Entretien chargé avec succès.");
  };

  const handleChange = (e) => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL et SGL peuvent modifier un entretien.");
      return;
    }
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };



  const addTypeOption = async () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL et SGL peuvent ajouter un type de faute.");
      return;
    }
    const value = defautTypeInput.trim();
    if (!value) return;

    try {
      const res = await fauteService.create({ nom: value });
      const newFaute = res.data;
      setTypeOptions((prev) => [...prev, newFaute.nom]);
      setFormData((prev) => ({ ...prev, typeFaute: newFaute.nom }));
      setDefautTypeInput("");
      setShowDefautModal(false);
      setStatusMessage("Type de défaut ajouté avec succès.");
      showSuccessToast("Faute ajoutée");
    } catch (err) {
      setError("Erreur lors de l'ajout du défaut.");
      showErrorAlert("Ajout impossible", "Erreur lors de l'ajout du défaut.");
    }
  };

  const handleModifier = async () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL et SGL peuvent modifier un entretien.");
      return;
    }
    if (entretiensList.length === 0) {
      setError("Aucun entretien existant à modifier.");
      return;
    }
    const dernier = entretiensList.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
    chargerEntretienDansFormulaire(dernier);
    showInfoToast("Dernier entretien chargé");
  };

  const handleEnregistrer = () => {
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL et SGL peuvent enregistrer un brouillon.");
      return;
    }
    setSavingDraft(true);
    try {
      const payload = { ...formData };
      localStorage.setItem(`entretien-explicatif-draft-${matricule}`, JSON.stringify(payload));
      setStatusMessage("Brouillon enregistré avec succès.");
      showSuccessToast("Brouillon enregistré");
    } catch (err) {
      setError("Impossible d'enregistrer le brouillon.");
      showErrorAlert("Brouillon non enregistré", "Impossible d'enregistrer le brouillon.");
    } finally {
      setSavingDraft(false);
    }
  };

  const handleCreateAndValidate = async () => {
    setSaving(true);
    setError("");
    setStatusMessage("");

    try {
      const entretienData = {
        notes: formData.commentaire || "",
        date: formData.dateFaute,
        typeFaute: formData.typeFaute,
        description: formData.description,
        mesuresCorrectives: formData.mesuresCorrectives,
        defautGrave: formData.defautGrave,
      };

      const response = await entretienService.create(matricule, entretienData);
      
      if (response && response.data && response.data.id) {
        await entretienService.validate(response.data.id);
        setStatusMessage("Entretien créé et validé avec succès.");
        await showSuccessAlert("Entretien créé et validé", "L'entretien explicatif a été créé et validé avec succès.");
      } else {
        setStatusMessage("Entretien créé avec succès.");
        await showSuccessAlert("Entretien créé", "L'entretien explicatif a été créé avec succès.");
      }

      localStorage.removeItem(`entretien-explicatif-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      setError("Erreur : " + (err.response?.data?.message || err.message));
      console.error(err);
      showErrorAlert("Enregistrement impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateOnly = async () => {
    if (!currentEntretienId) {
      setError("Aucun entretien à modifier.");
      return;
    }

    setSaving(true);
    setError("");
    setStatusMessage("");

    try {
      const entretienData = {
        notes: formData.commentaire || "",
        date: formData.dateFaute,
        typeFaute: formData.typeFaute,
        description: formData.description,
        mesuresCorrectives: formData.mesuresCorrectives,
        defautGrave: formData.defautGrave,
      };

      await entretienService.update(matricule, currentEntretienId, entretienData);
      
      setStatusMessage("Entretien modifié avec succès.");
      await showSuccessAlert("Entretien modifié", "L'entretien explicatif a été modifié avec succès.");

      localStorage.removeItem(`entretien-explicatif-draft-${matricule}`);
      await loadAllEntretiens();
    } catch (err) {
      setError("Erreur : " + (err.response?.data?.message || err.message));
      console.error(err);
      showErrorAlert("Modification impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleValidateOnly = async () => {
    if (!currentEntretienId) {
      setError("Aucun entretien à valider.");
      return;
    }

    setSaving(true);
    setError("");
    setStatusMessage("");

    try {
      await entretienService.validate(currentEntretienId);
      setIsValidated(true);
      setStatusMessage("Entretien validé avec succès.");
      await showSuccessAlert("Entretien validé", "L'entretien explicatif a été validé avec succès.");
      
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      setError("Erreur : " + (err.response?.data?.message || err.message));
      console.error(err);
      showErrorAlert("Validation impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!canModify) {
      showErrorAlert("Permission refusée", "Seuls les SL et SGL peuvent créer ou modifier un entretien.");
      return;
    }

    if (!formData.typeFaute) {
      showErrorAlert("Champ requis", "Veuillez sélectionner un type de faute.");
      return;
    }

    if (!formData.dateFaute) {
      showErrorAlert("Champ requis", "Veuillez sélectionner une date.");
      return;
    }

    if (currentEntretienId) {
      await handleUpdateOnly();
    } else {
      await handleCreateAndValidate();
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("fr-FR");
  };

  if (loading) {
    return (
      <div className="leoni-loading">
        <div className="leoni-spinner"></div>
        <p>Chargement...</p>
      </div>
    );
  }

  return (
    <div className="leoni-shell">
      <div className="leoni-header">
        <div className="leoni-header-left">
          <button onClick={() => navigate(`/paq-dossier/${matricule}`)} className="leoni-btn-back">
            Retour
          </button>
        </div>

        <div className="leoni-header-title">
          <div className="leoni-logo-bar">
            <div className="leoni-logo-accent"></div>
            <h1>Etape 1: Entretien Explicatif</h1>
          </div>
          {collaborator && (
            <span className="leoni-header-sub">
              {collaborator.name || ""} {collaborator.prenom || ""} — {collaborator.matricule || matricule}
            </span>
          )}
        </div>

        <div className="leoni-header-actions">

          {isSGL && !isSL && (
            <span className="leoni-badge leoni-badge-warning">Mode SGL - Validation requise</span>
          )}
          {isSL && (
            <span className="leoni-badge leoni-badge-success"></span>
          )}
          {!canModify && (
            <span className="leoni-badge leoni-badge-gray">Mode lecture seule</span>
          )}
          {isValidated && (
            <span className="leoni-badge leoni-badge-info">✓ Entretien validé</span>
          )}
        </div>
      </div>

      {statusMessage && <div className="leoni-alert leoni-alert-success">{statusMessage}</div>}
      {error && <div className="leoni-alert leoni-alert-error">{error}</div>}

      <div className="leoni-grid-main">
        {/* Reste du formulaire inchangé... */}
        <div className="leoni-col-left">
          {collaborator && (
            <div className="leoni-card">
              <div className="leoni-card-header">
                Informations Collaborateur
              </div>
              <div className="leoni-card-body">
                <div className="leoni-collab-avatar">
                  {`${collaborator.name?.[0] || ""}${collaborator.prenom?.[0] || ""}`.toUpperCase()}
                </div>
                <div className="leoni-collab-name">
                  {collaborator.name || "-"} {collaborator.prenom || ""}
                </div>
                <div className="leoni-collab-info-grid">
                  <div className="leoni-info-item">
                    <span className="leoni-info-label">Matricule</span>
                    <span className="leoni-info-value leoni-mono">{collaborator.matricule || "-"}</span>
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
        </div>

        <div className="leoni-col-right">
          <div className="leoni-card">
            <div className="leoni-card-header">
              Formulaire d'entretien explicatif
            </div>
            <div className="leoni-card-body">
              <form id="entretien-explicatif-form" onSubmit={handleSubmit} className="leoni-form-stack">
                {/* Le reste du formulaire reste identique */}
                <div className="leoni-form-group">
                  <label>Type de faute </label>
                  <div className="leoni-inline">
                    {canModify && (
                      <button type="button" onClick={() => setShowDefautModal(true)} className="leoni-btn leoni-btn-warning leoni-btn-sm">
                        + Ajouter faute
                      </button>
                    )}
                    <div className="leoni-dropdown-container" style={{ flex: 1 }}>
                      <input
                        type="text"
                        placeholder="Rechercher ou sélectionner une faute"
                        value={formData.typeFaute}
                        onChange={(e) => {
                          setFormData(prev => ({ ...prev, typeFaute: e.target.value }));
                          setSearch(e.target.value);
                          setShowDropdown(true);
                        }}
                        onFocus={() => setShowDropdown(true)}
                        className="leoni-input"
                        disabled={!canModify || isValidated}
                        required
                      />
                      {showDropdown && canModify && !isValidated && (
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

                {/* Autres champs... */}
                <div className="leoni-form-group">
                  <label>Date de l'entretien </label>
                  <input 
                    type="date" 
                    name="dateFaute" 
                    value={formData.dateFaute} 
                    onChange={handleChange} 
                    className="leoni-input" 
                    disabled={!canModify || isValidated}
                    required
                  />
                </div>

                <div className="leoni-form-group">
                  <label>Cause de la faute</label>
                  <textarea 
                    name="description" 
                    value={formData.description} 
                    onChange={handleChange} 
                    className="leoni-textarea" 
                    rows="3" 
                    disabled={!canModify || isValidated}
                  />
                </div>

                <div className="leoni-form-group">
                  <label>Mesures correctives</label>
                  <textarea 
                    name="mesuresCorrectives" 
                    value={formData.mesuresCorrectives} 
                    onChange={handleChange} 
                    className="leoni-textarea" 
                    rows="3" 
                    disabled={!canModify || isValidated}
                  />
                </div>

                <div className="leoni-form-group">
                  <label>Commentaire</label>
                  <textarea 
                    name="commentaire" 
                    value={formData.commentaire} 
                    onChange={handleChange} 
                    className="leoni-textarea" 
                    rows="3" 
                    disabled={!canModify || isValidated}
                  />
                </div>

                {/* Checkbox Défaut Grave */}
                <div className="leoni-form-group">
                  <div
                    style={{
                      background: formData.defautGrave ? "#2d1a1a" : "#1e1e2e",
                      border: `1.5px solid ${formData.defautGrave ? "#ff4444" : "#444"}`,
                      borderRadius: "8px",
                      padding: "12px 16px",
                    }}
                  >
                    <label
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                        cursor: canModify && !isValidated ? "pointer" : "default",
                        color: formData.defautGrave ? "#ff6b6b" : "#ccc",
                        fontWeight: "600",
                        marginBottom: 0,
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={formData.defautGrave}
                        disabled={!canModify || isValidated}
                        onChange={(e) => {
                          if (!canModify || isValidated) return;
                          setFormData((prev) => ({ ...prev, defautGrave: e.target.checked }));
                        }}
                        style={{ width: "16px", height: "16px", accentColor: "#ff4444" }}
                      />
                      ⚠️ Défaut grave – Activation immédiate SGL obligatoire
                    </label>

                    {formData.defautGrave && (
                      <div
                        style={{
                          marginTop: "8px",
                          padding: "8px 12px",
                          background: "#2a2000",
                          border: "1px solid #ffaa00",
                          borderRadius: "6px",
                          color: "#ffcc44",
                          fontSize: "13px",
                        }}
                      >
                        ⚠ Si coché, le SGL sera automatiquement notifié et devra participer dès ce niveau 1.
                      </div>
                    )}
                  </div>
                </div>

                <div className="leoni-form-actions">
                  {canModify && !isValidated && (
                    <>
                      <button 
                        type="button" 
                        onClick={handleEnregistrer} 
                        className="leoni-btn leoni-btn-outline-dark" 
                        disabled={savingDraft}
                      >
                        {savingDraft ? "Enregistrement..." : "Brouillon"}
                      </button>
                      <button 
                        type="submit" 
                        className="leoni-btn leoni-btn-primary" 
                        disabled={saving}
                      >
                        {saving ? "Validation..." : (currentEntretienId ? "Modifier" : "Créer et valider")}
                      </button>
                    </>
                  )}
                  
                  {isSGL && currentEntretienId && !isValidated && (
                    <button
                      type="button"
                      onClick={handleValidateOnly}
                      className="leoni-btn leoni-btn-success"
                      disabled={saving}
                    >
                      {saving ? "Validation..." : "✓ Valider l'entretien"}
                    </button>
                  )}
                  
                  {!canModify && !isSGL && (
                    <div className="leoni-readonly-message">
                      Vous n'avez pas les droits pour modifier cet entretien.
                    </div>
                  )}
                  
                  {isValidated && (
                    <div className="leoni-readonly-message leoni-readonly-success">
                      ✓ Cet entretien a déjà été validé. Aucune modification n'est possible.
                    </div>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      {showDefautModal && (
        <div className="leoni-modal-overlay" onClick={() => setShowDefautModal(false)}>
          <div className="leoni-modal" onClick={(e) => e.stopPropagation()}>
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
                  onChange={(e) => setDefautTypeInput(e.target.value)} 
                  className="leoni-input" 
                  placeholder="Saisir un nouveau type de faute" 
                />
              </div>
            </div>

            <div className="leoni-modal-footer">
              <button type="button" className="leoni-btn leoni-btn-outline" onClick={() => setShowDefautModal(false)}>
                Annuler
              </button>
              <button type="button" className="leoni-btn leoni-btn-warning" onClick={addTypeOption}>
                Ajouter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}