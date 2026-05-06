import org.junit.jupiter.api.*;
import tn.esprit.entities.Jeu;
import tn.esprit.services.ServiceJeu;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceJeuTest {

    static ServiceJeu service;
    static int idJeuTest;

    @BeforeAll
    static void setup() {
        service = new ServiceJeu();
    }

    @Test
    @Order(1)
    void testAjouterJeu() throws SQLException {
        Jeu jeu = new Jeu("JeuTestJUnit", "MOBA", "PC", "Description test", "ACTIF");
        service.ajouter(jeu);

        List<Jeu> jeux = service.getAll();
        assertFalse(jeux.isEmpty());
        assertTrue(jeux.stream().anyMatch(j -> j.getNom().equals("JeuTestJUnit")));

        idJeuTest = jeux.stream()
                .filter(j -> j.getNom().equals("JeuTestJUnit"))
                .findFirst()
                .orElseThrow()
                .getId();

        System.out.println("testAjouterJeu passed - id = " + idJeuTest);
    }

    @Test
    @Order(2)
    void testModifierJeu() throws SQLException {
        Jeu jeu = new Jeu(idJeuTest, "JeuTestModifie", "FPS", "PC", "Description modifiee", "INACTIF");
        service.modifier(jeu);

        List<Jeu> jeux = service.getAll();
        boolean trouve = jeux.stream()
                .anyMatch(j -> j.getId() == idJeuTest && j.getNom().equals("JeuTestModifie"));
        assertTrue(trouve);

        System.out.println("testModifierJeu passed");
    }

    @Test
    @Order(3)
    void testSupprimerJeu() throws SQLException {
        service.supprimer(idJeuTest);

        List<Jeu> jeux = service.getAll();
        boolean existe = jeux.stream().anyMatch(j -> j.getId() == idJeuTest);
        assertFalse(existe);

        System.out.println("testSupprimerJeu passed");
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        List<Jeu> jeux = service.getAll();
        jeux.stream()
                .filter(j -> j.getNom().equals("JeuTestJUnit") || j.getNom().equals("JeuTestModifie"))
                .forEach(j -> {
                    try {
                        service.supprimer(j.getId());
                    } catch (SQLException ignored) {
                    }
                });
        System.out.println("Nettoyage ServiceJeuTest termine");
    }
}
