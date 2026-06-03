// src/context/AuthContext.jsx
import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true); // ✅ Ajouté pour le chargement

  // ✅ Restaurer la session au chargement de l'application
  useEffect(() => {
    const storedToken = sessionStorage.getItem("access_token");
    const storedUser = sessionStorage.getItem("user");
    
    if (storedToken && storedUser) {
      try {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
        console.log("✅ Session restaurée:", JSON.parse(storedUser));
      } catch (error) {
        console.error("Erreur lors de la restauration:", error);
        sessionStorage.removeItem("access_token");
        sessionStorage.removeItem("refresh_token");
        sessionStorage.removeItem("user");
      }
    }
    setLoading(false);
  }, []);

  // ✅ Login : stocker user + token
  const login = (userData, accessToken, refreshToken) => {
     if (userData.siteName) userData.siteName = userData.siteName.trim();
    if (userData.plantName) userData.plantName = userData.plantName.trim();
    
    setUser(userData);
    setUser(userData);
    setToken(accessToken);
    sessionStorage.setItem("access_token", accessToken);
    sessionStorage.setItem("refresh_token", refreshToken);
    sessionStorage.setItem("user", JSON.stringify(userData));
    console.log("✅ Utilisateur connecté:", userData);
  };

  // ✅ Logout : tout vider
  const logout = () => {
    setUser(null);
    setToken(null);
    sessionStorage.removeItem("access_token");
    sessionStorage.removeItem("refresh_token");
    sessionStorage.removeItem("user");
    console.log("✅ Utilisateur déconnecté");
  };

  // ✅ Méthode pour restaurer manuellement (si besoin)
  const restoreSession = () => {
    const storedToken = sessionStorage.getItem("access_token");
    const storedUser = sessionStorage.getItem("user");
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
      return true;
    }
    return false;
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout, restoreSession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth doit être utilisé à l'intérieur de AuthProvider");
  }
  return context;
}