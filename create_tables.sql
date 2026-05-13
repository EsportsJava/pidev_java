CREATE DATABASE IF NOT EXISTS `esport-db`;
USE `esport-db`;

CREATE TABLE IF NOT EXISTS test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS jeu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    plateforme VARCHAR(100),
    description TEXT,
    statut VARCHAR(50),
    lien_officiel VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS equipe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    max_members INT DEFAULT 5,
    logo VARCHAR(255),
    owner_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_equipe_user FOREIGN KEY (owner_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS equipe_user (
    equipe_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (equipe_id, user_id),
    CONSTRAINT fk_equipe_user_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_equipe_user_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournoi_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS match_game (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date_match DATETIME,
    score_team1 INT DEFAULT NULL,
    score_team2 INT DEFAULT NULL,
    statut VARCHAR(50) DEFAULT 'Planifié',
    equipe1_id INT NOT NULL,
    equipe2_id INT NOT NULL,
    tournoi_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_equipe1 FOREIGN KEY (equipe1_id) REFERENCES equipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_equipe2 FOREIGN KEY (equipe2_id) REFERENCES equipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tournoi_inscription (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tournoi_id INT NOT NULL,
    user_id INT DEFAULT NULL,
    equipe_id INT DEFAULT NULL,
    nom VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ti_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE,
    CONSTRAINT fk_ti_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT fk_ti_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS join_request (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipe_id INT NOT NULL,
    user_id INT NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_id INT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by_id INT,
    motif TEXT,
    CONSTRAINT fk_jr_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_jr_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT fk_jr_created_by FOREIGN KEY (created_by_id) REFERENCES `user`(id) ON DELETE SET NULL,
    CONSTRAINT fk_jr_updated_by FOREIGN KEY (updated_by_id) REFERENCES `user`(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    CONSTRAINT fk_stream_reaction_stream FOREIGN KEY (stream_id) REFERENCES stream(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    path TEXT,
    public_id VARCHAR(255),
    thumbnail VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS stream_comment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stream_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_stream_comment_stream (stream_id),
    CONSTRAINT fk_stream_comment_stream FOREIGN KEY (stream_id) REFERENCES stream(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_comment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    video_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_video_comment_video (video_id),
    CONSTRAINT fk_video_comment_video FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_reaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    video_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_video_reaction_video (video_id),
    CONSTRAINT fk_video_reaction_video FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO stream (url, is_active)
SELECT 'http://100.89.37.94:8080/hls/match1.m3u8', 1
WHERE NOT EXISTS (SELECT 1 FROM stream LIMIT 1);

CREATE TABLE IF NOT EXISTS blog (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(100),
    image_name VARCHAR(255),
    comment_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    blog_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_country VARCHAR(100) NULL,
    user_country_code VARCHAR(10) NULL,
    user_flag VARCHAR(16) NULL,
    KEY idx_comment_blog (blog_id),
    KEY idx_comment_user (user_id),
    CONSTRAINT fk_comment_blog FOREIGN KEY (blog_id) REFERENCES blog(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_reaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    reaction_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_comment_reaction (comment_id, user_id),
    KEY idx_comment_reaction_comment (comment_id),
    KEY idx_comment_reaction_user (user_id),
    CONSTRAINT fk_comment_reaction_comment FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_reaction_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS blog_like (
    id INT AUTO_INCREMENT PRIMARY KEY,
    blog_id INT NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_blog_like (blog_id, user_id),
    INDEX idx_blog_like_blog (blog_id),
    INDEX idx_blog_like_user (user_id),
    CONSTRAINT fk_blog_like_blog FOREIGN KEY (blog_id) REFERENCES blog(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_like_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rating (
    id INT AUTO_INCREMENT PRIMARY KEY,
    blog_id INT NOT NULL,
    user_id INT NOT NULL,
    value INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_blog_rating (blog_id, user_id),
    INDEX idx_blog_rating_blog (blog_id),
    INDEX idx_blog_rating_user (user_id),
    CONSTRAINT fk_blog_rating_blog FOREIGN KEY (blog_id) REFERENCES blog(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_rating_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
