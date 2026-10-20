/** Dégradés de repli, déclinés de la palette (mauves, verts, touche de jaune). */
const FALLBACK_GRADIENTS: [string, string][] = [
  ['#a78bfa', '#7c3aed'],
  ['#10b981', '#047857'],
  ['#c084fc', '#8b5cf6'],
  ['#8b5cf6', '#10b981'],
  ['#f59e0b', '#d97706'],
  ['#e879f9', '#a78bfa'],
];

/** Visuel de repli d'un cours sans image de couverture : dégradé stable, déterminé par l'id du cours. */
export function fallbackCover(courseId: number): string {
  const [from, to] = FALLBACK_GRADIENTS[courseId % FALLBACK_GRADIENTS.length];
  return `linear-gradient(135deg, ${from}, ${to})`;
}
