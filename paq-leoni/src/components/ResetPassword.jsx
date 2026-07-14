// src/components/ResetPassword.jsx
import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { passwordResetService } from "../services/api";
import { showSuccessAlert, showErrorAlert } from "../utils/entretienAlerts";
import "../styles/reset-password.css";

// Page de réinitialisation du mot de passe après réception du lien.
export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const t = (key) => key;
  
  const token = searchParams.get('token');
  
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);
  const [resetSuccess, setResetSuccess] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState({
    score: 0,
    message: ""
  });

  useEffect(() => {
    validateToken();
  }, [token]);

  useEffect(() => {
    checkPasswordStrength(newPassword);
  }, [newPassword]);

  // Vérifie la validité du token de réinitialisation.
  const validateToken = async () => {
    if (!token) {
      await showErrorAlert(t("invalid_link"), t("no_token_provided"));
      setValidating(false);
      setTokenValid(false);
      return;
    }

    try {
      const response = await passwordResetService.validateResetToken(token);
      if (response.success) {
        setTokenValid(true);
      } else {
        setTokenValid(false);
        await showErrorAlert(t("invalid_link"), response.message || t("token_expired"));
      }
    } catch (error) {
      setTokenValid(false);
      await showErrorAlert(t("invalid_link"), t("token_validation_error"));
    } finally {
      setValidating(false);
    }
  };


// Évalue la solidité du mot de passe saisi par l'utilisateur.
const checkPasswordStrength = (password) => {
  if (!password) {
    setPasswordStrength({ score: 0, message: "" });
    return;
  }

  let score = 0;
  let messages = [];

  // Seule condition: 8 caractères minimum
  if (password.length >= 8) {
    score = 5; // Score maximum directement si 8+ caractères
  } else {
    messages.push(t("password_min_8_chars"));
  }

  let message = "";
  let strengthClass = "";
  
  if (password.length >= 8) {
    message = t("password_strong");
    strengthClass = "strong";
  } else {
    message = t("password_weak");
    strengthClass = "weak";
  }

  setPasswordStrength({
    score,
    message,
    strengthClass,
    details: messages
  });
};

  // Soumet le nouveau mot de passe si toutes les validations passent.
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!newPassword || !confirmPassword) {
      await showErrorAlert(t("error"), t("fill_all_fields"));
      return;
    }

    if (newPassword !== confirmPassword) {
      await showErrorAlert(t("error"), t("passwords_do_not_match"));
      return;
    }

    if (newPassword.length < 8) {
  await showErrorAlert(t("weak_password"), t("password_min_8_chars"));
  return;
}

    setLoading(true);

    try {
      const response = await passwordResetService.resetPassword(
        token,
        newPassword,
        confirmPassword
      );

      if (response.success) {
        setResetSuccess(true);
        await showSuccessAlert(t("password_reset"), t("password_reset_success"));
        
        setTimeout(() => {
          navigate("/login");
        }, 3000);
      } else {
        await showErrorAlert(t("reset_failed"), response.message);
      }
    } catch (error) {
      await showErrorAlert(t("error"), error.message || t("reset_error"));
    } finally {
      setLoading(false);
    }
  };

  if (validating) {
    return (
      <div className="rp-container">
        <div className="rp-loading">
          <div className="rp-spinner"></div>
          <p>{t("validating_link")}</p>
        </div>
      </div>
    );
  }

  if (!tokenValid) {
    return (
      <div className="rp-container">
        <div className="rp-card rp-error-card">
          <div className="rp-error-icon">⚠️</div>
          <h2>{t("invalid_reset_link")}</h2>
          <p>{t("link_expired_or_invalid")}</p>
          <button 
            className="rp-button rp-button-primary"
            onClick={() => navigate("/login")}
          >
            {t("back_to_login")}
          </button>
          <button 
            className="rp-button rp-button-secondary"
            onClick={() => navigate("/forgot-password")}
          >
            {t("request_new_link")}
          </button>
        </div>
      </div>
    );
  }

  if (resetSuccess) {
    return (
      <div className="rp-container">
        <div className="rp-card rp-success-card">
          <div className="rp-success-icon">✓</div>
          <h2>{t("password_updated")}</h2>
          <p>{t("redirecting_to_login")}</p>
          <button 
            className="rp-button rp-button-primary"
            onClick={() => navigate("/login")}
          >
            {t("go_to_login")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="rp-container animate-fade-in">
      <div className="rp-card animate-scale-in">
        <div className="rp-header">
          <h2>{t("reset_password")}</h2>
          <p>{t("choose_new_password")}</p>
        </div>

        <form onSubmit={handleSubmit} className="rp-form">
          <div className="rp-field">
            <label htmlFor="new-password">{t("new_password")}</label>
            <div className="rp-input-wrapper">
              <input
                id="new-password"
                type={showPassword ? "text" : "password"}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder={t("enter_new_password")}
                disabled={loading}
                autoComplete="new-password"
                required
              />
              <button
                type="button"
                className="rp-eye-button"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? "👁️" : "👁️‍🗨️"}
              </button>
            </div>
          </div>

          {newPassword && (
            <div className={`rp-strength rp-strength-${passwordStrength.strengthClass}`}>
              <div className="rp-strength-bar">
                <div 
                  className="rp-strength-fill" 
                  style={{ width: `${(passwordStrength.score / 5) * 100}%` }}
                />
              </div>
              <p className="rp-strength-text">{passwordStrength.message}</p>
              {passwordStrength.details && passwordStrength.details.length > 0 && (
                <ul className="rp-strength-details">
                  {passwordStrength.details.map((detail, index) => (
                    <li key={index}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          )}

          <div className="rp-field">
            <label htmlFor="confirm-password">{t("confirm_password")}</label>
            <div className="rp-input-wrapper">
              <input
                id="confirm-password"
                type={showPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder={t("confirm_new_password")}
                disabled={loading}
                autoComplete="new-password"
                required
              />
            </div>
          </div>

          {confirmPassword && newPassword !== confirmPassword && (
            <div className="rp-error-message">
              {t("passwords_do_not_match")}
            </div>
          )}

<button 
  type="submit" 
  className="rp-submit" 
  disabled={loading || !newPassword || !confirmPassword || newPassword !== confirmPassword || newPassword.length < 8}
>
  {loading ? (
    <span className="rp-loading-spinner"></span>
  ) : (
    t("reset_password")
  )}
</button>
        </form>

        <div className="rp-footer">
          <button 
            type="button" 
            onClick={() => navigate("/login")}
            className="rp-back-link"
          >
            {t("back_to_login")}
          </button>
        </div>
      </div>
    </div>
  );
}