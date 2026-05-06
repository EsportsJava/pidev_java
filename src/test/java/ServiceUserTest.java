import org.junit.jupiter.api.*;
import tn.esprit.entities.User;
import tn.esprit.services.ServiceUser;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceUserTest {

    static ServiceUser service;
    static int idUserTest;

    @BeforeAll
    static void setup() {
        service = new ServiceUser();
    }

    @Test
    @Order(1)
    void testAjouterUser() throws SQLException {
        User u = new User();
        u.setEmail("sarra@esprit.tn");
        u.setPassword("Test@1234");
        u.setNom("Test User");
        u.setRoles("[\"ROLE_USER\"]");
        u.setActive(true);
        u.setIs2faEnabled(false);
        u.setFaceEnabled(false);

        service.ajouter(u);

        List<User> users = service.getAll();

        assertFalse(users.isEmpty());
        assertTrue(
                users.stream().anyMatch(user -> user.getEmail().equals("sarra@esprit.tn"))
        );

        idUserTest = users.stream()
                .filter(user -> user.getEmail().equals("sarra@esprit.tn"))
                .findFirst().get().getId();

        System.out.println("testAjouterUser passed — id = " + idUserTest);
    }

    @Test
    @Order(2)
    void testGetAllUsers() throws SQLException {
        List<User> users = service.getAll();

        assertNotNull(users);
        assertFalse(users.isEmpty());

        System.out.println("testGetAllUsers passed — " + users.size() + " utilisateurs");
    }

    @Test
    @Order(3)
    void testEmailExists() throws SQLException {
        boolean exists = service.emailExists("sarra@esprit.tn");

        assertTrue(exists);

        System.out.println("testEmailExists passed");
    }

    @Test
    @Order(4)
    void testAuthenticate_Success() throws SQLException {
        User user = service.authenticate("sarra@esprit.tn", "Test@1234");

        assertNotNull(user);
        assertEquals("sarra@esprit.tn", user.getEmail());

        System.out.println(" testAuthenticate_Success passed");
    }

    @Test
    @Order(5)
    void testAuthenticate_WrongPassword() throws SQLException {
        User user = service.authenticate("sarra@esprit.tn", "WrongPassword!");

        assertNull(user);

        System.out.println(" testAuthenticate_WrongPassword passed");
    }

    @Test
    @Order(6)
    void testModifierUser() throws SQLException {
        User u = new User();
        u.setId(idUserTest);
        u.setEmail("sarra@esprit.tn");
        u.setPassword("Test@1234");
        u.setNom("Nom Modifié");
        u.setRoles("[\"ROLE_USER\"]");
        u.setActive(true);
        u.setIs2faEnabled(false);
        u.setFaceEnabled(false);

        service.modifier(u);

        List<User> users = service.getAll();

        boolean trouve = users.stream()
                .anyMatch(user -> user.getNom().equals("Nom Modifié"));

        assertTrue(trouve);

        System.out.println(" testModifierUser passed");
    }

    @Test
    @Order(7)
    void testSupprimerUser() throws SQLException {
        service.supprimer(idUserTest);

        List<User> users = service.getAll();

        boolean existe = users.stream()
                .anyMatch(user -> user.getId() == idUserTest);

        assertFalse(existe);

        System.out.println("testSupprimerUser passed");
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        List<User> users = service.getAll();

        users.stream()
                .filter(user -> user.getEmail().equals("sarra@esprit.tn"))
                .forEach(user -> {
                    try {
                        service.supprimer(user.getId());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        System.out.println(" Nettoyage terminé");
    }
}