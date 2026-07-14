// EditUser.jsx - Version avec cascade Site → Plant → Segment
import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { userService, siteService, plantService, getSegments } from "../../services/api";

export default function EditUser() {
  const t = (key) => key;
  const { id } = useParams();

  const roles = [
    { value: "ADMIN", label: t("admin") },
    { value: "SL", label: t("sl") },
    { value: "QM_SEGMENT", label: t("qm_segment") },
    { value: "QM_PLANT", label: t("qm_plant") },
    { value: "SGL", label: t("sgl") },
    { value: "HP", label: t("hp") },
    { value: "RH", label: t("rh") }
  ];

  const [form, setForm] = useState({
    nomUtilisateur: "",
    login: "",
    email: "",
    password: "",
    role: "ADMIN",
    active: true,
    siteIds: [],
    plantIds: [],
    segmentIds: []
  });
  const [sites, setSites] = useState([]);
  const [allPlants, setAllPlants] = useState([]);
  const [allSegments, setAllSegments] = useState([]);
  const [filteredPlants, setFilteredPlants] = useState([]);
  const [filteredSegments, setFilteredSegments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, [id]);

  // Met à jour les segments quand les plants sélectionnés changent
  useEffect(() => {
    if (form.plantIds && form.plantIds.length > 0) {
      const segmentsForPlants = allSegments.filter(segment => 
        form.plantIds.includes(segment.plantId)
      );
      setFilteredSegments(segmentsForPlants);
    } else {
      setFilteredSegments([]);
    }
  }, [form.plantIds, allSegments]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [userRes, sitesRes, plantsRes, segmentsRes] = await Promise.all([
        userService.getUserById(id),
        siteService.getAll(),
        plantService.getAll(),
        getSegments()
      ]);
      
      setSites(sitesRes.data || []);
      const plantsData = plantsRes.data || [];
      setAllPlants(plantsData);
      const segmentsData = segmentsRes.data || [];
      setAllSegments(segmentsData);
      
      const user = userRes.data;
      setForm({
        nomUtilisateur: user.nomUtilisateur || "",
        login: user.login || "",
        email: user.email || "",
        password: "",
        role: user.role || "ADMIN",
        active: user.active ?? true,
        siteIds: user.siteIds || [],
        plantIds: user.plantIds || [],
        segmentIds: user.segmentIds || []
      });
      
      // Filter plants based on selected sites
      if (user.siteIds && user.siteIds.length > 0) {
        const filtered = plantsData.filter(plant => user.siteIds.includes(plant.siteId));
        setFilteredPlants(filtered);
      }
      
      // Filter segments based on selected plants
      if (user.plantIds && user.plantIds.length > 0) {
        const segmentsForPlants = segmentsData.filter(segment => 
          user.plantIds.includes(segment.plantId)
        );
        setFilteredSegments(segmentsForPlants);
      }
    } catch (err) {
      setError(t("error_loading_data") + ": " + (err.response?.data?.message || err.message));
      console.error("Erreur chargement:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value
    });
  };

  const handleSiteChange = (e) => {
    const selectedOptions = Array.from(e.target.selectedOptions);
    const siteIds = selectedOptions.map(opt => parseInt(opt.value));
    
    setForm({
      ...form,
      siteIds: siteIds,
      plantIds: [], // Reset plants when sites change
      segmentIds: [] // Reset segments when sites change
    });
    
    if (siteIds.length > 0) {
      const filtered = allPlants.filter(plant => siteIds.includes(plant.siteId));
      setFilteredPlants(filtered);
    } else {
      setFilteredPlants([]);
    }
    setFilteredSegments([]);
  };

  const handlePlantChange = (e) => {
    const selectedOptions = Array.from(e.target.selectedOptions);
    const plantIds = selectedOptions.map(opt => parseInt(opt.value));
    setForm({
      ...form,
      plantIds: plantIds,
      segmentIds: [] // Reset segments when plants change
    });
    
    if (plantIds.length > 0) {
      const segmentsForPlants = allSegments.filter(segment => 
        plantIds.includes(segment.plantId)
      );
      setFilteredSegments(segmentsForPlants);
    } else {
      setFilteredSegments([]);
    }
  };

  const handleSegmentChange = (e) => {
    const selectedOptions = Array.from(e.target.selectedOptions);
    const segmentIds = selectedOptions.map(opt => parseInt(opt.value));
    setForm({
      ...form,
      segmentIds: segmentIds
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);

    try {
      const dataToSend = { 
        nomUtilisateur: form.nomUtilisateur,
        login: form.login,
        email: form.email,
        role: form.role,
        active: form.active,
        siteIds: form.siteIds,
        plantIds: form.plantIds,
        segmentIds: form.segmentIds
      };

      let updatedPassword = null;

      if (form.password && form.password.trim()) {
        dataToSend.password = form.password;
        updatedPassword = form.password;
      }

      await userService.updateUser(id, dataToSend);

      navigate("/admin/users", {
        state: updatedPassword
          ? { updatedPassword, userId: parseInt(id) }
          : {}
      });

    } catch (err) {
      setError(t("error_saving_data") + ": " + (err.response?.data?.message || err.message));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="container-fluid mt-4">
        <div className="text-center py-4">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">{t("loading")}</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container-fluid mt-4">
      <div className="row justify-content-center">
        <div className="col-md-10 col-lg-8">
          <div className="card shadow-sm">
            <div className="card-header bg-warning text-dark">
              <h4 className="card-title mb-0">
                <i className="fas fa-user-edit me-2"></i>
                {t("edit_user")}
              </h4>
            </div>
            <div className="card-body">
              {error && (
                <div className="alert alert-danger" role="alert">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit}>
                <div className="row">
                  <div className="col-md-6 mb-3">
                    <label htmlFor="nomUtilisateur" className="form-label">{t("full_name")}</label>
                    <input
                      type="text"
                      className="form-control"
                      id="nomUtilisateur"
                      name="nomUtilisateur"
                      value={form.nomUtilisateur}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  <div className="col-md-6 mb-3">
                    <label htmlFor="login" className="form-label">{t("login")}</label>
                    <input
                      type="text"
                      className="form-control"
                      id="login"
                      name="login"
                      value={form.login}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="row">
                  <div className="col-md-6 mb-3">
                    <label htmlFor="email" className="form-label">{t("email")}</label>
                    <input
                      type="email"
                      className="form-control"
                      id="email"
                      name="email"
                      value={form.email}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  <div className="col-md-6 mb-3">
                    <label htmlFor="password" className="form-label">{t("password")}</label>
                    <input
                      type="password"
                      className="form-control"
                      id="password"
                      name="password"
                      placeholder={t("leave_empty_to_keep")}
                      value={form.password}
                      onChange={handleChange}
                    />
                    <div className="form-text">
                      {t("leave_empty_to_keep")}
                    </div>
                  </div>
                </div>

                <div className="mb-3">
                  <label htmlFor="role" className="form-label">{t("role")}</label>
                  <select
                    name="role"
                    value={form.role}
                    onChange={handleChange}
                    className="form-select"
                  >
                    {roles.map(role => (
                      <option key={role.value} value={role.value}>{role.label}</option>
                    ))}
                  </select>
                </div>

                <div className="mb-3">
                  <label className="form-label">
                    <i className="fas fa-building me-2"></i>{t("sites")}
                  </label>
                  <select 
                    multiple 
                    className="form-select" 
                    onChange={handleSiteChange}
                    value={form.siteIds.map(id => id.toString())}
                    style={{ minHeight: '120px' }}
                  >
                    {sites.map(site => (
                      <option key={site.id} value={site.id}>
                        {site.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mb-3">
                  <label className="form-label">
                    <i className="fas fa-industry me-2"></i>{t("plants")}
                  </label>
                  <select 
                    multiple 
                    className="form-select" 
                    onChange={handlePlantChange}
                    disabled={form.siteIds.length === 0}
                    value={form.plantIds.map(id => id.toString())}
                    style={{ minHeight: '120px' }}
                  >
                    {filteredPlants.length > 0 ? (
                      filteredPlants.map(plant => (
                        <option key={plant.id} value={plant.id}>
                          {plant.name}
                        </option>
                      ))
                    ) : (
                      <option disabled>{t("select_site_first")}</option>
                    )}
                  </select>
                  {form.siteIds.length === 0 && (
                    <small className="text-muted" style={{ color: "#f59e0b" }}>
                    </small>
                  )}
                 
                </div>

                <div className="mb-4">
                  <label className="form-label">
                    <i className="fas fa-tag me-2"></i>{t("segments")}
                  </label>
                  <select 
                    multiple 
                    className="form-select" 
                    onChange={handleSegmentChange}
                    disabled={form.plantIds.length === 0}
                    value={form.segmentIds.map(id => id.toString())}
                    style={{ minHeight: '120px' }}
                  >
                    {filteredSegments.length > 0 ? (
                      filteredSegments.map(segment => (
                        <option key={segment.id} value={segment.id}>
                          {segment.nomSegment}
                        </option>
                      ))
                    ) : (
                      <option disabled>{t("select_plant_first")}</option>
                    )}
                  </select>
                  {form.plantIds.length === 0 && form.siteIds.length > 0 && (
                    <small className="text-muted" style={{ color: "#f59e0b" }}>
                    </small>
                  )}
                
                </div>

                <div className="d-flex gap-2 justify-content-end">
                  <button type="button" className="btn btn-secondary" onClick={() => navigate("/admin/users")} disabled={saving}>
                    {t("cancel")}
                  </button>
                  <button type="submit" className="btn btn-warning" disabled={saving}>
                    {saving ? t("saving") : t("edit_user")}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}