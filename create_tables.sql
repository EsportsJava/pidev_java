-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS `esport-db`;
USE `esport-db`;

-- Table test
CREATE TABLE IF NOT EXISTS test (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    name VARCHAR(255) NOT NULL
    );

-- Table jeu
CREATE TABLE IF NOT EXISTS jeu (
                                   id INT AUTO_INCREMENT PRIMARY KEY,
                                   nom VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    plateforme VARCHAR(100),
    description TEXT,
    statut VARCHAR(50)
    );

-- Table tournoi
CREATE TABLE IF NOT EXISTS tournoi (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       nom VARCHAR(255) NOT NULL,
    date_debut DATE,
    date_fin DATE,
    statut VARCHAR(50),
    type VARCHAR(100),
    max_participants INT,
    cagnotte DOUBLE,
    date_inscription_limite DATE,
    frais_inscription DOUBLE,
    description TEXT,
    jeu_id INT,
    FOREIGN KEY (jeu_id) REFERENCES jeu(id)
    );

-- Table profiling
CREATE TABLE IF NOT EXISTS profiling (
                                         id INT AUTO_INCREMENT PRIMARY KEY,
                                         nom VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    plateforme VARCHAR(100),
    description TEXT,
    statut VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS `user` (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      email VARCHAR(255) NOT NULL UNIQUE,
    roles VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    google2fa_secret VARCHAR(255),
    is_2fa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    google_oauth_id VARCHAR(255),
    oauth_provider VARCHAR(100),
    profile_image_url TEXT,
    face_encoding TEXT,
    is_face_enabled BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS password_reset_token (
                                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                                    user_id INT NOT NULL,
                                                    token VARCHAR(20) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
    );

-- Stream / vidéos : même base que l'app (jdbc …/esport-db), pas une base séparée
CREATE TABLE IF NOT EXISTS stream (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      url VARCHAR(512),
    is_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS stream_reaction (
                                               id INT AUTO_INCREMENT PRIMARY KEY,
                                               type VARCHAR(50) NOT NULL,
    comment TEXT,
    username VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stream_id INT NOT NULL,
    FOREIGN KEY (stream_id) REFERENCES stream(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     title VARCHAR(255),
    path TEXT
    );

INSERT INTO stream (url, is_active)
SELECT 'http://192.168.100.81:8080/hls/match1.m3u8', 1
    WHERE NOT EXISTS (SELECT 1 FROM stream LIMIT 1);
