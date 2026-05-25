import './App.css'
import { Routes, Route } from "react-router-dom";

import AppLayout from "./components/AppLayout";
import SiteSelection from "./pages/SiteSelection";
import PlantSelection from "./pages/PlantSelection";
import Login from "./pages/login/Login";

import AdminDashboard from "./pages/Admin/adminDashboard";
import Dashboard from "./pages/Dashboard";
import AddUser from "./pages/Admin/AddUser";
import EditUser from "./pages/Admin/EditUser";
import AdminLayout from "./pages/Admin/AdminLayout";
import UsersManagement from "./pages/Admin/UsersManagement";
import SegmentManagement from "./pages/Admin/SegmentManagement";
import CollaboratorManagement from "./pages/SL/CollaboratorManagement";
import AddCollaborator from "./pages/SL/AddCollaborator";
import EditCollaborator from "./pages/SL/EditCollaborator";
import PaqDossier from "./pages/SL/PaqDossier";
import EntretienExplicatif from "./pages/entretiens/EntretienExplicatif";
import EntretienDaccord from "./pages/entretiens/EntretienDaccord";
import EntretienDeMesure from "./pages/entretiens/EntretienDeMesure";
import EntretienDeDecision from "./pages/entretiens/EntretienDeDecision";
import EntretienFinal from "./pages/entretiens/EntretienFinal";
import PaqDossierIndex from "./pages/SL/PaqDossierIndex";
import EntretienPositif from "./pages/entretiens/EntretienPositif";
import Notifications from "./pages/Notifications";
import SitesManagement from "./pages/Admin/SitesManagement"; 
import PlantManagement from "./pages/Admin/PlantManagement";
import Archive from './pages/Archive';
import ScrollToTop from "./components/ScrollToTop";
import { AuthProvider } from "./context/AuthContext";
import { NotificationProvider } from "./context/NotificationContext";
import { useAuth } from "./context/AuthContext"; 
import { useI18n } from "./context/I18nContext";
import { SelectionProvider } from "./context/SelectionContext";
import ResetPassword from "./components/ResetPassword";






function App() {

   const { loading } = useAuth(); //  Récupérer l'état de chargement
   const { t } = useI18n();

  // Afficher un écran de chargement pendant la restauration
  if (loading) {
    return (
      <div className="app-loading">
        <div className="loading-spinner"></div>
        <p>{t("loading")}</p>
      </div>
    );
  }

  return (

     <SelectionProvider>
      {/* Vos routes et composants */}
    <AuthProvider>
      <NotificationProvider>

        <ScrollToTop />

        <Routes>

          <Route path="/" element={<SiteSelection />} />
          <Route path="/plants/:siteId" element={<PlantSelection />} />
          <Route path="/login" element={<Login />} />

          <Route path="/" element={<AppLayout />}>
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="collaborateurs" element={<CollaboratorManagement />} />
            <Route path="add-collaborator" element={<AddCollaborator />} />
            <Route path="edit-collaborator/:matricule" element={<EditCollaborator />} />
            <Route path="paq-dossier" element={<PaqDossierIndex />} />
            <Route path="paq-dossier/:matricule" element={<PaqDossier />} />
            <Route path="entretien-positif" element={<EntretienPositif />} />
            <Route path="entretien-explicatif/:matricule/*" element={<EntretienExplicatif niveau={1} />} />
            <Route path="entretien-daccord/:matricule" element={<EntretienDaccord niveau={2} />} />
            <Route path="entretien-de-mesure/:matricule" element={<EntretienDeMesure niveau={3} />} />
<Route path="entretien-de-decision/:matricule" element={<EntretienDeDecision niveau={4} />} />
            <Route path="entretien-final/:matricule" element={<EntretienFinal niveau={5} />} />
            <Route path="notifications" element={<Notifications />} />
            <Route path="notifications/:matricule" element={<Notifications />} />
            <Route path="archive" element={<Archive />} />
<Route path="/reset-password" element={<ResetPassword />} />


          </Route>

          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<AdminDashboard />} />
            <Route path="users" element={<UsersManagement />} />
            <Route path="segments" element={<SegmentManagement />} />
            <Route path="add-user" element={<AddUser />} />
            <Route path="edit-user/:id" element={<EditUser />} />
            <Route path="sites" element={<SitesManagement />} />
            <Route path="plants" element={<PlantManagement />} />
          </Route>

          <Route path="*" element={<SiteSelection />} />

        </Routes>

      </NotificationProvider>
    </AuthProvider>
        </SelectionProvider>

  );
}

export default App;

