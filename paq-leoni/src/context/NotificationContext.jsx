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
  const { user, token } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connected, setConnected] = useState(false);
  const stompClient = useRef(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  const loadInitialNotifications = useCallback(async () => {
    if (!user || !token) {
      return;
    }
    try {
      const [allRes, unreadRes] = await Promise.all([
        notificationService.getAll(),
        notificationService.countUnread()
      ]);
      setNotifications(allRes.data || []);
      setUnreadCount(unreadRes.data?.count || 0);
    } catch (err) {
      console.error("Erreur chargement initial:", err);
    }
  }, [user, token]);

  const connectWebSocket = useCallback(() => {
    if (!user || !token) {
      console.log("Utilisateur non connecté, WebSocket non connecté");
      return;
    }

    const stompUrl = `${import.meta.env.VITE_API_URL || "http://localhost:8083"}/ws`;
    const socket = new SockJS(stompUrl);
    
    stompClient.current = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        'Authorization': `Bearer ${token}`
      },
      debug: (str) => {
        console.log("STOMP Debug:", str);
      },
      onConnect: () => {
        console.log("WebSocket connecté avec succès");
        setConnected(true);
        reconnectAttempts.current = 0;
        
        stompClient.current.subscribe("/user/queue/notifications", (message) => {
          console.log("Nouvelle notification reçue:", message.body);
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
      },
      onStompError: (frame) => {
        console.error("Erreur STOMP:", frame);
        setConnected(false);
      },
      onDisconnect: () => {
        console.log("WebSocket déconnecté");
        setConnected(false);
      },
      onWebSocketError: (event) => {
        console.error("WebSocket error:", event);
        setConnected(false);
      }
    });
    
    stompClient.current.activate();
  }, [user, token]);

  // Reconnexion automatique
  useEffect(() => {
    if (!connected && user && token && reconnectAttempts.current < maxReconnectAttempts) {
      const timer = setTimeout(() => {
        reconnectAttempts.current++;
        connectWebSocket();
      }, 5000 * reconnectAttempts.current);
      return () => clearTimeout(timer);
    }
  }, [connected, user, token, connectWebSocket]);

  useEffect(() => {
    if (user && token) {
      loadInitialNotifications();
      connectWebSocket();
    } else {
      setNotifications([]);
      setUnreadCount(0);
      setConnected(false);
      if (stompClient.current) {
        try {
          stompClient.current.deactivate();
        } catch (err) {
          console.error("Erreur déconnexion WebSocket:", err);
        }
      }
    }
    
    return () => {
      if (stompClient.current) {
        try {
          stompClient.current.deactivate();
        } catch (err) {
          console.error("Erreur déconnexion WebSocket:", err);
        }
      }
    };
  }, [user, token, loadInitialNotifications, connectWebSocket]);

  const markAsRead = async (id) => {
    if (!user || !token) return;
    try {
      await notificationService.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, lu: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (err) {
      console.error("Erreur marquage lu:", err);
    }
  };

  const markAllAsRead = async () => {
    if (!user || !token) return;
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, lu: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error("Erreur marquage tout lu:", err);
    }
  };

  const addNotification = (notification) => {
    setNotifications(prev => [notification, ...prev]);
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
      refreshUnreadCount: loadInitialNotifications
    }}>
      {children}
    </NotificationContext.Provider>
  );
};