package be.heh.clipclap.catalog.internal;

import be.heh.clipclap.catalog.Category;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class CategoryRepository {

	private final JdbcClient jdbc;

	CategoryRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	List<Category> findAll() {
		return jdbc.sql("SELECT id, name FROM categories ORDER BY name")
				.query(Category.class)
				.list();
	}
}
