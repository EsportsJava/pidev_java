-- Table d'inscriptions tournois (pour bouton "S'inscrire" + KPIs de remplissage).
-- MySQL / MariaDB

CREATE TABLE IF NOT EXISTS tournoi_inscription (
  id INT AUTO_INCREMENT PRIMARY KEY,
  tournoi_id INT NOT NULL,
  user_id INT NULL,
  nom VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ti_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE,
  UNIQUE KEY uk_ti_unique (tournoi_id, user_id, email)
);

