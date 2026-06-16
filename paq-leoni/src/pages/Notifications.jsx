import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";  // ✅ Ajout de l'import manquant
import { notificationService } from "../services/api";
import { useI18n } from "../context/I18nContext";
import "../styles/Notifications.css";

export default function Notifications({ matricule: propMatricule }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("all"); // all, unread
  const navigate = useNavigate();
  const { t } = useI18n();

  useEffect(() => {
    loadNotifications();
  }, [filter]);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      let res;
      if (filter === "unread") {
        res = await notificationService.getUnread();
      } else {
        res = await notificationService.getAll();
      }
      setNotifications(res.data || []);
    } catch (err) {
      console.error("Erreur chargement notifications:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id) => {
    try {
      await notificationService.markAsRead(id);
      setNotifications(prev => 
        prev.map(n => n.id === id ? { ...n, lu: true } : n)
      );
    } catch (err) {
      console.error("Erreur:", err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, lu: true })));
    } catch (err) {
      console.error("Erreur:", err);
    }
  };

  const handleNotificationClick = (notification) => {
    if (!notification.lu) {
      handleMarkAsRead(notification.id);
    }
    
    if (notification.matriculeCollaborateur) {
      if (notification.typeEntretien === "EXPLICATIF") {
        navigate(`/entretien-explicatif/${notification.matriculeCollaborateur}`);
      } else if (notification.typeEntretien === "ACCORD") {
        navigate(`/entretien-daccord/${notification.matriculeCollaborateur}`);
      } else if (notification.typeEntretien === "MESURE") {
        navigate(`/entretien-de-mesure/${notification.matriculeCollaborateur}`);
      } else if (notification.typeEntretien === "DECISION") {
        navigate(`/entretien-de-decision/${notification.matriculeCollaborateur}`);
      } else {
        navigate(`/paq-dossier/${notification.matriculeCollaborateur}`);
      }
    }
  };

  const unreadCount = notifications.filter(n => !n.lu).length;
  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    return new Date(dateStr).toLocaleString("fr-FR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  

  const getTypeClass = (type) => {
    switch (type?.toUpperCase()) {
      case "EXPLICATIF": return "type-explicatif";
      case "ACCORD": return "type-accord";
      case "MESURE": return "type-mesure";
      case "DECISION": return "type-decision";
      default: return "type-other";
    }
  };

  return (
    <div className="notifications-page">
      <div className="notifications-header">
        <h1>{t("notifications")}</h1>
        <div className="notifications-actions">
          <div className="filter-buttons">
            <button 
              className={`filter-btn ${filter === "all" ? "active" : ""}`}
              onClick={() => setFilter("all")}
            >
              {t("all_notifications", { count: notifications.length })}
            </button>
            <button 
              className={`filter-btn ${filter === "unread" ? "active" : ""}`}
              onClick={() => setFilter("unread")}
            >
              {t("unread_notifications", { count: unreadCount })}
            </button>
          </div>
          {unreadCount > 0 && (
            <button className="mark-all-btn" onClick={handleMarkAllRead}>
              {t("mark_all_read")}
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="notifications-loading">
          <div className="spinner"></div>
          <p>{t("loading_notifications")}</p>
        </div>
      ) : notifications.length === 0 ? (
        <div className="notifications-empty">
          <svg width="60" height="60" viewBox="0 0 24 24" fill="none">
            <path d="M18 8C18 6.4087 17.3679 4.88258 16.2426 3.75736C15.1174 2.63214 13.5913 2 12 2C10.4087 2 8.88258 2.63214 7.75736 3.75736C6.63214 4.88258 6 6.4087 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z" stroke="#cbd5e1" strokeWidth="2"/>
            <path d="M13.73 21C13.5542 21.3031 13.3019 21.5547 12.9982 21.7295C12.6946 21.9044 12.3504 21.9965 12 21.9965C11.6496 21.9965 11.3054 21.9044 11.0018 21.7295C10.6982 21.5547 10.4458 21.3031 10.27 21" stroke="#cbd5e1" strokeWidth="2"/>
          </svg>
          <p>Aucune notification</p>
          <span>Vous n'avez pas encore de notifications</span>
        </div>
      ) : (
        <div className="notifications-list">
          {notifications.map((notif) => (
            <div 
              key={notif.id} 
              className={`notification-card ${!notif.lu ? "unread" : ""} ${getTypeClass(notif.typeEntretien)}`}
              onClick={() => handleNotificationClick(notif)}
            >
              
              <div className="notification-body">
                <div className="notification-title">{notif.titre}</div>
                <div className="notification-message">{notif.message}</div>
                <div className="notification-footer-info">
                  <span className="notification-date">{formatDate(notif.createdAt)}</span>
                  
                </div>
              </div>
              {!notif.lu && <div className="unread-dot"></div>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
