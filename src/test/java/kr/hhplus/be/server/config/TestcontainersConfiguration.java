package kr.hhplus.be.server.config;

import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.annotation.PreDestroy;

@Configuration
class TestcontainersConfiguration {

	public static final MySQLContainer<?> MYSQL_CONTAINER;

	static {
		MySQLContainer<?> tempContainer = null;
		try {
			tempContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
				.withDatabaseName("hhplus")
				.withUsername("test")
				.withPassword("test");
			tempContainer.start();
			System.setProperty("spring.datasource.url", tempContainer.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC");
			System.setProperty("spring.datasource.username", tempContainer.getUsername());
			System.setProperty("spring.datasource.password", tempContainer.getPassword());
		} catch (Exception e) {
			System.err.println("[TestcontainersConfiguration] MySQL Testcontainer 시작 실패 - application.yml datasource 사용: " + e.getMessage());
		}
		MYSQL_CONTAINER = tempContainer;
	}

	@PreDestroy
	public void preDestroy() {
		if (MYSQL_CONTAINER != null && MYSQL_CONTAINER.isRunning()) {
			MYSQL_CONTAINER.stop();
		}
	}
}