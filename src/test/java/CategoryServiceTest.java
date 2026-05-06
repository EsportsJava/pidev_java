import org.junit.jupiter.api.*;
import tn.esprit.entities.Category;
import tn.esprit.services.CategoryService;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryServiceTest {

    static CategoryService service;
    static int idCategoryTest;

    @BeforeAll
    static void setup() {
        service = new CategoryService();
    }

    // ✅ Test 1 : Ajouter une catégorie
    @Test
    @Order(1)
    void testAjouterCategory() {
        Category c = new Category("TestCategory");
        service.addCategory(c);

        List<Category> categories = service.getAllCategories();
        assertFalse(categories.isEmpty());
        assertTrue(
                categories.stream().anyMatch(cat -> cat.getName().equals("TestCategory"))
        );

        idCategoryTest = categories.stream()
                .filter(cat -> cat.getName().equals("TestCategory"))
                .findFirst().get().getId();

        System.out.println("✅ testAjouterCategory passed — id = " + idCategoryTest);
    }

    // ✅ Test 2 : Afficher toutes les catégories
    @Test
    @Order(2)
    void testAfficherCategories() {
        List<Category> categories = service.getAllCategories();

        assertNotNull(categories);
        assertFalse(categories.isEmpty());
        System.out.println("✅ testAfficherCategories passed — " + categories.size() + " catégories");
    }

    // ✅ Test 3 : Modifier une catégorie
    @Test
    @Order(3)
    void testModifierCategory() {
        Category c = new Category();
        c.setId(idCategoryTest);
        c.setName("CategoryModifiee");
        service.updateCategory(c);

        List<Category> categories = service.getAllCategories();
        boolean trouve = categories.stream()
                .anyMatch(cat -> cat.getName().equals("CategoryModifiee"));
        assertTrue(trouve);
        System.out.println("✅ testModifierCategory passed");
    }

    // ✅ Test 4 : Supprimer une catégorie
    @Test
    @Order(4)
    void testSupprimerCategory() {
        service.deleteCategory(idCategoryTest);

        List<Category> categories = service.getAllCategories();
        boolean existe = categories.stream()
                .anyMatch(cat -> cat.getId() == idCategoryTest);
        assertFalse(existe);
        System.out.println("✅ testSupprimerCategory passed");
    }

    // ✅ Nettoyage à la fin
    @AfterAll
    static void cleanUp() {
        List<Category> categories = service.getAllCategories();
        categories.stream()
                .filter(cat -> cat.getName().equals("TestCategory")
                        || cat.getName().equals("CategoryModifiee"))
                .forEach(cat -> service.deleteCategory(cat.getId()));
        System.out.println("Nettoyage terminé");
    }
}