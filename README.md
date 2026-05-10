# Esportify Desktop - eSports Management Platform

## Overview

This project was developed as part of the **PIDEV - 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year **2025-2026**).

**Esportify Desktop** is a Java desktop application built with **JavaFX** for managing eSports activities and community features in one platform. The application covers tournament and match management, teams, users, blogs, products, livestream content, and player statistics through an interactive desktop interface.

## Features

- User authentication and profile management
- Two-factor authentication (TOTP + QR code)
- Face recognition login support
- Tournament management and registration
- Game and match management
- Team creation, membership requests, and rankings
- Blog, comments, likes, ratings, and reports
- Product catalog, cart, checkout, and order management
- Stream and video management with reactions and comments
- Notification system
- Statistics dashboards and KPI views
- External API integration for gaming data and services

## Tech Stack

### Backend / Application Logic

- Java 17
- Maven
- JDBC

### Desktop Frontend

- JavaFX
- FXML
- CSS

### Database

- MySQL

### Integrations & Libraries

- Cloudinary
- OpenCV
- ZXing
- jBCrypt
- Stripe Java SDK
- Jakarta Mail
- WebSocket

## Architecture

The application follows a layered Java desktop architecture:

- **Entities**: domain models such as `User`, `Tournoi`, `Jeu`, `Equipe`, `MatchGame`, `Blog`, `Product`, `Stream`, and `Video`
- **Services**: business logic and database access with JDBC
- **Controllers**: JavaFX controllers handling UI actions and workflows
- **Views**: FXML interfaces and CSS styles
- **Utils**: database connection, session handling, configuration, and helper classes

## Main Modules

- **User & Security**: login, registration, password reset, 2FA, face recognition
- **Tournaments**: creation, dashboard, inscriptions, matches, KPIs
- **Games**: game management, catalog, dashboard, KPIs
- **Teams**: team creation, owner dashboard, join requests, rankings
- **Matches**: match management and validation
- **Blogs**: posts, comments, ratings, likes, moderation
- **Shop**: categories, products, cart, checkout, orders
- **Streaming**: live streams, uploaded videos, reactions, comments
- **Statistics**: dashboards and game-related stats

## Getting Started

### Prerequisites

- Java 17
- Maven 3.8+
- MySQL Server
- JavaFX-compatible environment

### Installation

```bash
# Clone the repository
git clone https://github.com/your-username/pidev_java.git

# Navigate to the project directory
cd pidev_java

# Install dependencies and compile
mvn clean install
```

### Database Setup

1. Create the MySQL database:

```sql
CREATE DATABASE `esport-db`;
```

2. Import the main SQL script:

```bash
mysql -u root -p esport-db < create_tables.sql
```

3. If needed, apply additional migration scripts from the `sql/` folder.

### Configuration

Before launching the application, review these configuration points:

- Database connection in [MyDatabase.java](/C:/Users/MSI/pidev_java/src/main/java/tn/esprit/utils/MyDatabase.java)
- Azure Face config in `azure-face.properties`
- Cloudinary config in `cloudinary.properties`
- Optional external API keys in [ApiKeys.java](/C:/Users/MSI/pidev_java/src/main/java/tn/esprit/utils/ApiKeys.java)

Use the provided example files as templates:

- `azure-face.properties.example`
- `cloudinary.properties.example`

### Run the Application

```bash
mvn javafx:run
```

The configured entry point is:

`tn.esprit.test.FxLauncher`

The application currently starts from the login screen:

`/Login.fxml`



## Contributors

Rajhi Mohamed Aziz
Chaabani Sarra
Saada Maryem
Benmansour Salma
Jery Mouhamed Amine
Laamouri Tayssir

## Academic Context

Developed at **Esprit School of Engineering - Tunisia**  
**PIDEV - 3A | 2025-2026**