// pages/QualificationDashboard.jsx
import React, { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { qualificationService } from "../services/api";
import { showErrorAlert, showSuccessToast } from "../utils/entretienAlerts";

export default function QualificationDashboard() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading]             = useState(true);
  const [forceLoading, setForceLoading]   = useState(null);
  const [filter, setFilter]               = useState("all");
  const [search, setSearch]               = useState("");
  const navigate = useNavigate();

  useEffect(() => { load(); }, [filter]);

  const load = async () => {
    setLoading(true);
    try {
      const res = filter === "pending"
        ? await qualificationService.getPending()
        : await qualificationService.getAll();
      setNotifications(res.data || []);
    } catch {
      showErrorAlert("Erreur", "Impossible de charger les qualifications");
    } finally {
      setLoading(false);
    }
  };

  const handleForcerEnvoi = async (id, matricule) => {
    setForceLoading(id);
    try {
      await qualificationService.forcerEnvoi(id);
      showSuccessToast("Notification envoyée au SL");
      load();
    } catch {
      showErrorAlert("Erreur", "Impossible de forcer l'envoi");
    } finally {
      setForceLoading(null);
    }
  };

  const displayed = useMemo(() => {
    const s = search.toLowerCase().trim();
    return notifications.filter(n =>
      !s ||
      n.matricule?.toLowerCase().includes(s) ||
      n.collaborateurNom?.toLowerCase().includes(s) ||
      n.collaborateurPrenom?.toLowerCase().includes(s) ||
      n.segment?.toLowerCase().includes(s)
    );
  }, [notifications, search]);

  // Compteurs pour les KPI cards
  const total3m    = notifications.filter(n => n.typeJalon === "3_MOIS").length;
  const total6m    = notifications.filter(n => n.typeJalon === "6_MOIS").length;
  const envoyees   = notifications.filter(n => n.envoye).length;
  const enAttente  = notifications.filter(n => !n.envoye).length;
  const urgentes   = notifications.filter(
    n => !n.envoye && n.joursAvantEnvoi >= 0 && n.joursAvantEnvoi <= 3
  ).length;

  const fmtDate = (d) => {
    if (!d) return "—";
    return new Date(d).toLocaleDateString("fr-FR");
  };

  const jalonBadge = (typeJalon) => {
    const is3 = typeJalon === "3_MOIS";
    return (
      <span style={{
        display: "inline-block",
        padding: "3px 10px",
        borderRadius: "12px",
        fontSize: "12px",
        fontWeight: 600,
        background: is3 ? "#dbeafe" : "#dcfce7",
        color:      is3 ? "#1d4ed8" : "#15803d",
      }}>
        {is3 ? "3 mois" : "6 mois"}
      </span>
    );
  };

  const statutBadge = (notif) => {
    if (notif.envoye) {
      return (
        <span style={{
          padding: "3px 10px", borderRadius: "12px",
          fontSize: "12px", fontWeight: 600,
          background: "#dcfce7", color: "#15803d",
        }}>✅ Envoyé</span>
      );
    }
    const j = notif.joursAvantEnvoi;
    if (j < 0) return (
      <span style={{
        padding: "3px 10px", borderRadius: "12px",
        fontSize: "12px", fontWeight: 600,
        background: "#fee2e2", color: "#b91c1c",
      }}>⚠ En retard</span>
    );
    if (j <= 3) return (
      <span style={{
        padding: "3px 10px", borderRadius: "12px",
        fontSize: "12px", fontWeight: 600,
        background: "#fef3c7", color: "#92400e",
      }}>⏰ Dans {j}j</span>
    );
    return (
      <span style={{
        padding: "3px 10px", borderRadius: "12px",
        fontSize: "12px", fontWeight: 600,
        background: "#f1f5f9", color: "#475569",
      }}>📅 Dans {j}j</span>
    );
  };

  return (
    <div style={{ padding: "24px", maxWidth: "1200px", margin: "0 auto" }}>

      {/* ── En-tête ─────────────────────────────────────────────────────── */}
      <div style={{
        display: "flex", alignItems: "center",
        justifyContent: "space-between", marginBottom: "24px",
        flexWrap: "wrap", gap: "12px",
      }}>
        <div>
          <h2 style={{ margin: 0, fontSize: "22px", fontWeight: 700,
                       color: "#1a3c6e" }}>
            Suivi des qualifications
          </h2>
          <p style={{ margin: "4px 0 0", color: "#64748b", fontSize: "14px" }}>
            Notifications automatiques J-2 avant les jalons 3 mois et 6 mois
          </p>
        </div>
        <button
          onClick={() => navigate(-1)}
          style={{
            padding: "8px 18px", background: "#f1f5f9",
            border: "1px solid #cbd5e1", borderRadius: "8px",
            cursor: "pointer", fontSize: "13px", color: "#475569",
          }}
        >
          ← Retour
        </button>
      </div>

      {/* ── KPI Cards ───────────────────────────────────────────────────── */}
      <div style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
        gap: "16px", marginBottom: "24px",
      }}>
        {[
          { label: "Total",       value: notifications.length, color: "#1a3c6e", bg: "#eff6ff" },
          { label: "Jalon 3 mois",value: total3m,              color: "#1d4ed8", bg: "#dbeafe" },
          { label: "Jalon 6 mois",value: total6m,              color: "#15803d", bg: "#dcfce7" },
          { label: "Envoyées",    value: envoyees,             color: "#15803d", bg: "#f0fdf4" },
          { label: "En attente",  value: enAttente,            color: "#92400e", bg: "#fffbeb" },
          { label: "Urgentes ≤3j",value: urgentes,             color: "#b91c1c", bg: "#fef2f2" },
        ].map((k) => (
          <div key={k.label} style={{
            background: k.bg, borderRadius: "12px",
            padding: "18px 20px", border: `1px solid ${k.color}22`,
          }}>
            <div style={{
              fontSize: "28px", fontWeight: 700, color: k.color,
            }}>{k.value}</div>
            <div style={{
              fontSize: "12px", color: "#64748b", marginTop: "4px",
            }}>{k.label}</div>
          </div>
        ))}
      </div>

      {/* ── Filtres ─────────────────────────────────────────────────────── */}
      <div style={{
        display: "flex", gap: "12px", marginBottom: "20px",
        flexWrap: "wrap", alignItems: "center",
      }}>
        <div style={{ display: "flex", gap: "8px" }}>
          {[
            { key: "all",     label: "Toutes" },
            { key: "pending", label: "En attente" },
          ].map(f => (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              style={{
                padding: "7px 16px", borderRadius: "8px",
                border: "1px solid",
                borderColor: filter === f.key ? "#1a3c6e" : "#cbd5e1",
                background:  filter === f.key ? "#1a3c6e" : "#fff",
                color:       filter === f.key ? "#fff"    : "#475569",
                cursor: "pointer", fontSize: "13px", fontWeight: 500,
              }}
            >
              {f.label}
            </button>
          ))}
        </div>

        <input
          type="text"
          placeholder="Rechercher matricule, nom, segment..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            flex: 1, minWidth: "220px",
            padding: "8px 14px", borderRadius: "8px",
            border: "1px solid #cbd5e1", fontSize: "13px",
            outline: "none",
          }}
        />
        {search && (
          <button
            onClick={() => setSearch("")}
            style={{
              padding: "8px 12px", background: "#f1f5f9",
              border: "1px solid #cbd5e1", borderRadius: "8px",
              cursor: "pointer", fontSize: "13px",
            }}
          >✕</button>
        )}
      </div>

      {/* ── Tableau ─────────────────────────────────────────────────────── */}
      {loading ? (
        <div style={{ textAlign: "center", padding: "60px", color: "#64748b" }}>
          <div style={{
            width: "40px", height: "40px", border: "4px solid #e2e8f0",
            borderTop: "4px solid #1a3c6e", borderRadius: "50%",
            animation: "spin 0.8s linear infinite", margin: "0 auto 16px",
          }} />
          Chargement...
        </div>
      ) : displayed.length === 0 ? (
        <div style={{
          textAlign: "center", padding: "60px",
          background: "#f8fafc", borderRadius: "12px",
          border: "1px dashed #cbd5e1",
        }}>
          <div style={{ fontSize: "40px", marginBottom: "12px" }}>📋</div>
          <p style={{ color: "#64748b", margin: 0 }}>
            Aucune notification qualification trouvée
          </p>
        </div>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{
            width: "100%", borderCollapse: "collapse",
            fontSize: "14px", background: "#fff",
            borderRadius: "12px", overflow: "hidden",
            boxShadow: "0 1px 4px rgba(0,0,0,.08)",
          }}>
            <thead>
              <tr style={{ background: "#1a3c6e", color: "#fff" }}>
                {["Matricule", "Collaborateur", "Segment",
                  "Date embauche", "Jalon", "Date jalon",
                  "Date envoi J-2", "Statut", "Action"].map(h => (
                  <th key={h} style={{
                    padding: "12px 14px", textAlign: "left",
                    fontWeight: 600, fontSize: "12px",
                    letterSpacing: "0.03em",
                  }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {displayed.map((n, idx) => (
                <tr
                  key={n.id}
                  style={{
                    background: idx % 2 === 0 ? "#fff" : "#f8fafc",
                    borderBottom: "1px solid #e2e8f0",
                  }}
                >
                  <td style={{ padding: "12px 14px", fontWeight: 600,
                                color: "#1a3c6e", fontFamily: "monospace" }}>
                    {n.matricule}
                  </td>
                  <td style={{ padding: "12px 14px" }}>
                    {n.collaborateurNom} {n.collaborateurPrenom}
                  </td>
                  <td style={{ padding: "12px 14px", color: "#475569" }}>
                    {n.segment || "—"}
                  </td>
                  <td style={{ padding: "12px 14px", color: "#475569" }}>
                    {fmtDate(n.hireDate)}
                  </td>
                  <td style={{ padding: "12px 14px" }}>
                    {jalonBadge(n.typeJalon)}
                  </td>
                  <td style={{ padding: "12px 14px", color: "#475569" }}>
                    {fmtDate(n.dateJalon)}
                  </td>
                  <td style={{
                    padding: "12px 14px",
                    color: n.envoye ? "#64748b" : "#92400e",
                    fontWeight: n.envoye ? 400 : 600,
                  }}>
                    {fmtDate(n.dateEnvoi)}
                  </td>
                  <td style={{ padding: "12px 14px" }}>
                    {statutBadge(n)}
                  </td>
                  <td style={{ padding: "12px 14px" }}>
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        onClick={() =>
                          navigate(`/paq-dossier/${n.matricule}`)
                        }
                        style={{
                          padding: "5px 12px", borderRadius: "6px",
                          border: "1px solid #1a3c6e",
                          background: "transparent", color: "#1a3c6e",
                          cursor: "pointer", fontSize: "12px",
                        }}
                      >
                        Dossier
                      </button>
                      {!n.envoye && (
                        <button
                          onClick={() => handleForcerEnvoi(n.id, n.matricule)}
                          disabled={forceLoading === n.id}
                          style={{
                            padding: "5px 12px", borderRadius: "6px",
                            border: "none",
                            background: forceLoading === n.id
                              ? "#94a3b8" : "#f59e0b",
                            color: "#fff",
                            cursor: forceLoading === n.id
                              ? "not-allowed" : "pointer",
                            fontSize: "12px", fontWeight: 600,
                          }}
                        >
                          {forceLoading === n.id ? "..." : "Forcer envoi"}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p style={{ color: "#94a3b8", fontSize: "12px",
                      marginTop: "10px", textAlign: "right" }}>
            {displayed.length} entrée(s) affichée(s)
          </p>
        </div>
      )}

      {/* ── Légende ─────────────────────────────────────────────────────── */}
      <div style={{
        marginTop: "28px", background: "#f8fafc",
        borderRadius: "10px", padding: "18px 22px",
        border: "1px solid #e2e8f0",
      }}>
        <p style={{ margin: "0 0 10px", fontWeight: 600,
                    fontSize: "13px", color: "#475569" }}>
          Fonctionnement des notifications automatiques
        </p>
        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
          gap: "10px", fontSize: "13px", color: "#64748b",
        }}>
          <div>⏰ <strong>J-2 avant 3 mois</strong> : mail SL pour test qualification</div>
          <div>⏰ <strong>J-2 avant 6 mois</strong> : mail SL pour test qualification</div>
          <div>📬 Scheduler déclenché chaque jour à <strong>08h00</strong></div>
          <div>🔁 Bouton <strong>Forcer envoi</strong> pour tester immédiatement</div>
        </div>
      </div>
    </div>
  );
}