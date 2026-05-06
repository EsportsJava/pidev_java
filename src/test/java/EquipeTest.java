import org.junit.jupiter.api.*;
import tn.esprit.entities.Equipe;
import tn.esprit.services.ServiceEquipe;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EquipeTest {

    static ServiceEquipe service;
    static int idEquipeTest;
    static final String BASE_NAME = "TestEquipeJunit";

    @BeforeAll
    static void setup() {
        service = new ServiceEquipe();
    }

    @Test
    @Order(1)
    void testAjouterEquipe() throws SQLException {
        Equipe equipe = new Equipe(BASE_NAME, 11, "logo-test.png");
        service.ajouter(equipe);

        List<Equipe> equipes = service.getAll();
        assertFalse(equipes.isEmpty());
        assertTrue(equipes.stream().anyMatch(e -> BASE_NAME.equals(e.getNom())));

        idEquipeTest = equipes.stream()
                .filter(e -> BASE_NAME.equals(e.getNom()))
                .findFirst()
                .orElseThrow()
                .getId();

        System.out.println("✅ testAjouterEquipe passed — id = " + idEquipeTest);
    }

    @Test
    @Order(2)
    void testAfficherEquipes() throws SQLException {
        List<Equipe> equipes = service.getAll();
        assertNotNull(equipes);
        assertFalse(equipes.isEmpty());
        System.out.println("✅ testAfficherEquipes passed — " + equipes.size() + " equipes");
    }

    @Test
    @Order(3)
    void testModifierEquipe() throws SQLException {
        Equipe equipe = new Equipe();
        equipe.setId(idEquipeTest);
        equipe.setNom(BASE_NAME + "Modifiee");
        equipe.setMaxMembers(15);
        equipe.setLogo("logo-modifie.png");

        service.modifier(equipe);

        List<Equipe> equipes = service.getAll();
        boolean trouve = equipes.stream()
                .anyMatch(e -> e.getId() == idEquipeTest && (BASE_NAME + "Modifiee").equals(e.getNom()));
        assertTrue(trouve);
        System.out.println("✅ testModifierEquipe passed");
    }

    @Test
    @Order(4)
    void testSupprimerEquipe() throws SQLException {
        service.supprimer(idEquipeTest);

        List<Equipe> equipes = service.getAll();
        boolean existe = equipes.stream().anyMatch(e -> e.getId() == idEquipeTest);
        assertFalse(existe);
        System.out.println("✅ testSupprimerEquipe passed");
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        List<Equipe> equipes = service.getAll();
        for (Equipe e : equipes) {
            if (BASE_NAME.equals(e.getNom()) || (BASE_NAME + "Modifiee").equals(e.getNom())) {
                service.supprimer(e.getId());
            }
        }
        System.out.println("🧹 Nettoyage Equipe terminé");
    }
}
