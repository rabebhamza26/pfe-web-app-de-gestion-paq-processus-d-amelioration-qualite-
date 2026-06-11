// src/context/NotificationContext.jsx
import React, { createContext, useContext, useEffect, useState, useRef, useCallback } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { notificationService } from "../services/api";
import { useAuth } from "./AuthContext";

const NotificationContext = createContext();

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error("useNotifications must be used within NotificationProvider");
  }
  return context;
};

export const NotificationProvider = ({ children }) => {
  const { user, token, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connected, setConnected] = useState(false);
  const stompClient = useRef(null);
  const isConnecting = useRef(false);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;
  const isMounted = useRef(true);

  // ✅ Ajouter cette fonction pour rafraîchir toutes les notifications
  const refreshNotifications = useCallback(async () => {
    if (!isAuthenticated || !token) return;
    try {
      const res = await notificationService.getAll();
      if (isMounted.current) {
        setNotifications(res.data || []);
      }
    } catch (err) {
      console.error("Erreur rafraîchissement notifications:", err);
    }
  }, [isAuthenticated, token]);

  const loadInitialNotifications = useCallback(async () => {
    if (!isAuthenticated || !token) return;
    try {
      const [allRes, unreadRes] = await Promise.all([
        notificationService.getAll(),
        notificationService.countUnread()
      ]);
      if (isMounted.current) {
        setNotifications(allRes.data || []);
        setUnreadCount(unreadRes.data?.count || 0);
      }
    } catch (err) {
      console.error("Erreur chargement initial:", err);
    }
  }, [isAuthenticated, token]);

  const refreshUnreadCount = useCallback(async () => {
    if (!isAuthenticated || !token) return;
    try {
      const res = await notificationService.countUnread();
      if (isMounted.current) {
        setUnreadCount(res.data?.count || 0);
      }
    } catch (err) {
      console.error("Erreur refresh compteur:", err);
    }
  }, [isAuthenticated, token]);

  // Nettoyer et déconnecter WebSocket
  const disconnectWebSocket = useCallback(() => {
    if (stompClient.current) {
      try {
        if (stompClient.current.active) {
          stompClient.current.deactivate();
        }
      } catch (err) {
        console.error("Erreur déconnexion WebSocket:", err);
      }
      stompClient.current = null;
    }
    setConnected(false);
    isConnecting.current = false;
  }, []);

  // Connexion WebSocket
  const connectWebSocket = useCallback(() => {
    if (!isAuthenticated || !token) {
      console.log("Utilisateur non connecté, WebSocket non connecté");
      return;
    }

    // Éviter les connexions multiples simultanées
    if (isConnecting.current) {
      console.log("Connexion WebSocket déjà en cours...");
      return;
    }

    // Nettoyer l'ancienne connexion
    disconnectWebSocket();

    isConnecting.current = true;
    const stompUrl = `${import.meta.env.VITE_API_URL || "http://localhost:8083"}/ws`;
    const socket = new SockJS(stompUrl);
    
    stompClient.current = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        'Authorization': `Bearer ${token}`
      },
      debug: (str) => {
        // Désactiver les logs en production
        if (import.meta.env.DEV && str.includes("ERROR")) {
          console.log("STOMP:", str);
        }
      },
      onConnect: () => {
        if (!isMounted.current) return;
        console.log("WebSocket connecté avec succès");
        setConnected(true);
        isConnecting.current = false;
        reconnectAttempts.current = 0;
        
        // Attendre un court instant pour que la connexion soit stable
        setTimeout(() => {
          if (stompClient.current && stompClient.current.active) {
            try {
              stompClient.current.subscribe("/user/queue/notifications", (message) => {
                console.log("🔔 Nouvelle notification reçue:", message.body);
                try {
                  const newNotification = JSON.parse(message.body);
                  
                  setNotifications(prev => {
                    const exists = prev.some(n => n.id === newNotification.id);
                    if (exists) return prev;
                    return [newNotification, ...prev];
                  });
                  
                  if (!newNotification.lu) {
                    setUnreadCount(prev => prev + 1);
                  }
                } catch (err) {
                  console.error("Erreur parsing notification:", err);
                }
              });
              console.log("✅ Abonnement /user/queue/notifications réussi");
            } catch (err) {
              console.error("Erreur lors de l'abonnement:", err);
            }
          }
        }, 100);
      },
      onStompError: (frame) => {
        console.error("Erreur STOMP:", frame);
        if (isMounted.current) {
          setConnected(false);
          isConnecting.current = false;
        }
      },
      onDisconnect: () => {
        console.log("WebSocket déconnecté");
        if (isMounted.current) {
          setConnected(false);
          isConnecting.current = false;
        }
      },
      onWebSocketError: (event) => {
        console.error("WebSocket error:", event);
        if (isMounted.current) {
          setConnected(false);
          isConnecting.current = false;
        }
      }
    });
    
    stompClient.current.activate();
  }, [isAuthenticated, token, disconnectWebSocket]);

  // Reconnexion automatique
  useEffect(() => {
    if (!connected && isAuthenticated && token && reconnectAttempts.current < maxReconnectAttempts && !isConnecting.current) {
      const timer = setTimeout(() => {
        reconnectAttempts.current++;
        console.log(`Tentative de reconnexion ${reconnectAttempts.current}/${maxReconnectAttempts}`);
        connectWebSocket();
      }, 5000 * reconnectAttempts.current);
      return () => clearTimeout(timer);
    }
  }, [connected, isAuthenticated, token, connectWebSocket]);

  // Connexion initiale
  useEffect(() => {
    isMounted.current = true;
    
    if (isAuthenticated && token) {
      loadInitialNotifications();
      // Attendre que l'utilisateur soit complètement chargé
      setTimeout(() => {
        if (isMounted.current) {
          connectWebSocket();
        }
      }, 500);
    } else {
      disconnectWebSocket();
      if (isMounted.current) {
        setNotifications([]);
        setUnreadCount(0);
      }
    }
    
    return () => {
      isMounted.current = false;
      disconnectWebSocket();
    };
  }, [isAuthenticated, token, loadInitialNotifications, connectWebSocket, disconnectWebSocket]);

  const markAsRead = async (id) => {
    if (!isAuthenticated || !token) return;
    try {
      await notificationService.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, lu: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (err) {
      console.error("Erreur marquage lu:", err);
    }
  };

  const markAllAsRead = async () => {
    if (!isAuthenticated || !token) return;
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, lu: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error("Erreur marquage tout lu:", err);
    }
  };

  const addNotification = (notification) => {
    setNotifications(prev => {
      const exists = prev.some(n => n.id === notification.id);
      if (exists) return prev;
      return [notification, ...prev];
    });
    if (!notification.lu) {
      setUnreadCount(prev => prev + 1);
    }
  };

  return (
    <NotificationContext.Provider value={{
      notifications,
      unreadCount,
      connected,
      markAsRead,
      markAllAsRead,
      addNotification,
      refreshUnreadCount,
      refreshNotifications,  // ✅ EXPOSER LA FONCTION
      loadInitialNotifications  // ✅ Optionnel: aussi utile
    }}>
      {children}
    </NotificationContext.Provider>
  );
};