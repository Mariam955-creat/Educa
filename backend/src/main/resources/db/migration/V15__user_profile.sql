-- Profil utilisateur enrichi : titre et biographie (affichés aux apprenants pour un formateur), pays, téléphone.
ALTER TABLE users ADD COLUMN headline VARCHAR(120);
ALTER TABLE users ADD COLUMN bio      TEXT;
ALTER TABLE users ADD COLUMN country  VARCHAR(2);
ALTER TABLE users ADD COLUMN phone    VARCHAR(30);
