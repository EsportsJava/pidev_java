import org.junit.jupiter.api.*;
import tn.esprit.entities.Blog;
import tn.esprit.services.ServiceBlog;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceBlogTest {

    static ServiceBlog service;
    static int idBlogTest;

    @BeforeAll
    static void setup() {
        service = new ServiceBlog();
    }


    @Test
    @Order(1)
    void testAjouterBlog() {
        Blog b = new Blog();
        b.setTitle("Test Blog");
        b.setContent("Contenu de test");
        b.setCategory("TestCategory");
        b.setImageName("test.jpg");

        service.ajouter(b);

        List<Blog> blogs = service.afficher();

        assertFalse(blogs.isEmpty());
        assertTrue(
                blogs.stream().anyMatch(blog -> blog.getTitle().equals("Test Blog"))
        );

        idBlogTest = blogs.stream()
                .filter(blog -> blog.getTitle().equals("Test Blog"))
                .findFirst().get().getId();

        System.out.println("✅ testAjouterBlog passed — id = " + idBlogTest);
    }


    @Test
    @Order(2)
    void testAfficherBlogs() {
        List<Blog> blogs = service.afficher();

        assertNotNull(blogs);
        assertFalse(blogs.isEmpty());

        System.out.println("✅ testAfficherBlogs passed — " + blogs.size() + " blogs");
    }


    @Test
    @Order(3)
    void testModifierBlog() {
        Blog b = new Blog();
        b.setId(idBlogTest);
        b.setTitle("Blog Modifié");
        b.setContent("Contenu modifié");
        b.setCategory("UpdatedCategory");
        b.setImageName("updated.jpg");

        service.modifier(b);

        List<Blog> blogs = service.afficher();

        boolean trouve = blogs.stream()
                .anyMatch(blog -> blog.getTitle().equals("Blog Modifié"));

        assertTrue(trouve);

        System.out.println("✅ testModifierBlog passed");
    }


    @Test
    @Order(4)
    void testSupprimerBlog() {
        service.supprimer(idBlogTest);

        List<Blog> blogs = service.afficher();

        boolean existe = blogs.stream()
                .anyMatch(blog -> blog.getId() == idBlogTest);

        assertFalse(existe);

        System.out.println("✅ testSupprimerBlog passed");
    }


    @AfterAll
    static void cleanUp() {
        List<Blog> blogs = service.afficher();

        blogs.stream()
                .filter(blog -> blog.getTitle().equals("Test Blog")
                        || blog.getTitle().equals("Blog Modifié"))
                .forEach(blog -> service.supprimer(blog.getId()));

        System.out.println("🧹 Nettoyage terminé");
    }
}