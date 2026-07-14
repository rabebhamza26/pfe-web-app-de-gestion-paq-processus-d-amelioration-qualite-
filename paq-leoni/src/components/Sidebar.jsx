import React from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/sidebar.css";

function Sidebar() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const t = (key) => key;

  // Vérifier si l'utilisateur a le rôle SL
  const isSL = user?.role === "SL";

  return (
    <aside className="app-sidebar">
      <div className="sidebar-header">
      </div>
      <ul className="sidebar-menu">
        <li><NavLink to="/dashboard">{t("dashboard")}</NavLink></li>
        <li><NavLink to="/collaborateurs">{t("collaborators")}</NavLink></li>
        <li><NavLink to="/paq-dossier">{t("paq_dossier")}</NavLink></li>
        
        {/* Entretien positif - visible uniquement pour les SL */}
        {isSL && (
            <li>
                <NavLink to="/entretien-positif">
                  {t("positif")} {t("entretien")}
                </NavLink>
              </li>
        )}

        {/* ✅ Notifications - visible pour tous les utilisateurs connectés */}
         {isSL && (
          <li>
          <NavLink to="/notifications">
            {t("notifications")}
          </NavLink>
        </li>
         )}

         {isSL && (
        <li><NavLink to="/archive">{t("archive")}</NavLink></li>
        )}
      </ul>
    </aside>
  );
}

export default Sidebar;