import React, { useState, useEffect } from "react";
import {
  collaboratorService,
  fauteService,
  entretienFinalService,
  entretienDecisionService,
} from "../../services/api";
import { useNavigate, useParams } from "react-router-dom";
import { showConfirmAlert, showErrorAlert, showInfoToast, showSuccessAlert, showSuccessToast } from "../../utils/entretienAlerts";

import "../../styles/entretien-final.css";
import "../../styles/paq-dossier.css";

const DECISIONS = ["Licenciement", "Avertissement", "Formation", "Mutation", "Suspension"];

const buildDefaultForm = () => ({
  typeFaute: "",
  dateEntretien: new Date().toISOString().split("T")[0],
  decision: "",
  commentaireRH: "",
  causePrincipale: "",
  ksk: "", // Champ KSK optionnel
});

export default function EntretienFinal({ niveau = 5 }) {
  const { matricule } = useParams();
  const navigate = useNavigate();

  const [typeOptions,       setTypeOptions]       = useState([]);
  const [showDefautModal,   setShowDefautModal]   = useState(false);
  const [defautTypeInput,   setDefautTypeInput]   = useState("");
  const [showDropdown,      setShowDropdown]      = useState(false);

  const [collaborator,      setCollaborator]      = useState(null);
  const [resumeN4,          setResumeN4]          = useState(null);
  const [entretiensList,    setEntretiensList]    = useState([]);
  const [loading,           setLoading]           = useState(true);
  const [saving,            setSaving]            = useState(false);
  const [savingDraft,       setSavingDraft]       = useState(false);
  const [loadingDraft,      setLoadingDraft]      = useState(false);
  const [error,             setError]             = useState("");
  const [statusMessage,     setStatusMessage]     = useState("");
  const [currentId,         setCurrentId]         = useState(null);

  const [formData, setFormData] = useState(buildDefaultForm());

  useEffect(() => {
    loadCollaborator();
    loadDraft();
    loadResumeN4();
    loadFautes();
    loadAllEntretiens();
  }, [matricule]);

  const resetForm = () => {
    setFormData(buildDefaultForm());
    setCurrentId(null);
    if (matricule) {
      localStorage.removeItem(`entretien-final-draft-${matricule}`);
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
      setTypeOptions(res.data.map(f => f.nom));
    } catch { setTypeOptions([]); }
  };

  const loadResumeN4 = async () => {
    try {
      const res = await entretienDecisionService.getByMatricule(matricule);
      const list = Array.isArray(res.data) ? res.data : [];
      setResumeN4(list.at(-1) || null);
    } catch { setResumeN4(null); }
  };

  const loadAllEntretiens = async () => {
    try {
      const res = await entretienFinalService.getByMatricule(matricule);
      const list = Array.isArray(res.data) ? res.data : [];
      setEntretiensList(list);
      
      if (list.length > 0) {
        const dernier = list.sort((a, b) => new Date(b.dateEntretien) - new Date(a.dateEntretien))[0];
        chargerEntretienDansFormulaire(dernier);
      }
    } catch (err) {
      console.warn("Impossible de charger les entretiens finaux:", err);
    }
  };

  const chargerEntretienDansFormulaire = (entretien) => {
    if (!entretien) return;
    
    setCurrentId(entretien.id);
    setFormData({
      typeFaute: entretien.typeFaute || "",
      dateEntretien: entretien.dateEntretien || new Date().toISOString().split("T")[0],
      decision: entretien.decision || "",
      commentaireRH: entretien.commentaireRH || "",
      causePrincipale: entretien.causePrincipale || "",
      ksk: entretien.ksk ?? entretien.casca ?? "", // Charger KSK (fallback casca)
    });
    
    if (entretien.typeFaute && !typeOptions.includes(entretien.typeFaute)) {
      setTypeOptions(prev => [...prev, entretien.typeFaute]);
    }
    
    setStatusMessage("Entretien chargé avec succès.");
    setTimeout(() => setStatusMessage(""), 3000);
  };

  const loadDraft = () => {
    try {
      const draft = localStorage.getItem(`entretien-final-draft-${matricule}`);
      if (!draft) return;
      const parsed = JSON.parse(draft);
      setFormData(prev => ({ ...prev, ...parsed }));
      if (parsed.id) setCurrentId(parsed.id);
    } catch (err) { console.warn("Brouillon non chargeable:", err); }
  };

  const handleChange = e => setFormData({ ...formData, [e.target.name]: e.target.value });

  // Gestion spécifique pour le champ KSK (numérique)
  const handleKskChange = (e) => {
    const value = e.target.value;
    if (value === "" || value === "-" || /^-?\d*\.?\d*$/.test(value)) {
      setFormData({ ...formData, ksk: value });
    }
  };

  const addTypeOption = async () => {
    const value = defautTypeInput.trim();
    if (!value) return;
    try {
      const res = await fauteService.create({ nom: value });
      const nom = res.data.nom;
      setTypeOptions(prev => prev.includes(nom) ? prev : [...prev, nom]);
      setFormData(prev => ({ ...prev, typeFaute: nom }));
      setDefautTypeInput("");
      setShowDefautModal(false);
      setStatusMessage("Type de faute ajouté avec succès.");
      showSuccessToast("Faute ajoutée");
    } catch {
      setError("Erreur ajout faute");
      showErrorAlert("Ajout impossible", "Erreur lors de l'ajout du type de faute.");
    }
  };

  const handleEnregistrer = () => {
    setSavingDraft(true);
    try {
      const payload = { ...formData, id: currentId };
      localStorage.setItem(`entretien-final-draft-${matricule}`, JSON.stringify(payload));
      setStatusMessage("Brouillon enregistré.");
      showSuccessToast("Brouillon enregistré");
      setTimeout(() => setStatusMessage(""), 3000);
    } catch {
      setError("Impossible d'enregistrer le brouillon.");
      showErrorAlert("Brouillon non enregistré", "Impossible d'enregistrer le brouillon.");
    }
    finally { setSavingDraft(false); }
  };

  const handleAjouter = () => {
    resetForm();
    setStatusMessage("Formulaire réinitialisé.");
    showInfoToast("Formulaire réinitialisé");
    setTimeout(() => setStatusMessage(""), 2000);
  };

  const handleModifier = async () => {
    if (entretiensList.length === 0) {
      setError("Aucun entretien final existant à modifier.");
      return;
    }
    const dernier = entretiensList.sort((a, b) => new Date(b.dateEntretien) - new Date(a.dateEntretien))[0];
    chargerEntretienDansFormulaire(dernier);
    showInfoToast("Dernier entretien chargé");
  };

  const handleSupprimer = async () => {
    if (!currentId) {
      setError("Aucun entretien chargé pour suppression.");
      return;
    }
    
    const result = await showConfirmAlert({
      title: "Supprimer l'entretien final ?",
      text: "Cette action est définitive.",
      confirmButtonText: "Oui, supprimer",
    });
    if (!result.isConfirmed) return;
    
    setSaving(true);
    try {
      await entretienFinalService.delete(currentId);
      resetForm();
      await loadAllEntretiens();
      setStatusMessage("Entretien final supprimé avec succès.");
      await showSuccessAlert("Entretien supprimé", "L'entretien final a bien été supprimé.");
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      setError("Erreur lors de la suppression : " + (err.response?.data?.message || err.message));
      showErrorAlert("Suppression impossible", err.response?.data?.message || err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(""); 
    setStatusMessage("");
    setSaving(true);

    if (!formData.typeFaute) {
      setError("Le type de faute est obligatoire !");
      setSaving(false);
      return;
    }
    if (!formData.decision) {
      setError("La décision RH est obligatoire !");
      setSaving(false);
      return;
    }

    try {
      const payload = {
        typeFaute: formData.typeFaute,
        dateEntretien: formData.dateEntretien,
        decision: formData.decision,
        commentaireRH: formData.commentaireRH,
        causePrincipale: formData.causePrincipale || "",
        ksk: formData.ksk ? parseFloat(formData.ksk) : null, // Envoyer KSK
      };

      if (currentId) {
        await entretienFinalService.update(matricule, currentId, payload);
        setStatusMessage("Entretien final modifié avec succès.");
        await showSuccessAlert("Entretien modifié", "La modification a été enregistrée avec succès.");
      } else {
        await entretienFinalService.create(matricule, payload);
        setStatusMessage("Entretien final créé avec succès, dossier clôturé ✓");
        await showSuccessAlert("Entretien final créé", "Le dossier a été clôturé avec succès.");
      }

      localStorage.removeItem(`entretien-final-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.message || "Erreur lors de l'enregistrement");
      showErrorAlert("Enregistrement impossible", err.response?.data?.message || err.message || "Erreur lors de l'enregistrement");
    } finally { 
      setSaving(false);
    }
  };

  const fmt = d => { if (!d) return "—"; try { return new Date(d).toLocaleDateString("fr-FR"); } catch { return d; } };

  if (loading) return <div className="ef-loading">Chargement...</div>;

  return (
    <>
      <div className="ef-root">

        <div className="leoni-header">
          <div className="leoni-header-left">
            <button onClick={() => navigate(`/paq-dossier/${matricule}`)} className="leoni-btn-back">
              Retour au dossier
            </button>
          </div>
          <div className="leoni-header-title">
            <div className="leoni-logo-bar">
              <div className="leoni-logo-accent" />
              <h1>Etape 5: Entretien Final</h1>
            </div>
            {collaborator && (
              <span className="leoni-header-sub">
                {collaborator.name} {collaborator.prenom} - {collaborator.matricule}
              </span>
            )}
          </div>
          <div className="leoni-header-actions">
            {/* Champ KSK dans l'en-tête */}
            <div className="leoni-casca-field">
              <label htmlFor="ksk" className="leoni-casca-label">
                KSK
              </label>
              <input
                type="text"
                id="ksk"
                name="ksk"
                value={formData.ksk}
                onChange={handleKskChange}
                className="leoni-input leoni-input-casca"
                placeholder="Optionnel"
                style={{ width: "100px", textAlign: "center" }}
              />
            </div>
          </div>
        </div>

        <div className="ef-page">

          <aside className="ef-sidebar">
            <div className="ef-card">
              <div className="ef-card-hd">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke="currentColor" strokeWidth="2"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" strokeWidth="2"/>
                </svg>
                Informations Collaborateur
              </div>
              <div className="ef-card-bd">
                <div className="ef-avatar">
                  {`${collaborator?.name?.[0]||""}${collaborator?.prenom?.[0]||""}`.toUpperCase() || "?"}
                </div>
                <div className="ef-cname">{collaborator?.name} {collaborator?.prenom}</div>
                <div className="ef-igrid">
                  <div className="ef-icell"><span className="ef-ilbl">Matricule</span><span className="ef-ival">{collaborator?.matricule||"—"}</span></div>
                  <div className="ef-icell"><span className="ef-ilbl">Segment</span><span className="ef-ival">{collaborator?.segment||"—"}</span></div>
                  <div className="ef-icell"><span className="ef-ilbl">Embauche</span><span className="ef-ival">{fmt(collaborator?.hireDate)}</span></div>
                  <div className="ef-icell"><span className="ef-ilbl">Statut</span><span className="ef-ival green">{collaborator?.status||"ACTIF"}</span></div>
                </div>
              </div>
            </div>

            <div className="ef-card">
              <div className="ef-card-hd amber">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                  <path d="M9 12l2 2 4-4M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" strokeWidth="2"/>
                </svg>
                Résumé de l'Entretien de Décision (N4)
              </div>
              <div className="ef-card-bd">
                {resumeN4 ? (
                  <div className="ef-rrow">
                    <div className="ef-rline"><span className="ef-rlbl">Type faute</span><span className="ef-rval">{resumeN4.typeFaute||"—"}</span></div>
                    <div className="ef-rline"><span className="ef-rlbl">Date</span><span className="ef-rval">{fmt(resumeN4.dateEntretien||resumeN4.date)}</span></div>
                    <div className="ef-rline"><span className="ef-rlbl">Décision</span><span className="ef-rval">{resumeN4.decision||"—"}</span></div>
                    {resumeN4.justification && (
                      <div className="ef-rline"><span className="ef-rlbl">Justif.</span><span className="ef-rval" style={{fontSize:11}}>{resumeN4.justification}</span></div>
                    )}
                  </div>
                ) : (
                  <div className="ef-rempty">Aucun entretien de décision trouvé</div>
                )}
              </div>
            </div>
          </aside>

          <div className="ef-main">

            {statusMessage && (
              <div className="ef-alert ef-alert-ok">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                  <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                {statusMessage}
              </div>
            )}
            {error && (
              <div className="ef-alert ef-alert-err">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
                  <path d="M12 8v4M12 16h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
                {error}
              </div>
            )}

            <div className="ef-form-card">
              <div className="leoni-card-header">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                  <path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
                Formulaire
              </div>

              <div className="ef-form-bd">
                <form onSubmit={handleSubmit}>

                  <div className="ef-fg">
                    <label className="ef-lbl">Type de faute <span className="req"></span></label>
                    <div className="ef-faute-row">
                      <div className="ef-dw">
                        <input type="text" className="ef-inp"
                          placeholder="Rechercher ou sélectionner une faute..."
                          value={formData.typeFaute}
                          onChange={e => { setFormData(p=>({...p,typeFaute:e.target.value})); setShowDropdown(true); }}
                          onFocus={() => setShowDropdown(true)}
                        />
                        {showDropdown && typeOptions.length > 0 && (
                          <div className="ef-dlist">
                            {typeOptions
                              .filter(o => o.toLowerCase().includes(formData.typeFaute.toLowerCase()))
                              .map((o, i) => (
                                <div key={i} className="ef-ditem"
                                  onMouseDown={() => { setFormData(p=>({...p,typeFaute:o})); setShowDropdown(false); }}>
                                  {o}
                                </div>
                              ))}
                          </div>
                        )}
                      </div>
                      <button type="button" className="ef-btn-add" onClick={() => setShowDefautModal(true)}>
                        + Ajouter Faute
                      </button>
                    </div>
                  </div>

                    <div className="ef-fg">
                      <label className="ef-lbl">Date entretien </label>
                      <input type="date" name="dateEntretien" className="ef-inp"
                        value={formData.dateEntretien} onChange={handleChange}/>
                    </div>
                     <div className="ef-fg">
                    <label className="ef-lbl">Cause principale</label>
                    <input type="text" name="causePrincipale" className="ef-inp"
                      value={formData.causePrincipale} onChange={handleChange}
                      placeholder="Indiquez la cause principale" />
                  </div>

                    <div className="ef-fg">
                      <label className="ef-lbl">Décision RH <span className="req"></span></label>
                      <select name="decision" className="ef-sel"
                        value={formData.decision} onChange={handleChange}>
                        <option value="">— Choisir —</option>
                        {DECISIONS.map(d => <option key={d} value={d}>{d}</option>)}
                      </select>
                    </div>

                 

                  <div className="ef-fg">
                    <label className="ef-lbl">Commentaire RH</label>
                    <textarea name="commentaireRH" className="ef-ta" rows={3}
                      value={formData.commentaireRH} onChange={handleChange}
                      placeholder="Motivez la décision finale prise par les RH..."/>
                  </div>

                  <div className="ef-actions">
                    <button type="button" className="ef-btn ef-btn-draft"
                      onClick={handleEnregistrer} disabled={savingDraft}>
                      {savingDraft ? "Enregistrement..." : "Enregistrer Brouillon"}
                    </button>
                    <button type="submit" className="ef-btn ef-btn-valider" disabled={saving}>
                      {saving ? "..." : "Valider"}
                    </button>

                    <button type="button" className="ef-btn ef-btn-modifier"
                      onClick={handleModifier} disabled={loadingDraft}>
                      {loadingDraft ? "..." : "Modifier"}
                    </button>
                    
                  </div>

                </form>
              </div>
            </div>
          </div>
        </div>
      </div>

      {showDefautModal && (
        <div className="ef-moverlay" onClick={() => setShowDefautModal(false)}>
          <div className="ef-modal" onClick={e => e.stopPropagation()}>
            <h3>Ajouter un type de faute</h3>
            <label className="ef-lbl">Nom du type de faute</label>
            <input className="ef-inp" style={{marginTop:6}} value={defautTypeInput}
              onChange={e => setDefautTypeInput(e.target.value)}
              placeholder="Saisir un nouveau type de faute"
              onKeyDown={e => e.key === "Enter" && addTypeOption()}
              autoFocus/>
            <div className="ef-modal-acts">
              <button className="ef-mbtn-cancel" onClick={() => setShowDefautModal(false)}>Annuler</button>
              <button className="ef-mbtn-ok" onClick={addTypeOption} disabled={!defautTypeInput.trim()}>Ajouter</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}