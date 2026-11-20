package com.ims.inventory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.dao.CategoryDAO;
import com.ims.inventory.dao.UserDAO;
import com.ims.inventory.model.Category;
import com.ims.inventory.service.UserService;

@SpringBootApplication
public class InventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserService userService, UserDAO userDAO, CategoryDAO categoryDAO) {
		return args -> {
			try {
				Connection conn = DatabaseConnection.getInstance().getConnection();
				try (Statement st = conn.createStatement()) {
					// 1. Ensure activity_logs table exists
					st.execute("CREATE TABLE IF NOT EXISTS activity_logs (" +
						"log_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
						"action_type VARCHAR(50) NOT NULL, " +
						"entity_type VARCHAR(50) NOT NULL, " +
						"entity_name VARCHAR(255), " +
						"details TEXT, " +
						"performed_by VARCHAR(50) NOT NULL, " +
						"timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

					// 2. Ensure image_url column in products
					try {
						st.execute("ALTER TABLE products ADD COLUMN image_url VARCHAR(255)");
					} catch (SQLException ignored) {}
				}

				// 3. Ensure default admin and user exist if not present
				if (userDAO.findByUsername("admin").isEmpty()) {
					try {
						userService.register("admin", "admin123", "ADMIN");
						System.out.println(">>> Created default admin account: admin / admin123");
					} catch (Exception ignored) {}
				}

				if (userDAO.findByUsername("user").isEmpty()) {
					try {
						userService.register("user", "user123", "USER");
						System.out.println(">>> Created default user account: user / user123");
					} catch (Exception ignored) {}
				}

				// 4. Ensure default categories exist if table is empty
				try {
					if (categoryDAO.getAllCategories().isEmpty()) {
						categoryDAO.addCategory(new Category(null, "Electronics", "Computers, Mobiles, and Electronics"));
						categoryDAO.addCategory(new Category(null, "Clothing", "Apparel, Shoes, and Fashion"));
						categoryDAO.addCategory(new Category(null, "Groceries", "Food, Beverages, and Essentials"));
						categoryDAO.addCategory(new Category(null, "Home & Kitchen", "Furniture and Appliances"));
						categoryDAO.addCategory(new Category(null, "Office Supplies", "Stationery, Desks, and Printers"));
						System.out.println(">>> Seeded default product categories");
					}
				} catch (Exception ignored) {}

			} catch (Exception e) {
				System.err.println("Warning during startup database check: " + e.getMessage());
			}
		};
	}
}
