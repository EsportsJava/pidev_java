import org.junit.jupiter.api.*;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceTournoiTest {

    static ServiceTournoi serviceTournoi;
    static ServiceJeu serviceJeu;
    static int idTournoiTest;
    static int idJeuSupport;

    @BeforeAll
    static void setup() throws SQLException {
        serviceTournoi = new ServiceTournoi();
        serviceJeu = new ServiceJeu();

        Jeu jeuSupport = new Jeu("JeuSupportTournoiTest", "Sport", "PC", "Jeu support pour FK tournoi", "ACTIF");
        serviceJeu.ajouter(jeuSupport);

        idJeuSupport = serviceJeu.getAll().stream()
                .filter(j -> j.getNom().equals("JeuSupportTournoiTest"))
                .map(Jeu::getId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Order(1)
    void testAjouterTournoi() throws SQLException {
        Date now = new Date();
        Date dateDebut = new Date(now.getTime() + 86_400_000L);
        Date dateFin = new Date(now.getTime() + 2L * 86_400_000L);

        Tournoi tournoi = new Tournoi(
                "TournoiTestJUnit",
                dateDebut,
                dateFin,
                "OUVERT",
                "SOLO",
                32,
                1000.0,
                now,
                10.0,
                "Description test",
                idJeuSupport
        );

        serviceTournoi.ajouter(tournoi);

        List<Tournoi> tournois = serviceTournoi.getAll();
        assertTrue(tournois.stream().anyMatch(t -> t.getNom().equals("TournoiTestJUnit")));

        idTournoiTest = tournois.stream()
                .filter(t -> t.getNom().equals("TournoiTestJUnit"))
                .findFirst()
                .orElseThrow()
                .getId();

        System.out.println("testAjouterTournoi passed - id = " + idTournoiTest);
    }

    @Test
    @Order(2)
    void testModifierTournoi() throws SQLException {
        Date now = new Date();
        Date dateDebut = new Date(now.getTime() + 3L * 86_400_000L);
        Date dateFin = new Date(now.getTime() + 4L * 86_400_000L);

        Tournoi tournoi = new Tournoi(
                idTournoiTest,
                "TournoiTestModifie",
                dateDebut,
                dateFin,
                "FERME",
                "TEAM",
                16,
                2000.0,
                now,
                20.0,
                "Description modifiee",
                idJeuSupport
        );

        serviceTournoi.modifier(tournoi);

        List<Tournoi> tournois = serviceTournoi.getAll();
        boolean trouve = tournois.stream()
                .anyMatch(t -> t.getId() == idTournoiTest && t.getNom().equals("TournoiTestModifie"));
        assertTrue(trouve);

        System.out.println("testModifierTournoi passed");
    }

    @Test
    @Order(3)
    void testSupprimerTournoi() throws SQLException {
        serviceTournoi.supprimer(idTournoiTest);

        List<Tournoi> tournois = serviceTournoi.getAll();
        boolean existe = tournois.stream().anyMatch(t -> t.getId() == idTournoiTest);
        assertFalse(existe);

        System.out.println("testSupprimerTournoi passed");
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        List<Tournoi> tournois = serviceTournoi.getAll();
        tournois.stream()
                .filter(t -> t.getNom().equals("TournoiTestJUnit") || t.getNom().equals("TournoiTestModifie"))
                .forEach(t -> {
                    try {
                        serviceTournoi.supprimer(t.getId());
                    } catch (SQLException ignored) {
                    }
                });

        List<Jeu> jeux = serviceJeu.getAll();
        jeux.stream()
                .filter(j -> j.getNom().equals("JeuSupportTournoiTest"))
                .forEach(j -> {
                    try {
                        serviceJeu.supprimer(j.getId());
                    } catch (SQLException ignored) {
                    }
                });

        System.out.println("Nettoyage ServiceTournoiTest termine");
    }
}
