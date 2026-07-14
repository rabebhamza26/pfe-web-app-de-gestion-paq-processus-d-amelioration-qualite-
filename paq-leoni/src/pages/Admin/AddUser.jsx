import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userService, siteService, plantService, getSegments } from "../../services/api";

export default function AddUser() {
  const t = (key) => key;
  const initialForm = {
    nomUtilisateur: "",
    login: "",
    email: "", 
    password: "",
    role: "ADMIN",
    active: true,
    siteIds: [],
    plantIds: [],
    segmentIds: []
  };
  
  const [form, setForm] = useState(initialForm);
  const [sites, setSites] = useState([]);
  const [allPlants, setAllPlants] = useState([]);
  const [allSegments, setAllSegments] = useState([]);
  const [filteredPlants, setFilteredPlants] = useState([]);
  const [filteredSegments, setFilteredSegments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const roles = [
    { value: "ADMIN", label: t("admin") },
    { value: "SL", label: t("sl") },
    { value: "QM_SEGMENT", label: t("qm_segment") },
    { value: "QM_PLANT", label: t("qm_plant") },
    { value: "SGL", label: t("sgl") },
    { value: "HP", label: t("hp") },
    { value: "RH", label: t("rh") }
  ];

  useEffect(() => {
    loadData();
  }, []);

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
      setLoadingData(true);
      const [sitesRes, plantsRes, segmentsRes] = await Promise.all([
        siteService.getAll(),
        plantService.getAll(),
        getSegments()
      ]);
      setSites(sitesRes.data || []);
      setAllPlants(plantsRes.data || []);
      setAllSegments(segmentsRes.data || []);
    } catch (err) {
      console.error("Erreur chargement données:", err);
      setError("Erreur lors du chargement des données");
    } finally {
      setLoadingData(false);
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
      plantIds: [], 
      segmentIds: [] 
    });
    
    // Filter plants based on selected sites
    if (siteIds.length > 0) {
      const filtered = allPlants.filter(plant => siteIds.includes(plant.siteId));
      setFilteredPlants(filtered);
    } else {
      setFilteredPlants([]);
    }
    // Reset filtered segments
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
    
    // Filter segments based on selected plants
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
    setLoading(true);

    try {
      const payload = { 
        nomUtilisateur: form.nomUtilisateur,
        login: form.login,
        email: form.email,
        password: form.password,
        role: form.role,
        active: true,
        siteIds: form.siteIds,
        plantIds: form.plantIds,
        segmentIds: form.segmentIds
      };

      const res = await userService.createUser(payload);

      navigate("/admin/users", {
        state: {
          newUserPassword: form.password,
          userId: res.data.id
        }
      });

    } catch (err) {
      setError(t("error_saving_data") + ": " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  if (loadingData) {
    return (
      <div className="container-fluid mt-4">
        <div className="text-center py-4">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">{t("loading")}...</span>
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
            <div className="card-header bg-primary text-white">
              <h4 className="card-title mb-0">
                <i className="fas fa-user-plus me-2"></i>
                {t("add_user")}
              </h4>
            </div>
            <div className="card-body">
              {error && (
                <div className="alert alert-danger" role="alert">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} autoComplete="off">
                <div className="row">
                  <div className="col-md-6 mb-3">
                    <label htmlFor="nomUtilisateur" className="form-label">
                      {t("full_name")}
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      id="nomUtilisateur"
                      name="nomUtilisateur"
                      placeholder={t("enter_full_name")}
                      value={form.nomUtilisateur}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  <div className="col-md-6 mb-3">
                    <label htmlFor="login" className="form-label">
                      {t("login")}
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      id="login"
                      name="login"
                      placeholder={t("enter_login")}
                      value={form.login}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="row">
                  <div className="col-md-6 mb-3">
                    <label htmlFor="email" className="form-label">
                      {t("email")}
                    </label>
                    <input
                      type="email"
                      className="form-control"
                      id="email"
                      name="email"
                      placeholder={t("enter_email")}
                      value={form.email}
                      onChange={handleChange}
                      required
                    />
                  </div>

                  <div className="col-md-6 mb-3">
                    <label htmlFor="password" className="form-label">
                      {t("password")}
                    </label>
                    <input
                      type="password"
                      className="form-control"
                      id="password"
                      name="password"
                      placeholder={t("enter_password")}
                      value={form.password}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="mb-3">
                  <label htmlFor="role" className="form-label">
                    {t("role")}
                  </label>
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
                    value={form.siteIds.map(id => id.toString())}
                    onChange={handleSiteChange}
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
                    value={form.plantIds.map(id => id.toString())}
                    onChange={handlePlantChange}
                    disabled={form.siteIds.length === 0}
                    style={{ minHeight: '120px' }}
                  >
                    {filteredPlants.length > 0 ? (
                      filteredPlants.map(plant => (
                        <option key={plant.id} value={plant.id}>
                          {plant.name}
                        </option>
                      ))
                    ) : (
                      <option disabled>Sélectionnez d'abord un site</option>
                    )}
                  </select>
                 
                  {form.siteIds.length > 0 && (
                    <small className="text-muted">Maintenez Ctrl (Cmd) pour sélectionner plusieurs plants</small>
                  )}
                </div>

                <div className="mb-4">
                  <label className="form-label">
                    <i className="fas fa-tag me-2"></i>{t("segments")}
                  </label>
                  <select 
                    multiple 
                    className="form-select" 
                    value={form.segmentIds.map(id => id.toString())}
                    onChange={handleSegmentChange}
                    disabled={form.plantIds.length === 0}
                    style={{ minHeight: '120px' }}
                  >
                    {filteredSegments.length > 0 ? (
                      filteredSegments.map(segment => (
                        <option key={segment.id} value={segment.id}>
                          {segment.nomSegment}
                        </option>
                      ))
                    ) : (
                      <option disabled>Sélectionnez d'abord un plant</option>
                    )}
                  </select>
                  {form.plantIds.length === 0 && form.siteIds.length > 0 && (
                    <small className="text-muted" style={{ color: "#f59e0b" }}>
                    </small>
                  )}
                  {form.plantIds.length > 0 && (
                    <small className="text-muted"></small>
                  )}
                </div>

                <div className="d-flex gap-2 justify-content-end">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => navigate("/admin/users")}
                    disabled={loading}
                  >
                    <i className="fas fa-times me-2"></i>
                    {t("cancel")}
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={loading}
                  >
                    {loading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        {t("loading")}...
                      </>
                    ) : (
                      <>
                        <i className="fas fa-save me-2"></i>
                        {t("add_user")}
                      </>
                    )}
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