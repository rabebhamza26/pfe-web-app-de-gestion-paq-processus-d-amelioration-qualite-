import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { collaboratorService, getSegments } from "../../services/api";
import "../../styles/collaborator.css";
import { useSelection } from "../../context/SelectionContext";


export default function EditCollaborator() {
    const { matricule } = useParams();
        const { selectedSite, selectedPlant } = useSelection(); 

    const [formData, setFormData] = useState({
        matricule: "",
        name: "",
        prenom: "",
        segment: "",
        hireDate: "",
    });

  const [segments, setSegments] = useState([]);
const [allSegments, setAllSegments] = useState([]);

const [error, setError] = useState("");
const [success, setSuccess] = useState("");
const [loading, setLoading] = useState(true);
const [submitting, setSubmitting] = useState(false);
const [segmentsLoading, setSegmentsLoading] = useState(true);
    const navigate = useNavigate();

    const getSegmentLabel = (segment) =>
        segment?.nomSegment ||
        segment?.name ||
        segment?.code ||
        segment?.segment ||
        segment?.libelle ||
        String(segment ?? "");

    /**
     * Charge les données du collaborateur au montage
     */
    useEffect(() => {
        loadCollaborator();
    }, [matricule]);

   useEffect(() => {
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

useEffect(() => {
    if (allSegments.length === 0) return;

    let filteredSegments = [...allSegments];

    if (selectedPlant?.id) {
        filteredSegments = allSegments.filter(
            seg =>
                seg.plantId === selectedPlant.id ||
                seg.plant?.id === selectedPlant.id ||
                seg.plant_id === selectedPlant.id
        );
    } else if (selectedSite?.id) {
        filteredSegments = allSegments.filter(
            seg =>
                seg.siteId === selectedSite.id ||
                seg.site?.id === selectedSite.id ||
                seg.site_id === selectedSite.id
        );
    }

    const sorted = [...filteredSegments].sort((a, b) =>
        getSegmentLabel(a).localeCompare(
            getSegmentLabel(b),
            "fr",
            { sensitivity: "base" }
        )
    );

    setSegments(sorted);
}, [selectedSite, selectedPlant, allSegments]);

    /**
     * Récupère les informations du collaborateur à modifier
     */
    const loadCollaborator = async () => {
        try {
            setLoading(true);

            const response = await collaboratorService.getById(matricule);
const data = response.data;

console.log("Collaborateur :", data);
console.log("Segment reçu :", data.segment);
            
            setFormData({
                matricule: data.matricule,
                name: data.name || "",
                prenom: data.prenom || "",
                segment:
    data.segment?.nomSegment ||
    data.segment?.name ||
    data.segment?.code ||
    data.segment?.segment ||
    data.segment?.libelle ||
    data.segment ||
    "",
                hireDate: data.hireDate ? data.hireDate.slice(0, 10) : "",
            });
        } catch (err) {
            console.error("Erreur:", err);
            setError("Impossible de charger les informations du collaborateur");
        } finally {
            setLoading(false);
        }
    };

    
    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

   
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        // Validation des champs obligatoires
        if (!formData.name || !formData.prenom || !formData.segment || !formData.hireDate) {
            setError("Tous les champs sont obligatoires");
            return;
        }

        // Validation de la date
        const hireDate = new Date(formData.hireDate);
        const today = new Date();
        if (hireDate > today) {
            setError("La date d'embauche ne peut pas être dans le futur");
            return;
        }

        try {
            setSubmitting(true);
            
            const payload = {
                matricule: formData.matricule,
                name: formData.name.trim(),
                prenom: formData.prenom.trim(),
                segment: formData.segment,
                hireDate: formData.hireDate,
            };
            
            await collaboratorService.update(matricule, payload);
            setSuccess("Collaborateur modifié avec succès !");
            
            // Redirection après 1.5 secondes
            setTimeout(() => {
                navigate("/collaborateurs");
            }, 1500);
            
        } catch (err) {
            const errorMessage = err.response?.data?.message || "Erreur lors de la mise à jour";
            setError(errorMessage);
            console.error("Erreur:", err);
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div className="container py-4 collab-form-page">
                <div className="collab-form-loading">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Chargement...</span>
                    </div>
                    <p className="mt-2">Chargement des informations...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-4 collab-form-page">
            <div className="collab-form-shell">
                <div className="collab-form-header">
                    <div>
                        <h2 className="collab-form-title">
                        Modifier le Collaborateur
                        </h2>
                    </div>
                </div>
            <div className="form-container card shadow-sm border-0 collab-form-card">
                <div className="card-body p-4 p-md-5">

                    {error && (
                        <div className="alert alert-danger alert-dismissible fade show">
                            <i className="fas fa-exclamation-triangle me-2"></i>
                            {error}
                            <button type="button" className="btn-close" onClick={() => setError("")}></button>
                        </div>
                    )}
                    
                    {success && (
                        <div className="alert alert-success alert-dismissible fade show">
                            <i className="fas fa-check-circle me-2"></i>
                            {success}
                            <button type="button" className="btn-close" onClick={() => setSuccess("")}></button>
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="collab-form-grid">
                        <div className="form-group mb-3 collab-field">
                            <label htmlFor="matricule" className="form-label">Matricule:</label>
                            <input
                                type="text"
                                id="matricule"
                                name="matricule"
                                value={formData.matricule}
                                className="form-control bg-light"
                                disabled
                            />
                            <small className="text-muted">Le matricule ne peut pas être modifié</small>
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
                            >
                                <option value="">
                                    {segmentsLoading ? "Chargement..." : "Sélectionner un segment"}
                                </option>
                                {segments.map((segment) => {
                                    const label = getSegmentLabel(segment);
                                    const key = segment?.idSegment ?? segment?.id ?? label;
                                    return (
                                        <option key={key} value={label}>
                                            {label}
                                        </option>
                                    );
                                })}
                            </select>
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
                                disabled={submitting} 
                                className="btn btn-primary"
                            >
                                {submitting ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm me-2"></span>
                                        Mise à jour...
                                    </>
                                ) : "Mettre à jour"}
                            </button>
                            <button 
                                type="button" 
                                onClick={() => navigate("/collaborateurs")} 
                                className="btn btn-outline-secondary"
                            >
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
