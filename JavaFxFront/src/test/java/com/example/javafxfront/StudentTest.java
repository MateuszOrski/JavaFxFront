package com.example.javafxfront;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Testy jednostkowe dla klasy Student
 * Testujemy wszystkie funkcjonalności związane ze studentem
 */
@DisplayName("Testy klasy Student")
class StudentTest {

    private Student student;
    private final String FIRST_NAME = "Jan";
    private final String LAST_NAME = "Kowalski";
    private final String INDEX_NUMBER = "123456";
    private final String GROUP_NAME = "INF-A";

    @BeforeEach
    void setUp() {
        student = new Student(FIRST_NAME, LAST_NAME, INDEX_NUMBER, GROUP_NAME);
    }

    @Nested
    @DisplayName("Tworzenie studenta")
    class StudentCreation {

        @Test
        @DisplayName("Powinien utworzyć studenta z pełnymi danymi")
        void shouldCreateStudentWithFullData() {
            // Given & When
            Student newStudent = new Student("Anna", "Nowak", "654321", "INF-B");

            // Then
            assertThat(newStudent.getFirstName()).isEqualTo("Anna");
            assertThat(newStudent.getLastName()).isEqualTo("Nowak");
            assertThat(newStudent.getIndexNumber()).isEqualTo("654321");
            assertThat(newStudent.getGroupName()).isEqualTo("INF-B");
            assertThat(newStudent.getAddedDate()).isNotNull();
            assertThat(newStudent.getAddedDate()).isBefore(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("Powinien utworzyć studenta bez grupy")
        void shouldCreateStudentWithoutGroup() {
            // Given & When
            Student studentWithoutGroup = new Student("Piotr", "Wiśniewski", "789123", null);

            // Then
            assertThat(studentWithoutGroup.getGroupName()).isNull();
            assertThat(studentWithoutGroup.getFirstName()).isEqualTo("Piotr");
            assertThat(studentWithoutGroup.getLastName()).isEqualTo("Wiśniewski");
            assertThat(studentWithoutGroup.getIndexNumber()).isEqualTo("789123");
        }

        @Test
        @DisplayName("Powinien ustawić datę dodania na bieżący czas")
        void shouldSetAddedDateToCurrentTime() {
            // Given
            LocalDateTime beforeCreation = LocalDateTime.now();

            // When
            Student newStudent = new Student("Test", "User", "000000", "TEST");

            // Then
            LocalDateTime afterCreation = LocalDateTime.now();
            assertThat(newStudent.getAddedDate())
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
            assertThat(student.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(student.getLastName()).isEqualTo(LAST_NAME);
            assertThat(student.getIndexNumber()).isEqualTo(INDEX_NUMBER);
            assertThat(student.getGroupName()).isEqualTo(GROUP_NAME);
        }

        @Test
        @DisplayName("Settery powinny ustawić nowe wartości")
        void shouldSetNewValues() {
            // Given
            String newFirstName = "Maria";
            String newLastName = "Kowalczyk";
            String newIndexNumber = "999888";
            String newGroupName = "INF-C";

            // When
            student.setFirstName(newFirstName);
            student.setLastName(newLastName);
            student.setIndexNumber(newIndexNumber);
            student.setGroupName(newGroupName);

            // Then
            assertThat(student.getFirstName()).isEqualTo(newFirstName);
            assertThat(student.getLastName()).isEqualTo(newLastName);
            assertThat(student.getIndexNumber()).isEqualTo(newIndexNumber);
            assertThat(student.getGroupName()).isEqualTo(newGroupName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Setter powinien przyjąć wartości null i puste")
        void shouldAcceptNullAndEmptyValues(String value) {
            // When
            student.setFirstName(value);
            student.setLastName(value);
            student.setGroupName(value);

            // Then
            assertThat(student.getFirstName()).isEqualTo(value);
            assertThat(student.getLastName()).isEqualTo(value);
            assertThat(student.getGroupName()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("Metody pomocnicze")
    class HelperMethods {

        @Test
        @DisplayName("getFullName() powinien zwrócić imię i nazwisko")
        void shouldReturnFullName() {
            // When
            String fullName = student.getFullName();

            // Then
            assertThat(fullName).isEqualTo("Jan Kowalski");
        }

        @ParameterizedTest
        @CsvSource({
                "Anna, Nowak, Anna Nowak",
                "Piotr, Kowalski-Wiśniewski, Piotr Kowalski-Wiśniewski",
                "Jan, de la Cruz, Jan de la Cruz",
                "A, B, A B"
        })
        @DisplayName("getFullName() powinien obsłużyć różne kombinacje imion i nazwisk")
        void shouldHandleDifferentNameCombinations(String firstName, String lastName, String expectedFullName) {
            // Given
            student.setFirstName(firstName);
            student.setLastName(lastName);

            // When
            String fullName = student.getFullName();

            // Then
            assertThat(fullName).isEqualTo(expectedFullName);
        }

        @Test
        @DisplayName("getFormattedDate() powinien zwrócić sformatowaną datę")
        void shouldReturnFormattedDate() {
            // When
            String formattedDate = student.getFormattedDate();

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
            String stringRepresentation = student.toString();

            // Then
            assertThat(stringRepresentation)
                    .contains(FIRST_NAME)
                    .contains(LAST_NAME)
                    .contains(INDEX_NUMBER)
                    .contains(GROUP_NAME)
                    .isEqualTo("Jan Kowalski (123456) - INF-A");
        }

        @Test
        @DisplayName("toString() powinien obsłużyć studenta bez grupy")
        void shouldHandleStudentWithoutGroupInToString() {
            // Given
            student.setGroupName(null);

            // When
            String stringRepresentation = student.toString();

            // Then
            assertThat(stringRepresentation).isEqualTo("Jan Kowalski (123456) - null");
        }
    }

    @Nested
    @DisplayName("Walidacja danych")
    class DataValidation {

        @ParameterizedTest
        @ValueSource(strings = {"123456", "000000", "999999", "654321"})
        @DisplayName("Powinien zaakceptować poprawne numery indeksów")
        void shouldAcceptValidIndexNumbers(String indexNumber) {
            // When
            student.setIndexNumber(indexNumber);

            // Then
            assertThat(student.getIndexNumber()).isEqualTo(indexNumber);
        }

        @Test
        @DisplayName("Powinien obsłużyć specjalne znaki w imieniu i nazwisku")
        void shouldHandleSpecialCharactersInNames() {
            // Given
            String nameWithSpecialChars = "José-María";
            String surnameWithSpecialChars = "Müller-Żółć";

            // When
            student.setFirstName(nameWithSpecialChars);
            student.setLastName(surnameWithSpecialChars);

            // Then
            assertThat(student.getFirstName()).isEqualTo(nameWithSpecialChars);
            assertThat(student.getLastName()).isEqualTo(surnameWithSpecialChars);
            assertThat(student.getFullName()).isEqualTo("José-María Müller-Żółć");
        }
    }

    @Nested
    @DisplayName("Testy granic i przypadków skrajnych")
    class EdgeCases {

        @Test
        @DisplayName("Powinien obsłużyć bardzo długie imiona i nazwiska")
        void shouldHandleVeryLongNames() {
            // Given
            String veryLongName = "A".repeat(100);

            // When
            student.setFirstName(veryLongName);
            student.setLastName(veryLongName);

            // Then
            assertThat(student.getFirstName()).hasSize(100);
            assertThat(student.getLastName()).hasSize(100);
            assertThat(student.getFullName()).hasSize(201); // 100 + " " + 100
        }

        @Test
        @DisplayName("Powinien obsłużyć pojedyncze znaki")
        void shouldHandleSingleCharacters() {
            // When
            student.setFirstName("A");
            student.setLastName("B");

            // Then
            assertThat(student.getFullName()).isEqualTo("A B");
        }

        @Test
        @DisplayName("Powinien obsłużyć zmiany grupy")
        void shouldHandleGroupChanges() {
            // Given
            String originalGroup = student.getGroupName();
            String newGroup = "MAT-B";

            // When
            student.setGroupName(newGroup);

            // Then
            assertThat(student.getGroupName()).isEqualTo(newGroup);
            assertThat(student.getGroupName()).isNotEqualTo(originalGroup);
        }
    }

    @Nested
    @DisplayName("Testy immutability czasów")
    class TimeImmutability {

        @Test
        @DisplayName("Data dodania nie powinna się zmieniać po utworzeniu")
        void shouldNotChangeAddedDateAfterCreation() {
            // Given
            LocalDateTime originalDate = student.getAddedDate();

            // When - wykonaj różne operacje
            student.setFirstName("NewName");
            student.setLastName("NewLastName");
            student.setGroupName("NewGroup");

            // Then
            assertThat(student.getAddedDate()).isEqualTo(originalDate);
        }

        @Test
        @DisplayName("Formatowana data powinna być spójna")
        void shouldHaveConsistentFormattedDate() {
            // When
            String formattedDate1 = student.getFormattedDate();
            String formattedDate2 = student.getFormattedDate();

            // Then
            assertThat(formattedDate1).isEqualTo(formattedDate2);
        }
    }
}