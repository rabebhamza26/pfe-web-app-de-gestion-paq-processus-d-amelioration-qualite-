import React, { useEffect, useMemo, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { collaboratorService, paqService } from "../../services/api";
import { showConfirmAlert, showErrorAlert } from "../../utils/entretienAlerts";
import "../../styles/collaborator.css";
import { useSelection } from "../../context/SelectionContext";

export default function CollaboratorManagement() {
  const [collaborators, setCollaborators] = useState([]);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState("");
  const [search, setSearch]               = useState("");
  const [sortMode, setSortMode]           = useState("latest_added");
  const navigate  = useNavigate();
  const location  = useLocation();

  // ── Sélection courante du user (site / plant) ──────────────
  const { selectedSite, selectedPlant } = useSelection();

  // ── Rôle du user connecté ──────────────────────────────────
  const [userRole, setUserRole] = useState('');
  const [userPermissions, setUserPermissions] = useState({ sites: [], plants: [], segments: [] });

  useEffect(() => {
    const userStr = sessionStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      setUserRole(user.role || '');
      setUserPermissions({
        sites: user.sites || [],
        plants: user.plants || [],
        segments: user.segments || []
      });
    }
  }, []);

  // ── Rechargement quand le site/plant sélectionné change ────
  useEffect(() => {
    loadCollaborators();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedSite, selectedPlant]);

  const getSortableTimestamp = (collab) => {
    const raw = collab?.createdAt || collab?.updatedAt || collab?.hireDate;
    if (!raw) return 0;
    const time = new Date(raw).getTime();
    return Number.isFinite(time) ? time : 0;
  };

  const sortCollaborators = (list, latestMatricule, mode) => {
    const sorted = [...list];

    sorted.sort((a, b) => {
      if (mode === "matricule") {
        return String(a.matricule || "").localeCompare(String(b.matricule || ""), "fr", { numeric: true });
      }

      const aIsLatest = mode === "latest_added" && latestMatricule && String(a.matricule) === String(latestMatricule);
      const bIsLatest = mode === "latest_added" && latestMatricule && String(b.matricule) === String(latestMatricule);
      if (aIsLatest !== bIsLatest) return aIsLatest ? -1 : 1;

      const timeDiff = getSortableTimestamp(b) - getSortableTimestamp(a);
      if (timeDiff !== 0) return timeDiff;

      return String(a.matricule || "").localeCompare(String(b.matricule || ""), "fr", { numeric: true });
    });

    return sorted;
  };

  // ── Chargement des collaborateurs ─────────────────────────
  const loadCollaborators = async () => {
    try {
      setLoading(true);
      setError("");

      const filterParams = {
        siteId: selectedSite?.id || null,
        plantId: selectedPlant?.id || null,
      };

      const response = await collaboratorService.getAll(filterParams);
      let data = Array.isArray(response.data) ? response.data : response.data?.data || [];

      const newCollab = location.state?.newCollaborator;
      const latestMatricule = newCollab?.matricule || sessionStorage.getItem("latest_collaborator_matricule");
      if (newCollab?.matricule) {
        const sameMatricule = (c) => String(c.matricule) === String(newCollab.matricule);
        const existing = data.find(sameMatricule);
        const prioritized = existing || newCollab;
        data = [prioritized, ...data.filter((c) => !sameMatricule(c))];
      }
      setCollaborators(sortCollaborators(data, latestMatricule, sortMode));
    } catch (err) {
      console.error(err);
      setError("Impossible de charger la liste des collaborateurs");
    } finally {
      setLoading(false);
    }
  };

  const deleteCollaborator = async (matricule, fullName) => {
    const result = await showConfirmAlert({
      title: "Supprimer le collaborateur ?",
      text: `Êtes-vous sûr de vouloir supprimer ${fullName} ?`,
      confirmButtonText: "Oui, supprimer",
      cancelButtonText: "Annuler",
    });
    if (!result.isConfirmed) return;
    try {
      await collaboratorService.delete(matricule);
      setCollaborators(prev => prev.filter(c => c.matricule !== matricule));
    } catch {
      showErrorAlert("Erreur", "Erreur lors de la suppression du collaborateur");
    }
  };

  const hasSixMonthsPassed = (collab) => {
    if (!collab?.hireDate) return false;
    const limit = new Date(collab.hireDate);
    limit.setMonth(limit.getMonth() + 6);
    return new Date() >= limit;
  };

 const hasActivePaq = (collab) => {
    // Utiliser le champ hasActivePaq du backend s'il existe
    if (collab.hasActivePaq !== undefined) {
        return collab.hasActivePaq === true;
    }
    // Fallback: vérifier par le statut
    const s = (collab.statut || "").toUpperCase();
    return s !== "POSITIF" && s !== "N/A" && collab.niveau !== undefined && collab.niveau !== null;
};

const peutCreerPaq = (collab) => {
    // Si le backend fournit l'information
    if (collab.peutCreerPaq !== undefined) {
        return collab.peutCreerPaq === true;
    }
    
    // Fallback: si pas de PAQ actif
    if (hasActivePaq(collab)) return false;
    
    // Vérifier la date de création du dernier PAQ dans le localStorage
    const lastPaqDate = localStorage.getItem(`last_paq_${collab.matricule}`);
    if (!lastPaqDate) {
        // Premier PAQ : toujours possible
        return true;
    }
    
    const lastDate = new Date(lastPaqDate);
    const sixMonthsLater = new Date(lastDate);
    sixMonthsLater.setMonth(sixMonthsLater.getMonth() + 6);
    
    return new Date() >= sixMonthsLater;
};

  const formatDate = (d) => {
    if (!d) return "N/A";
    try { return new Date(d).toLocaleDateString("fr-FR"); } catch { return d; }
  };

  const filteredCollaborators = useMemo(() => {
    const s = search.toLowerCase().trim();
    const latestMatricule = location.state?.newCollaborator?.matricule
      || sessionStorage.getItem("latest_collaborator_matricule");

    const filtered = collaborators.filter((c) => {
      if (!s) return true;
      return (
        (c.matricule && c.matricule.toLowerCase().includes(s)) ||
        (c.nom && c.nom.toLowerCase().includes(s)) ||
        (c.prenom && c.prenom.toLowerCase().includes(s)) ||
        (c.segment && c.segment.toLowerCase().includes(s))
      );
    });

    return sortCollaborators(filtered, latestMatricule, sortMode);
  }, [collaborators, search, sortMode, location.state]);

  const contextLabel = selectedPlant
    ? `Plant : ${selectedPlant.name || selectedPlant.id}`
    : selectedSite
    ? `Site : ${selectedSite.name || selectedSite.id}`
    : "Tous les sites / plants";

  const getNiveauLabel = (collab) => {
    const niveau = collab?.niveau ?? 0;
    switch (niveau) {
      case 0: return "N0";
      case 1: return "N1";
      case 2: return "N2";
      case 3: return "N3";
      case 4: return "N4";
      case 5: return "N5";
      default: return "N0";
    }
  };

  const getNiveauClass = (collab) => {
    const niveau = collab?.niveau ?? 0;
    switch (niveau) {
      case 0: return "niveau-badge n0";
      case 1: return "niveau-badge n1";
      case 2: return "niveau-badge n2";
      case 3: return "niveau-badge n3";
      case 4: return "niveau-badge n4";
      case 5: return "niveau-badge n5";
      default: return "niveau-badge n0";
    }
  };

  return (
    <div className="container py-4 collab-page">
      <div className="collab-topbar">
        <div>
          <div className="collab-title">Gestion Collaborateurs</div>
          <small className="text-muted">
            <i className="fas fa-filter me-1"></i>{contextLabel}
          </small>
        </div>
        <button
          type="button"
          onClick={() => navigate("/add-collaborator")}
          className="btn btn-primary btn-sm"
        >
          Ajouter Collaborateur
        </button>
      </div>

      <div className="collab-search mb-4">
        <div className="collab-toolbar">
          <div className="input-group collab-search-group">
            <span className="input-group-text"><i className="fas fa-search"></i></span>
            <input
              type="text"
              className="form-control"
              placeholder="Rechercher par matricule, nom..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            {search && (
              <button className="btn btn-outline-secondary" onClick={() => setSearch("")}>✕</button>
            )}
          </div>
          <div className="collab-filter-box">
            <label htmlFor="collab-sort" className="collab-filter-label">Tri</label>
            <select
              id="collab-sort"
              className="form-select collab-filter-select"
              value={sortMode}
              onChange={(e) => setSortMode(e.target.value)}
            >
              <option value="latest_added">Dernier ajouté en premier</option>
              <option value="hire_date">Date embauche récente</option>
              <option value="matricule">Matricule</option>
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status"></div>
          <p className="mt-2 text-muted">Chargement des collaborateurs...</p>
        </div>
      ) : error ? (
        <div className="alert alert-danger text-center">
          {error}
          <button className="btn btn-link" onClick={loadCollaborators}>Réessayer</button>
        </div>
      ) : filteredCollaborators.length === 0 ? (
        <div className="text-center py-5">
          <p className="text-muted">Aucun collaborateur trouvé</p>
          {search && (
            <button className="btn btn-outline-secondary" onClick={() => setSearch("")}>
              Effacer la recherche
            </button>
          )}
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="custom-table">
            <thead>
              <tr>
                <th>MATRICULE</th>
                <th>NOM & PRÉNOM</th>
                <th>SEGMENT</th>
                <th>DATE EMBAUCHE</th>
                <th>NIVEAU PAQ</th>
                <th>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {filteredCollaborators.map((c) => {
                const sixMonthsPassed = hasSixMonthsPassed(c);
                const hasPaq = hasActivePaq(c);

                // CORRECTION : Utiliser 'c' au lieu de 'collab'
               let paqButton = null;
if (hasActivePaq(c)) {
    paqButton = (
        <button
            className="action-btn btn-view"
            onClick={() => navigate(`/paq-dossier/${c.matricule}`)}
            title="Consulter le dossier PAQ"
        >
            Voir PAQ
        </button>
    );
} else if (peutCreerPaq(c)) {
    paqButton = (
        <button
            className="action-btn btn-paq"
            onClick={() => navigate(`/paq-dossier/${c.matricule}`)}
            title="Créer le dossier PAQ"
        >
            Créer PAQ
        </button>
    );
} else {
    const prochainPaqDate = c.prochainPaqDate ? new Date(c.prochainPaqDate) : null;
    paqButton = (
        <button
            className="action-btn btn-paq-disabled"
            disabled
            title={prochainPaqDate ? `Nouveau PAQ disponible à partir du ${prochainPaqDate.toLocaleDateString('fr-FR')}` : "PAQ non disponible"}
        >
            Créer PAQ
        </button>
    );
}
                return (
                  <tr key={c.matricule}>
                    <td className="matricule-cell">{c.matricule}</td>
                    <td className="name-cell">{c.nom} {c.prenom}</td>
                    <td className="segment-cell">{c.segment}</td>
                    <td className="date-cell">{formatDate(c.hireDate)}</td>
                    <td className="niveau-cell">
                      <span className={getNiveauClass(c)}>
                        {getNiveauLabel(c)}
                      </span>
                    </td>
                    <td className="actions-cell">
                      <div className="actions-group">
                        {paqButton}
                        <button
                          className="action-btn btn-edit"
                          onClick={() =>
                            navigate(`/edit-collaborator/${c.matricule}`, {
                              state: { collaborator: c },
                            })
                          }
                          title="Modifier le collaborateur"
                        >
                          Modifier
                        </button>
                        <button
                          className="action-btn btn-delete"
                          onClick={() => deleteCollaborator(c.matricule, `${c.nom} ${c.prenom}`)}
                          title="Supprimer le collaborateur"
                        >
                          Supprimer
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}