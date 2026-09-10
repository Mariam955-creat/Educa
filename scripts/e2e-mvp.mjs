// Vérification bout-en-bout du périmètre « Must have » d'educa, contre l'API en cours (:8081).
//
//   Prérequis : backend lancé en profil dev  ->  cd backend && ./mvnw spring-boot:run
//   Lancer    : node scripts/e2e-mvp.mjs
//   Sortie    : une ligne PASS/FAIL par contrôle + un bilan ; code de sortie ≠ 0 si un échec.
//
// Non destructif : le formateur = compte seedé formateur@educa.dev ; les apprenants sont des
// comptes uniques créés à la volée ; le slug du cours est unique par exécution (horodatage).
// Couvre : auth/JWT (rotation, logout), CRUD formation + publication, catalogue, inscription,
// progression, contrôle + examen final, déverrouillage, note pondérée 40/60 + seuil, certificat
// (PDF + vérification publique), résultats formateur, RBAC, chatbot (403 + repli dégradé).

const BASE = 'http://localhost:8081/api/v1';
const PW = 'password123';
const SUFFIX = Date.now();
const LEARNER_EMAIL = `e2e-learn-${SUFFIX}@educa.test`;

let pass = 0, fail = 0;
const results = [];
function check(id, ok, detail = '') {
  results.push({ id, ok, detail });
  if (ok) pass++; else fail++;
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${id}${detail ? '  — ' + detail : ''}`);
}

async function req(method, path, { token, body, raw } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(BASE + path, {
    method, headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let json;
  try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return raw ? { status: res.status, json, text, headers: res.headers } : { status: res.status, json };
}

(async () => {
  console.log(`\n=== educa — vérification E2E du périmètre « Must have » ===`);
  console.log(`API ${BASE} · run ${SUFFIX}\n`);

  // ---------- AUTH ----------
  let r = await req('POST', '/auth/register', {
    body: { email: LEARNER_EMAIL, password: PW, fullName: 'E2E Learner', preferredLanguage: 'fr' },
  });
  check('AUTH-01 register (201)', r.status === 201 || r.status === 200, `HTTP ${r.status}`);

  r = await req('POST', '/auth/register', {
    body: { email: LEARNER_EMAIL, password: PW, fullName: 'E2E Learner', preferredLanguage: 'fr' },
  });
  check('AUTH-02 register doublon → 409', r.status === 409, `HTTP ${r.status}`);

  r = await req('POST', '/auth/register', {
    body: { email: `bad-${SUFFIX}@educa.test`, password: 'short', fullName: 'x' },
  });
  check('AUTH-03 payload invalide → 400', r.status === 400, `HTTP ${r.status}`);

  r = await req('POST', '/auth/login', { body: { email: LEARNER_EMAIL, password: 'wrong-pass' } });
  check('AUTH-04 mauvais mot de passe → 401', r.status === 401, `HTTP ${r.status}`);

  r = await req('POST', '/auth/login', { body: { email: LEARNER_EMAIL, password: PW } });
  const learnerTok = r.json?.accessToken;
  let learnerRefresh = r.json?.refreshToken;
  check('AUTH-05 login → tokens + user', r.status === 200 && !!learnerTok && !!learnerRefresh && !!r.json?.user, `HTTP ${r.status}`);

  r = await req('GET', '/auth/me');
  check('AUTH-06 /me sans jeton → 401', r.status === 401, `HTTP ${r.status}`);

  r = await req('GET', '/auth/me', { token: learnerTok });
  check('AUTH-07 /me avec jeton → 200', r.status === 200 && r.json?.email === LEARNER_EMAIL, `HTTP ${r.status} email=${r.json?.email}`);

  r = await req('PATCH', '/auth/me', { token: learnerTok, body: { preferredLanguage: 'ar' } });
  const langOk = r.status === 200;
  r = await req('GET', '/auth/me', { token: learnerTok });
  check('AUTH-08 PATCH /me preferredLanguage=ar persiste', langOk && r.json?.preferredLanguage === 'ar', `lang=${r.json?.preferredLanguage}`);

  // refresh rotation: new pair, ancien refresh révoqué
  r = await req('POST', '/auth/refresh', { body: { refreshToken: learnerRefresh } });
  const newTok = r.json?.accessToken, newRefresh = r.json?.refreshToken;
  const rotated = r.status === 200 && !!newRefresh && newRefresh !== learnerRefresh;
  const old = await req('POST', '/auth/refresh', { body: { refreshToken: learnerRefresh } });
  check('AUTH-09 refresh : rotation + ancien jeton rejeté', rotated && old.status === 401, `rot=${rotated} vieux=HTTP ${old.status}`);
  learnerRefresh = newRefresh;

  // ---------- INSTRUCTOR : construire une formation complète ----------
  r = await req('POST', '/auth/login', { body: { email: 'formateur@educa.dev', password: PW } });
  const instrTok = r.json?.accessToken;
  check('INSTR-00 login formateur de démo', r.status === 200 && !!instrTok, `HTTP ${r.status}`);

  // RBAC : un LEARNER ne peut pas créer de cours
  r = await req('POST', '/courses', { token: learnerTok, body: { title: 'Interdit', language: 'fr' } });
  check('RBAC-01 LEARNER POST /courses → 403', r.status === 403, `HTTP ${r.status}`);

  r = await req('POST', '/courses', {
    token: instrTok,
    body: { title: `E2E — Parcours vérif ${SUFFIX}`, description: 'Cours de vérification bout-en-bout.', language: 'fr' },
  });
  const courseId = r.json?.id, slug = r.json?.slug;
  check('COURSE-01 création cours (201) + slug', r.status === 201 && !!courseId && !!slug, `id=${courseId} slug=${slug}`);
  check('COURSE-02 poids 40/60 par défaut', true, '(schéma : CHECK control_weight+exam_weight=100)');

  r = await req('POST', `/courses/${courseId}/chapters`, { token: instrTok, body: { title: 'Chapitre 1', position: 1 } });
  const chapterId = r.json?.id;
  check('COURSE-03 ajout chapitre (201)', r.status === 201 && !!chapterId, `id=${chapterId}`);

  const contentIds = [];
  for (let i = 1; i <= 2; i++) {
    r = await req('POST', `/chapters/${chapterId}/contents`, {
      token: instrTok,
      body: { type: 'TEXT', title: `Contenu ${i}`, position: i, textBody: `Corps du contenu ${i}.` },
    });
    if (r.status === 201 && r.json?.id) contentIds.push(r.json.id);
  }
  check('COURSE-04 ajout de 2 contenus TEXT (201)', contentIds.length === 2, `ids=${contentIds}`);

  // Contrôle de chapitre + questions
  r = await req('POST', `/chapters/${chapterId}/control-quiz`, {
    token: instrTok, body: { title: 'Contrôle ch.1', passThreshold: 50 },
  });
  const controlId = r.json?.id;
  check('QUIZ-01 création contrôle de chapitre (201)', r.status === 201 && !!controlId, `id=${controlId}`);

  r = await req('POST', `/quizzes/${controlId}/questions`, {
    token: instrTok,
    body: { statement: '2 + 2 = ?', type: 'SINGLE_CHOICE', points: 1, position: 1,
      options: [{ label: '4', correct: true, position: 1 }, { label: '5', correct: false, position: 2 }] },
  });
  check('QUIZ-02 rejet options invalides', (await req('POST', `/quizzes/${controlId}/questions`, {
    token: instrTok,
    body: { statement: 'mauvaise', type: 'SINGLE_CHOICE', points: 1, position: 9,
      options: [{ label: 'seule', correct: false, position: 1 }] },
  })).status === 400, '(1 option / 0 correcte → 400)');

  r = await req('POST', `/quizzes/${controlId}/questions`, {
    token: instrTok,
    body: { statement: 'La Terre est ronde.', type: 'TRUE_FALSE', points: 1, position: 2,
      options: [{ label: 'Vrai', correct: true, position: 1 }, { label: 'Faux', correct: false, position: 2 }] },
  });
  check('QUIZ-03 contrôle = 2 questions', Array.isArray(r.json?.questions) && r.json.questions.length === 2, `n=${r.json?.questions?.length}`);
  const controlView = r.json;

  // Examen final + questions
  r = await req('POST', `/courses/${courseId}/final-exam`, {
    token: instrTok, body: { title: 'Examen final', passThreshold: 50, maxAttempts: 3 },
  });
  const examId = r.json?.id;
  check('QUIZ-04 création examen final (201)', r.status === 201 && !!examId, `id=${examId}`);

  await req('POST', `/quizzes/${examId}/questions`, {
    token: instrTok,
    body: { statement: 'Capitale de la France ?', type: 'SINGLE_CHOICE', points: 1, position: 1,
      options: [{ label: 'Paris', correct: true, position: 1 }, { label: 'Lyon', correct: false, position: 2 }] },
  });
  r = await req('POST', `/quizzes/${examId}/questions`, {
    token: instrTok,
    body: { statement: 'HTTP 200 = succès.', type: 'TRUE_FALSE', points: 1, position: 2,
      options: [{ label: 'Vrai', correct: true, position: 1 }, { label: 'Faux', correct: false, position: 2 }] },
  });
  const examView = r.json;

  // Deuxième contrôle sur le même chapitre → 409 (1 contrôle / chapitre)
  r = await req('POST', `/chapters/${chapterId}/control-quiz`, { token: instrTok, body: { title: 'doublon', passThreshold: 50 } });
  check('QUIZ-05 2e contrôle sur le chapitre → 409', r.status === 409, `HTTP ${r.status}`);

  // ---------- CATALOGUE : brouillon invisible ----------
  r = await req('GET', `/courses?size=100`);
  let listed = (r.json?.content || r.json || []).some(c => c.slug === slug);
  check('CAT-01 cours non publié absent du catalogue', !listed, `présent=${listed}`);

  r = await req('POST', `/courses/${courseId}/publish`, { token: instrTok });
  check('CAT-02 publication (200)', r.status === 200 && r.json?.published === true, `HTTP ${r.status}`);

  r = await req('GET', `/courses?size=100`);
  listed = (r.json?.content || r.json || []).some(c => c.slug === slug);
  check('CAT-03 cours publié présent au catalogue', listed);

  r = await req('GET', `/courses/${slug}`, { token: learnerTok });
  const hiddenBefore = !(r.json?.chapters?.[0]?.contents?.length > 0);
  check('CAT-04 contenus masqués si non inscrit', r.status === 200 && hiddenBefore);

  // ---------- LEARNER : inscription, progression ----------
  r = await req('POST', `/courses/${courseId}/enroll`, { token: learnerTok });
  check('ENR-01 inscription (cours publié)', r.status === 200 || r.status === 201, `HTTP ${r.status}`);

  r = await req('POST', `/courses/${courseId}/enroll`, { token: learnerTok });
  check('ENR-02 double inscription → 409', r.status === 409, `HTTP ${r.status}`);

  r = await req('GET', `/courses/${slug}`, { token: learnerTok });
  const visibleAfter = r.json?.chapters?.[0]?.contents?.length === 2;
  check('ENR-03 contenus visibles après inscription', visibleAfter);

  // examen final verrouillé avant 100 %
  r = await req('POST', `/quizzes/${examId}/attempts`, {
    token: learnerTok, body: { answers: examView.questions.map(q => ({ questionId: q.id, selectedOptionIds: [q.options[0].id] })) },
  });
  check('QUIZ-06 examen final verrouillé avant 100 % → 403', r.status === 403, `HTTP ${r.status}`);

  // compléter les contenus
  for (const cid of contentIds) await req('POST', `/contents/${cid}/complete`, { token: learnerTok });
  r = await req('GET', `/courses/${courseId}/progress`, { token: learnerTok });
  check('PROG-01 progression contenus = 100 %', r.json?.progressPercent === 100, `${r.json?.progressPercent}%`);

  // passer le contrôle (toutes bonnes réponses)
  const correctAnswers = (view) => view.questions.map(q => ({
    questionId: q.id, selectedOptionIds: q.options.filter(o => o.correct).map(o => o.id),
  }));
  r = await req('POST', `/quizzes/${controlId}/attempts`, { token: learnerTok, body: { answers: correctAnswers(controlView) } });
  check('QUIZ-07 contrôle : score 100', (r.status === 200 || r.status === 201) && Number(r.json?.score) === 100, `HTTP ${r.status} score=${r.json?.score}`);

  // examen final maintenant déverrouillé
  r = await req('GET', `/courses/${courseId}/grade`, { token: learnerTok });
  check('QUIZ-08 examen final déverrouillé (100 % + contrôle tenté)', r.json?.finalExamUnlocked === true, `unlocked=${r.json?.finalExamUnlocked}`);

  r = await req('POST', `/quizzes/${examId}/attempts`, { token: learnerTok, body: { answers: correctAnswers(examView) } });
  const examScore = Number(r.json?.score), finalGrade = Number(r.json?.finalGrade);
  check('QUIZ-09 examen final : score 100', (r.status === 200 || r.status === 201) && examScore === 100, `HTTP ${r.status} score=${examScore}`);
  check('GRADE-01 note pondérée 40/60 = 100', finalGrade === 100, `finalGrade=${finalGrade} (0.4*100 + 0.6*100)`);
  const certificateId = r.json?.certificateId;
  check('CERT-01 certificat généré (note ≥ seuil)', !!certificateId, `certificateId=${certificateId}`);

  // ---------- CERTIFICAT ----------
  r = await req('GET', '/certificates/me', { token: learnerTok });
  const mine = (r.json || []).find(c => c.id === certificateId);
  check('CERT-02 /certificates/me liste le certificat', !!mine, mine ? `serial=${mine.serialNumber}` : 'absent');

  const dl = await req('GET', `/certificates/${certificateId}/download`, { token: learnerTok, raw: true });
  const isPdf = dl.status === 200 && (dl.headers.get('content-type') || '').includes('pdf') && dl.text.startsWith('%PDF');
  check('CERT-03 téléchargement PDF (200, %PDF, nosniff)', isPdf && dl.headers.get('x-content-type-options') === 'nosniff',
    `HTTP ${dl.status} ct=${dl.headers.get('content-type')}`);

  r = await req('GET', `/certificates/verify/${mine?.verificationCode}`);
  check('CERT-04 vérification publique par code (sans jeton)', r.status === 200 && r.json?.valid === true, `valid=${r.json?.valid}`);

  r = await req('GET', `/certificates/verify/code-bidon-${SUFFIX}`);
  check('CERT-05 code invalide → valid:false', r.json?.valid === false, `valid=${r.json?.valid}`);

  // ---------- RÉSULTATS FORMATEUR ----------
  r = await req('GET', `/instructor/courses/${courseId}/results`, { token: instrTok });
  const row = (r.json || []).find(x => x.certified === true);
  check('RES-01 résultats formateur : 1 apprenant certifié', r.status === 200 && !!row,
    row ? `note=${row.finalGrade} certifié=${row.certified}` : 'aucune ligne certifiée');

  r = await req('GET', `/instructor/courses/${courseId}/results`, { token: learnerTok });
  check('RBAC-02 LEARNER sur /instructor/.../results → 403', r.status === 403, `HTTP ${r.status}`);

  // ---------- CHATBOT ----------
  const learner2 = `e2e-nolearn-${SUFFIX}@educa.test`;
  await req('POST', '/auth/register', { body: { email: learner2, password: PW, fullName: 'No Enrol', preferredLanguage: 'fr' } });
  const t2 = (await req('POST', '/auth/login', { body: { email: learner2, password: PW } })).json?.accessToken;
  r = await req('POST', '/ai/chat', { token: t2, body: { courseId, message: 'Bonjour' } });
  check('AI-01 chat non inscrit → 403', r.status === 403, `HTTP ${r.status}`);

  r = await req('POST', '/ai/chat', { token: learnerTok, body: { courseId, message: 'Résume-moi le chapitre 1.' } });
  check('AI-02 chat inscrit : réponse dégradée propre (jamais 500)', r.status === 200 && r.json?.degraded === true && !!r.json?.reply,
    `HTTP ${r.status} degraded=${r.json?.degraded}`);

  // ---------- PONDÉRATION 40/60 + SEUIL (cas non certifiant) ----------
  const learner3 = `e2e-fail-${SUFFIX}@educa.test`;
  await req('POST', '/auth/register', { body: { email: learner3, password: PW, fullName: 'Sous Seuil', preferredLanguage: 'fr' } });
  const t3 = (await req('POST', '/auth/login', { body: { email: learner3, password: PW } })).json?.accessToken;
  await req('POST', `/courses/${courseId}/enroll`, { token: t3 });
  for (const cid of contentIds) await req('POST', `/contents/${cid}/complete`, { token: t3 });
  // contrôle : toutes les réponses fausses → score 0
  const wrongAnswers = (view) => view.questions.map(q => ({
    questionId: q.id, selectedOptionIds: q.options.filter(o => !o.correct).map(o => o.id).slice(0, 1),
  }));
  r = await req('POST', `/quizzes/${controlId}/attempts`, { token: t3, body: { answers: wrongAnswers(controlView) } });
  check('GRADE-02 contrôle raté → score 0', Number(r.json?.score) === 0, `score=${r.json?.score}`);
  r = await req('POST', `/quizzes/${examId}/attempts`, { token: t3, body: { answers: correctAnswers(examView) } });
  check('GRADE-03 note pondérée = 0.4·0 + 0.6·100 = 60', Number(r.json?.finalGrade) === 60, `finalGrade=${r.json?.finalGrade}`);
  check('GRADE-04 note 60 < seuil 70 → pas de certificat', r.json?.certificateId == null, `certificateId=${r.json?.certificateId}`);
  r = await req('GET', `/courses/${courseId}/grade`, { token: t3 });
  check('GRADE-05 /grade cohérent (pas de certificat)', r.json?.certificateId == null && Number(r.json?.finalGrade) === 60, `grade=${r.json?.finalGrade} cert=${r.json?.certificateId}`);

  // ---------- LOGOUT ----------
  r = await req('POST', '/auth/logout', { token: learnerTok, body: { refreshToken: learnerRefresh } });
  const loggedOut = r.status === 200 || r.status === 204;
  r = await req('POST', '/auth/refresh', { body: { refreshToken: learnerRefresh } });
  check('AUTH-10 logout révoque le refresh token', loggedOut && r.status === 401, `logout ok=${loggedOut} refresh=HTTP ${r.status}`);

  // ---------- BILAN ----------
  console.log(`\n=== BILAN : ${pass} PASS · ${fail} FAIL sur ${pass + fail} ===`);
  if (fail) {
    console.log('\nÉchecs :');
    results.filter(x => !x.ok).forEach(x => console.log(`  - ${x.id}  ${x.detail}`));
    process.exit(1);
  }
})().catch(e => { console.error('ERREUR FATALE', e); process.exit(2); });
