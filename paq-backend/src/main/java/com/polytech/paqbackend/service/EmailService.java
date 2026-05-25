package com.polytech.paqbackend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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



    @Value("${spring.mail.username}")
    private String fromEmail;

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
    }

    private String[] getAdminEmails() {
        // Récupérer les emails des administrateurs depuis la base de données
        // Cette méthode peut être améliorée pour récupérer dynamiquement les emails des admins
        return new String[]{"admin@example.com"};
    }


    /**
     * Envoie un email via Gmail SMTP
     *
     * @param expediteurEmail  email de l'utilisateur connecté (expéditeur réel)
     * @param destinataireEmail email du destinataire
     * @param sujet            objet de l'email
     * @param contenuHtml      corps HTML de l'email
     */
    public void sendEmail(String expediteurEmail,
                          String destinataireEmail,
                          String sujet,
                          String contenuHtml) {

        if (mailSender == null) {
            log.error("JavaMailSender n'est pas configuré");
            notificationService.envoyerNotification(
                    expediteurEmail,
                    "❌ Erreur d'envoi email",
                    "Service d'email non configuré",
                    "ERROR"
            );
            throw new RuntimeException("Service d'email non configuré");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinataireEmail);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);
            helper.setFrom("paqleoni@gmail.com"); // Email expéditeur fixe

            mailSender.send(mimeMessage);

            log.info("Email envoyé avec succès à {} par {}", destinataireEmail, expediteurEmail);

            // Notifier l'expéditeur que l'email a été envoyé
            notificationService.envoyerNotification(
                    expediteurEmail,
                    "✅ Email envoyé",
                    "Votre email à " + destinataireEmail + " a été envoyé avec succès",
                    "SUCCESS"
            );

        } catch (Exception e) {
            log.error("Échec envoi email vers {}: {}", destinataireEmail, e.getMessage());

            // Notifier l'expéditeur de l'échec
            notificationService.envoyerNotification(
                    expediteurEmail,
                    "❌ Erreur d'envoi email",
                    "L'email à " + destinataireEmail + " n'a pas pu être envoyé : " + e.getMessage(),
                    "ERROR"
            );
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage(), e);
        }
    }

    // ─── Templates d'email pour les entretiens ─────────────────────────────────

    /**
     * Envoie un email de notification après validation d'entretien.
     * Utilisé par EntretienExplicatifService pour chaque type d'entretien.
     *
     * @param expediteurEmail email de l'utilisateur qui valide
     * @param destinataireEmail destinataire (QM, SGL, HP, RH...)
     * @param collaborateurNom nom du collaborateur concerné
     * @param typeEntretien EXPLICATIF | ACCORD | MESURE | DECISION
     * @param matricule matricule pour le lien dans l'email
     */
    public void envoyerEmailValidationEntretien(String expediteurEmail,
                                                String destinataireEmail,
                                                String collaborateurNom,
                                                String typeEntretien,
                                                String matricule) {
        // Ne pas envoyer si destinataire est null
        if (destinataireEmail == null || destinataireEmail.isBlank()) {
            log.warn("Destinataire email null pour entretien {} de {}", typeEntretien, matricule);
            return;
        }

        String sujet = String.format("[PAQ] Entretien %s validé - %s", typeEntretien, collaborateurNom);
        String html = buildEmailTemplate(collaborateurNom, typeEntretien, matricule);
        sendEmail(expediteurEmail, destinataireEmail, sujet, html);
    }

    /** Génère le corps HTML de l'email */
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

    /**
     * Envoi d'email simple (sans template d'entretien)
     */
    public void envoyerEmailSimple(String destinataireEmail, String sujet, String contenuHtml) {
        sendEmail("system@paq.com", destinataireEmail, sujet, contenuHtml);
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
        } catch (Exception e) {
            log.error("Erreur envoi mail défaut grave au SGL {}: {}", sglEmail, e.getMessage());
        }
    }


}