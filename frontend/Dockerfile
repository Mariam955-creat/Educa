# syntax=docker/dockerfile:1

# ---------- étape 1 : build Angular ----------
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build          # sortie : dist/frontend/browser

# ---------- étape 2 : service statique + reverse-proxy ----------
FROM nginx:1.27-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
