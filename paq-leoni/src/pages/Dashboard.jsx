import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { dashboardService, paqService } from "../services/api";
import { showErrorAlert } from "../utils/entretienAlerts";
import { useI18n } from "../context/I18nContext";
import { useSelection } from "../context/SelectionContext";
import "../styles/dashboard.css";

export default function Dashboard() {
  const navigate = useNavigate();
  const { selectedSite, selectedPlant } = useSelection();
  const { t } = useI18n();

  const [loading, setLoading] = useState(true);
  const [exportLoading, setExportLoading] = useState(false);
  
  const [stats, setStats] = useState({
    totalCollaborateurs: 0,
    sansFaute: [],
    paqParNiveau: {},
    paqEnCours: [],
  });

  const [entretienTotals, setEntretienTotals] = useState({
    explicatif: 0,
    accord: 0,
    mesure: 0,
    decision: 0,
    final: 0,
    total: 0
  });
  
  const [entretienEvolution, setEntretienEvolution] = useState([]);
  const [trendPercentage, setTrendPercentage] = useState(0);
  const [maxEvolutionValue, setMaxEvolutionValue] = useState(1);

  useEffect(() => {
    loadDashboard();
  }, [selectedSite, selectedPlant]);

 const loadDashboard = async () => {
  try {
    setLoading(true);
    const params = {};
    if (selectedSite?.id) params.siteId = selectedSite.id;
    if (selectedPlant?.id) params.plantId = selectedPlant.id;

    console.log("Chargement dashboard avec filtres:", params);

    const [
      statsRes,
      paqRes,
      entretienTotalsRes,
      entretienEvolutionRes
    ] = await Promise.all([
      dashboardService.getStats(params).catch(err => ({ data: {} })),
      paqService.getAll(params).catch(err => ({ data: [] })),
      dashboardService.getEntretiensTotals(params).catch(err => ({ data: {} })),
      dashboardService.getEntretiensEvolution(params).catch(err => {
        console.warn("Erreur évolution:", err);
        return { data: null };
      }),
    ]);

    // Traitement des PAQs
    const paqs = paqRes.data || [];
    const paqActifs = paqs.filter(p => p.statut !== "CLOTURE" && p.statut !== "ARCHIVE");
    const paqParNiveau = {};
    paqs.forEach(p => {
      const niveau = p.niveau || 1;
      paqParNiveau[niveau] = (paqParNiveau[niveau] || 0) + 1;
    });

    setStats({
      totalCollaborateurs: statsRes.data?.totalCollaborateurs || 0,
      paqEnCours: paqActifs,
      paqParNiveau,
      sansFaute: statsRes.data?.sansFaute || [],
    });

    // Traitement des totaux
    const totals = entretienTotalsRes.data || {};
    const totalEntretiens = (totals.explicatif || 0) + (totals.accord || 0) + 
                           (totals.mesure || 0) + (totals.decision || 0) + (totals.final || 0);
    
    setEntretienTotals({
      explicatif: totals.explicatif || 0,
      accord: totals.accord || 0,
      mesure: totals.mesure || 0,
      decision: totals.decision || 0,
      final: totals.final || 0,
      total: totalEntretiens
    });
    
    // ===== CORRECTION IMPORTANTE =====
    // Traitement des données d'évolution - Gestion multiple des structures
    let evolutionData = [];
    
    console.log("=== DÉBOGAGE RÉPONSE ÉVOLUTION ===");
    console.log("entretienEvolutionRes complet:", entretienEvolutionRes);
    console.log("entretienEvolutionRes.data:", entretienEvolutionRes?.data);
    console.log("Type de entretienEvolutionRes:", typeof entretienEvolutionRes);
    
    // Vérifier différentes structures possibles
    if (entretienEvolutionRes?.data && Array.isArray(entretienEvolutionRes.data)) {
      // Structure: { data: [...] }
      evolutionData = entretienEvolutionRes.data;
      console.log("✅ Structure 1: data est un tableau");
    } 
    else if (entretienEvolutionRes?.data && typeof entretienEvolutionRes.data === 'object') {
      // Structure: { data: { ... } } - peut-être un objet avec des propriétés
      console.log("⚠️ Structure 2: data est un objet", entretienEvolutionRes.data);
      // Essayer de convertir l'objet en tableau
      if (entretienEvolutionRes.data.content && Array.isArray(entretienEvolutionRes.data.content)) {
        evolutionData = entretienEvolutionRes.data.content;
      } else if (entretienEvolutionRes.data.items && Array.isArray(entretienEvolutionRes.data.items)) {
        evolutionData = entretienEvolutionRes.data.items;
      }
    }
    else if (Array.isArray(entretienEvolutionRes)) {
      // Structure: directement un tableau
      evolutionData = entretienEvolutionRes;
      console.log("✅ Structure 3: réponse directe est un tableau");
    }
    
   
    
    console.log("=== DONNÉES FINALES ===");
    console.log("evolutionData:", evolutionData);
    console.log("Nombre de mois:", evolutionData.length);
    
    setEntretienEvolution(evolutionData);
    
    // Calcul de la tendance et de la valeur max
    if (evolutionData.length > 0) {
      const values = evolutionData.map(item => {
        const count = typeof item.count === 'number' ? item.count : Number(item.count) || 0;
        console.log(`- ${item.periode}: ${count}`);
        return count;
      });
      const maxVal = Math.max(...values, 1);
      setMaxEvolutionValue(maxVal);
      console.log("Valeur max pour échelle:", maxVal);
      
      if (evolutionData.length >= 2) {
        const firstValue = values[0];
        const lastValue = values[values.length - 1];
        if (firstValue > 0) {
          const change = ((lastValue - firstValue) / firstValue) * 100;
          setTrendPercentage(Math.abs(Math.round(change)));
          console.log(`Tendance: ${firstValue} → ${lastValue} (${change}%)`);
        } else {
          setTrendPercentage(12);
        }
      } else {
        setTrendPercentage(12);
      }
    }

  } catch (error) {
    console.error("Erreur chargement dashboard", error);
    showErrorAlert("Erreur", "Impossible de charger les statistiques");
  } finally {
    setLoading(false);
  }
};

  const handleExport = async (format) => {
    setExportLoading(true);
    try {
      const params = {};
      if (selectedSite?.id) params.siteId = selectedSite.id;
      if (selectedPlant?.id) params.plantId = selectedPlant.id;

      const response = await dashboardService.exportReport(format, params);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `rapport-semestriel.${format}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error("Erreur export", error);
      showErrorAlert("Export impossible", "Erreur lors de l'export.");
    } finally {
      setExportLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>{t("loading_dashboard")}</p>
      </div>
    );
  }

  const levelColors = { 1: "#3b82f6", 2: "#16a34a", 3: "#f59e0b", 4: "#ef4444", 5: "#64748b" };

  const levelCounts = [1, 2, 3, 4, 5].map(level => stats.paqParNiveau[level] || 0);
  const totalPaq = levelCounts.reduce((sum, v) => sum + v, 0);
  let donutGradient = "conic-gradient(#e5e7eb 0deg, #e5e7eb 360deg)";
  if (totalPaq > 0) {
    let currentAngle = 0;
    const segments = levelCounts.map((value, index) => {
      const level = index + 1;
      const angle = (value / totalPaq) * 360;
      const start = currentAngle;
      const end = currentAngle + angle;
      currentAngle = end;
      return `${levelColors[level]} ${start}deg ${end}deg`;
    });
    donutGradient = `conic-gradient(${segments.join(", ")})`;
  }

  const getFilterLabel = () => {
    if (selectedPlant?.name) return ` — ${selectedPlant.name}`;
    if (selectedSite?.name) return ` — ${selectedSite.name}`;
    return "";
  };

  // Ordre des mois pour l'affichage
  
  // Créer un map des données d'évolution par mois
  const evolutionMap = {};
  entretienEvolution.forEach(item => {
    if (item.periode && item.count !== undefined) {
      evolutionMap[item.periode] = item.count;
    }
  });
  

  // S'assurer que tous les mois sont présents

  const chartData = entretienEvolution.map(item => ({
  month: item.periode,
  value: item.count || 0
}));
console.log("Chart data final:", chartData);


  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div>
          <h2>
            Tableau de bord PAQ
            {getFilterLabel()}
          </h2>
          {(selectedSite || selectedPlant) && (
            <p className="filter-info text-muted">
              <i className="fas fa-filter me-1"></i>
              Statistiques filtrées par {selectedPlant ? "plant" : "site"} :{" "}
              <strong>{selectedPlant?.name || selectedSite?.name}</strong>
            </p>
          )}
        </div>
        <div className="dashboard-actions">
          <button className="btn btn-primary" onClick={() => navigate("/paq-dossier")}>
            Ouvrir dossier PAQ
          </button>
          <button className="btn btn-outline-success" onClick={() => handleExport("pdf")} disabled={exportLoading}>
            {exportLoading ? "Export..." : "Export PDF"}
          </button>
          <button className="btn btn-outline-success" onClick={() => handleExport("excel")} disabled={exportLoading}>
            {exportLoading ? "Export..." : "Export Excel"}
          </button>
        </div>
      </div>

      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-label">Total collaborateurs</div>
          <div className="stat-value text-primary">{stats.totalCollaborateurs}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Sans faute</div>
          <div className="stat-value text-success">{stats.sansFaute.length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">PAQ en cours</div>
          <div className="stat-value text-warning">{stats.paqEnCours.length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Total entretiens</div>
          <div className="stat-value text-info">{entretienTotals.total}</div>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="dashboard-main">
          
          <section className="panel">
            <div className="panel-header">
              <h5>Répartition des collaborateurs par niveau PAQ</h5>
              <span className="panel-meta">Total dossiers: {totalPaq}</span>
            </div>
            <div className="level-layout">
              <div className="level-bars">
                {[1, 2, 3, 4, 5].map((niveau, index) => {
                  const value = levelCounts[index];
                  const width = totalPaq > 0 ? Math.round((value / totalPaq) * 100) : 0;
                  return (
                    <div key={niveau} className="level-row">
                      <div className="level-title">
                        <span className="level-dot" style={{ background: levelColors[niveau] }} />
                        Niveau {niveau}
                      </div>
                      <div className="level-bar">
                        <div className="level-fill" style={{ width: `${width}%`, background: levelColors[niveau] }} />
                      </div>
                      <div className="level-value">{value}</div>
                    </div>
                  );
                })}
              </div>
              <div className="level-donut">
                <div className="donut" style={{ background: donutGradient }}>
                  <div className="donut-center">
                    <div className="donut-total">{totalPaq}</div>
                    <div className="donut-label">Dossiers PAQ</div>
                  </div>
                </div>
                <div className="donut-legend">
                  {[1, 2, 3, 4, 5].map(niveau => (
                    <div key={niveau} className="legend-item">
                      <span className="legend-dot" style={{ background: levelColors[niveau] }} />
                      Niveau {niveau}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {/* Section Évolution des entretiens - Version corrigée */}
          <section className="panel evolution-panel">
            <div className="panel-header">
              <h5>Évolution des entretiens (6 derniers mois)</h5>
              
            </div>
            
            <div className="evolution-chart-new">
              <div className="chart-header">
                <div className="chart-legend">
                  <span className="legend-dot" style={{ backgroundColor: "#C8102E" }}></span>
                  <span className="legend-label">Nombre d'entretiens</span>
                </div>
              </div>
              
              <div className="bars-container">
                {chartData.map((item, idx) => {
                  const barHeight = maxEvolutionValue > 0 ? (item.value / maxEvolutionValue) * 100 : 0;
                  
                  return (
                    <div key={idx} className="bar-item">
                      <div className="bar-wrapper">
                        <div 
                          className="bar" 
                          style={{ 
                            height: `${barHeight}%`,
                            backgroundColor: "#C8102E"
                          }}
                        >
                          <span className="bar-value">{item.value}</span>
                        </div>
                      </div>
                      <div className="bar-label">{item.month}</div>
                    </div>
                  );
                })}
              </div>
              
              <div className="chart-footer">
                <span className="footer-trend"> Tendance baissière</span>
                <span className="footer-percentage">-{trendPercentage}%</span>
              </div>
            </div>
          </section>

          <section className="panel compact">
            <div className="panel-header">
              <h5>Dossiers PAQ en cours</h5>
              <span className="panel-meta">{stats.paqEnCours.length}</span>
            </div>
            <div className="table-responsive">
              <table className="table dashboard-table compact-table">
                <thead>
                  <tr><th>Matricule</th><th>Niveau</th><th>Date création</th></tr>
                </thead>
                <tbody>
                  {stats.paqEnCours.length > 0 ? (
                    stats.paqEnCours.slice(0, 5).map((p, idx) => (
                      <tr key={p.id ?? p.collaboratorMatricule ?? idx}>
                        <td data-label="Matricule">{p.collaboratorMatricule}</td>
                        <td data-label="Niveau"><span className="badge-level">Niveau {p.niveau}</span></td>
                        <td data-label="Date création">{p.createdAt ? new Date(p.createdAt).toLocaleDateString("fr-FR") : "-"}</td>
                      </tr>
                    ))
                  ) : (
                    <tr><td colSpan="3" className="empty-table">Aucun dossier PAQ en cours</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="panel compact">
            <div className="panel-header">
              <h5>Collaborateurs sans faute</h5>
              <span className="panel-meta">{stats.sansFaute.length}</span>
            </div>
            <div className="table-responsive">
              <table className="table dashboard-table compact-table">
                <thead>
                  <tr><th>Matricule</th><th>Nom complet</th><th>Segment</th></tr>
                </thead>
                <tbody>
                  {stats.sansFaute.length > 0 ? (
                    stats.sansFaute.slice(0, 5).map((c, idx) => (
                      <tr key={c.id ?? c.matricule ?? idx}>
                        <td data-label="Matricule">{c.matricule}</td>
                        <td data-label="Nom complet">{c.nom} {c.prenom || ""}</td>
                        <td data-label="Segment"><span className="badge-segment">{c.segment}</span></td>
                      </tr>
                    ))
                  ) : (
                    <tr><td colSpan="3" className="empty-table">Aucun collaborateur sans faute</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}