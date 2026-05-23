// src/services/api.js
import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8083";

// ✅ Créez l'instance avec le nom 'API' (majuscules) pour correspondre à vos services
const API = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Intercepteur avec logs pour débogage
API.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("access_token");
  console.log(`📤 ${config.method?.toUpperCase()} ${config.url}`);
  console.log(`🔑 Token présent: ${!!token}`);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    console.log(`✅ Authorization: Bearer ${token.substring(0, 20)}...`);
  } else {
    console.warn(`⚠️ Pas de token pour ${config.url}`);
  }
  return config;
}, (error) => {
  console.error("❌ Erreur intercepteur:", error);
  return Promise.reject(error);
});

API.interceptors.response.use(
  (res) => {
    console.log(`✅ Réponse ${res.config.url}:`, res.status);
    return res;
  },
  (error) => {
    console.error(`❌ Erreur ${error.config?.url}:`, error.response?.status, error.response?.data);
    if (error.response?.status === 401) {
      console.warn("Token expiré, redirection vers login");
      sessionStorage.clear();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);


// ------------------ dashboard Service ------------------
export const dashboardService = {

    getStats: (params) => API.get('/api/dashboard/stats', { params }),
    getEntretiensTotals: (params) => API.get('/api/dashboard/entretiens/totals', { params }),
    getEntretiensEvolution: (params) => API.get('/api/dashboard/entretiens/evolution', { params }),
    exportReport: (format, params) => API.get(`/api/dashboard/export/${format}`, { params, responseType: 'blob' })
};


  



// ------------------ Auth Service ------------------
export const authService = {
  login: (credentials) => API.post("/api/auth/login", credentials),
  logout: () => API.post("/api/auth/logout"),
   forgotPassword: (data) => API.post("/api/auth/forgot-password", data), // Accepte { email, login }
  adminResetPassword: (data) => API.post("/api/auth/admin/reset-password", data), // Accepte { userId, newPassword }
  getUsers: () => API.get("/api/auth/admin/users"),
};

// ------------------ Site Service ------------------
export const siteService = {
  getAll: () => API.get("/api/sites"),
  getById: (id) => API.get(`/api/sites/${id}`),
  create: (data) => API.post("/api/sites", data),
  update: (id, data) => API.put(`/api/sites/${id}`, data),
  delete: (id) => API.delete(`/api/sites/${id}`),
};

// ------------------ Plant Service ------------------
export const plantService = {
  getAll: () => API.get("/api/plants"),
  getBySite: (siteId) => API.get(`/api/plants/site/${siteId}`),
  getById: (id) => API.get(`/api/plants/${id}`),
  create: (data) => API.post("/api/plants", data),
  update: (id, data) => API.put(`/api/plants/${id}`, data),
  delete: (id) => API.delete(`/api/plants/${id}`),
};

// ------------------ User Service ------------------
export const userService = {
   getAllUsers: () => API.get("/api/users"),
  getUserById: (id) => API.get(`/api/users/${id}`),
  createUser: (data) => API.post("/api/users", data),
  updateUser: (id, data) => API.put(`/api/users/${id}`, data),
  deleteUser: (id) => API.delete(`/api/users/${id}`),
  toggleActive: (id) => API.patch(`/api/users/${id}/toggle-active`),
 getAllEmails: () => API.get("/api/users/emails"), 
  getSlEmails: () => API.get("/api/users/sl/emails"),
  getUsersBySite: () => API.get("/api/users/by-site"),
    
      resetPassword: (userId, data) => API.patch(`/api/users/${userId}/reset-password`, data),
  resetPasswordByAdmin: (userId, newPassword) => 
    API.patch(`/api/users/${userId}/reset-password-admin`, { newPassword }),

   // Récupérer les emails par Site et Plant
  getEmailsBySiteAndPlant: (siteId, plantId) => 
    API.get(`/api/users/emails/by-site-plant?siteId=${siteId}&plantId=${plantId}`),
  
  // Récupérer les emails par Site
  getEmailsBySite: (siteId) => 
    API.get(`/api/users/emails/by-site?siteId=${siteId}`),
  
  // Récupérer les emails par Plant
  getEmailsByPlant: (plantId) => 
    API.get(`/api/users/emails/by-plant?plantId=${plantId}`),
  
  // Récupérer les emails par plusieurs Sites et Plants
  getEmailsBySitesAndPlants: (siteIds, plantIds) => 
    API.post(`/api/users/emails/by-sites-plants`, { siteIds, plantIds }),
};

// ------------------ Segment Service ------------------
export const getSegments = () => API.get("/api/segments");
export const createSegment = (segment) => API.post("/api/segments", segment);
export const updateSegment = (id, segment) => API.put(`/api/segments/${id}`, segment);
export const deleteSegment = (id) => API.delete(`/api/segments/${id}`);

export const getSegmentsBySite = (siteId) => {
    return API.get(`/segments/site/${siteId}`);
};

export const getSegmentsByPlant = (plantId) => {
    return API.get(`/segments/plant/${plantId}`);
};

export const getSegmentsBySiteAndPlant = (siteId, plantId) => {
    return API.get(`/segments/site/${siteId}/plant/${plantId}`);
};

// ------------------ Collaborateur Service ------------------
export const collaboratorService = {
  /**
   * Récupère tous les collaborateurs visibles par le user connecté.
   * @param {Object} options  - { siteId, plantId } optionnels (sélection courante)
   */
  getAll: async ({ siteId, plantId } = {}) => {
    try {
      // Construction des query params
      const params = {};
      if (plantId) params.plantId = plantId;
      else if (siteId) params.siteId = siteId;
 
      return await API.get("/api/collaborators/view", { params });
    } catch (err) {
      console.warn("Erreur /api/collaborators/view :", err);
      return { data: [] };
    }
  },
 
  getById: async (matricule) => {
    try {
      return await API.get(`/api/collaborators/${matricule}`);
    } catch (err) {
      try {
        return await API.get(`/api/collaborators/view/${matricule}`);
      } catch (err2) {
        const listRes = await API.get("/api/collaborators/view");
        const list = Array.isArray(listRes.data) ? listRes.data : listRes.data?.data || [];
        const found =
          list.find((c) => String(c.matricule) === String(matricule)) ||
          list.find((c) => String(c.id ?? c._id) === String(matricule));
        if (found) return { data: found };
        throw err2;
      }
    }
  },
 
  create: (data) => API.post("/api/collaborators", data),
  update: (matricule, data) => API.put(`/api/collaborators/${matricule}`, data),
  delete: (matricule) => API.delete(`/api/collaborators/${matricule}`),
};

// ------------------ PAQ Service ------------------
export const paqService = {
  getByMatricule: (matricule) => API.get(`/api/paq/${matricule}`),
  getAllByMatricule: (matricule) => API.get(`/api/paq/history/${matricule}`),
  getAll: () => API.get("/api/paq/all"),
  getHistory: (matricule, fromDate) => API.get(`/api/paq/${matricule}/history`, { params: { fromDate } }),
  create: (matricule) => API.post(`/api/paq/create/${matricule}`),
  createPremierEntretien: (matricule, data) => API.post(`/api/entretiens/${matricule}`, data),
  createDeuxiemeEntretien: (matricule, data) => API.post(`/api/paq/${matricule}/deuxieme-entretien`, data),
  createTroisiemeEntretien: (matricule, data) => API.post(`/api/paq/${matricule}/troisieme-entretien`, data),
  createQuatriemeEntretien: (matricule, data) => API.post(`/api/paq/${matricule}/quatrieme-entretien`, data),
  createCinquiemeEntretien: (matricule, data) => API.post(`/api/paq/${matricule}/cinquieme-entretien`, data),
  enregistrerFaute: (matricule, data) => API.post(`/api/paq/${matricule}/faute`, data),
  upgradeNiveau: (matricule) => API.post(`/api/paq/${matricule}/upgrade`),
  archive: (matricule) => API.post(`/api/paq/${matricule}/archive`),
};

// ------------------ Entretien Decision Service ------------------
export const entretienDecisionService = {
  create: (matricule, data) => API.post(`/api/entretiens-decision/${matricule}`, data),
  update: (matricule, id, data) => API.put(`/api/entretiens-decision/${matricule}/${id}`, data),
  updateWithNotification: (matricule, id, data) =>
    API.put(`/api/entretiens-decision/${matricule}/${id}`, data),

  // ✅ SL valide (envoi email à HP, SGL et QM_PLANT)
  validerParSL: (matricule, id, data) =>
    API.post(`/api/entretiens-decision/${matricule}/${id}/valider-sl`, data),

  // ✅ HP/SGL valident (1ère validation)
  valider1: (matricule, id, data) =>
    API.post(`/api/entretiens-decision/${matricule}/${id}/valider1`, data),

  // ✅ QM_PLANT valide (2ème validation)
  valider2: (matricule, id, data) =>
    API.post(`/api/entretiens-decision/${matricule}/${id}/valider2`, data),

  getByMatricule: (matricule) => API.get(`/api/entretiens-decision/matricule/${matricule}`),
  getById: (id) => API.get(`/api/entretiens-decision/${id}`),
  delete: (id) => API.delete(`/api/entretiens-decision/${id}`),
  deleteWithNotification: (matricule, id, destinataireEmail, nomCollab) =>
    API.delete(`/api/entretiens-decision/${matricule}/${id}`, {
      data: { destinataireEmail, nomCollab },
    }),
};

// ------------------ Entretien D'accord Service ------------------
export const entretienDaccordService = {
  create: (matricule, data) =>
    API.post(`/api/entretiens-daccord/${matricule}`, data),
 
  update: (matricule, id, data) =>
    API.put(`/api/entretiens-daccord/${matricule}/${id}`, data),
 
  // SL soumet pour validation (envoi email)
  validerPremiere: (matricule, id, data) =>
    API.post(`/api/entretiens-daccord/${matricule}/${id}/valider-premiere`, data),
 
  // QM_SEGMENT valide finalement (sans email) - met à jour le PAQ
  validerFinale: (matricule, id, data) =>
    API.post(`/api/entretiens-daccord/${matricule}/${id}/valider`, data),
 
  getByMatricule: (matricule) =>
    API.get(`/api/entretiens-daccord/matricule/${matricule}`),
 
  getById: (id) =>
    API.get(`/api/entretiens-daccord/${id}`),
};
// ------------------ Entretien Explicatif Service ------------------
const API_URL = "/api/entretiens";

export const entretienService = {
 create: (matricule, data) => API.post(`/api/entretiens/${matricule}?niveau=1`, data),
  update: (matricule, id, data) => API.put(`/api/entretiens/${matricule}/${id}?niveau=1`, data),
  getByMatricule: (matricule) => API.get(`/api/entretiens/matricule/${matricule}`),
  getById: (id) => API.get(`/api/entretiens/${id}`),
  validate: (id) => API.post(`/api/entretiens/${id}/validate`),

  
};

// ------------------ Entretien Final Service ------------------
export const entretienFinalService = {
  create: (matricule, data) => API.post(`/api/entretien-final/${matricule}`, data),
  update: (matricule, id, data) => API.put(`/api/entretien-final/${matricule}/${id}`, data),
 
  valider: (matricule, id, data) =>
    API.post(`/api/entretien-final/${matricule}/${id}/valider`, data),
  getByMatricule: (matricule) => API.get(`/api/entretien-final/${matricule}`),
  getById: (id) => API.get(`/api/entretien-final/${id}`),  
};

// ------------------ Entretien Mesure Service ------------------
export const entretienMesureService = {
  create: (matricule, data) => API.post(`/api/entretiens-mesures/${matricule}`, data),
  update: (matricule, id, data) => API.put(`/api/entretiens-mesures/${matricule}/${id}`, data),
  
  // SL soumet pour validation (envoi email à QM_SEGMENT et SGL)
  validerPremiere: (matricule, id, data) =>
    API.post(`/api/entretiens-mesures/${matricule}/${id}/valider-premiere`, data),
  
  // QM_SEGMENT 1ère validation
  valider1: (matricule, id, data) =>
    API.post(`/api/entretiens-mesures/${matricule}/${id}/valider1`, data),
  
  // SGL 2ème validation
  valider2: (matricule, id, data) =>
    API.post(`/api/entretiens-mesures/${matricule}/${id}/valider2`, data),
  
  getByMatricule: (matricule) => API.get(`/api/entretiens-mesures/matricule/${matricule}`),
  getById: (id) => API.get(`/api/entretiens-mesures/${id}`),
  
  deleteWithNotification: (matricule, id, destinataireEmail, nomCollab) =>
    API.delete(`/api/entretiens-mesures/${matricule}/${id}`, {
      data: { destinataireEmail, nomCollab }
    }),
  
  
};
// ------------------ Entretien Positif Service ------------------
export const entretienPositifService = {

    getSansFaute: () => API.get("/api/entretiens-positifs/sans-faute"),
  envoyerAuSL: (payload) => API.post("/api/entretiens-positifs/envoyer-sl", payload),
  archiverEtCreer: (payload) => API.post("/api/entretiens-positifs/archiver", payload),
  exportPdf: () => API.get("/api/entretiens-positifs/export-pdf", { responseType: "blob" }),
  // Emails SL depuis la BD (nécessite auth SL)
  getSlEmails: () => API.get("/api/entretiens-positifs/public/emails"),
};

// ------------------ Notification Service ------------------
export const notificationService = {
  getAll: () => API.get("/api/notifications"),
  create: (data) => API.post("/api/notifications", data),
  delete: (id) => API.delete(`/api/notifications/${id}`),
  envoyerNotificationDirecte: (data) => API.post("/api/notifications/envoyer", data),
  getUnread: () => API.get("/api/notifications/unread"),
  countUnread: () => API.get("/api/notifications/count/unread"),
  markAsRead: (id) => API.post(`/api/notifications/${id}/read`),
  markAllAsRead: () => API.post("/api/notifications/mark-all-read"),
};

// ------------------ Archive Service ------------------
export const archiveService = {
  getAll: () => API.get("/api/archives"),
  getById: (id) => API.get(`/api/archives/${id}`),
  searchByMatricule: (matricule) => API.get("/api/archives/search", { params: { matricule } }),
  getByType: (type) => API.get(`/api/archives/type/${type}`),
  getByMatriculeExact: (matricule) => API.get(`/api/archives/matricule/${matricule}`),
};

// ------------------ Faute Service ------------------
export const fauteService = {
  getAll: () => API.get("/api/fautes"),
  search: (q) => API.get(`/api/fautes/search?q=${q}`),
  create: (data) => API.post("/api/fautes", data),
};

export const qualificationService = {
    getAll: () => API.get("/api/qualification"),
    getPending: () => API.get("/api/qualification/pending"),
    getByMatricule: (matricule) => API.get(`/api/qualification/collaborateur/${matricule}`),
    forcerEnvoi: (id) => API.post(`/api/qualification/${id}/forcer-envoi`),
};



export const passwordResetService = {
    /**
     * Demande de réinitialisation de mot de passe
     * @param {string} email - Adresse email de l'utilisateur
     * @returns {Promise}
     */
    forgotPassword: async (email) => {
        try {
            const response = await API.post('/api/auth/forgot-password', { email });
            return response.data;
        } catch (error) {
            console.error('Erreur forgotPassword:', error);
            throw error.response?.data || { success: false, message: 'Erreur de connexion' };
        }
    },

    /**
     * Valide un token de réinitialisation
     * @param {string} token - Token de réinitialisation
     * @returns {Promise}
     */
    validateResetToken: async (token) => {
        try {
            const response = await API.get(`/api/auth/validate-reset-token?token=${token}`);
            return response.data;
        } catch (error) {
            console.error('Erreur validateToken:', error);
            throw error.response?.data || { success: false, message: 'Token invalide' };
        }
    },

    /**
     * Réinitialise le mot de passe
     * @param {string} token - Token de réinitialisation
     * @param {string} newPassword - Nouveau mot de passe
     * @param {string} confirmPassword - Confirmation du mot de passe
     * @returns {Promise}
     */
    resetPassword: async (token, newPassword, confirmPassword) => {
        try {
            const response = await API.post('/api/auth/reset-password', {
                token,
                newPassword,
                confirmPassword
            });
            return response.data;
        } catch (error) {
            console.error('Erreur resetPassword:', error);
            throw error.response?.data || { success: false, message: 'Erreur lors de la réinitialisation' };
        }
    }
};


export default API;