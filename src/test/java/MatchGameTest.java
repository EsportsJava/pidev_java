import org.junit.jupiter.api.*;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.MatchGame;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceEquipe;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceMatchGame;
import tn.esprit.services.ServiceTournoi;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MatchGameTest {

    static ServiceMatchGame serviceMatch;
    static ServiceEquipe serviceEquipe;
    static ServiceJeu serviceJeu;
    static ServiceTournoi serviceTournoi;

    static int idMatchTest;
    static int idEquipe1;
    static int idEquipe2;
    static int idJeu;
    static int idTournoi;

    static final String EQ1_NAME = "EquipeMG_A";
    static final String EQ2_NAME = "EquipeMG_B";
    static final String JEU_NAME = "JeuMG_Test";
    static final String TOURNOI_NAME = "TournoiMG_Test";

    @BeforeAll
    static void setup() throws SQLException {
        serviceMatch = new ServiceMatchGame();
        serviceEquipe = new ServiceEquipe();
        serviceJeu = new ServiceJeu();
        serviceTournoi = new ServiceTournoi();

        preparePrerequisites();
    }

    private static void preparePrerequisites() throws SQLException {
        serviceJeu.ajouter(new Jeu(JEU_NAME, "MOBA", "PC", "Jeu test", "Actif"));
        idJeu = serviceJeu.getAll().stream()
                .filter(j -> JEU_NAME.equals(j.getNom()))
                .findFirst()
                .orElseThrow()
                .getId();

        Date now = new Date();
        Date start = new Date(now.getTime() + 86400000L);
        Date end = new Date(now.getTime() + 172800000L);
        Date limit = new Date(now.getTime() + 43200000L);

        Tournoi tournoi = new Tournoi(
                TOURNOI_NAME,
                start,
                end,
                "Ouvert",
                "Team",
                16,
                1000.0,
                limit,
                20.0,
                "Tournoi test matchgame",
                idJeu
        );
        serviceTournoi.ajouter(tournoi);
        idTournoi = serviceTournoi.getAll().stream()
                .filter(t -> TOURNOI_NAME.equals(t.getNom()))
                .findFirst()
                .orElseThrow()
                .getId();

        serviceEquipe.ajouter(new Equipe(EQ1_NAME, 11, "eq1.png"));
        serviceEquipe.ajouter(new Equipe(EQ2_NAME, 11, "eq2.png"));

        idEquipe1 = serviceEquipe.getAll().stream()
                .filter(e -> EQ1_NAME.equals(e.getNom()))
                .findFirst()
                .orElseThrow()
                .getId();

        idEquipe2 = serviceEquipe.getAll().stream()
                .filter(e -> EQ2_NAME.equals(e.getNom()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Test
    @Order(1)
    void testAjouterMatchGame() throws SQLException {
        Timestamp date = Timestamp.valueOf("2026-04-16 18:30:00");
        MatchGame match = new MatchGame(date, 2, 1, "Planifie", idEquipe1, idEquipe2, idTournoi);
        serviceMatch.ajouter(match);

        List<MatchGame> matchs = serviceMatch.getAll();
        assertFalse(matchs.isEmpty());

        MatchGame inserted = matchs.stream()
                .filter(m -> m.getTournoiId() == idTournoi
                        && m.getEquipe1Id() == idEquipe1
                        && m.getEquipe2Id() == idEquipe2)
                .findFirst()
                .orElseThrow();

        idMatchTest = inserted.getId();
        System.out.println("✅ testAjouterMatchGame passed — id = " + idMatchTest);
    }

    @Test
    @Order(2)
    void testAfficherMatchGames() throws SQLException {
        List<MatchGame> matchs = serviceMatch.getAll();
        assertNotNull(matchs);
        assertFalse(matchs.isEmpty());
        System.out.println("✅ testAfficherMatchGames passed — " + matchs.size() + " matchs");
    }

    @Test
    @Order(3)
    void testModifierMatchGame() throws SQLException {
        MatchGame match = new MatchGame();
        match.setId(idMatchTest);
        match.setDateMatch(Timestamp.valueOf("2026-04-17 21:00:00"));
        match.setScoreTeam1(3);
        match.setScoreTeam2(2);
        match.setStatut("Termine");
        match.setEquipe1Id(idEquipe1);
        match.setEquipe2Id(idEquipe2);
        match.setTournoiId(idTournoi);

        serviceMatch.modifier(match);

        boolean trouve = serviceMatch.getAll().stream()
                .anyMatch(m -> m.getId() == idMatchTest
                        && Integer.valueOf(3).equals(m.getScoreTeam1())
                        && Integer.valueOf(2).equals(m.getScoreTeam2())
                        && "Termine".equals(m.getStatut()));

        assertTrue(trouve);
        System.out.println("✅ testModifierMatchGame passed");
    }

    @Test
    @Order(4)
    void testSupprimerMatchGame() throws SQLException {
        serviceMatch.supprimer(idMatchTest);

        boolean existe = serviceMatch.getAll().stream().anyMatch(m -> m.getId() == idMatchTest);
        assertFalse(existe);
        System.out.println("✅ testSupprimerMatchGame passed");
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        for (MatchGame m : serviceMatch.getAll()) {
            if (m.getTournoiId() == idTournoi
                    && m.getEquipe1Id() == idEquipe1
                    && m.getEquipe2Id() == idEquipe2) {
                serviceMatch.supprimer(m.getId());
            }
        }

        if (idTournoi > 0) {
            serviceTournoi.supprimer(idTournoi);
        }
        if (idEquipe1 > 0) {
            serviceEquipe.supprimer(idEquipe1);
        }
        if (idEquipe2 > 0) {
            serviceEquipe.supprimer(idEquipe2);
        }
        if (idJeu > 0) {
            serviceJeu.supprimer(idJeu);
        }

        for (Tournoi t : serviceTournoi.getAll()) {
            if (TOURNOI_NAME.equals(t.getNom())) {
                serviceTournoi.supprimer(t.getId());
            }
        }
        for (Equipe e : serviceEquipe.getAll()) {
            if (EQ1_NAME.equals(e.getNom()) || EQ2_NAME.equals(e.getNom())) {
                serviceEquipe.supprimer(e.getId());
            }
        }
        for (Jeu j : serviceJeu.getAll()) {
            if (JEU_NAME.equals(j.getNom())) {
                serviceJeu.supprimer(j.getId());
            }
        }

        System.out.println("🧹 Nettoyage MatchGame termine");
    }
}
