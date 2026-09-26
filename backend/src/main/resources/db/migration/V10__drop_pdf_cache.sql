-- Certificats et factures sont désormais générés à chaque téléchargement, dans la langue de l'interface :
-- l'ancien cache (clé de stockage d'un PDF unique, figé dans la langue du 1er téléchargement) est supprimé.
-- Les fichiers déjà stockés (certificates/, invoices/) deviennent orphelins et peuvent être effacés.
ALTER TABLE certificates DROP COLUMN pdf_key;
ALTER TABLE payments DROP COLUMN pdf_key;
