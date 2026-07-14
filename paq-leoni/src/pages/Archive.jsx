import React, { useEffect, useState, useCallback } from "react";
import { archiveService } from "../services/api";
import "../styles/archive.css";

// ─── Mapping type → label / couleur badge ────────────────────────────────────
const TYPE_LABELS = {
  N1: "Entretien Explicatif",
  N2: "Entretien D'accord",
  N3: "Entretien de mesure",
  N4: "Entretien de décision",
  N5: "Entretien final",
  ENTRETIEN_POSITIF: "Entretien Positif",
  PAQ: "Dossier PAQ",
};

const TYPE_COLORS = {
  N1: "badge-green",
  N2: "badge-blue",
  N3: "badge-yellow",
  N4: "badge-purple",
  N5: "badge-red",
  ENTRETIEN_POSITIF: "badge-gray",
  PAQ: "badge-gray",
};

// ─── Helpers ─────────────────────────────────────────────────────────────────
const normalizeType = (type) => {
  if (!type) return "N1";
  const t = type.toLowerCase();
  if (t.includes("positif")) return "ENTRETIEN_POSITIF";
  if (t.includes("explicatif")) return "N1";
  if (t.includes("accord")) return "N2";
  if (t.includes("mesure")) return "N3";
  if (t.includes("decision") || t.includes("décision")) return "N4";
  if (t.includes("final")) return "N5";
  if (t.includes("paq")) return "PAQ";
  return "N1";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "–";
  try {
    return new Date(dateStr).toLocaleDateString("fr-FR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
};

// ─── Modal de consultation d'une archive ─────────────────────────────────────
function ArchiveDetailModal({ archive, onClose }) {
  const t = (key) => key;
  if (!archive) return null;

  const type = normalizeType(archive.type);
  const typeLabel = TYPE_LABELS[type] || archive.type;

  let historiqueEvents = [];
  if (archive.historique) {
    try {
      historiqueEvents = JSON.parse(archive.historique);
    } catch {
      historiqueEvents = [];
    }
  }

  return (
    <div className="arch-modal-overlay" onClick={onClose}>
      <div
        className="arch-modal-content"
        onClick={(e) => e.stopPropagation()}
      >
        {/* En-tête */}
        <div className="arch-modal-header">
          <div>
            <h2 className="arch-modal-title">{t("archived_file")}</h2>
            <p className="arch-modal-subtitle">
              {t("archived_on", { date: formatDate(archive.dateArchivage) })}
            </p>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <span className={`arch-type-badge ${TYPE_COLORS[type]}`}>
              {typeLabel}
            </span>
            <button className="arch-modal-close" onClick={onClose}>
              ✕
            </button>
          </div>
        </div>

        {/* Corps */}
        <div className="arch-modal-body">
          {/* Section collaborateur */}
          <div className="arch-modal-section">
            <div className="arch-modal-section-title">
              {t("collaborator_information")}
            </div>
            <div className="arch-info-grid">
              <div className="arch-info-item">
                <label>Matricule</label>
                <span>{archive.matricule || "–"}</span>
              </div>
              <div className="arch-info-item">
                <label>Nom & Prénom</label>
                <span>{archive.nomPrenom || "–"}</span>
              </div>
              <div className="arch-info-item">
                <label>{t("type_of_meeting")}</label>
                <span>{typeLabel}</span>
              </div>
              <div className="arch-info-item">
                <label>{t("archiving_date")}</label>
                <span>{formatDate(archive.dateArchivage)}</span>
              </div>
              {archive.niveau !== null && archive.niveau !== undefined && (
                <div className="arch-info-item">
                  <label>{t("level_reached")}</label>
                  <span>
                    <div className="niveau-pills-modal">
                      {[1, 2, 3, 4, 5].map((n) => (
                        <span
                          key={n}
                          className={`niveau-pill-modal ${
                            n <= archive.niveau ? "active" : ""
                          }`}
                        >
                          N{n}
                        </span>
                      ))}
                    </div>
                  </span>
                </div>
              )}
              {archive.statut && (
                <div className="arch-info-item">
                  <label>{t("final_status")}</label>
                  <span className="statut-badge">{archive.statut}</span>
                </div>
              )}
              {archive.dateCreation && (
                <div className="arch-info-item">
                  <label>{t("paq_start")}</label>
                  <span>{formatDate(archive.dateCreation)}</span>
                </div>
              )}
              {archive.dateFin && (
                <div className="arch-info-item">
                  <label>{t("paq_end")}</label>
                  <span>{formatDate(archive.dateFin)}</span>
                </div>
              )}
            </div>
          </div>

          {/* Section historique */}
          {historiqueEvents.length > 0 ? (
            <div className="arch-modal-section">
              <div className="arch-modal-section-title">
                {t("event_history", { count: historiqueEvents.length })}
              </div>
              <div className="arch-historique-list">
                {historiqueEvents.map((evt, idx) => (
                  <div key={idx} className="arch-historique-item">
                    <div className="arch-historique-dot" />
                    <div className="arch-historique-content">
                      <div className="arch-historique-date">
                        {formatDate(evt.date)}
                      </div>
                      <div className="arch-historique-action">
                        {evt.action || "–"}
                      </div>
                      {evt.detail && (
                        <div className="arch-historique-detail">
                          {evt.detail}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="arch-modal-section">
              <div className="arch-modal-section-title">
                {t("event_history")}
              </div>
              <p style={{ color: "#94a3b8", fontSize: "13px" }}>
                {t("no_events_recorded")}
              </p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="arch-modal-footer">
          <button
            className="arch-btn arch-btn-primary"
            onClick={() => exportArchivePdf(archive, typeLabel, formatDate, historiqueEvents)}
          >
            📄 Exporter en PDF
          </button>
          <button className="arch-btn arch-btn-outline" onClick={onClose}>
            Fermer
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Export PDF (partagé modal + tableau) ────────────────────────────────────
function exportArchivePdf(archive, typeLabel, formatDateFn, historiqueEvents) {
  const fmt = formatDateFn || formatDate;
  const events = historiqueEvents || (() => {
    try { return JSON.parse(archive.historique || "[]"); } catch { return []; }
  })();

  const historiqueRows = events
    .map(
      (e) => `
      <tr>
        <td style="padding:6px 10px;border-bottom:1px solid #e2e8f0;">${fmt(e.date)}</td>
        <td style="padding:6px 10px;border-bottom:1px solid #e2e8f0;">${e.action || ""}</td>
        <td style="padding:6px 10px;border-bottom:1px solid #e2e8f0;">${e.detail || ""}</td>
      </tr>`
    )
    .join("");

  const htmlContent = `
    <!DOCTYPE html>
    <html lang="fr">
    <head>
      <meta charset="UTF-8"/>
      <title>Archive PAQ — ${archive.nomPrenom || archive.matricule}</title>
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: Arial, sans-serif; font-size: 13px; color: #1e293b; padding: 32px; }
        .header { display: flex; justify-content: space-between; align-items: flex-start;
                  border-bottom: 3px solid #005baa; padding-bottom: 16px; margin-bottom: 24px; }
        .title { font-size: 22px; font-weight: bold; color: #005baa; }
        .subtitle { font-size: 13px; color: #64748b; margin-top: 4px; }
        .badge { display: inline-block; padding: 3px 10px; border-radius: 12px;
                 font-size: 11px; font-weight: 600; background: #dbeafe; color: #1d4ed8; }
        .section { margin-bottom: 20px; }
        .section-title { font-size: 13px; font-weight: 700; color: #475569;
                         text-transform: uppercase; letter-spacing: .5px; margin-bottom: 10px;
                         padding-bottom: 4px; border-bottom: 1px solid #e2e8f0; }
        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
        .info-item label { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 2px; }
        .info-item span { font-weight: 600; }
        table { width: 100%; border-collapse: collapse; margin-top: 4px; }
        thead tr { background: #f1f5f9; }
        th { padding: 8px 10px; text-align: left; font-size: 11px; color: #64748b;
             text-transform: uppercase; letter-spacing: .4px; }
        .footer { margin-top: 32px; font-size: 11px; color: #94a3b8;
                  text-align: right; border-top: 1px solid #e2e8f0; padding-top: 8px; }
        @media print { body { padding: 16px; } }
      </style>
    </head>
    <body>
      <div class="header">
        <div>
          <div class="title">Archive Dossier PAQ</div>
          <div class="subtitle">Exporté le ${fmt(new Date().toISOString())}</div>
        </div>
        <span class="badge">${typeLabel}</span>
      </div>
      <div class="section">
        <div class="section-title">Informations Collaborateur</div>
        <div class="info-grid">
          <div class="info-item"><label>Matricule</label><span>${archive.matricule || "–"}</span></div>
          <div class="info-item"><label>Nom & Prénom</label><span>${archive.nomPrenom || "–"}</span></div>
          <div class="info-item"><label>Type d'entretien</label><span>${typeLabel}</span></div>
          <div class="info-item"><label>Date d'archivage</label><span>${fmt(archive.dateArchivage)}</span></div>
          ${archive.niveau != null ? `<div class="info-item"><label>Niveau atteint</label><span>${archive.niveau}/5</span></div>` : ""}
          ${archive.statut ? `<div class="info-item"><label>Statut final</label><span>${archive.statut}</span></div>` : ""}
          ${archive.dateCreation ? `<div class="info-item"><label>Début PAQ</label><span>${fmt(archive.dateCreation)}</span></div>` : ""}
          ${archive.dateFin ? `<div class="info-item"><label>Fin PAQ</label><span>${fmt(archive.dateFin)}</span></div>` : ""}
        </div>
      </div>
      ${events.length > 0 ? `
      <div class="section">
        <div class="section-title">Historique des Événements</div>
        <table>
          <thead><tr><th>Date</th><th>Action</th><th>Détail</th></tr></thead>
          <tbody>${historiqueRows}</tbody>
        </table>
      </div>` : ""}
      <div class="footer">Document généré automatiquement par le système PAQ — LEONI</div>
    </body>
    </html>
  `;

  const printWindow = window.open("", "_blank", "width=900,height=700");
  if (printWindow) {
    printWindow.document.write(htmlContent);
    printWindow.document.close();
    printWindow.onload = () => {
      printWindow.focus();
      printWindow.print();
      printWindow.onafterprint = () => printWindow.close();
    };
  }
}

// ─── Composant principal Archive ──────────────────────────────────────────────
export default function Archive() {
  const [archives, setArchives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [exportingId, setExportingId] = useState(null);
  const [selectedArchive, setSelectedArchive] = useState(null); // Pour le modal

  // ── Chargement ──────────────────────────────────────────────────────────────
  const loadArchives = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const res = await archiveService.getAll();
      setArchives(res.data || []);
    } catch {
      setError("Erreur lors du chargement des archives. Vérifiez que le serveur est démarré.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadArchives();
  }, [loadArchives]);

  // ── Recherche ───────────────────────────────────────────────────────────────
  const handleSearch = async () => {
    if (!search.trim()) return loadArchives();
    try {
      setLoading(true);
      setError("");
      const res = await archiveService.searchByMatricule(search.trim());
      setArchives(res.data || []);
    } catch {
      setError("Erreur lors de la recherche");
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") handleSearch();
  };

  const handleClearSearch = () => {
    setSearch("");
    loadArchives();
  };

  // ── Consultation archive (modal) ────────────────────────────────────────────
  const handleConsulter = async (archive) => {
    // Si on a déjà toutes les données (historique, etc.), ouvrir directement
    if (archive.historique !== undefined) {
      setSelectedArchive(archive);
      return;
    }
    // Sinon charger le détail complet depuis l'API
    try {
      const res = await archiveService.getById(archive.id);
      setSelectedArchive(res.data);
    } catch {
      // Fallback : utiliser les données déjà disponibles
      setSelectedArchive(archive);
    }
  };

  // ── Export PDF individuel ───────────────────────────────────────────────────
  const handleExportPdf = (archive) => {
    setExportingId(archive.id);
    const type = normalizeType(archive.type);
    const typeLabel = TYPE_LABELS[type] || archive.type;
    let events = [];
    try { events = JSON.parse(archive.historique || "[]"); } catch {}
    exportArchivePdf(archive, typeLabel, formatDate, events);
    setTimeout(() => setExportingId(null), 1500);
  };

  // ── Export PDF de toutes les archives ──────────────────────────────────────
  const exportAllPdf = () => {
    const rows = archives
      .map((a) => {
        const type = normalizeType(a.type);
        return `
          <tr>
            <td style="padding:7px 10px;border-bottom:1px solid #e2e8f0;">${TYPE_LABELS[type] || a.type}</td>
            <td style="padding:7px 10px;border-bottom:1px solid #e2e8f0;">${a.matricule || "–"}</td>
            <td style="padding:7px 10px;border-bottom:1px solid #e2e8f0;">${a.nomPrenom || "–"}</td>
            <td style="padding:7px 10px;border-bottom:1px solid #e2e8f0;">${formatDate(a.dateArchivage)}</td>
          </tr>`;
      })
      .join("");

    const htmlContent = `
      <!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8"/>
      <title>Archives PAQ</title>
      <style>
        body { font-family: Arial, sans-serif; font-size: 13px; padding: 32px; color: #1e293b; }
        h1 { font-size: 20px; color: #005baa; border-bottom: 3px solid #005baa;
             padding-bottom: 10px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; }
        thead tr { background: #f1f5f9; }
        th { padding: 9px 10px; text-align: left; font-size: 11px; color: #64748b;
             text-transform: uppercase; letter-spacing: .4px; }
        td { vertical-align: top; }
        .footer { margin-top: 24px; font-size: 11px; color: #94a3b8; text-align: right; }
      </style></head><body>
      <h1>Registre des Archives PAQ</h1>
      <p style="margin-bottom:16px;color:#64748b;font-size:12px;">
        Exporté le ${formatDate(new Date().toISOString())} — ${archives.length} enregistrement(s)
      </p>
      <table>
        <thead><tr><th>Type</th><th>Matricule</th><th>Nom & Prénom</th><th>Date d'archivage</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <div class="footer">Document généré automatiquement par le système PAQ — LEONI</div>
      </body></html>`;

    const printWindow = window.open("", "_blank", "width=1000,height=700");
    if (printWindow) {
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      printWindow.onload = () => {
        printWindow.focus();
        printWindow.print();
        printWindow.onafterprint = () => printWindow.close();
      };
    } else {
      setError("Impossible d'ouvrir la fenêtre d'impression. Vérifiez les popups.");
    }
  };

  // ── Rendu ───────────────────────────────────────────────────────────────────
  return (
    <div className="arch-page">
      {/* Modal de consultation */}
      {selectedArchive && (
        <ArchiveDetailModal
          archive={selectedArchive}
          onClose={() => setSelectedArchive(null)}
        />
      )}

      {/* HEADER */}
      <div className="arch-header">
        <div className="arch-header-left">
          <div className="arch-title">Archivage des entretiens</div>
          <div className="arch-subtitle">
            {archives.length} dossier{archives.length !== 1 ? "s" : ""} archivé{archives.length !== 1 ? "s" : ""}
          </div>
        </div>
        <div style={{ display: "flex", gap: "8px" }}>
          <button className="arch-btn arch-btn-outline" onClick={loadArchives}>
            ↻ Actualiser
          </button>
          {archives.length > 0 && (
            <button
              className="arch-btn arch-btn-primary"
              onClick={exportAllPdf}
              title="Exporter toutes les archives en PDF"
            >
              📄 Exporter tout en PDF
            </button>
          )}
        </div>
      </div>

      {/* ERREUR */}
      {error && (
        <div className="arch-alert arch-alert-error">
          ✕ {error}
          <button
            style={{ marginLeft: 12, background: "none", border: "none", cursor: "pointer", color: "inherit" }}
            onClick={() => setError("")}
          >
            ✕
          </button>
        </div>
      )}

      {/* RECHERCHE */}
      <div className="arch-toolbar">
        <div className="arch-search-wrap">
          <input
            type="text"
            placeholder="Rechercher par matricule…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={handleKeyDown}
            className="arch-search-input"
          />
          {search && (
            <button className="arch-search-clear" onClick={handleClearSearch}>
              ✕
            </button>
          )}
          <button className="arch-btn arch-btn-primary" onClick={handleSearch}>
            Rechercher
          </button>
        </div>
      </div>

      {/* TABLEAU */}
      <div className="arch-table-card">
        {loading ? (
          <div style={{ textAlign: "center", padding: "40px", color: "#64748b" }}>
            <div className="arch-spinner" />
            <p style={{ marginTop: 12 }}>Chargement des archives…</p>
          </div>
        ) : archives.length === 0 ? (
          <div style={{ textAlign: "center", padding: "40px", color: "#94a3b8" }}>
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" style={{ marginBottom: 12 }}>
              <polyline points="21 8 21 21 3 21 3 8" stroke="currentColor" strokeWidth="1.5" />
              <rect x="1" y="3" width="22" height="5" rx="1" stroke="currentColor" strokeWidth="1.5" />
            </svg>
            <p>Aucune archive disponible</p>
          </div>
        ) : (
          <table className="arch-table">
            <thead>
              <tr>
                <th>Type d'entretien</th>
                <th>Matricule</th>
                <th>Nom & Prénom</th>
                <th>Date d'archivage</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {archives.map((a) => {
                const type = normalizeType(a.type);
                return (
                  <tr key={a.id}>
                    <td>
                      <span className={`arch-type-badge ${TYPE_COLORS[type]}`}>
                        {TYPE_LABELS[type]}
                      </span>
                    </td>
                    <td>{a.matricule || "–"}</td>
                    <td>{a.nomPrenom || "–"}</td>
                    <td>{formatDate(a.dateArchivage)}</td>
                    <td>
                      <span className="arch-statut-badge">
                        {a.statut || "ARCHIVE"}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: "flex", gap: "6px" }}>
                        {/* Consulter → ouvre le modal avec les données archivées */}
                        <button
                          className="arch-btn arch-btn-view"
                          onClick={() => handleConsulter(a)}
                          title="Consulter les données archivées"
                        >
                          👁 Consulter
                        </button>

                       
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
