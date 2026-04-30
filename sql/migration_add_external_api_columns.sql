USE `esport-db`;

ALTER TABLE jeu
    ADD COLUMN IF NOT EXISTS lien_officiel VARCHAR(500) NULL;
