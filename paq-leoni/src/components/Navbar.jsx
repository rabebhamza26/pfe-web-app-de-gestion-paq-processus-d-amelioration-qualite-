// src/components/Navbar.jsx
import React from "react";
import { useAuth } from "../context/AuthContext";
import { useI18n } from "../context/I18nContext";
import NotificationBell from "./NotificationBell";
import API from "../services/api";
import "../styles/navbar.css";

function Navbar() {
  const { user, logout, loading } = useAuth();
  const { t } = useI18n();

  const handleLogout = async () => {
    try {
      await API.post("/auth/logout");
    } catch (err) {
      console.warn("Logout backend échoué:", err.message);
    } finally {
      logout();
      window.location.href = "/";
    }
  };

  const getFormattedDate = () => {
    const now = new Date();
    const jours = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];
    const mois = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];
    
    const jourSemaine = jours[now.getDay()];
    const jour = String(now.getDate()).padStart(2, '0');
    const moisNom = mois[now.getMonth()];
    const annee = now.getFullYear();
    
    return `${jourSemaine} ${jour} ${moisNom} ${annee}`;
  };

  // ✅ Afficher un placeholder ou rien pendant le chargement
  if (loading) {
    return (
      <nav className="navbar">
        <div className="navbar-left">
          <div className="navbar-logo">
            <div className="navbar-title">
              <h2>{t("app_name")}</h2>
              <span>{t("app_tagline")}</span>
            </div>
          </div>
        </div>
        <div className="navbar-center">
          <div className="navbar-date">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
            <span>{getFormattedDate()}</span>
          </div>
        </div>
      </nav>
    );
  }

  return (
    <nav className="navbar">
      <div className="navbar-left">
        <div className="navbar-logo">
          <div className="navbar-title">
            <h2>{t("app_name")}</h2>
            <span>{t("app_tagline")}</span>
          </div>
        </div>
      </div>

      <div className="navbar-center">
        <div className="navbar-date">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          <span>{getFormattedDate()}</span>
        </div>
      </div>

      <div className="navbar-right">
        {user && (
          <>
            <NotificationBell />
            <div className="navbar-user">
              <div className="navbar-avatar">
                {user.fullName?.[0] || user.login?.[0] || "U"}
              </div>
              <div className="navbar-user-meta">
                <span className="navbar-user-name">
                  {user.fullName || user.login || t("username")}
                </span>
                <span className="navbar-user-role">
                  {user.role || "ROLE"}
                </span>
              </div>
            </div>
            <button onClick={handleLogout} className="navbar-logout" title={t("logout")}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              <span>{t("logout")}</span>
            </button>
          </>
        )}
      </div>
    </nav>
  );
}

export default Navbar;