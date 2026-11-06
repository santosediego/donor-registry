package com.santosediego.donorregistry.entities.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.santosediego.donorregistry.services.exceptions.InvalidEnumValueException;

class BloodTypeTest {

	// Explicit table on purpose: it fixes the API contract (code -> constant)
	static Stream<Arguments> codes() {
		return Stream.of(
			Arguments.of("A+", BloodType.A_POSITIVE),
			Arguments.of("B+", BloodType.B_POSITIVE),
			Arguments.of("AB+", BloodType.AB_POSITIVE),
			Arguments.of("O+", BloodType.O_POSITIVE),
			Arguments.of("A-", BloodType.A_NEGATIVE),
			Arguments.of("B-", BloodType.B_NEGATIVE),
			Arguments.of("AB-", BloodType.AB_NEGATIVE),
			Arguments.of("O-", BloodType.O_NEGATIVE),
			Arguments.of("FO-", BloodType.FALSE_O_NEGATIVE),
			Arguments.of("SD", BloodType.GOLDEN_BLOOD)
		);
	}

	@ParameterizedTest(name = "{0} → {1}")
	@MethodSource("codes")
	@DisplayName("Dado um código válido, quando chama fromId, então devolve a constante correspondente")
	void returnsConstantForValidCode(String code, BloodType expected) {
		// Arrange (code and expected come from the table)

		// Act
		BloodType result = BloodType.fromId(code);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@Test
	@DisplayName("Dado a tabela de códigos, quando compara com os valores do enum, então todas as constantes estão na tabela (e nenhuma repetida)")
	void tableCoversEveryConstantExactlyOnce() {
		// Arrange
		List<BloodType> tableConstants = codes().map(args -> (BloodType) args.get()[1]).toList();

		// Act
		EnumSet<BloodType> tableSet = EnumSet.copyOf(tableConstants);

		// Assert
		assertThat(tableConstants).hasSize(BloodType.values().length);
		assertThat(tableSet).containsExactlyInAnyOrder(BloodType.values());
	}

	@Test
	@DisplayName("Dado null, quando chama fromId, então devolve null (campo opcional ou obrigatório é regra do DTO)")
	void returnsNullForNull() {
		// Arrange
		String code = null;

		// Act
		BloodType result = BloodType.fromId(code);

		// Assert
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Dado um código inexistente, quando chama fromId, então lança InvalidEnumValueException listando todos os códigos aceitos")
	void throwsForUnknownCode() {
		// Arrange
		String code = "X";

		// Act
		Throwable thrown = catchThrowable(() -> BloodType.fromId(code));

		// Assert
		assertThat(thrown).isInstanceOf(InvalidEnumValueException.class);
		assertThat(acceptedCodesIn(thrown)).containsExactlyInAnyOrderElementsOf(allCodes());
	}

	@Test
	@DisplayName("Dado um código vazio, quando chama fromId, então lança InvalidEnumValueException listando todos os códigos aceitos")
	void throwsForEmptyCode() {
		// Arrange
		String code = "";

		// Act
		Throwable thrown = catchThrowable(() -> BloodType.fromId(code));

		// Assert
		assertThat(thrown).isInstanceOf(InvalidEnumValueException.class);
		assertThat(acceptedCodesIn(thrown)).containsExactlyInAnyOrderElementsOf(allCodes());
	}

	@Test
	@DisplayName("Dado um código válido em minúscula, quando chama fromId, então lança InvalidEnumValueException (a comparação diferencia maiúscula)")
	void throwsForLowerCaseCode() {
		// Arrange
		String code = "sd";

		// Act
		Throwable thrown = catchThrowable(() -> BloodType.fromId(code));

		// Assert
		assertThat(thrown).isInstanceOf(InvalidEnumValueException.class);
		assertThat(acceptedCodesIn(thrown)).containsExactlyInAnyOrderElementsOf(allCodes());
	}

	private static List<String> allCodes() {
		return Arrays.stream(BloodType.values()).map(BloodType::getId).toList();
	}

	// Reads only the list after "Valores aceitos: " from the message, not the whole text
	private static List<String> acceptedCodesIn(Throwable thrown) {
		String message = thrown.getMessage();
		String list = message.substring(message.indexOf("Valores aceitos: ") + "Valores aceitos: ".length());
		return List.of(list.substring(0, list.length() - 1).split(", "));
	}
}
