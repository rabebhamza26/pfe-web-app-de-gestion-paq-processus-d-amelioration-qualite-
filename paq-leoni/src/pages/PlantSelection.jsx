import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { plantService, siteService } from "../services/api";
import { useI18n } from "../context/I18nContext";
import { useSelection } from "../context/SelectionContext";
import "../styles/plant-selection.css";


export default function PlantSelection() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const { siteId } = useParams();
    const { setSelectedSite, setSelectedPlant, selectedSite } = useSelection(); 


  const [plants, setPlants] = useState([]);
  const [siteName, setSiteName] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Charger le nom du site
  useEffect(() => {
    siteService
      .getById(siteId)
      .then((res) => setSiteName(res.data?.name || t("unknown_site")))
      .catch(() => setSiteName(t("unknown_site")));
  }, [siteId]);

  // Charger les plants FILTRÉS par siteId
  useEffect(() => {
    loadPlants();
   siteService
      .getById(siteId)
      .then((res) => {
        setSiteName(res.data?.name || t("unknown_site"));
        // Stocker le site si pas déjà fait
        if (!selectedSite) {
          setSelectedSite(res.data);
        }
      })
      .catch(() => setSiteName(t("unknown_site")));
  }, [siteId]);

  const loadPlants = async () => {
    try {
      setLoading(true);
      setError("");
      // On utilise le endpoint /plants/site/:siteId pour ne récupérer
      // que les plants appartenant à ce site
      const res = await plantService.getBySite(siteId);
      setPlants(Array.isArray(res.data) ? res.data : []);
    } catch {
      setError(t("plant_load_error"));
      setPlants([]);
    } finally {
      setLoading(false);
    }
  };

  const handlePlantSelect = (plant) => {
     setSelectedPlant(plant);
    navigate("/login", {
      state: { siteName, plantName: plant.name, siteId, plantId: plant.id },
    });
  };

  return (
    <div className="ps-root">
      

      <div className="ps-content">
        
    <header className="ps-hero">
     <h1 className="ss-hero-title">{"PAQ Web System"}</h1>
          <p className="ss-hero-sub">{"Processus d'amélioration qualité"}</p>
</header>

{/* Breadcrumb amélioré */}
<div className="ps-breadcrumb">
  <span className="ps-bc-site">{siteName || t("loading")}</span>

  <svg viewBox="0 0 16 16" fill="none" className="ps-bc-icon">
    <path d="M5 3l5 5-5 5"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>

  <span className="ps-bc-current">{t("choose_plant")}</span>
</div>

        {/* Error */}
        {error && <div className="ps-error">{error}</div>}

        {/* Grid */}
        <section className="ps-grid">
          {loading
            ? Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="ps-card ps-skeleton" />
              ))
            : plants.length === 0
            ? (
                <div className="ps-empty">
                  <svg viewBox="0 0 48 48" fill="none" width="48" height="48">
                    <circle cx="24" cy="24" r="20" stroke="currentColor" strokeWidth="1.5" opacity=".3"/>
                    <path d="M24 16v8M24 30h.01" stroke="currentColor" strokeWidth="2"
                      strokeLinecap="round"/>
                  </svg>
                  <p>{t("no_plant_available")}</p>
                </div>
              )
            : plants.map((plant, i) => (
                <div
                  key={plant.id}
                  className="ps-card"
                  style={{ animationDelay: `${i * 60}ms` }}
                  onClick={() => handlePlantSelect(plant)}
                >
                  <div className="ps-card-icon">
                    <svg viewBox="0 0 40 40" fill="none">
                      <rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor" opacity=".9"/>
                      <rect x="22" y="6" width="12" height="12" rx="2" fill="currentColor" opacity=".6"/>
                      <rect x="6" y="22" width="12" height="12" rx="2" fill="currentColor" opacity=".6"/>
                      <rect x="22" y="22" width="12" height="12" rx="2" fill="currentColor" opacity=".9"/>
                    </svg>
                  </div>
                  <div className="ps-card-body">
                    <span className="ps-card-label">{t("plant")}</span>
                    <h3 className="ps-card-name">{plant.name}</h3>
                  </div>
                  <div className="ps-card-arrow">
                    <svg viewBox="0 0 20 20" fill="none">
                      <path d="M4 10h12M11 5l5 5-5 5" stroke="currentColor" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </div>
                </div>
              ))}
        </section>
      </div>
    </div>
  );
}