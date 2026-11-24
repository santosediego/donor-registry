package com.santosediego.donorregistry.resources;

import static com.santosediego.donorregistry.support.ErrorAssertions.assertStandardErrorBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * LIST-18. The "malformed parameter" error is raised by Tomcat when it reads the query string, and MockMvc
 * does not go through Tomcat. So this test starts a real server (RANDOM_PORT) and sends the request by a raw
 * socket: ordinary HTTP clients refuse to build a URL with a lone "%".
 * It does not read or change donors, so it does not need the seeds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DonorResourceMalformedParamTest {

	@LocalServerPort
	private int port;

	@ParameterizedTest(name = "GET {0}")
	@ValueSource(strings = { "/donors?search=J%o", "/donors?search=%" })
	@DisplayName("LIST-18: Dado um parâmetro com % solto enviado cru ao servidor real, quando faço GET /donors, então devolve 400 com a mensagem de parâmetro mal codificado")
	void looseSignInQueryReturnsBadRequest(String target) throws Exception {
		// Arrange
		Instant before = Instant.now();

		// Act
		String response = sendRaw(target);
		Instant after = Instant.now();

		// Assert
		assertThat(response).matches("(?s)HTTP/1\\.[01] 400 .*");
		String body = response.substring(response.indexOf("\r\n\r\n") + 4);
		assertStandardErrorBody(body, 400, "Requisição inválida", "A URL contém um parâmetro mal codificado.",
				"/donors", before, after);
	}

	// HTTP/1.0: the server closes the connection after the answer (we read until the end) and sends no chunked body
	private String sendRaw(String target) throws IOException {
		try (Socket socket = new Socket("localhost", port)) {
			socket.getOutputStream().write(("GET " + target + " HTTP/1.0\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
			socket.getOutputStream().flush();
			return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
