package com.polytech.paqbackend.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.polytech.paqbackend.dto.CollaborateurSansFauteDto;
import com.polytech.paqbackend.dto.ValiderEntretienPositifRequest;
import com.polytech.paqbackend.entity.*;
import com.polytech.paqbackend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntretienPositifService {

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private PaqRepository paqRepository;

    @Autowired
    private EntretienPositifRepository entretienPositifRepository;

    @Autowired
    private ArchiveRepository archiveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOIS_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("fr"));

    // ── Historique helper ──────────────────────────────────────────────────────

    public static class HistoriqueEvent {
        private String date;
        private String action;
        private String detail;

        public HistoriqueEvent() {}

        public HistoriqueEvent(String date, String action, String detail) {
            this.date = date;
            this.action = action;
            this.detail = detail;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }

    private String addHistoriqueEvent(String historiqueJson, String action, String detail) {
        try {
            List<HistoriqueEvent> list;
            if (historiqueJson == null || historiqueJson.isBlank() || "[]".equals(historiqueJson)) {
                list = new ArrayList<>();
            } else {
                list = objectMapper.readValue(historiqueJson, new TypeReference<List<HistoriqueEvent>>() {});
            }
            list.add(new HistoriqueEvent(LocalDate.now().toString(), action, detail));
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return String.format("[{\"date\":\"%s\",\"action\":\"%s\",\"detail\":\"%s\"}]",
                    LocalDate.now(), action, detail);
        }
    }

    // ── Collaborateurs sans faute (Niveau 0 depuis 6 mois) ─────────────────────

    /**
     * Récupère les collaborateurs qui sont au niveau 0 depuis 6 mois
     * Conditions:
     * 1. Le collaborateur a le niveau 0
     * 2. Le collaborateur a une ancienneté >= 6 mois
     * 3. Le collaborateur n'a pas de PAQ actif
     * 4. Le dernier PAQ (s'il existe) date de plus de 6 mois
     */
    public List<CollaborateurSansFauteDto> getCollaborateursSansFaute() {
        LocalDate sixMoisAvant = LocalDate.now().minusMonths(6);

        // Récupérer tous les collaborateurs actifs avec niveau 0
        List<Collaborator> allNiveauZero = collaboratorRepository
                .findAllByNiveauAndActifAndHireDateBefore(0, true, sixMoisAvant);

        if (allNiveauZero == null || allNiveauZero.isEmpty()) {
            return new ArrayList<>();
        }

        List<CollaborateurSansFauteDto> result = new ArrayList<>();

        for (Collaborator c : allNiveauZero) {
            // Vérifier si le collaborateur peut être inclus
            if (estEligiblePourEntretienPositif(c)) {
                CollaborateurSansFauteDto dto = new CollaborateurSansFauteDto();
                dto.setMatricule(c.getMatricule());
                dto.setNom(c.getName());
                dto.setPrenom(c.getPrenom());
                dto.setSegment(c.getSegment());
                // Calculer les jours depuis l'embauche
                long jours = ChronoUnit.DAYS.between(c.getHireDate(), LocalDate.now());
                dto.setJoursSansFaute((int) jours);
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * Vérifie si un collaborateur est éligible à l'entretien positif
     * Conditions:
     * 1. Niveau = 0
     * 2. Pas de PAQ actif
     * 3. Si un PAQ existe, il doit être archivé depuis au moins 6 mois
     * 4. Ancienneté >= 6 mois
     */
    private boolean estEligiblePourEntretienPositif(Collaborator collaborator) {
        // Vérifier l'ancienneté minimale de 6 mois
        if (collaborator.getHireDate() == null) return false;
        if (collaborator.getHireDate().plusMonths(6).isAfter(LocalDate.now())) {
            return false; // Moins de 6 mois d'ancienneté
        }

        // Vérifier si un PAQ actif existe
        Optional<PaqDossier> activePaq = paqRepository
                .findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(collaborator.getMatricule());

        if (activePaq.isPresent()) {
            return false; // PAQ actif présent
        }

        // Vérifier l'historique des PAQ
        List<PaqDossier> allPaqs = paqRepository
                .findAllByCollaboratorMatriculeOrderByDateCreationDesc(collaborator.getMatricule());

        if (!allPaqs.isEmpty()) {
            PaqDossier dernierPaq = allPaqs.get(0);
            // Si le dernier PAQ a été créé il y a moins de 6 mois, pas éligible
            if (dernierPaq.getDateCreation().plusMonths(6).isAfter(LocalDate.now())) {
                return false;
            }
        }

        return true;
    }

    // Récupérer les collaborateurs sans faute pour un SL spécifique (basé sur ses segments)
    public List<CollaborateurSansFauteDto> getCollaborateursSansFauteParSl(User sl) {
        List<CollaborateurSansFauteDto> tous = getCollaborateursSansFaute();
        Set<String> segmentsSl = getSegmentsForUser(sl);

        if (segmentsSl.isEmpty()) {
            return tous;
        }
        return tous.stream()
                .filter(c -> segmentsSl.contains(c.getSegment()))
                .collect(Collectors.toList());
    }

    private Set<String> getSegmentsForUser(User user) {
        Set<String> segments = new HashSet<>();

        if (user.getSegments() != null) {
            user.getSegments().forEach(seg -> {
                if (seg != null && seg.getNomSegment() != null) {
                    segments.add(seg.getNomSegment());
                }
            });
        }

        if (user.getPlants() != null) {
            user.getPlants().forEach(plant -> {
                if (plant != null && plant.getSegments() != null) {
                    plant.getSegments().forEach(seg -> {
                        if (seg != null && seg.getNomSegment() != null) {
                            segments.add(seg.getNomSegment());
                        }
                    });
                }
            });
        }

        if (user.getSites() != null) {
            user.getSites().forEach(site -> {
                if (site != null && site.getPlants() != null) {
                    site.getPlants().forEach(plant -> {
                        if (plant != null && plant.getSegments() != null) {
                            plant.getSegments().forEach(seg -> {
                                if (seg != null && seg.getNomSegment() != null) {
                                    segments.add(seg.getNomSegment());
                                }
                            });
                        }
                    });
                }
            });
        }

        return segments;
    }

    /**
     * Envoi automatique programmé - Pour test toutes les minutes
     * En production: @Scheduled(cron = "0 0 8 * * ?") pour tous les jours à 8h
     * L'email est envoyé uniquement si des collaborateurs niveau 0 avec 6 mois d'ancienneté existent
     */
    //@Scheduled(fixedDelay = 60000)  // Toutes les minutes pour test
    // @Scheduled(cron = "0 0 8 * * ?")  // Décommenter pour production (tous les jours à 8h)
    @Transactional
    public void envoyerAutomatiquementAuxSL() {
        System.out.println("=== [EntretienPositif] Vérification des collaborateurs niveau 0 depuis 6 mois ===");
        System.out.println("Timestamp: " + LocalDateTime.now());

        if (mailSender == null) {
            System.err.println("❌ MailSender non configuré! Vérifiez votre configuration SMTP.");
            return;
        }

        List<User> slUsers = userRepository.findAllSL();
        System.out.println("Nombre de SL trouvés: " + (slUsers != null ? slUsers.size() : 0));

        if (slUsers == null || slUsers.isEmpty()) {
            System.out.println("⚠️ Aucun utilisateur avec rôle SL trouvé");
            return;
        }

        int totalEnvoyes = 0;
        LocalDate aujourdhui = LocalDate.now();

        for (User sl : slUsers) {
            System.out.println("\n--- Traitement SL: " + sl.getLogin() + " ---");
            System.out.println("Email SL: " + sl.getEmail());

            if (sl.getEmail() == null || sl.getEmail().isEmpty()) {
                System.out.println("⚠️ SL sans email: " + sl.getLogin());
                continue;
            }

            // Récupérer les collaborateurs éligibles pour ce SL
            List<CollaborateurSansFauteDto> collabPourSL = getCollaborateursSansFauteParSl(sl);

            System.out.println("Collaborateurs éligibles (niveau 0 + 6 mois ancienneté): " + collabPourSL.size());

            // Afficher les détails pour déboguer
            for (CollaborateurSansFauteDto c : collabPourSL) {
                System.out.println("  - " + c.getNom() + " " + c.getPrenom() + " (" + c.getMatricule() + ") - " + c.getJoursSansFaute() + " jours");
            }

            if (!collabPourSL.isEmpty()) {
                try {
                    boolean emailEnvoye = envoyerEmailAutomatique(sl, collabPourSL);
                    if (emailEnvoye) {
                        totalEnvoyes++;
                        System.out.println("✅ Email envoyé avec succès à " + sl.getEmail());

                        // Enregistrer l'envoi
                        EntretienPositif entretien = new EntretienPositif();
                        entretien.setSlDestinataire(sl.getEmail());
                        entretien.setDateEnvoi(aujourdhui);
                        entretien.setNote("Envoi automatique - Collaborateurs niveau 0 depuis 6 mois");
                        entretien.setCreatedAt(LocalDateTime.now());
                        entretien.setMatriculeCollaborateur(
                                collabPourSL.stream()
                                        .map(CollaborateurSansFauteDto::getMatricule)
                                        .collect(Collectors.joining(","))
                        );
                        entretienPositifRepository.save(entretien);
                    } else {
                        System.out.println("❌ Échec envoi email à " + sl.getEmail());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur envoi email à " + sl.getEmail() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("ℹ️ Aucun collaborateur éligible (niveau 0 depuis 6 mois) pour ce SL");
            }
        }

        System.out.println("\n=== Envoi automatique terminé: " + totalEnvoyes + " email(s) envoyé(s) ===");
    }

    private boolean envoyerEmailAutomatique(User sl, List<CollaborateurSansFauteDto> collaborateurs) throws Exception {
        if (mailSender == null) {
            System.err.println("MailSender est null");
            return false;
        }

        byte[] pdfBytes = generatePdf(collaborateurs);
        LocalDate now = LocalDate.now();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(sl.getEmail());
        helper.setFrom(fromEmail != null ? fromEmail : "paq@leoni.com");
        helper.setSubject("✨ Entretien Positif – Collaborateurs niveau 0 depuis 6 mois – " + now.format(MOIS_FORMATTER));

        String htmlContent = buildEmailContent(sl, collaborateurs, now);
        helper.setText(htmlContent, true);
        helper.addAttachment(
                "collaborateurs_a_feliciter_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf",
                new ByteArrayResource(pdfBytes)
        );

        mailSender.send(mimeMessage);
        return true;
    }

    private String buildEmailContent(User sl, List<CollaborateurSansFauteDto> collaborateurs, LocalDate dateEnvoi) {
        int totalJours = collaborateurs.stream().mapToInt(c -> (int) c.getJoursSansFaute()).sum();
        int moyenneJours = collaborateurs.isEmpty() ? 0 : totalJours / collaborateurs.size();
        String scope = getScopeForUser(sl);

        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append(String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='UTF-8'>
                <style>
                    .email-container { font-family: 'Segoe UI', Arial, sans-serif; max-width: 700px; margin: 0 auto; }
                    .email-header { background: #10B981; padding: 25px 30px; border-radius: 12px 12px 0 0; text-align: center; }
                    .email-header h1 { color: white; margin: 0; font-size: 24px; }
                    .email-header p { color: rgba(255,255,255,0.9); margin: 8px 0 0; font-size: 14px; }
                    .email-body { background: #FFFFFF; border: 1px solid #E5E7EB; border-top: none; padding: 28px 30px; border-radius: 0 0 12px 12px; }
                    .email-message { background: #ECFDF5; border-left: 4px solid #10B981; padding: 16px 20px; border-radius: 0 8px 8px 0; margin: 20px 0; text-align: center; }
                    .email-message p { margin: 0; font-size: 18px; font-weight: bold; color: #065F46; }
                    .email-stats { display: flex; justify-content: space-between; margin: 20px 0; gap: 10px; }
                    .email-stat-card { background: #F0FDF4; border-radius: 10px; padding: 12px; text-align: center; flex: 1; }
                    .email-stat-number { font-size: 28px; font-weight: bold; color: #10B981; }
                    .email-stat-label { font-size: 12px; color: #4B5563; }
                    .email-table { width: 100%%; border-collapse: collapse; margin-top: 20px; font-size: 13px; }
                    .email-table th { background: #10B981; color: white; padding: 12px; text-align: left; border: 1px solid #34D399; }
                    .email-table td { padding: 10px 12px; border: 1px solid #E5E7EB; }
                    .email-table .matricule { font-family: monospace; }
                    .email-table .jours { text-align: right; color: #10B981; font-weight: 600; }
                    .email-footer { color: #6B7280; font-size: 11px; margin-top: 25px; text-align: center; border-top: 1px solid #E5E7EB; padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class='email-container'>
                    <div class='email-header'>
                        <h1>✨ FÉLICITATIONS ! ✨</h1>
                        <p>Entretien Positif - %s</p>
                    </div>
                    <div class='email-body'>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <div class='email-message'>
                            <p>🎉 FÉLICITEZ VOS COLLABORATEURS ! 🎉</p>
                        </div>
                        <p>Nous avons le plaisir de vous informer que <strong>%d collaborateur(s)</strong> 
                        de %s sont au <strong>niveau 0 depuis plus de 6 mois</strong> sans aucune faute.</p>
                        
                        <p><strong>Liste des collaborateurs à féliciter :</strong></p>
                        <table class='email-table'>
                            <thead><tr><th>Nom</th><th>Prénom</th><th>Matricule</th><th>Ancienneté</th></tr></thead>
                            <tbody>
            """,
                dateEnvoi.format(DATE_FORMATTER),
                sl.getNomUtilisateur() != null ? sl.getNomUtilisateur() : sl.getLogin(),
                collaborateurs.size(),
                scope
        ));

        for (CollaborateurSansFauteDto c : collaborateurs) {
            String anciennete = formatAnciennete(c.getJoursSansFaute());
            htmlBuilder.append(String.format("""
                                <tr>
                                    <td>%s</td>
                                    <td>%s</td>
                                    <td class='matricule'>%s</td>
                                    <td class='jours'>%s</td>
                                </tr>
                            """,
                    escapeHtml(c.getNom() != null ? c.getNom() : ""),
                    escapeHtml(c.getPrenom() != null ? c.getPrenom() : ""),
                    escapeHtml(c.getMatricule()),
                    anciennete
            ));
        }

        htmlBuilder.append("""
                            </tbody>
                        </table>
                        <div class='email-footer'>
                            <p>ℹ Cet email est envoyé automatiquement par le système PAQ.</p>
                            <p>💪 Félicitez et valorisez vos collaborateurs pour leur engagement !</p>
                            <p>&copy; 2026 PAQ System - LEONI</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """);

        return htmlBuilder.toString();
    }

    private String formatAnciennete(long jours) {
        long mois = jours / 30;
        long annees = mois / 12;
        mois = mois % 12;

        if (annees > 0) {
            return String.format("%d an%s %d mois", annees, annees > 1 ? "s" : "", mois);
        } else if (mois > 0) {
            return String.format("%d mois", mois);
        } else {
            return String.format("%d jours", jours);
        }
    }

    private String getScopeForUser(User user) {
        if (user.getSites() != null && !user.getSites().isEmpty()) {
            return "du site: " + user.getSites().stream()
                    .map(Site::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        }
        if (user.getPlants() != null && !user.getPlants().isEmpty()) {
            return "du plant: " + user.getPlants().stream()
                    .map(Plant::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        }
        return "votre périmètre";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ── Archivage entretien positif ────────────────────────────────────────────

    @Transactional
    public Map<String, Object> archiverPaq(ValiderEntretienPositifRequest request) {
        List<String> matricules = request.getMatricules();
        int archivedCount = 0;
        List<String> erreurs = new ArrayList<>();

        for (String matricule : matricules) {
            try {
                Optional<Collaborator> collaboratorOpt = collaboratorRepository.findById(matricule);
                if (!collaboratorOpt.isPresent()) {
                    erreurs.add(matricule + ": Collaborateur non trouvé");
                    continue;
                }
                Collaborator collaborator = collaboratorOpt.get();
                String nomPrenom = collaborator.getName() + " " + collaborator.getPrenom();

                Optional<PaqDossier> ancienPaqOpt = paqRepository
                        .findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);

                Long paqDossierId = null;

                if (ancienPaqOpt.isPresent()) {
                    PaqDossier ancienPaq = ancienPaqOpt.get();
                    ancienPaq.setArchived(true);
                    ancienPaq.setActif(false);
                    ancienPaq.setStatut("ARCHIVE_POSITIF");
                    ancienPaq.setDateFin(LocalDate.now());
                    String newHistorique = addHistoriqueEvent(
                            ancienPaq.getHistorique(),
                            "Archivage suite entretien positif",
                            "Dossier archivé après entretien positif"
                    );
                    ancienPaq.setHistorique(newHistorique);
                    paqRepository.save(ancienPaq);
                    paqDossierId = ancienPaq.getId();
                }

                Archive archive = new Archive();
                archive.setType("ENTRETIEN_POSITIF");
                archive.setMatricule(matricule);
                archive.setNomPrenom(nomPrenom);
                archive.setDateArchivage(LocalDate.now());
                if (paqDossierId != null) {
                    archive.setPaqDossierId(paqDossierId);
                }
                archiveRepository.save(archive);

                archivedCount++;

            } catch (Exception e) {
                erreurs.add(matricule + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", erreurs.isEmpty());
        response.put("archivedCount", archivedCount);
        response.put("errors", erreurs);
        return response;
    }

    // ── Génération PDF ─────────────────────────────────────────────────────────

    public byte[] generatePdf(List<CollaborateurSansFauteDto> list) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("✨ FÉLICITATIONS - COLLABORATEURS NIVEAU 0 DEPUIS 6 MOIS ✨")
                .setBold().setFontSize(18).setFontColor(ColorConstants.GREEN));
        document.add(new Paragraph("Généré le : " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(10));

        Table table = new Table(new float[]{3, 3, 3, 2});
        table.setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"Nom", "Prénom", "Matricule", "Ancienneté"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header))
                    .setBold()
                    .setBackgroundColor(ColorConstants.GREEN));
        }

        for (CollaborateurSansFauteDto c : list) {
            String anciennete = formatAnciennete(c.getJoursSansFaute());
            table.addCell(new Cell().add(new Paragraph(c.getNom() != null ? c.getNom() : "")));
            table.addCell(new Cell().add(new Paragraph(c.getPrenom() != null ? c.getPrenom() : "")));
            table.addCell(new Cell().add(new Paragraph(c.getMatricule() != null ? c.getMatricule() : "")));
            table.addCell(new Cell().add(new Paragraph(anciennete)));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public Optional<EntretienPositif> findById(Long id) {
        return entretienPositifRepository.findById(id);
    }
}