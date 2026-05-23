import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { siteService, plantService } from "../../services/api";
import "../../styles/sites-management.css";
import { Pencil, Eye, Trash2 } from "lucide-react";
import { showConfirmAlert, showErrorAlert, showSuccessAlert } from "../../utils/entretienAlerts";



export default function SitesManagement() {
  const navigate = useNavigate();
  const [sites, setSites] = useState([]);
  const [plantCounts, setPlantCounts] = useState({});
  const [form, setForm] = useState({ name: "" });
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [openModal, setOpenModal] = useState(false);

  useEffect(() => { loadSites(); }, []);

  const loadSites = async () => {
  try {
    setLoading(true);
    const res = await siteService.getAll();

    const list = Array.isArray(res.data)
      ? res.data
      : Array.isArray(res.data?.content)
      ? res.data.content
      : [];

    setSites(list);

    const counts = {};
    await Promise.all(
      list.map(async (site) => {
        try {
          const pr = await plantService.getBySite(site.id);
          counts[site.id] = (pr.data || []).length;
        } catch {
          counts[site.id] = 0;
        }
      })
    );

    setPlantCounts(counts);
  } catch {
    setError("Erreur de chargement des sites");
  } finally {
    setLoading(false);
  }
};

  const openAddModal = () => {
    setForm({ name: "" });
    setEditingId(null);
    setError("");
    setOpenModal(true);
  };

  const openEditModal = (site) => {
    setForm({ name: site.name });
    setEditingId(site.id);
    setError("");
    setOpenModal(true);
  };

  const closeModal = () => {
    setOpenModal(false);
    setForm({ name: "" });
    setEditingId(null);
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) {
      setError("Le nom est obligatoire.");
      await showErrorAlert("Champ requis", "Le nom du site est obligatoire.");
      return;
    }
    setSaving(true);
    try {
      if (editingId) {
        await siteService.update(editingId, form);
      } else {
        await siteService.create(form);
      }
      closeModal();
      await loadSites();
      await showSuccessAlert(
        editingId ? "Site modifie" : "Site ajoute",
        editingId ? "Les informations du site ont ete mises a jour." : "Le site a ete ajoute avec succes."
      );
    } catch (err) {
      const message = err.response?.data?.message || "Echec de l'enregistrement";
      setError(message);
      await showErrorAlert("Enregistrement impossible", message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    const count = plantCounts[id] ?? 0;
    const result = await showConfirmAlert({
      title: "Supprimer ce site ?",
      text: count > 0
        ? `Ce site contient ${count} plant(s). La suppression restera definitive.`
        : "Cette action est irreversible.",
      confirmButtonText: "Supprimer",
    });
    if (!result.isConfirmed) return;

    try {
      await siteService.delete(id);
      await loadSites();
      await showSuccessAlert("Site supprime", "Le site a ete supprime avec succes.");
    } catch (err) {
      const message = err.response?.data?.message || "Erreur lors de la suppression";
      setError(message);
      await showErrorAlert("Suppression impossible", message);
    }
  };

  return (
    <div className="sm-page">

      {/* ── HEADER ── */}
      <div className="sm-header">
        <div className="sm-header-text">
          <h1 className="sm-header-title">Gestion des Sites</h1>
        </div>
        <div className="sm-header-stats">
          <div className="sm-stat">
            <span className="sm-stat-val">{sites.length}</span>
            <span className="sm-stat-lbl">Sites</span>
          </div>
          <div className="sm-stat">
            <span className="sm-stat-val">
              {Object.values(plantCounts).reduce((a, b) => a + b, 0)}
            </span>
            <span className="sm-stat-lbl">Plants total</span>
          </div>
        </div>
      </div>

      {/* ── TOOLBAR ── */}
      <div className="sm-toolbar">
        <button className="sm-btn-add" onClick={openAddModal}>
          <svg viewBox="0 0 20 20" fill="none" width="16" height="16">
            <circle cx="10" cy="10" r="9" stroke="currentColor" strokeWidth="1.5"/>
            <path d="M10 6v8M6 10h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
          </svg>
          Nouveau Site
        </button>
      </div>

      {/* ── ERROR ── */}
      {error && !openModal && (
        <div className="sm-alert">
          <svg viewBox="0 0 20 20" fill="none" width="16" height="16">
            <circle cx="10" cy="10" r="9" stroke="currentColor" strokeWidth="1.5"/>
            <path d="M10 6v5M10 14h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
          </svg>
          {error}
        </div>
      )}

      {/* ── GRID ── */}
      {loading ? (
        <div className="sm-grid">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="sm-card sm-skeleton" />
          ))}
        </div>
      ) : sites.length === 0 ? (
        <div className="sm-empty">
          <svg viewBox="0 0 48 48" fill="none" width="48" height="48">
            <circle cx="24" cy="24" r="20" stroke="currentColor" strokeWidth="1.5" opacity=".3"/>
            <path d="M24 16v8M24 30h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" opacity=".6"/>
          </svg>
          <p>Aucun site trouvé. Créez-en un !</p>
        </div>
      ) : (
        <div className="sm-grid">
          {sites.map((site) => (
            <div key={site.id} className="sm-card">
              <div className="sm-card-top">
                <div className="sm-card-icon">
                  <svg viewBox="0 0 32 32" fill="none" width="22" height="22">
                    <rect x="3" y="16" width="5" height="13" rx="1" fill="currentColor" opacity=".8"/>
                    <rect x="10" y="11" width="5" height="18" rx="1" fill="currentColor" opacity=".65"/>
                    <rect x="17" y="6" width="5" height="23" rx="1" fill="currentColor" opacity=".9"/>
                    <rect x="24" y="13" width="5" height="16" rx="1" fill="currentColor" opacity=".6"/>
                    <line x1="1" y1="29" x2="31" y2="29" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                </div>
                <div className="sm-card-actions">
                  <button
                    className="sm-icon-btn sm-icon-edit"
                    title="Modifier"
                    onClick={() => openEditModal(site)}
                  >
                    <Pencil size={18} strokeWidth={2} />
                     

                  </button>
                  <button
                    className="sm-icon-btn sm-icon-plants"
                    title="Voir les plants"
                    onClick={() => navigate(`/admin/plants?siteId=${site.id}`)}
                  >
                     <Eye size={18} strokeWidth={2} />
                  </button>
                  <button
                    className="sm-icon-btn sm-icon-delete"
                    title="Supprimer"
                    onClick={() => handleDelete(site.id)}
                  >
                     <Trash2 size={18} strokeWidth={2} />
                  </button>
                </div>
              </div>
              <div className="sm-card-body">
                <h3 className="sm-card-name">{site.name}</h3>
                <div className="sm-card-meta">
                  <span className="sm-tag">
                    <svg viewBox="0 0 12 12" fill="none" width="11" height="11">
                      <rect x="1" y="1" width="4" height="4" rx=".5" fill="currentColor"/>
                      <rect x="7" y="1" width="4" height="4" rx=".5" fill="currentColor" opacity=".6"/>
                      <rect x="1" y="7" width="4" height="4" rx=".5" fill="currentColor" opacity=".6"/>
                      <rect x="7" y="7" width="4" height="4" rx=".5" fill="currentColor"/>
                    </svg>
                    {plantCounts[site.id] ?? 0} plant{(plantCounts[site.id] ?? 0) !== 1 ? "s" : ""}
                  </span>
                  <span className="sm-tag sm-tag-id">ID #{site.id}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── MODAL ── */}
      {openModal && (
        <div className="sm-overlay" onClick={(e) => e.target === e.currentTarget && closeModal()}>
          <div className="sm-modal">
            <div className="sm-modal-header">
              <div className="sm-modal-icon-wrap">
                {editingId ? (
                  <svg viewBox="0 0 20 20" fill="none" width="18" height="18">
                    <path d="M14 3l3 3L6 17H3v-3L14 3z" stroke="currentColor" strokeWidth="1.8"
                      strokeLinejoin="round"/>
                  </svg>
                ) : (
                  <svg viewBox="0 0 20 20" fill="none" width="18" height="18">
                    <circle cx="10" cy="10" r="8" stroke="currentColor" strokeWidth="1.8"/>
                    <path d="M10 6v8M6 10h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                  </svg>
                )}
              </div>
              <div>
                <h3>{editingId ? "Modifier le site" : "Nouveau site"}</h3>
                <p>{editingId ? "Mettez à jour le nom du site." : "Ajoutez un nouveau site de production."}</p>
              </div>
              <button className="sm-modal-close" onClick={closeModal}>✕</button>
            </div>

            <div className="sm-modal-body">
              {error && (
                <div className="sm-alert sm-alert-modal">{error}</div>
              )}
              <form onSubmit={handleSubmit}>
                <div className="sm-field">
                  <label>Nom du site <span className="sm-required"></span></label>
                  <input
                    className="sm-input"
                    type="text"
                    value={form.name}
                    onChange={(e) => setForm({ name: e.target.value })}
                    placeholder="Ex : Saisie le nom de site"
                    required
                    autoFocus
                  />
                </div>
                <div className="sm-modal-footer">
                  <button type="button" className="sm-btn-cancel" onClick={closeModal}>
                    Annuler
                  </button>
                  <button type="submit" className="sm-btn-submit" disabled={saving}>
                    {saving ? "Enregistrement…" : editingId ? "Enregistrer" : "Créer le site"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
