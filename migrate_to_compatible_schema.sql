-- ============================================
-- Migration vers schéma compatible PHP/Java
-- Date: 13 Mai 2026
-- ============================================

USE `esport-db`;

-- 1. Nettoyer les anciennes tables incompatibles
DROP TABLE IF EXISTS inscription_tournoi;
DROP TABLE IF EXISTS equipe_tournoi;

-- 2. Vérifier que match_game existe avec les bonnes colonnes
ALTER TABLE match_game MODIFY COLUMN equipe1_id INT NOT NULL;
ALTER TABLE match_game MODIFY COLUMN equipe2_id INT NOT NULL;
ALTER TABLE match_game MODIFY COLUMN tournoi_id INT NOT NULL;

-- 3. Ajouter les FKs manquantes pour match_game
ALTER TABLE match_game
ADD CONSTRAINT fk_match_equipe1 FOREIGN KEY (equipe1_id) REFERENCES equipe(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_match_equipe2 FOREIGN KEY (equipe2_id) REFERENCES equipe(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_match_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE;

-- 4. Vérifier que toutes les FK manquantes pour equipe_user sont présentes
ALTER TABLE equipe_user 
ADD CONSTRAINT fk_equipe_user_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_equipe_user_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE;

-- 5. Vérifier que tournoi_inscription a les bonnes FK
ALTER TABLE tournoi_inscription
ADD CONSTRAINT fk_ti_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_ti_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE;

-- 6. Créer les indexes pour performances
CREATE INDEX idx_equipe_user_user ON equipe_user(user_id);
CREATE INDEX idx_match_game_tournoi ON match_game(tournoi_id);
CREATE INDEX idx_match_game_equipe1 ON match_game(equipe1_id);
CREATE INDEX idx_match_game_equipe2 ON match_game(equipe2_id);
CREATE INDEX idx_tournoi_inscription_tournoi ON tournoi_inscription(tournoi_id);
CREATE INDEX idx_tournoi_inscription_equipe ON tournoi_inscription(equipe_id);

-- 7. Afficher le statut final
SELECT '✅ Migration complétée avec succès' AS status;
SELECT TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'esport-db' AND TABLE_NAME IN ('equipe', 'match_game', 'tournoi_inscription', 'equipe_user', 'join_request', 'team_invitation');
