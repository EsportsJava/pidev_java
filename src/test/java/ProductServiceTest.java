import org.junit.jupiter.api.*;
import tn.esprit.entities.Product;
import tn.esprit.services.ProductService;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductServiceTest {

    static ProductService service;
    static int idProductTest;

    @BeforeAll
    static void setup() {
        service = new ProductService();
    }

    // ✅ Test 1 : Ajouter un produit
    @Test
    @Order(1)
    void testAjouterProduct() {
        Product p = new Product("TestProduct", "TestDesc", 99.99, 5, "test.png", 1);
        service.addProduct(p);

        List<Product> products = service.getAllProducts();
        assertFalse(products.isEmpty());
        assertTrue(
                products.stream().anyMatch(prod -> prod.getName().equals("TestProduct"))
        );

        idProductTest = products.stream()
                .filter(prod -> prod.getName().equals("TestProduct"))
                .findFirst().get().getId();

        System.out.println("✅ testAjouterProduct passed — id = " + idProductTest);
    }

    // ✅ Test 2 : Afficher tous les produits
    @Test
    @Order(2)
    void testAfficherProducts() {
        List<Product> products = service.getAllProducts();

        assertNotNull(products);
        assertFalse(products.isEmpty());
        System.out.println("✅ testAfficherProducts passed — " + products.size() + " produits");
    }

    // ✅ Test 3 : Modifier un produit
    @Test
    @Order(3)
    void testModifierProduct() {
        Product p = new Product();
        p.setId(idProductTest);
        p.setName("ProductModifie");
        p.setDescription("DescModifiee");
        p.setPrice(149.99);
        p.setStock(10);
        p.setImage("modif.png");
        p.setCategoryId(1);
        service.updateProduct(p);

        List<Product> products = service.getAllProducts();
        boolean trouve = products.stream()
                .anyMatch(prod -> prod.getName().equals("ProductModifie"));
        assertTrue(trouve);
        System.out.println("✅ testModifierProduct passed");
    }

    // ✅ Test 4 : Supprimer un produit
    @Test
    @Order(4)
    void testSupprimerProduct() {
        service.deleteProduct(idProductTest);

        List<Product> products = service.getAllProducts();
        boolean existe = products.stream()
                .anyMatch(prod -> prod.getId() == idProductTest);
        assertFalse(existe);
        System.out.println("✅ testSupprimerProduct passed");
    }

    // ✅ Nettoyage à la fin
    @AfterAll
    static void cleanUp() {
        List<Product> products = service.getAllProducts();
        products.stream()
                .filter(prod -> prod.getName().equals("TestProduct")
                        || prod.getName().equals("ProductModifie"))
                .forEach(prod -> service.deleteProduct(prod.getId()));
        System.out.println("🧹 Nettoyage terminé");
    }
}