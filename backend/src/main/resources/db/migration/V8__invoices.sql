-- Facture PDF par paiement réussi — pas de nouvelle table, `payments` porte directement la facture
-- (même esprit que `certificates` : numéro + clé du PDF généré, patron déjà en place).

ALTER TABLE payments ADD COLUMN invoice_number VARCHAR(40) UNIQUE;
ALTER TABLE payments ADD COLUMN pdf_key VARCHAR(500);
