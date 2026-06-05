package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;  // ✅ Ajouter pour résoudre login à partir de l'email

    @Value("${spring.mail.username}")
    private String fromEmail;

    private String getLoginFromEmail(String email) {
        if (email == null || email.isBlank()) return null;
        User user = userRepository.findByEmail(email);
        return user != null ? user.getLogin() : null;
    }

    private String getCurrentUserLogin() {
        try {
            org.springframework.security.core.Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String principal = auth.getName();
                User user = userRepository.findByEmailOrLogin(principal);
                if (user != null && user.getLogin() != null) {
                    return user.getLogin();
                }
                return principal;
            }
        } catch (Exception e) {
            log.error("Erreur récupération utilisateur courant: {}", e.getMessage());
        }
        return null;
    }

    public void sendPasswordResetRequestToAdmin(String userEmail, String userLogin, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(getAdminEmails());
        message.setSubject("Demande de réinitialisation de mot de passe");
        message.setText(String.format(
                "Bonjour Admin,\n\n" +
                        "Un utilisateur a demandé la réinitialisation de son mot de passe.\n\n" +
                        "Détails de l'utilisateur :\n" +
                        "• Nom : %s\n" +
                        "• Login : %s\n" +
                        "• Email : %s\n\n" +
                        "Veuillez vous connecter à l'interface d'administration pour réinitialiser son mot de passe.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe technique",
                userName, userLogin, userEmail
        ));
        mailSender.send(message);

        // ✅ Envoyer une notification système à l'admin (utiliser le login, pas l'email)
        String adminLogin = getLoginFromEmail("admin@example.com");
        if (adminLogin != null) {
            notificationService.envoyerNotification(
                    adminLogin,
                    "📧 Demande de réinitialisation",
                    "L'utilisateur " + userName + " (" + userLogin + ") a demandé une réinitialisation de mot de passe",
                    "INFO"
            );
        }
    }

    public void sendNewPasswordToUser(String userEmail, String userLogin, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(userEmail);
        message.setSubject("Votre nouveau mot de passe");
        message.setText(String.format(
                "Bonjour %s,\n\n" +
                        "Suite à votre demande, votre mot de passe a été réinitialisé.\n\n" +
                        "Vos nouvelles identifiants de connexion :\n" +
                        "• Login : %s\n" +
                        "• Nouveau mot de passe : %s\n\n" +
                        "Nous vous recommandons de changer ce mot de passe lors de votre prochaine connexion.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe technique",
                userLogin, userLogin, newPassword
        ));
        mailSender.send(message);

        // ✅ Envoyer une notification système à l'utilisateur (utiliser le LOGIN)
        notificationService.envoyerNotification(
                userLogin,  // ← LOGIN, pas email!
                "🔐 Mot de passe réinitialisé",
                "Votre mot de passe a été réinitialisé par l'administrateur.",
                "INFO"
        );
    }

    private String[] getAdminEmails() {
        return new String[]{"admin@example.com"};
    }

    /**
     * Envoie un email via Gmail SMTP avec notification système pour l'expéditeur
     */
    public void sendEmail(String expediteurEmail,
                          String destinataireEmail,
                          String sujet,
                          String contenuHtml) {

        if (mailSender == null) {
            log.error("JavaMailSender n'est pas configuré");
            String expediteurLogin = getCurrentUserLogin();
            if (expediteurLogin != null) {
                notificationService.envoyerNotification(
                        expediteurLogin,
                        "❌ Erreur d'envoi email",
                        "Service d'email non configuré",
                        "ERROR"
                );
            }
            throw new RuntimeException("Service d'email non configuré");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinataireEmail);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);
            helper.setFrom("paqleoni@gmail.com");

            mailSender.send(mimeMessage);

            log.info("Email envoyé avec succès à {} par {}", destinataireEmail, expediteurEmail);

            // ✅ NOTIFICATION POUR L'EXPÉDITEUR - Utiliser l'utilisateur connecté
            String expediteurLogin = getCurrentUserLogin();
            if (expediteurLogin != null) {
                notificationService.envoyerNotification(
                        expediteurLogin,
                        "✅ Email envoyé",
                        "Votre email à " + destinataireEmail + " a été envoyé avec succès",
                        "SUCCESS"
                );
                log.info("✅ Notification envoyée à l'expéditeur: {}", expediteurLogin);
            } else {
                log.warn("⚠️ Impossible de trouver le login de l'expéditeur pour la notification");
            }

        } catch (Exception e) {
            log.error("Échec envoi email vers {}: {}", destinataireEmail, e.getMessage());

            // ✅ NOTIFICATION D'ERREUR POUR L'EXPÉDITEUR
            String expediteurLogin = getCurrentUserLogin();
            if (expediteurLogin != null) {
                notificationService.envoyerNotification(
                        expediteurLogin,
                        "❌ Erreur d'envoi email",
                        "L'email à " + destinataireEmail + " n'a pas pu être envoyé : " + e.getMessage(),
                        "ERROR"
                );
            }
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage(), e);
        }
    }
    /**
     * Envoie un email de notification après validation d'entretien
     * avec notification système pour le destinataire
     */
    public void envoyerEmailValidationEntretien(String expediteurEmail,
                                                String destinataireEmail,
                                                String collaborateurNom,
                                                String typeEntretien,
                                                String matricule) {

        if (destinataireEmail == null || destinataireEmail.isBlank()) {
            log.warn("Destinataire email null pour entretien {} de {}", typeEntretien, matricule);
            return;
        }

        String sujet = String.format("[PAQ] Entretien %s validé - %s", typeEntretien, collaborateurNom);
        String html = buildEmailTemplate(collaborateurNom, typeEntretien, matricule);
        sendEmail(expediteurEmail, destinataireEmail, sujet, html);

        // ✅ NOTIFICATION POUR LE DESTINATAIRE
        String destinataireLogin = getLoginFromEmail(destinataireEmail);
        if (destinataireLogin != null) {
            String titreNotif = String.format("📋 Entretien %s - %s", typeEntretien, collaborateurNom);
            String messageNotif = String.format(
                    "Un entretien %s a été validé pour le collaborateur %s. Veuillez vous connecter pour consulter le dossier.",
                    typeEntretien, collaborateurNom
            );

            notificationService.envoyerNotification(
                    destinataireLogin,
                    titreNotif,
                    messageNotif,
                    "ENTRETIEN",
                    matricule,
                    typeEntretien
            );
            log.info("Notification système envoyée à l'utilisateur: {}", destinataireLogin);
        }

        // ✅ NOTIFICATION POUR L'EXPÉDITEUR (SL) - DÉPLACÉE ICI AUSSI POUR PLUS DE CLARTÉ
        String expediteurLogin = getLoginFromEmail(expediteurEmail);
        if (expediteurLogin != null) {
            notificationService.envoyerNotification(
                    expediteurLogin,
                    "📧 Email envoyé",
                    "L'email de validation d'entretien a été envoyé à " + destinataireEmail,
                    "INFO",
                    matricule,
                    typeEntretien
            );
        }
    }

    private String buildEmailTemplate(String collaborateurNom, String typeEntretien, String matricule) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
              <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; padding: 30px;">
                <div style="background: #C8102E; padding: 20px; border-radius: 8px 8px 0 0; margin: -30px -30px 0 -30px;">
                  <h2 style="color: white; margin: 0;"> PAQ - Validation d'entretien</h2>
                </div>
                <div style="padding: 20px 0;">
                  <p>Bonjour,</p>
                  <p>Un entretien <strong>%s</strong> a été validé pour :</p>
                  <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                    <tr style="background: #f8f9fa;">
                      <td style="padding: 10px; border: 1px solid #dee2e6;">Collaborateur</td>
                      <td style="padding: 10px; border: 1px solid #dee2e6;"><strong>%s</strong></td>
                    </tr>
                    <tr>
                      <td style="padding: 10px; border: 1px solid #dee2e6;">Matricule</td>
                      <td style="padding: 10px; border: 1px solid #dee2e6;"><strong>%s</strong></td>
                    </tr>
                    <tr style="background: #f8f9fa;">
                      <td style="padding: 10px; border: 1px solid #dee2e6;">Type d'entretien</td>
                      <td style="padding: 10px; border: 1px solid #dee2e6;"><strong>%s</strong></td>
                     </tr>
                  </table>
                  <p>Veuillez vous connecter au système PAQ pour prendre connaissance du dossier.</p>
                </div>
                <div style="color: #666; font-size: 12px; margin-top: 30px; border-top: 1px solid #eee; padding-top: 10px;">
                  <p>Cet email est généré automatiquement par le système PAQ. Ne pas répondre.</p>
                  <p>&copy; 2026 PAQ System - LEONI</p>
                </div>
              </div>
            </body>
            </html>
            """, typeEntretien, collaborateurNom, matricule, typeEntretien);
    }

    public void envoyerEmailSimple(String destinataireEmail, String sujet, String contenuHtml) {
        sendEmail("system@paq.com", destinataireEmail, sujet, contenuHtml);

        // ✅ Notifier le destinataire dans le système
        String destinataireLogin = getLoginFromEmail(destinataireEmail);
        if (destinataireLogin != null) {
            notificationService.envoyerNotification(
                    destinataireLogin,
                    sujet,
                    "Un nouvel email vous a été envoyé",
                    "INFO"
            );
        }
    }

    public void sendDefautGraveNotificationToSGL(
            String sglEmail,
            String matricule,
            String collaborateurNom,
            String typeFaute,
            String expediteurEmail
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(sglEmail);
            message.setFrom(expediteurEmail);
            message.setSubject("[PAQ] Défaut grave détecté — Participation SGL requise");
            message.setText(
                    "Bonjour,\n\n" +
                            "Un défaut grave a été enregistré pour le collaborateur suivant :\n\n" +
                            "  Matricule     : " + matricule + "\n" +
                            "  Collaborateur : " + collaborateurNom + "\n" +
                            "  Type de faute : " + typeFaute + "\n\n" +
                            "En tant que SGL (Chef de segment), votre participation à l'entretien " +
                            "explicatif est obligatoire conformément au processus PAQ.\n\n" +
                            "Merci de vous connecter à la plateforme PAQ pour consulter et valider cet entretien.\n\n" +
                            "Cordialement,\n" +
                            "Système PAQ — Notification automatique"
            );
            mailSender.send(message);
            log.info("Notification défaut grave envoyée au SGL {} pour le matricule {}", sglEmail, matricule);

            // ✅ Envoyer une notification système au SGL
            String sglLogin = getLoginFromEmail(sglEmail);
            if (sglLogin != null) {
                notificationService.envoyerNotification(
                        sglLogin,
                        "⚠️ Défaut grave - " + collaborateurNom,
                        "Un défaut grave de type '" + typeFaute + "' a été détecté. Votre participation à l'entretien explicatif est requise.",
                        "DEFUT_GRAVE",
                        matricule,
                        "EXPLICATIF"
                );
            }

        } catch (Exception e) {
            log.error("Erreur envoi mail défaut grave au SGL {}: {}", sglEmail, e.getMessage());
        }
    }

    private void envoyerNotificationExpediteur(String expediteurEmail, String destinataireEmail, String sujet, boolean succes, String erreurMessage) {
        String expediteurLogin = getLoginFromEmail(expediteurEmail);
        if (expediteurLogin == null) {
            log.warn("Impossible de trouver le login pour l'expéditeur: {}", expediteurEmail);
            return;
        }

        if (succes) {
            notificationService.envoyerNotification(
                    expediteurLogin,
                    "✅ Email envoyé",
                    "Votre email à " + destinataireEmail + " a été envoyé avec succès",
                    "SUCCESS",
                    null,
                    null
            );
        } else {
            notificationService.envoyerNotification(
                    expediteurLogin,
                    "❌ Erreur d'envoi email",
                    "L'email à " + destinataireEmail + " n'a pas pu être envoyé : " + erreurMessage,
                    "ERROR",
                    null,
                    null
            );
        }
    }
}