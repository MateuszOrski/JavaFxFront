package com.example.javafxfront;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Testy jednostkowe dla klasy Group
 */
@DisplayName("Testy klasy Group")
class GroupTest {

    private Group group;
    private final String GROUP_NAME = "INF-A";
    private final String SPECIALIZATION = "Informatyka";

    @BeforeEach
    void setUp() {
        group = new Group(GROUP_NAME, SPECIALIZATION);
    }

    @Nested
    @DisplayName("Tworzenie grupy")
    class GroupCreation {

        @Test
        @DisplayName("Powinien utworzyć grupę z pełnymi danymi")
        void shouldCreateGroupWithFullData() {
            // Given & When
            Group newGroup = new Group("MAT-B", "Matematyka");

            // Then
            assertThat(newGroup.getName()).isEqualTo("MAT-B");
            assertThat(newGroup.getSpecialization()).isEqualTo("Matematyka");
            assertThat(newGroup.getCreatedDate()).isNotNull();
            assertThat(newGroup.getCreatedDate()).isBefore(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("Powinien ustawić datę utworzenia na bieżący czas")
        void shouldSetCreatedDateToCurrentTime() {
            // Given
            LocalDateTime beforeCreation = LocalDateTime.now();

            // When
            Group newGroup = new Group("TEST", "Test Specialization");

            // Then
            LocalDateTime afterCreation = LocalDateTime.now();
            assertThat(newGroup.getCreatedDate())
                    .isAfter(beforeCreation.minusSeconds(1))
                    .isBefore(afterCreation.plusSeconds(1));
        }
    }

    @Nested
    @DisplayName("Gettery i Settery")
    class GettersAndSetters {

        @Test
        @DisplayName("Gettery powinny zwrócić poprawne wartości")
        void shouldReturnCorrectValues() {
            assertThat(group.getName()).isEqualTo(GROUP_NAME);
            assertThat(group.getSpecialization()).isEqualTo(SPECIALIZATION);
            assertThat(group.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("Settery powinny ustawić nowe wartości")
        void shouldSetNewValues() {
            // Given
            String newName = "FIZ-C";
            String newSpecialization = "Fizyka";

            // When
            group.setName(newName);
            group.setSpecialization(newSpecialization);

            // Then
            assertThat(group.getName()).isEqualTo(newName);
            assertThat(group.getSpecialization()).isEqualTo(newSpecialization);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Setter powinien przyjąć wartości puste")
        void shouldAcceptEmptyValues(String value) {
            // When
            group.setName(value);
            group.setSpecialization(value);

            // Then
            assertThat(group.getName()).isEqualTo(value);
            assertThat(group.getSpecialization()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("Metody pomocnicze")
    class HelperMethods {

        @Test
        @DisplayName("getFormattedDate() powinien zwrócić sformatowaną datę")
        void shouldReturnFormattedDate() {
            // When
            String formattedDate = group.getFormattedDate();

            // Then
            assertThat(formattedDate)
                    .isNotBlank()
                    .matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")
                    .hasSize(16); // DD.MM.YYYY HH:MM
        }

        @Test
        @DisplayName("toString() powinien zwrócić reprezentację tekstową")
        void shouldReturnStringRepresentation() {
            // When
            String stringRepresentation = group.toString();

            // Then
            assertThat(stringRepresentation)
                    .contains(GROUP_NAME)
                    .contains(SPECIALIZATION)
                    .isEqualTo("INF-A (Informatyka)");
        }

        @ParameterizedTest
        @CsvSource({
                "INF-A, Informatyka, INF-A (Informatyka)",
                "MAT-B, Matematyka, MAT-B (Matematyka)",
                "FIZ-C, Fizyka Teoretyczna, FIZ-C (Fizyka Teoretyczna)",
                "CHEM, Chemia Organiczna, CHEM (Chemia Organiczna)"
        })
        @DisplayName("toString() powinien obsłużyć różne kombinacje nazw i specjalizacji")
        void shouldHandleDifferentNameSpecializationCombinations(String name, String specialization, String expected) {
            // Given
            group.setName(name);
            group.setSpecialization(specialization);

            // When
            String result = group.toString();

            // Then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Walidacja danych")
    class DataValidation {

        @ParameterizedTest
        @ValueSource(strings = {
                "INF-A", "MAT-B", "FIZ-C", "CHEM-1",
                "BIO_2023", "HIST-III", "A1", "XYZ-999"
        })
        @DisplayName("Powinien zaakceptować różne formaty nazw grup")
        void shouldAcceptVariousGroupNameFormats(String groupName) {
            // When
            group.setName(groupName);

            // Then
            assertThat(group.getName()).isEqualTo(groupName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Informatyka", "Matematyka Stosowana", "Fizyka Teoretyczna",
                "Chemia Organiczna", "Biologia Molekularna", "Historia Sztuki",
                "Inżynieria Oprogramowania", "Sztuczna Inteligencja"
        })
        @DisplayName("Powinien zaakceptować różne specjalizacje")
        void shouldAcceptVariousSpecializations(String specialization) {
            // When
            group.setSpecialization(specialization);

            // Then
            assertThat(group.getSpecialization()).isEqualTo(specialization);
        }

        @Test
        @DisplayName("Powinien obsłużyć specjalne znaki w nazwie i specjalizacji")
        void shouldHandleSpecialCharacters() {
            // Given
            String nameWithSpecialChars = "ÉŃG-Ą";
            String specializationWithSpecialChars = "Inżynieria Środowiska";

            // When
            group.setName(nameWithSpecialChars);
            group.setSpecialization(specializationWithSpecialChars);

            // Then
            assertThat(group.getName()).isEqualTo(nameWithSpecialChars);
            assertThat(group.getSpecialization()).isEqualTo(specializationWithSpecialChars);
        }
    }

    @Nested
    @DisplayName("Testy przypadków skrajnych")
    class EdgeCases {

        @Test
        @DisplayName("Powinien obsłużyć bardzo długie nazwy")
        void shouldHandleVeryLongNames() {
            // Given
            String veryLongName = "A".repeat(100);
            String veryLongSpecialization = "B".repeat(200);

            // When
            group.setName(veryLongName);
            group.setSpecialization(veryLongSpecialization);

            // Then
            assertThat(group.getName()).hasSize(100);
            assertThat(group.getSpecialization()).hasSize(200);
            assertThat(group.toString()).contains(veryLongName);
            assertThat(group.toString()).contains(veryLongSpecialization);
        }

        @Test
        @DisplayName("Powinien obsłużyć pojedyncze znaki")
        void shouldHandleSingleCharacters() {
            // When
            group.setName("A");
            group.setSpecialization("B");

            // Then
            assertThat(group.getName()).isEqualTo("A");
            assertThat(group.getSpecialization()).isEqualTo("B");
            assertThat(group.toString()).isEqualTo("A (B)");
        }

        @Test
        @DisplayName("Powinien obsłużyć null values")
        void shouldHandleNullValues() {
            // When
            group.setName(null);
            group.setSpecialization(null);

            // Then
            assertThat(group.getName()).isNull();
            assertThat(group.getSpecialization()).isNull();
            assertThat(group.toString()).isEqualTo("null (null)");
        }
    }

    @Nested
    @DisplayName("Testy immutability czasów")
    class TimeImmutability {

        @Test
        @DisplayName("Data utworzenia nie powinna się zmieniać po modyfikacjach")
        void shouldNotChangeCreatedDateAfterModifications() {
            // Given
            LocalDateTime originalDate = group.getCreatedDate();

            // When - wykonaj różne operacje
            group.setName("NewName");
            group.setSpecialization("NewSpecialization");

            // Then
            assertThat(group.getCreatedDate()).isEqualTo(originalDate);
        }

        @Test
        @DisplayName("Formatowana data powinna być spójna")
        void shouldHaveConsistentFormattedDate() {
            // When
            String formattedDate1 = group.getFormattedDate();
            String formattedDate2 = group.getFormattedDate();

            // Then
            assertThat(formattedDate1).isEqualTo(formattedDate2);
        }
    }

    @Nested
    @DisplayName("Testy logiki biznesowej")
    class BusinessLogic {

        @Test
        @DisplayName("Różne grupy powinny mieć różne nazwy")
        void shouldHaveDifferentNamesForDifferentGroups() {
            // Given
            Group group1 = new Group("INF-A", "Informatyka");
            Group group2 = new Group("INF-B", "Informatyka");

            // Then
            assertThat(group1.getName()).isNotEqualTo(group2.getName());
            assertThat(group1.getSpecialization()).isEqualTo(group2.getSpecialization());
        }

        @Test
        @DisplayName("Grupy z tą samą nazwą i specjalizacją powinny być równoważne")
        void shouldBeEquivalentWithSameNameAndSpecialization() {
            // Given
            Group group1 = new Group("INF-A", "Informatyka");
            Group group2 = new Group("INF-A", "Informatyka");

            // Then
            assertThat(group1.getName()).isEqualTo(group2.getName());
            assertThat(group1.getSpecialization()).isEqualTo(group2.getSpecialization());
            assertThat(group1.toString()).isEqualTo(group2.toString());
        }

        @Test
        @DisplayName("Powinien zachować oryginalną specjalizację przy zmianie nazwy")
        void shouldKeepOriginalSpecializationWhenChangingName() {
            // Given
            String originalSpecialization = group.getSpecialization();

            // When
            group.setName("NEW-NAME");

            // Then
            assertThat(group.getSpecialization()).isEqualTo(originalSpecialization);
        }

        @Test
        @DisplayName("Powinien zachować oryginalną nazwę przy zmianie specjalizacji")
        void shouldKeepOriginalNameWhenChangingSpecialization() {
            // Given
            String originalName = group.getName();

            // When
            group.setSpecialization("Nowa Specjalizacja");

            // Then
            assertThat(group.getName()).isEqualTo(originalName);
        }
    }
}