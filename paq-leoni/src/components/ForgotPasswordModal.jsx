// src/components/ForgotPasswordModal.jsx
import React, { useState } from "react";

import { passwordResetService } from "../services/api";

import { showErrorAlert, showSuccessAlert } from "../utils/entretienAlerts";

import "../styles/forgot-password-modal.css";



// Modal de récupération de mot de passe.
export default function ForgotPasswordModal({ isOpen, onClose }) {
  const t = (key) => key;
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [emailSent, setEmailSent] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  // Envoie la demande de réinitialisation si l'email est valide.
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation email
    if (!email || !email.includes('@')) {
      setErrorMessage(t("valid_email_required"));
      return;
    }
    
    setLoading(true);
    setErrorMessage("");
    
    try {
      const response = await passwordResetService.forgotPassword(email);
      
      if (response.success) {
        setEmailSent(true);
        await showSuccessAlert(
          t("email_sent"), 
          response.message || t("reset_link_sent")
        );
        
        // Fermer automatiquement après 3 secondes
        setTimeout(() => {
          onClose();
          resetForm();
        }, 3000);
      } else {
        setErrorMessage(response.message || t("generic_error"));
        await showErrorAlert(t("request_failed"), response.message);
      }
    } catch (err) {
      const message = err.message || t("generic_error");
      setErrorMessage(message);
      await showErrorAlert(t("request_failed"), message);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setEmail("");
    setEmailSent(false);
    setErrorMessage("");
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fpm-overlay animate-fade-in" onClick={handleClose}>
      <div className="fpm-modal animate-scale-in" onClick={(e) => e.stopPropagation()}>
        <div className="fpm-header">
          <h3>{t("forgot_password")}</h3>
          <button className="fpm-close" onClick={handleClose}>×</button>
        </div>

        <div className="fpm-body">
          {!emailSent ? (
            <>
              <p className="fpm-description">
                {t("forgot_password_description")}
              </p>

              {errorMessage && (
                <div className="fpm-message error">
                  <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 0C4.48 0 0 4.48 0 10s4.48 10 10 10 10-4.48 10-10S15.52 0 10 0zm1 15H9v-2h2v2zm0-4H9V5h2v6z"/>
                  </svg>
                  {errorMessage}
                </div>
              )}

              <form onSubmit={handleSubmit}>
                <div className="fpm-field">
                  <label htmlFor="reset-email">{t("email")}</label>
                  <input
                    id="reset-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder={t("email_placeholder")}
                    disabled={loading}
                    autoComplete="email"
                    required
                  />
                </div>

                <button 
                  type="submit" 
                  className="fpm-submit" 
                  disabled={loading || !email}
                >
                  {loading ? (
                    <span className="fpm-loading">
                      <span className="fpm-dot"></span>
                      <span className="fpm-dot"></span>
                      <span className="fpm-dot"></span>
                    </span>
                  ) : (
                    t("send_reset_link")
                  )}
                </button>
              </form>
            </>
          ) : (
            <div className="fpm-success">
              <div className="fpm-success-icon">✓</div>
              <p>{t("reset_link_sent")}</p>
              <p className="fpm-success-small">{t("check_email_spam")}</p>
            </div>
          )}
        </div>

        <div className="fpm-footer">
          <button type="button" onClick={handleClose} className="fpm-cancel">
            {emailSent ? t("close") : t("cancel")}
          </button>
        </div>
      </div>
    </div>
  );
}