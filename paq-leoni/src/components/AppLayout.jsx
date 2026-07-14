import React from "react";
import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";
import "../styles/layout.css";

// Layout principal utilisé pour toutes les pages authentifiées.
export default function AppLayout() {
  return (
    <div className="app-layout">
      {/* Barre latérale de navigation */}
      <div className="animate-fade-in-left">
        <Sidebar />
      </div>

      <div className="app-main">
        {/* Barre de navigation supérieure */}
        <div className="animate-fade-in-down">
          <Navbar />
        </div>

        {/* Zone principale où chaque page est rendue via Outlet */}
        <div className="app-content">
          <div className="animate-fade-in-up">
            <Outlet />
          </div>
        </div>
      </div>
    </div>
  );
}