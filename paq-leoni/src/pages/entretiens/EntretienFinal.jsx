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
});

export default function EntretienFinal({ niveau = 5 }) {
  const { matricule } = useParams();
  const navigate = useNavigate();
  const t = (key) => key;
  const lang = "fr";

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
      setError(t("unable_to_load_collaborator"));
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

    });
    
    if (entretien.typeFaute && !typeOptions.includes(entretien.typeFaute)) {
      setTypeOptions(prev => [...prev, entretien.typeFaute]);
    }
    
    setStatusMessage(t("meeting_loaded_success"));
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
      setStatusMessage(t("fault_type_added"));
      showSuccessToast(t("fault_added_toast"));
    } catch {
      setError(t("fault_add_error"));
      showErrorAlert(t("fault_add_error_title"), t("fault_add_error_text"));
    }
  };

  const handleEnregistrer = () => {
    setSavingDraft(true);
    try {
      const payload = { ...formData, id: currentId };
      localStorage.setItem(`entretien-final-draft-${matricule}`, JSON.stringify(payload));
      setStatusMessage(t("draft_saved"));
      showSuccessToast(t("draft_saved_toast"));
      setTimeout(() => setStatusMessage(""), 3000);
    } catch {
      setError(t("draft_save_failed_text"));
      showErrorAlert(t("draft_save_failed_title"), t("draft_save_failed_text"));
    }
    finally { setSavingDraft(false); }
  };

  const handleAjouter = () => {
    resetForm();
    setStatusMessage(t("form_reset_message"));
    showInfoToast(t("form_reset_toast"));
    setTimeout(() => setStatusMessage(""), 2000);
  };

  const handleModifier = async () => {
    if (entretiensList.length === 0) {
      setError(t("no_final_meeting_to_edit"));
      return;
    }
    const dernier = entretiensList.sort((a, b) => new Date(b.dateEntretien) - new Date(a.dateEntretien))[0];
    chargerEntretienDansFormulaire(dernier);
    showInfoToast(t("last_meeting_loaded"));
  };

  const handleSupprimer = async () => {
    if (!currentId) {
      setError(t("no_meeting_for_delete"));
      return;
    }
    
    const result = await showConfirmAlert({
      title: t("confirm_delete_final_meeting_title"),
      text: t("confirm_delete_final_meeting_text"),
      confirmButtonText: t("confirm_delete_yes"),
    });
    if (!result.isConfirmed) return;
    
    setSaving(true);
    try {
      await entretienFinalService.delete(currentId);
      resetForm();
      await loadAllEntretiens();
      setStatusMessage(t("final_meeting_deleted"));
      await showSuccessAlert(t("final_meeting_deleted_title"), t("final_meeting_deleted_text"));
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      setError(t("error_saving_data") + ": " + (err.response?.data?.message || err.message));
      showErrorAlert(t("cannot_save"), err.response?.data?.message || err.message || t("error_saving_data"));
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
      setError(t("fault_type_required"));
      setSaving(false);
      return;
    }
    if (!formData.decision) {
      setError(t("decision_required"));
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
      };

      if (currentId) {
        await entretienFinalService.update(matricule, currentId, payload);
        setStatusMessage(t("final_meeting_updated"));
        await showSuccessAlert(t("final_meeting_updated_title"), t("final_meeting_updated_text"));
      } else {
        await entretienFinalService.create(matricule, payload);
        setStatusMessage(t("final_meeting_created"));
        await showSuccessAlert(t("final_meeting_created_title"), t("final_meeting_created_text"));
      }

      localStorage.removeItem(`entretien-final-draft-${matricule}`);
      await loadAllEntretiens();
      setTimeout(() => navigate(`/paq-dossier/${matricule}`), 1500);
    } catch (err) {
      console.error(err);
      const errorMessage = err.response?.data?.message || err.message || t("error_saving_data");
      setError(errorMessage);
      showErrorAlert(t("cannot_save"), errorMessage);
    } finally { 
      setSaving(false);
    }
  };

  const fmt = (d) => {
    if (!d) return "—";
    try {
      const locale = lang === "ar" ? "ar-EG" : lang === "fr" ? "fr-FR" : "en-US";
      return new Intl.DateTimeFormat(locale).format(new Date(d));
    } catch {
      return d;
    }
  };

  if (loading) return <div className="ef-loading">{t("loading")}</div>;

  return (
    <>
      <div className="ef-root">

        <div className="leoni-header">
          <div className="leoni-header-left">
            <button onClick={() => navigate(`/paq-dossier/${matricule}`)} className="leoni-btn-back">
              {t("return_to_file")}
            </button>
          </div>
          <div className="leoni-header-title">
            <div className="leoni-logo-bar">
              <div className="leoni-logo-accent" />
              <h1>{t("final_meeting_step_title")}</h1>
            </div>
            {collaborator && (
              <span className="leoni-header-sub">
                {collaborator.name} {collaborator.prenom} - {collaborator.matricule}
              </span>
            )}
          </div>
          <div className="leoni-header-actions" />
        </div>

        <div className="ef-page">

          <aside className="ef-sidebar">
            <div className="ef-card">
              <div className="ef-card-hd">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke="currentColor" strokeWidth="2"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" strokeWidth="2"/>
                </svg>
                {t("collaborator_information")}
              </div>
              <div className="ef-card-bd">
                <div className="ef-avatar">
                  {`${collaborator?.name?.[0]||""}${collaborator?.prenom?.[0]||""}`.toUpperCase() || "?"}
                </div>
                <div className="ef-cname">{collaborator?.name} {collaborator?.prenom}</div>
                <div className="ef-igrid">
                  <div className="ef-icell"><span className="ef-ilbl">{t("matricule")}</span><span className="ef-ival">{collaborator?.matricule||"—"}</span></div>
                  <div className="ef-icell"><span className="ef-ilbl">{t("segment")}</span><span className="ef-ival">{collaborator?.segment||"—"}</span></div>
                  <div className="ef-icell"><span className="ef-ilbl">{t("hire_date")}</span><span className="ef-ival">{fmt(collaborator?.hireDate)}</span></div>
                  <div className="ef-icell"><span className="ef-ilbl">{t("status")}</span><span className="ef-ival green">{collaborator?.status||"ACTIF"}</span></div>
                </div>
              </div>
            </div>

            <div className="ef-card">
              <div className="ef-card-hd amber">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
                  <path d="M9 12l2 2 4-4M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" strokeWidth="2"/>
                </svg>
                {t("decision_summary_title")}
              </div>
              <div className="ef-card-bd">
                {resumeN4 ? (
                  <div className="ef-rrow">
                    <div className="ef-rline"><span className="ef-rlbl">{t("fault_type_label")}</span><span className="ef-rval">{resumeN4.typeFaute||"—"}</span></div>
                    <div className="ef-rline"><span className="ef-rlbl">{t("meeting_date_label")}</span><span className="ef-rval">{fmt(resumeN4.dateEntretien||resumeN4.date)}</span></div>
                    <div className="ef-rline"><span className="ef-rlbl">{t("rh_decision_label")}</span><span className="ef-rval">{resumeN4.decision ? t(`decision_${resumeN4.decision}`, resumeN4.decision) : "—"}</span></div>
                    {resumeN4.justification && (
                      <div className="ef-rline"><span className="ef-rlbl">{t("justification")}</span><span className="ef-rval" style={{fontSize:11}}>{resumeN4.justification}</span></div>
                    )}
                  </div>
                ) : (
                  <div className="ef-rempty">{t("no_decision_meeting_found")}</div>
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
                {t("form")}
              </div>

              <div className="ef-form-bd">
                <form onSubmit={handleSubmit}>

                  <div className="ef-fg">
                    <label className="ef-lbl">{t("fault_type_label")} <span className="req"></span></label>
                    <div className="ef-faute-row">
                      <div className="ef-dw">
                        <input type="text" className="ef-inp"
                          placeholder={t("search_or_select_fault")}
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
                        {t("add_fault_button")}
                      </button>
                    </div>
                  </div>

                    <div className="ef-fg">
                      <label className="ef-lbl">{t("meeting_date_label")}</label>
                      <input type="date" name="dateEntretien" className="ef-inp"
                        value={formData.dateEntretien} onChange={handleChange}/>
                    </div>
                     <div className="ef-fg">
                    <label className="ef-lbl">{t("main_cause_label")}</label>
                    <input type="text" name="causePrincipale" className="ef-inp"
                      value={formData.causePrincipale} onChange={handleChange}
                      placeholder={t("main_cause_placeholder")} />
                  </div>

                    <div className="ef-fg">
                      <label className="ef-lbl">{t("rh_decision_label")} <span className="req"></span></label>
                      <select name="decision" className="ef-sel"
                        value={formData.decision} onChange={handleChange}>
                        <option value="">{t("choose_option")}</option>
                        {DECISIONS.map(d => <option key={d} value={d}>{t(`decision_${d}`, d)}</option>)}
                      </select>
                    </div>

                 

                  <div className="ef-fg">
                    <label className="ef-lbl">{t("rh_comment_label")}</label>
                    <textarea name="commentaireRH" className="ef-ta" rows={3}
                      value={formData.commentaireRH} onChange={handleChange}
                      placeholder={t("final_decision_comment_placeholder")}/>
                  </div>

                  <div className="ef-actions">
                    <button type="button" className="ef-btn ef-btn-draft"
                      onClick={handleEnregistrer} disabled={savingDraft}>
                      {savingDraft ? t("saving") : t("save_draft")}
                    </button>
                    <button type="submit" className="ef-btn ef-btn-valider" disabled={saving}>
                      {saving ? "..." : t("validate")}
                    </button>

                    <button type="button" className="ef-btn ef-btn-modifier"
                      onClick={handleModifier} disabled={loadingDraft}>
                      {loadingDraft ? "..." : t("edit")}
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
            <h3>{t("add_fault_type_title")}</h3>
            <label className="ef-lbl">{t("fault_type_name_label")}</label>
            <input className="ef-inp" style={{marginTop:6}} value={defautTypeInput}
              onChange={e => setDefautTypeInput(e.target.value)}
              placeholder={t("enter_new_fault_type")}
              onKeyDown={e => e.key === "Enter" && addTypeOption()}
              autoFocus/>
            <div className="ef-modal-acts">
              <button className="ef-mbtn-cancel" onClick={() => setShowDefautModal(false)}>{t("cancel")}</button>
              <button className="ef-mbtn-ok" onClick={addTypeOption} disabled={!defautTypeInput.trim()}>{t("add")}</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}