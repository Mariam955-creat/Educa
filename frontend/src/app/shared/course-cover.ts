const FALLBACK_GRADIENTS: [string, string][] = [
  ['#7e22ce', '#db2777'],
  ['#2563eb', '#7c3aed'],
  ['#0d9488', '#2563eb'],
  ['#ea580c', '#db2777'],
  ['#059669', '#0d9488'],
  ['#4f46e5', '#0ea5e9'],
];

/** Visuel de repli d'un cours sans image de couverture : dégradé stable, déterminé par l'id du cours. */
export function fallbackCover(courseId: number): string {
  const [from, to] = FALLBACK_GRADIENTS[courseId % FALLBACK_GRADIENTS.length];
  return `linear-gradient(135deg, ${from}, ${to})`;
}
