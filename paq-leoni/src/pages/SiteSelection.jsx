import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { siteService } from "../services/api";
import { useI18n } from "../context/I18nContext";
import { useSelection } from "../context/SelectionContext";
import "../styles/site-selection.css";
import logo from "../assets/logo.jpg"; 

export default function SiteSelection() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [sites, setSites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { setSelectedSite, setSelectedPlant } = useSelection(); 


  useEffect(() => { loadSites(); }, []);

   const handleSiteSelect = (site) => {
    setSelectedSite(site); // Stocker le site sélectionné
    setSelectedPlant(null); // Réinitialiser le plant
    navigate(`/plants/${site.id}`);
  };

  const loadSites = async () => {
     try {
      setLoading(true);
      console.log("Appel à siteService.getAll()...");
      const res = await siteService.getAll();
      console.log("Réponse reçue:", res);
      console.log("Sites:", res.data);
      setSites(res.data || []);
    } catch (err) {
      console.error("Erreur détaillée:", err);
      console.error("Status:", err.response?.status);
      console.error("Message:", err.response?.data);
      setError(t("site_load_error"));
    } finally {
      setLoading(false);
    }
  };

  return (

    
    <div className="ss-root">

      <div className="ss-topbar">
   
    <div className="ss-admin-access">
     
    <button
      className="ss-admin-btn"
      onClick={() => navigate("/login")} 
    >
      <svg viewBox="0 0 20 20" fill="none">
        <path d="M10 2a4 4 0 014 4v2h-8V6a4 4 0 014-4z" stroke="currentColor" strokeWidth="1.5"/>
        <rect x="3" y="8" width="14" height="10" rx="2" stroke="currentColor" strokeWidth="1.5"/>
      </svg>
      {t("admin_login")}
    </button>
  </div>
  </div>
     
 
      

      <div className="ss-content">
        {/* Header */}
        <header className="ss-hero">
        
          <h1 className="ss-hero-title">{"PAQ Web System"}</h1>
          <p className="ss-hero-sub">{"Processus d'amélioration qualité"}</p>
        </header>

        {/* Section title */}
        <div className="ss-section-header">
          <div className="ss-section-line" />
          <h2 className="ss-section-title">{t("choose_site")}</h2>
          <div className="ss-section-line" />
        </div>

        {/* Error */}
        {error && <div className="ss-error">{error}</div>}

        {/* Grid */}
        <section className="ss-grid">
          {loading
            ? Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="ss-card ss-skeleton" />
              ))
            : sites.map((site, i) => (
                <div
                  key={site.id}
                  className="ss-card"
                  style={{ animationDelay: `${i * 60}ms` }}
                  onClick={() => navigate(`/plants/${site.id}`)}
                >
                  <div className="ss-card-icon">
                    <svg viewBox="0 0 40 40" fill="none">
                      <rect x="4" y="20" width="6" height="16" rx="1" fill="currentColor" opacity=".9"/>
                      <rect x="12" y="14" width="6" height="22" rx="1" fill="currentColor" opacity=".75"/>
                      <rect x="20" y="8" width="6" height="28" rx="1" fill="currentColor" opacity=".85"/>
                      <rect x="28" y="16" width="6" height="20" rx="1" fill="currentColor" opacity=".7"/>
                      <line x1="2" y1="36" x2="38" y2="36" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                    </svg>
                  </div>
                  <div className="ss-card-body">
                    <span className="ss-card-label">{t("site")}</span>
                    <h3 className="ss-card-name">{site.name}</h3>
                  </div>
                  <div className="ss-card-arrow">
                    <svg viewBox="0 0 20 20" fill="none">
                      <path d="M4 10h12M11 5l5 5-5 5" stroke="currentColor" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </div>
                </div>
              ))}
        </section>

        {!loading && sites.length === 0 && !error && (
          <div className="ss-empty">{t("no_site_configured")}</div>
        )}
      </div>
    </div>
  );
}