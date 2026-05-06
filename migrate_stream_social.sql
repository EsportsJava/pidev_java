-- À exécuter dans phpMyAdmin sur la base esport-db si stream_comment / video_comment / video_reaction manquent.
-- Pas de clés étrangères (évite l'erreur MySQL 150 sur d'anciens schémas).

USE `esport-db`;

CREATE TABLE IF NOT EXISTS stream_comment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stream_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_stream_comment_stream (stream_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_comment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    video_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_video_comment_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_reaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    video_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_video_reaction_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
