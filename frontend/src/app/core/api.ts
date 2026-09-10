/**
 * URL de base de l'API REST du backend.
 *
 * Par défaut relative (`/api/v1`) : le frontend et l'API sont servis sur la même origine
 *  - en développement via le proxy Angular (`frontend/proxy.conf.json` → `localhost:8081`) ;
 *  - en production via le reverse-proxy nginx de l'image Docker (voir `docs/07-deploiement.md`).
 *
 * Surcharge possible au déploiement sans reconstruire le bundle : définir
 * `window.EDUCA_API_BASE_URL` (p. ex. dans un `assets/config.js`) sur une URL absolue.
 */
const runtimeOverride = (globalThis as unknown as { EDUCA_API_BASE_URL?: string }).EDUCA_API_BASE_URL;

export const API_BASE_URL =
  runtimeOverride && runtimeOverride.trim() ? runtimeOverride.trim() : '/api/v1';
