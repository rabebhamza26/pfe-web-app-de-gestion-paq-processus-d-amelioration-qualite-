import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { collaboratorService, getSegments } from "../../services/api";
import "../../styles/collaborator.css";
import { useSelection } from "../../context/SelectionContext";

export default function AddCollaborator() {
    const [formData, setFormData] = useState({
        matricule: "",
        name: "",      
        prenom: "",
        segment: "",
        hireDate: "",
    });
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [loading, setLoading] = useState(false);
    const [segments, setSegments] = useState([]);
    const [allSegments, setAllSegments] = useState([]);
    const [segmentsLoading, setSegmentsLoading] = useState(true);
    const navigate = useNavigate();

    const { selectedSite, selectedPlant } = useSelection(); 

    const getSegmentLabel = (segment) =>
        segment?.nomSegment ||
        segment?.name ||
        segment?.code ||
        segment?.segment ||
        segment?.libelle ||
        String(segment ?? "");

    const getSegmentId = (segment) =>
        segment?.idSegment || segment?.id || segment?._id || getSegmentLabel(segment);

    useEffect(() => {
        // Charger tous les segments une seule fois au montage
        const loadAllSegments = async () => {
            try {
                setSegmentsLoading(true);
                const res = await getSegments();
                const list = Array.isArray(res.data) ? res.data : [];
                setAllSegments(list);
            } catch (err) {
                console.error("Erreur chargement segments:", err);
                setAllSegments([]);
            } finally {
                setSegmentsLoading(false);
            }
        };
        
        loadAllSegments();
    }, []);

    // Filtrer les segments quand la séchange (site/plant) change
    useEffect(() => {
        if (allSegments.length === 0) return;
        
        let filteredSegments = [...allSegments];
        
        if (selectedPlant && selectedPlant.id) {
            // Filtrer par plant
            filteredSegments = allSegments.filter(seg => 
                seg.plantId === selectedPlant.id || 
                seg.plant?.id === selectedPlant.id ||
                seg.plant_id === selectedPlant.id
            );
        } else if (selectedSite && selectedSite.id) {
            // Filtrer par site
            filteredSegments = allSegments.filter(seg => 
                seg.siteId === selectedSite.id || 
                seg.site?.id === selectedSite.id ||
                seg.site_id === selectedSite.id
            );
        }
        
        const sorted = [...filteredSegments].sort((a, b) =>
            getSegmentLabel(a).localeCompare(getSegmentLabel(b), "fr", { sensitivity: "base" })
        );
        setSegments(sorted);
    }, [selectedSite, selectedPlant, allSegments]);

    /**
     * Gère les changements dans les champs du formulaire
     * Validation spéciale pour le matricule (uniquement chiffres, 8 caractères)
     */
    const handleChange = (e) => {
        const { name, value } = e.target;
        
        if (name === "matricule") {
            // Uniquement des chiffres, maximum 8 caractères
            const digitsOnly = value.replace(/\D/g, "").slice(0, 8);
            setFormData({ ...formData, [name]: digitsOnly });
            return;
        }
        
        setFormData({ ...formData, [name]: value });
    };

    /**
     * Gère la soumission du formulaire
     */
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        // Validation des champs obligatoires
        if (!formData.matricule || !formData.name || !formData.prenom || 
            !formData.segment || !formData.hireDate) {
            setError("Tous les champs sont obligatoires");
            return;
        }

        // Validation du format du matricule
        if (!/^\d{8}$/.test(formData.matricule)) {
            setError("Le matricule doit contenir exactement 8 chiffres.");
            return;
        }

        // Validation de la date d'embauche
        const hireDate = new Date(formData.hireDate);
        const today = new Date();
        if (hireDate > today) {
            setError("La date d'embauche ne peut pas être dans le futur");
            return;
        }

        try {
            setLoading(true);
            
            // Construction du payload pour l'API
            const selectedSegmentObj = segments.find(seg => getSegmentLabel(seg) === formData.segment);
            
            const payload = {
                matricule: formData.matricule,
                name: formData.name.trim(),
                prenom: formData.prenom.trim(),
                segment: formData.segment,
                segmentId: selectedSegmentObj ? getSegmentId(selectedSegmentObj) : null,
                hireDate: formData.hireDate,
            };
            
            await collaboratorService.create(payload);
            const newCollabResponse = await collaboratorService.getById(formData.matricule);
            const newCollaborator = newCollabResponse.data;
            sessionStorage.setItem("latest_collaborator_matricule", String(formData.matricule));
            setSuccess("Collaborateur ajouté avec succès !");
            navigate("/collaborateurs", { state: { newCollaborator } });
            
        } catch (err) {
            const errorMessage = err.response?.data?.message || 
                                err.response?.data?.error ||
                                "Erreur lors de l'ajout du collaborateur";
            setError(errorMessage);
            console.error("Erreur:", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container py-4 collab-form-page">
            <div className="collab-form-shell">
                <div className="collab-form-header">
                    <div>
                        <h2 className="collab-form-title">Ajouter un Collaborateur</h2>
                    </div>
                </div>
                <div className="form-container card shadow-sm border-0 collab-form-card">
                    <div className="card-body p-4 p-md-5">

                        {error && (
                            <div className="alert alert-danger alert-dismissible fade show" role="alert">
                                <i className="fas fa-exclamation-triangle me-2"></i>
                                {error}
                                <button type="button" className="btn-close" onClick={() => setError("")}></button>
                            </div>
                        )}
                        
                        {success && (
                            <div className="alert alert-success alert-dismissible fade show" role="alert">
                                <i className="fas fa-check-circle me-2"></i>
                                {success}
                                <button type="button" className="btn-close" onClick={() => setSuccess("")}></button>
                            </div>
                        )}

                       

                        <form onSubmit={handleSubmit} className="collab-form-grid">
                            <div className="form-group mb-3 collab-field">
                                <label htmlFor="matricule" className="form-label">
                                    Matricule <span className="text-danger"></span>
                                </label>
                                <input
                                    type="text"
                                    id="matricule"
                                    name="matricule"
                                    value={formData.matricule}
                                    onChange={handleChange}
                                    inputMode="numeric"
                                    pattern="\d{8}"
                                    maxLength={8}
                                    className="form-control"
                                    placeholder="Entrez 8 chiffres"
                                    required
                                />
                            </div>

                            <div className="form-group mb-3 collab-field">
                                <label htmlFor="name" className="form-label">
                                    Nom <span className="text-danger"></span>
                                </label>
                                <input
                                    type="text"
                                    id="name"
                                    name="name"
                                    value={formData.name}
                                    onChange={handleChange}
                                    className="form-control"
                                    placeholder="Nom du collaborateur"
                                    required
                                />
                            </div>

                            <div className="form-group mb-3 collab-field">
                                <label htmlFor="prenom" className="form-label">
                                    Prénom <span className="text-danger"></span>
                                </label>
                                <input
                                    type="text"
                                    id="prenom"
                                    name="prenom"
                                    value={formData.prenom}
                                    onChange={handleChange}
                                    className="form-control"
                                    placeholder="Prénom du collaborateur"
                                    required
                                />
                            </div>

                            <div className="form-group mb-3 collab-field">
                                <label htmlFor="segment" className="form-label">
                                    Segment <span className="text-danger"></span>
                                </label>
                                <select
                                    id="segment"
                                    name="segment"
                                    value={formData.segment}
                                    onChange={handleChange}
                                    className="form-select"
                                    required
                                    disabled={segmentsLoading}
                                >
                                    <option value="">
                                        {segmentsLoading ? "Chargement..." : 
                                         (segments.length === 0 && !segmentsLoading ? "Aucun segment disponible" : "Sélectionner un segment")}
                                    </option>
                                    {segments.map((segment) => {
                                        const label = getSegmentLabel(segment);
                                        const key = getSegmentId(segment);
                                        return (
                                            <option key={key} value={label}>
                                                {label}
                                            </option>
                                        );
                                    })}
                                </select>
                                {selectedSite && !selectedPlant && segments.length === 0 && !segmentsLoading && (
                                    <small className="text-warning d-block mt-1">
                                        <i className="fas fa-exclamation-triangle me-1"></i>
                                        Aucun segment trouvé pour ce site
                                    </small>
                                )}
                                {selectedPlant && segments.length === 0 && !segmentsLoading && (
                                    <small className="text-warning d-block mt-1">
                                        <i className="fas fa-exclamation-triangle me-1"></i>
                                        Aucun segment trouvé pour ce plant
                                    </small>
                                )}
                               
                            </div>

                            <div className="form-group mb-4 collab-field collab-field-full">
                                <label htmlFor="hireDate" className="form-label">
                                    Date d'embauche <span className="text-danger"></span>
                                </label>
                                <input
                                    type="date"
                                    id="hireDate"
                                    name="hireDate"
                                    value={formData.hireDate}
                                    onChange={handleChange}
                                    className="form-control"
                                    required
                                />
                            </div>

                            <div className="form-buttons d-flex flex-column flex-sm-row gap-2 collab-form-actions collab-field-full">
                                <button 
                                    type="submit" 
                                    disabled={loading} 
                                    className="btn btn-primary"
                                >
                                    <span className="d-flex align-items-center justify-content-center">
                                        {loading ? (
                                            <span className="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
                                        ) : (
                                            <i className="fas fa-save me-2"></i>
                                        )}
                                        <span>{loading ? "Enregistrement..." : "Ajouter"}</span>
                                    </span>
                                </button>
                                <button 
                                    type="button" 
                                    onClick={() => navigate("/collaborateurs")} 
                                    className="btn btn-outline-secondary"
                                >
                                    <i className="fas fa-times me-2"></i>
                                    Annuler
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
}