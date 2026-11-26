package com.santosediego.donorregistry.auth.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.santosediego.donorregistry.auth.entities.UserAccount;
import com.santosediego.donorregistry.auth.services.PasswordHasher;
import com.santosediego.donorregistry.auth.services.UserAccountFactory;
import com.santosediego.donorregistry.support.TestPasswords;

/**
 * Tests what the database itself guarantees about tb_user_account. Real commits (no @Transactional).
 * The table starts empty (it is never seeded) and is emptied after each test.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserAccountDatabaseTest {

	private static final String PLACEHOLDER_HASH = "not-a-real-hash";

	private final UserAccountRepository repository;
	private final JdbcTemplate jdbc;
	private final PasswordHasher hasher = new PasswordHasher();
	private final UserAccountFactory factory = new UserAccountFactory(hasher);

	private boolean tableWasEmpty;

	@Autowired
	UserAccountDatabaseTest(UserAccountRepository repository, JdbcTemplate jdbc) {
		this.repository = repository;
		this.jdbc = jdbc;
	}

	@BeforeEach
	void tableStartsEmpty() {
		tableWasEmpty = repository.count() == 0;
		assertThat(tableWasEmpty).as("tb_user_account must start empty (no seeds, no leftovers)").isTrue();
	}

	@AfterEach
	void deleteTheAccountsOfTheTest() {
		// Never deletes an account that was there before the test
		if (tableWasEmpty) {
			repository.deleteAll();
		}
	}

	@Test
	@DisplayName("Dado uma conta nova, quando grava e lê de volta, então o nome é igual, o banco guarda só o hash e createdAt é o momento da gravação")
	void storesUsernameAndHashOnly() {
		// Arrange
		String password = TestPasswords.next();
		UserAccount account = factory.newAccount("admin.teste", password);
		Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

		// Act
		Long id = repository.saveAndFlush(account).getId();
		Instant after = Instant.now().plus(1, ChronoUnit.MICROS);

		// Assert
		Map<String, Object> row = jdbc.queryForMap("SELECT username, password_hash FROM tb_user_account WHERE id = ?", id);
		String storedHash = (String) row.get("password_hash");
		assertThat(row.get("username")).isEqualTo("admin.teste");
		assertThat(storedHash).startsWith("$argon2id$").isNotEqualTo(password);
		assertThat(hasher.matches(password, storedHash)).isTrue();
		assertThat(repository.findById(id).orElseThrow().getCreatedAt()).isBetween(before, after);
	}

	@Test
	@DisplayName("Dado uma conta gravada, quando procura a senha em texto puro em todas as colunas, então não a encontra")
	void plainPasswordIsInNoColumn() {
		// Arrange
		String password = TestPasswords.next();
		repository.saveAndFlush(factory.newAccount("admin.teste", password));

		// Act
		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM tb_user_account");

		// Assert
		assertThat(row).containsKeys("username", "password_hash");
		assertThat(row.values()).allSatisfy(value -> assertThat(String.valueOf(value)).doesNotContain(password));
	}

	// U-2. Why not "save the same name twice": the table accepts a single row, so ANY second insert is
	// rejected, with or without a unique constraint on the username. Which constraint the error names
	// depends on the order they were created in. So uniqueness is checked in two parts: the database
	// refuses other letter cases (below), and the unique constraint exists (next test).
	@Test
	@DisplayName("Dado um nome de usuário com maiúscula, quando insere direto no banco, então o banco recusa")
	void databaseRejectsUppercaseUsername() {
		// Arrange
		ThrowingCallable insert = () -> insert("Admin.Teste", PLACEHOLDER_HASH);

		// Act
		Throwable thrown = catchThrowable(insert);

		// Assert
		assertRejectedBy(thrown, "ck_tb_user_account_username_lower");
		assertThat(repository.count()).isZero();
	}

	@Test
	@DisplayName("Dado o esquema do banco, quando consulta as restrições, então o nome de usuário tem restrição única")
	void usernameHasAUniqueConstraint() {
		// Arrange
		String query = """
				SELECT LOWER(k.column_name)
				FROM information_schema.table_constraints c
				JOIN information_schema.key_column_usage k
				  ON k.constraint_name = c.constraint_name AND k.table_name = c.table_name
				WHERE c.constraint_type = 'UNIQUE'
				  AND LOWER(c.table_name) = 'tb_user_account'
				  AND LOWER(c.constraint_name) = 'uk_tb_user_account_username'
				""";

		// Act
		List<String> columns = jdbc.queryForList(query, String.class);

		// Assert
		assertThat(columns).containsExactly("username");
	}

	@Test
	@DisplayName("Dado que o administrador já existe, quando grava uma segunda conta, então o banco recusa e continua só uma")
	void rejectsASecondAccount() {
		// Arrange
		repository.saveAndFlush(factory.newAccount("admin.teste", TestPasswords.next()));
		UserAccount second = factory.newAccount("outro.teste", TestPasswords.next());

		// Act
		Throwable thrown = catchThrowable(() -> repository.saveAndFlush(second));

		// Assert
		assertRejectedBy(thrown, "uk_tb_user_account_singleton");
		assertThat(repository.count()).isEqualTo(1);
	}

	static Stream<Arguments> singletonBypassAttempts() {
		return Stream.of(
			Arguments.of(Named.of("singleton = 1", 1), "uk_tb_user_account_singleton"),
			Arguments.of(Named.of("singleton = 2", 2), "ck_tb_user_account_singleton"),
			Arguments.of(Named.of("singleton nulo", null), "\"singleton\"")
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("singletonBypassAttempts")
	@DisplayName("Dado que o administrador já existe, quando insere outro escolhendo o valor de singleton, então o banco recusa")
	void rejectsASecondAccountWhateverTheSingletonValue(Integer singleton, String rejectedBy) {
		// Arrange
		insert("admin.teste", PLACEHOLDER_HASH);

		// Act
		Throwable thrown = catchThrowable(() -> jdbc.update(
				"INSERT INTO tb_user_account (username, password_hash, created_at, singleton) VALUES (?, ?, CURRENT_TIMESTAMP, ?)",
				"outro.teste", PLACEHOLDER_HASH, singleton));

		// Assert
		assertRejectedBy(thrown, rejectedBy);
		assertThat(repository.count()).isEqualTo(1);
	}

	@ParameterizedTest(name = "{0} nulo")
	@ValueSource(strings = { "username", "password_hash" })
	@DisplayName("Dado uma conta sem nome de usuário ou sem hash, quando insere no banco, então o banco recusa")
	void databaseRejectsMissingCredential(String column) {
		// Arrange
		String username = column.equals("username") ? null : "admin.teste";
		String hash = column.equals("password_hash") ? null : PLACEHOLDER_HASH;

		// Act
		Throwable thrown = catchThrowable(() -> insert(username, hash));

		// Assert
		assertRejectedBy(thrown, "\"" + column + "\"");
		assertThat(repository.count()).isZero();
	}

	// On H2 the message never carries the row, so this test alone proves nothing about PostgreSQL:
	// there it depends on logServerErrorDetail=false (set in the dev and prod profiles).
	@Test
	@DisplayName("Dado uma conta que o banco recusa, quando a gravação falha, então o hash não aparece em nenhuma mensagem de erro")
	void rejectionMessagesNeverCarryTheHash() {
		// Arrange
		UserAccount account = factory.newAccount("admin.teste", TestPasswords.next());
		account.setUsername("ADMIN.TESTE"); // breaks the lowercase check
		String salt = account.getPasswordHash().split("\\$")[4];

		// Act
		Throwable thrown = catchThrowable(() -> repository.saveAndFlush(account));

		// Assert
		assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
		for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
			// PostgreSQL cuts long values, so look for pieces of the hash, not the whole text
			assertThat(String.valueOf(cause.getMessage())).doesNotContain("$argon2").doesNotContain(salt.substring(0, 8));
		}
	}

	private void insert(String username, String passwordHash) {
		jdbc.update("INSERT INTO tb_user_account (username, password_hash, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
				username, passwordHash);
	}

	/** The database rejected the row, naming this constraint (or this quoted column). */
	private static void assertRejectedBy(Throwable thrown, String name) {
		assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
		String message = NestedExceptionUtils.getMostSpecificCause(thrown).getMessage();
		// H2 appends the SQL statement, which names every column: ignore that part
		int statement = message.indexOf("; SQL statement");
		String reason = statement < 0 ? message : message.substring(0, statement);
		assertThat(reason.toLowerCase(Locale.ROOT)).contains(name);
	}
}
