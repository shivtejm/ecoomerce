package com.example.RegisterPage.config;

import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.entity.Product;
import com.example.RegisterPage.repository.CategoryRepository;
import com.example.RegisterPage.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Automatically seeds initial hierarchical categories and products
 * if the database is currently empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final com.example.RegisterPage.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           com.example.RegisterPage.repository.UserRepository userRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default Admin user if not exists
        com.example.RegisterPage.entity.User admin = userRepository.findByEmail("admin@mart.com").orElse(null);
        if (admin == null) {
            admin = new com.example.RegisterPage.entity.User(
                    "Admin",
                    "admin@mart.com",
                    passwordEncoder.encode("admin123"),
                    "ROLE_ADMIN"
            );
            admin = userRepository.save(admin);
        }

        if (categoryRepository.count() > 0) {
            return; // Categories & products already exist
        }

        // ==========================================
        // 1. TOP-LEVEL CATEGORIES (Level 1)
        // ==========================================
        Category electronics = new Category("Electronics", "electronics", 1, null);
        Category fashion = new Category("Fashion", "fashion", 1, null);
        Category homeLiving = new Category("Home & Living", "home-living", 1, null);
        categoryRepository.saveAll(List.of(electronics, fashion, homeLiving));

        // ==========================================
        // 2. PARENT CATEGORIES (Level 2)
        // ==========================================
        // Electronics Parents
        Category computers = new Category("Computers & Laptops", "computers-laptops", 2, electronics);
        Category mobiles = new Category("Mobiles & Audio", "mobiles-audio", 2, electronics);

        // Fashion Parents
        Category mensWear = new Category("Men's Wear", "mens-wear", 2, fashion);
        Category womensWear = new Category("Women's Wear", "womens-wear", 2, fashion);

        // Home & Living Parents
        Category furniture = new Category("Office Furniture", "office-furniture", 2, homeLiving);
        Category kitchen = new Category("Kitchen Appliances", "kitchen-appliances", 2, homeLiving);

        categoryRepository.saveAll(List.of(computers, mobiles, mensWear, womensWear, furniture, kitchen));

        // ==========================================
        // 3. CHILD CATEGORIES (Level 3)
        // ==========================================
        // Under Computers & Laptops
        Category gamingLaptops = new Category("Gaming Laptops", "gaming-laptops", 3, computers);
        Category ultrabooks = new Category("Ultrabooks", "ultrabooks", 3, computers);

        // Under Mobiles & Audio
        Category smartphones = new Category("Smartphones", "smartphones", 3, mobiles);
        Category wirelessEarbuds = new Category("Wireless Earbuds", "wireless-earbuds", 3, mobiles);

        // Under Men's Wear
        Category casualShirts = new Category("Casual Shirts", "casual-shirts", 3, mensWear);
        Category denimJeans = new Category("Denim Jeans", "denim-jeans", 3, mensWear);

        // Under Women's Wear
        Category dresses = new Category("Designer Dresses", "designer-dresses", 3, womensWear);
        Category handbags = new Category("Handbags", "handbags", 3, womensWear);

        // Under Office Furniture
        Category ergonomicChairs = new Category("Ergonomic Chairs", "ergonomic-chairs", 3, furniture);

        // Under Kitchen Appliances
        Category coffeeMakers = new Category("Coffee Makers", "coffee-makers", 3, kitchen);

        categoryRepository.saveAll(List.of(
                gamingLaptops, ultrabooks, smartphones, wirelessEarbuds,
                casualShirts, denimJeans, dresses, handbags,
                ergonomicChairs, coffeeMakers
        ));

        // ==========================================
        // 4. SAMPLE PRODUCTS LINKED TO CHILD CATEGORIES
        // ==========================================
        Product p1 = new Product(
                "Asus ROG Zephyrus G16",
                "High performance gaming laptop with Intel Core Ultra 9, RTX 4080, 32GB DDR5, and stunning 240Hz OLED display.",
                new BigDecimal("2199.99"),
                14,
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&auto=format&fit=crop&q=80",
                gamingLaptops,
                admin
        );

        Product p2 = new Product(
                "Apple MacBook Air M3",
                "Super-thin and fast ultrabook with Apple Silicon M3, 16GB unified memory, and up to 18 hours of battery life.",
                new BigDecimal("1299.00"),
                25,
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop&q=80",
                ultrabooks,
                admin
        );

        Product p3 = new Product(
                "Samsung Galaxy S24 Ultra",
                "Titanium frame flagship with 200MP camera, Snapdragon 8 Gen 3, integrated S-Pen, and Galaxy AI features.",
                new BigDecimal("1199.99"),
                30,
                "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800&auto=format&fit=crop&q=80",
                smartphones,
                admin
        );

        Product p4 = new Product(
                "Sony WH-1000XM5 Noise-Canceling Headphones",
                "Industry-leading active noise canceling wireless headphones with crystal-clear hands-free calling and 30-hour battery life.",
                new BigDecimal("349.99"),
                45,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80",
                wirelessEarbuds,
                admin
        );

        Product p5 = new Product(
                "Slim Fit Oxford Cotton Shirt",
                "Crafted from 100% premium breathable cotton with a crisp button-down collar, perfect for business-casual attire.",
                new BigDecimal("49.50"),
                80,
                "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800&auto=format&fit=crop&q=80",
                casualShirts,
                admin
        );

        Product p6 = new Product(
                "Vintage Wash Slim Straight Jeans",
                "Durable stretch denim offering classic vintage fading and maximum comfort for everyday style.",
                new BigDecimal("68.00"),
                60,
                "https://images.unsplash.com/photo-1542272604-780c96856592?w=800&auto=format&fit=crop&q=80",
                denimJeans,
                admin
        );

        Product p7 = new Product(
                "Floral Print Summer Midi Dress",
                "Elegant lightweight chiffon dress featuring a tailored waistline and flowing silhouette suitable for all occasions.",
                new BigDecimal("89.99"),
                35,
                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800&auto=format&fit=crop&q=80",
                dresses,
                admin
        );

        Product p8 = new Product(
                "Herman Miller Embody Ergonomic Chair",
                "Engineered with doctors and biomechanics experts for natural pressure distribution and ultimate posture support.",
                new BigDecimal("1495.00"),
                12,
                "https://images.unsplash.com/photo-1580481077114-1d8ef3df5309?w=800&auto=format&fit=crop&q=80",
                ergonomicChairs,
                admin
        );

        Product p9 = new Product(
                "De'Longhi Magnifica Espresso Machine",
                "Compact automatic espresso maker with integrated burr grinder, manual milk frother, and customizable drink options.",
                new BigDecimal("599.95"),
                18,
                "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=800&auto=format&fit=crop&q=80",
                coffeeMakers,
                admin
        );

        productRepository.saveAll(List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9));
    }
}
