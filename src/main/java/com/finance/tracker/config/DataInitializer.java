package com.finance.tracker.config;

import com.finance.tracker.model.Category;
import com.finance.tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = List.of(
                Category.builder().name("Food").icon("🍔").colorHex("#FF6B6B").isDefault(true).build(),
                Category.builder().name("Transport").icon("🚗").colorHex("#4ECDC4").isDefault(true).build(),
                Category.builder().name("Shopping").icon("🛍️").colorHex("#45B7D1").isDefault(true).build(),
                Category.builder().name("Entertainment").icon("🎬").colorHex("#96CEB4").isDefault(true).build(),
                Category.builder().name("Health").icon("💊").colorHex("#FFEAA7").isDefault(true).build(),
                Category.builder().name("Utilities").icon("⚡").colorHex("#DDA0DD").isDefault(true).build(),
                Category.builder().name("Education").icon("📚").colorHex("#98D8C8").isDefault(true).build(),
                Category.builder().name("Travel").icon("✈️").colorHex("#F7DC6F").isDefault(true).build(),
                Category.builder().name("Groceries").icon("🛒").colorHex("#82E0AA").isDefault(true).build(),
                Category.builder().name("Rent").icon("🏠").colorHex("#F0B27A").isDefault(true).build(),
                Category.builder().name("Subscriptions").icon("📱").colorHex("#AED6F1").isDefault(true).build(),
                Category.builder().name("Other").icon("📌").colorHex("#D5D8DC").isDefault(true).build()
            );
            categoryRepository.saveAll(defaultCategories);
            log.info("✅ Seeded {} default categories", defaultCategories.size());
        }
    }
}
