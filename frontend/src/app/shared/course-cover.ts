/** Dégradés de repli, déclinés de la palette (bleus, verts, touche de jaune). */
const FALLBACK_GRADIENTS: [string, string][] = [
  ['#2563eb', '#1e40af'],
  ['#10b981', '#047857'],
  ['#3b82f6', '#06b6d4'],
  ['#1d4ed8', '#10b981'],
  ['#f59e0b', '#d97706'],
  ['#0ea5e9', '#2563eb'],
];

/** Visuel de repli d'un cours sans image de couverture : dégradé stable, déterminé par l'id du cours. */
export function fallbackCover(courseId: number): string {
  const [from, to] = FALLBACK_GRADIENTS[courseId % FALLBACK_GRADIENTS.length];
  return `linear-gradient(135deg, ${from}, ${to})`;
}
