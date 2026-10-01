1= lancer frontend 

cd frontend/
npm start

Pour les relancer toi-même plus tard :
- Backend : cd backend && ./mvnw spring-boot:run
- Relais Stripe, seulement si tu refais un paiement : stripe listen --api-key <ta sk_test_…> --events checkout.session.completed --forward-to localhost:8081/api/v1/payments/webhooks/stripe

Pour que Claude Code n'arrête plus ces tâches en cas de mémoire faible, démarre-le avec la variable CLAUDE_CODE_DISABLE_BG_SHELL_PRESSURE_REAP=1 dans l'environnement.
