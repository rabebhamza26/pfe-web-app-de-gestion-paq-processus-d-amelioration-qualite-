// src/components/NotificationBell.jsx
import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useNotifications } from "../context/NotificationContext";
import { notificationService } from "../services/api";
import "../styles/notification-bell.css";

function NotificationBell() {
  const { 
    notifications: contextNotifications, 
    unreadCount: contextUnreadCount, 
    markAsRead, 
    markAllAsRead,
    refreshUnreadCount,
    fetchNotifications  // ✅ Utiliser fetchNotifications au lieu de refreshNotifications
  } = useNotifications();
  
  const [isOpen, setIsOpen] = useState(false);
  const [localNotifications, setLocalNotifications] = useState([]);
  const [showAll, setShowAll] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);
  const buttonRef = useRef(null);
  const navigate = useNavigate();

  // Charger les notifications
const loadNotifications = async () => {
  try {
    // ✅ Utiliser refreshUnreadCount au lieu de refreshNotifications
    await refreshUnreadCount();
    
    // ✅ Récupérer TOUTES les notifications depuis l'API
    const res = await notificationService.getAll();
    let notifications = res.data || [];
    
    // Trier par date décroissante
    notifications.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    
    // Afficher les 10 dernières notifications
    const recentNotifications = notifications.slice(0, 10);
    setLocalNotifications(recentNotifications);
  } catch (err) {
    console.error("Erreur chargement notifications:", err);
    // Fallback: utiliser les notifications du contexte
    const recentFromContext = [...contextNotifications].sort((a, b) => 
      new Date(b.createdAt) - new Date(a.createdAt)
    ).slice(0, 10);
    setLocalNotifications(recentFromContext);
  }
};
  // Rafraîchir quand le dropdown s'ouvre
  useEffect(() => {
    if (isOpen) {
      loadNotifications();
    }
  }, [isOpen]);

  // Rafraîchir le compteur périodiquement
  useEffect(() => {
    const interval = setInterval(() => {
      refreshUnreadCount();
    }, 30000);
    return () => clearInterval(interval);
  }, [refreshUnreadCount]);

  // Gestion du clic en dehors
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target) && 
          buttonRef.current && !buttonRef.current.contains(event.target)) {
        setIsOpen(false);
        setShowAll(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleToggle = async () => {
    if (!isOpen) {
      await loadNotifications();
    } else {
      setShowAll(false);
    }
    setIsOpen(!isOpen);
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.lu) {
      await markAsRead(notification.id);
      // Mettre à jour localement
      setLocalNotifications(prev => 
        prev.map(n => n.id === notification.id ? { ...n, lu: true } : n)
      );
    }
    
    // Navigation basée sur le type d'entretien
    if (notification.matriculeCollaborateur) {
      const type = notification.typeEntretien?.toUpperCase();
      switch (type) {
        case "EXPLICATIF":
          navigate(`/entretien-explicatif/${notification.matriculeCollaborateur}`);
          break;
        case "ACCORD":
          navigate(`/entretien-daccord/${notification.matriculeCollaborateur}`);
          break;
        case "MESURE":
          navigate(`/entretien-de-mesure/${notification.matriculeCollaborateur}`);
          break;
        case "DECISION":
          navigate(`/entretien-de-decision/${notification.matriculeCollaborateur}`);
          break;
        default:
          navigate(`/paq-dossier/${notification.matriculeCollaborateur}`);
      }
    }
    
    setIsOpen(false);
  };

  const handleMarkAllReadClick = async () => {
    await markAllAsRead();
    // Mettre à jour localement
    setLocalNotifications(prev => prev.map(n => ({ ...n, lu: true })));
    await refreshUnreadCount();
  };

  const handleViewAll = () => {
    navigate("/notifications");
    setIsOpen(false);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return "À l'instant";
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    if (diffDays < 7) return `Il y a ${diffDays} j`;
    return date.toLocaleDateString("fr-FR");
  };

  const getTypeIcon = (type) => {
    const typeUpper = type?.toUpperCase();
    switch (typeUpper) {
      case "EXPLICATIF": return "📋";
      case "ACCORD": return "🤝";
      case "MESURE": return "📊";
      case "DECISION": return "⚖️";
      case "ENTRETIEN": return "📧";
      case "INFO": return "ℹ️";
      case "SUCCESS": return "✅";
      case "ERROR": return "❌";
      default: return "🔔";
    }
  };

  const getTypeColor = (type) => {
    const typeUpper = type?.toUpperCase();
    switch (typeUpper) {
      case "EXPLICATIF": return "blue";
      case "ACCORD": return "green";
      case "MESURE": return "orange";
      case "DECISION": return "red";
      case "ENTRETIEN": return "purple";
      case "SUCCESS": return "success";
      case "ERROR": return "error";
      default: return "gray";
    }
  };

  // Filtrer selon l'état showAll
  const displayedNotifications = showAll 
    ? localNotifications 
    : localNotifications.filter(n => !n.lu);

  return (
    <div className="notification-bell">
      <button 
        ref={buttonRef}
        className={`bell-button ${contextUnreadCount > 0 ? "has-notifications" : ""}`}
        onClick={handleToggle}
        title="Notifications"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M18 8C18 6.4087 17.3679 4.88258 16.2426 3.75736C15.1174 2.63214 13.5913 2 12 2C10.4087 2 8.88258 2.63214 7.75736 3.75736C6.63214 4.88258 6 6.4087 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M13.73 21C13.5542 21.3031 13.3019 21.5547 12.9982 21.7295C12.6946 21.9044 12.3504 21.9965 12 21.9965C11.6496 21.9965 11.3054 21.9044 11.0018 21.7295C10.6982 21.5547 10.4458 21.3031 10.27 21" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        {contextUnreadCount > 0 && (
          <span className="notification-badge">{contextUnreadCount > 99 ? "99+" : contextUnreadCount}</span>
        )}
      </button>

      {isOpen && (
        <div className="notification-dropdown" ref={dropdownRef}>
          <div className="notification-header">
            <h4>Notifications</h4>
            <div className="notification-header-actions">
              {localNotifications.some(n => !n.lu) && (
                <button className="mark-all-read" onClick={handleMarkAllReadClick}>
                  Tout marquer comme lu
                </button>
              )}
              <button 
                className="toggle-view"
                onClick={() => setShowAll(!showAll)}
              >
                {showAll ? "📋 Non lues" : "📄 Toutes"}
              </button>
            </div>
          </div>
          
          <div className="notification-list">
            {loading ? (
              <div className="notification-loading">
                <div className="spinner-small"></div>
                <p>Chargement...</p>
              </div>
            ) : displayedNotifications.length === 0 ? (
              <div className="notification-empty">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" strokeWidth="1.5">
                  <path d="M18 8C18 6.4087 17.3679 4.88258 16.2426 3.75736C15.1174 2.63214 13.5913 2 12 2C10.4087 2 8.88258 2.63214 7.75736 3.75736C6.63214 4.88258 6 6.4087 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z"/>
                </svg>
                <p>{showAll ? "Aucune notification" : "Aucune notification non lue"}</p>
              </div>
            ) : (
              displayedNotifications.map((notif) => (
                <div 
                  key={notif.id} 
                  className={`notification-item ${!notif.lu ? "unread" : ""}`}
                  onClick={() => handleNotificationClick(notif)}
                >
                  <div className={`notification-icon ${getTypeColor(notif.typeEntretien)}`}>
                    {getTypeIcon(notif.typeEntretien)}
                  </div>
                  <div className="notification-content">
                    <div className="notification-title">{notif.titre}</div>
                    <div className="notification-message">{notif.message}</div>
                    <div className="notification-meta">
                      <span className="notification-time">{formatDate(notif.createdAt)}</span>
                      {notif.nomCollaborateur && (
                        <span className="notification-collab">
                          👤 {notif.nomCollaborateur}
                        </span>
                      )}
                    </div>
                  </div>
                  {!notif.lu && <div className="notification-dot"></div>}
                </div>
              ))
            )}
          </div>
          
          <div className="notification-footer">
            <button className="view-all" onClick={handleViewAll}>
              Voir toutes les notifications
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default NotificationBell;