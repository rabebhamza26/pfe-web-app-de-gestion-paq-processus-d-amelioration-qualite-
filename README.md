Application Web de Digitalisation du Processus d'Amélioration de la Qualité (PAQ)

📌 Présentation

Cette application web a été développée dans le cadre de mon Projet de Fin d'Études (PFE). Elle a pour objectif de digitaliser le Processus d'Amélioration de la Qualité (PAQ) au sein de l'entreprise, en remplaçant le suivi papier par un système numérique centralisé, sécurisé et collaboratif.

L'application permet de gérer l'ensemble du cycle de vie des dossiers PAQ : de la création du dossier d'un collaborateur jusqu'à son archivage automatique, en passant par la planification et le suivi des entretiens, avec des notifications en temps réel.

🎯 Contexte du projet
Le processus PAQ était jusqu'alors géré manuellement via des supports papiers, ce qui engendrait :

❌ Une perte d'information et des risques d'erreurs

❌ Un manque de traçabilité et de visibilité sur l'avancement

❌ Des difficultés de reporting et d'analyse

❌ Une gestion complexe des échéances et des entretiens

Objectifs de la digitalisation :

✅ Centraliser toutes les données PAQ dans une base unique

✅ Automatiser les notifications et le suivi des échéances

✅ Fournir un tableau de bord analytique pour les responsables

✅ Garantir la traçabilité et la sécurité des données

✅ Réduire les délais de traitement et les erreurs humaines

✨ Fonctionnalités principales
👥 Gestion des utilisateurs et de l'organisation
Gestion complète des utilisateurs avec rôles et permissions

Structuration hiérarchique : Sites → Plants → Segments

Authentification sécurisée via JWT

📁 Digitalisation du suivi PAQ
Suivi des collaborateurs et de leurs dossiers PAQ

Création, modification et consultation des dossiers par collaborateur

Historique et traçabilité des actions

🔄 Circuit complet des entretiens PAQ
Planification et gestion des entretiens PAQ

Workflow complet (création → validation → clôture)

Notifications en temps réel via WebSocket

Notifications par email pour les événements clés

📊 Tableau de bord analytique

Visualisation des indicateurs clés (KPI)

Suivi de l'avancement des dossiers PAQ

Statistiques par site, plant et segment

🗄️ Archivage automatique
Archivage automatique des dossiers PAQ clôturés

Conservation et consultation des archives

🛠 Stack technologique

-Frontend
Technologie	Usage
React	Framework JavaScript pour l'interface utilisateur
TailwindCSS	Framework CSS utilitaire pour le design
WebSocket (STOMP/SockJS)	Notifications en temps réel
-Backend
Technologie	Usage
Spring Boot	Framework Java pour l'API REST
Spring Security + JWT	Sécurisation des endpoints
Spring Data JPA	Persistance des données
Spring WebSocket	Communication temps réel
JavaMail	Envoi d'emails
Swagger / OpenAPI	Documentation de l'API
-Base de données
Technologie	Usage
MySQL	Base de données relationnelle
-DevOps & Qualité
Technologie	Usage
JUnit	Tests unitaires backend
Mockito	Mocking pour les tests
GitHub Actions	Pipeline CI/CD
Docker	Conteneurisation
