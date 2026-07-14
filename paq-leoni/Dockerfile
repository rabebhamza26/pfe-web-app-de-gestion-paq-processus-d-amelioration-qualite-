# ── Stage 1: Build ──────────────────────────────
# Utilise une image Node légère pour construire l'application React.
FROM node:24-alpine AS builder

# Définit le répertoire de travail à /app dans l'image builder.
WORKDIR /app

# Copie les fichiers package.json et package-lock.json pour installer les dépendances.
COPY package*.json ./
# Installe les dépendances de production et de développement.
RUN npm ci

# Copie tout le code source dans l'image.
COPY . .
# Construit l'application en production dans le dossier dist.
RUN npm run build

# ── Stage 2: Serve with Nginx ───────────────────
# Utilise une image Nginx légère pour servir le build statique.
FROM nginx:alpine

# Copie le dossier de build depuis l'étape builder vers le dossier public de Nginx.
COPY --from=builder /app/dist /usr/share/nginx/html

# Optionnel : remplace la configuration Nginx par un fichier personnalisé.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose le port 5173 pour le service web.
EXPOSE 5173

# Démarre Nginx au premier plan.
CMD ["nginx", "-g", "daemon off;"]