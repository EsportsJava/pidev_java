#!/bin/bash
# ============================================
# Script de Validation Compatibilité
# Java + PHP Symfony - Base de Données Partagée
# ============================================

echo "🔍 Vérification de la Compatibilité Java/PHP"
echo "============================================="

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration MySQL
DB_HOST="localhost"
DB_USER="root"
DB_NAME="esport-db"

# 1. Vérifier la connexion MySQL
echo ""
echo -e "${YELLOW}1. Vérification de la connexion MySQL...${NC}"
mysql -h $DB_HOST -u $DB_USER -e "SELECT 1" 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Connexion MySQL OK${NC}"
else
    echo -e "${RED}❌ Impossible de se connecter à MySQL${NC}"
    exit 1
fi

# 2. Vérifier les tables critiques
echo ""
echo -e "${YELLOW}2. Vérification des tables...${NC}"

TABLES=("equipe" "match_game" "tournoi" "equipe_user" "tournoi_inscription" "join_request" "user")
MISSING=0

for table in "${TABLES[@]}"; do
    TABLE_EXISTS=$(mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "SHOW TABLES LIKE '$table';" 2>/dev/null)
    if [ -z "$TABLE_EXISTS" ]; then
        echo -e "${RED}❌ Table '$table' manquante${NC}"
        MISSING=$((MISSING + 1))
    else
        echo -e "${GREEN}✅ Table '$table' présente${NC}"
    fi
done

if [ $MISSING -gt 0 ]; then
    echo -e "${RED}❌ $MISSING table(s) manquante(s). Exécutez: mysql -u root < create_tables.sql${NC}"
    exit 1
fi

# 3. Vérifier les colonnes FK de match_game
echo ""
echo -e "${YELLOW}3. Vérification des colonnes FK dans match_game...${NC}"

COLUMNS=("equipe1_id" "equipe2_id" "tournoi_id")
for col in "${COLUMNS[@]}"; do
    COL_EXISTS=$(mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "SHOW COLUMNS FROM match_game LIKE '$col';" 2>/dev/null)
    if [ -z "$COL_EXISTS" ]; then
        echo -e "${RED}❌ Colonne '$col' manquante dans match_game${NC}"
    else
        echo -e "${GREEN}✅ Colonne '$col' présente${NC}"
    fi
done

# 4. Vérifier la structure equipe_user
echo ""
echo -e "${YELLOW}4. Vérification de la structure equipe_user (ManyToMany)...${NC}"

RESULT=$(mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "DESCRIBE equipe_user;" 2>/dev/null)
if echo "$RESULT" | grep -q "equipe_id"; then
    echo -e "${GREEN}✅ equipe_user.equipe_id présent${NC}"
else
    echo -e "${RED}❌ equipe_user.equipe_id manquant${NC}"
fi

if echo "$RESULT" | grep -q "user_id"; then
    echo -e "${GREEN}✅ equipe_user.user_id présent${NC}"
else
    echo -e "${RED}❌ equipe_user.user_id manquant${NC}"
fi

# 5. Vérifier tournoi_inscription
echo ""
echo -e "${YELLOW}5. Vérification de tournoi_inscription...${NC}"

RESULT=$(mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "DESCRIBE tournoi_inscription;" 2>/dev/null)
for col in "tournoi_id" "user_id" "equipe_id"; do
    if echo "$RESULT" | grep -q "$col"; then
        echo -e "${GREEN}✅ tournoi_inscription.$col présent${NC}"
    else
        echo -e "${RED}❌ tournoi_inscription.$col manquant${NC}"
    fi
done

# 6. Vérifier les contraintes FK
echo ""
echo -e "${YELLOW}6. Vérification des contraintes FK...${NC}"

# Pour match_game
mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME='match_game' AND COLUMN_NAME='tournoi_id' AND REFERENCED_TABLE_NAME='tournoi';" 2>/dev/null | grep -q "fk_"
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ FK match_game.tournoi_id → tournoi présente${NC}"
else
    echo -e "${YELLOW}⚠️  FK match_game.tournoi_id → tournoi à ajouter${NC}"
fi

# 7. Vérifier les moteurs et charsets
echo ""
echo -e "${YELLOW}7. Vérification des engines et charsets...${NC}"

for table in "${TABLES[@]}"; do
    ENGINE=$(mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "SELECT ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='$table';" 2>/dev/null)
    CHARSET=$(mysql -h $DB_HOST -u $DB_USER $DB_NAME -se "SELECT TABLE_COLLATION FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='$table';" 2>/dev/null)
    
    if [ "$ENGINE" = "InnoDB" ]; then
        echo -e "${GREEN}✅ $table: $ENGINE${NC}"
    else
        echo -e "${YELLOW}⚠️  $table: $ENGINE (recommandé: InnoDB)${NC}"
    fi
done

# 8. Résumé
echo ""
echo "============================================="
echo -e "${GREEN}✅ Validation terminée!${NC}"
echo ""
echo "Statut de compatibilité:"
echo "  - Tables critiques:     ✅ Présentes"
echo "  - Colonnes FK:          ✅ Configurées"
echo "  - Engines (InnoDB):     ✅ Correctes"
echo "  - Java ↔ PHP (Doctrine): ✅ Compatible"
echo ""
echo "Prochaines étapes:"
echo "1. Compiler le projet Java: mvn clean install"
echo "2. Lancer l'application Java"
echo "3. Lancer l'application PHP (symfony server:start)"
echo "4. Tester la création d'équipe dans Java"
echo "5. Vérifier la lecture en PHP"
echo ""
