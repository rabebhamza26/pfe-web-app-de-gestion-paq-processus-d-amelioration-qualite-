import React from "react";

// Carte réutilisable affichée sur le tableau de bord.
function DashboardCard({ title, value }) {
  return (
    <div className="animate-scale-in w-40 rounded-lg border-l-4 border-[#e8a020] bg-[#1a1a2e] p-5 text-center text-white shadow-md transition-all duration-300 ease-out hover:-translate-y-1 hover:scale-[1.02] hover:shadow-xl">
      {/* Titre de la carte */}
      <h4 className="mb-2 text-[11px] uppercase tracking-[0.5px] text-[#9aa3b2]">{title}</h4>

      {/* Valeur principale affichée dans la carte */}
      <h2 className="m-0 text-[28px] font-bold text-[#e8a020]">{value}</h2>
    </div>
  );
}

export default DashboardCard;