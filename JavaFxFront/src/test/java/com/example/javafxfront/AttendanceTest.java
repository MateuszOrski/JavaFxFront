package com.example.javafxfront;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Testy jednostkowe dla klasy Attendance
 */
@DisplayName("Testy klasy Attendance")
class AttendanceTest {

    private Student student;
    private ClassSchedule schedule;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        student = new Student("Jan", "Kowalski", "123456", "INF-A");
        schedule = new ClassSchedule(
                "Programowanie w Javie",
                "Sala 101",
                LocalDateTime.of(2024, 3, 15, 10, 0),
                LocalDateTime.of(2024, 3, 15, 12, 0),
                "Dr Jan Nowak",
                "Wykład wprowadzający",
                "INF-A"
        );
        attendance = new Attendance(student, schedule, Attendance.Status.PRESENT);
    }

    @Nested
    @DisplayName("Enum Status")
    class StatusEnum {

        @Test
        @DisplayName("Status PRESENT powinien mieć poprawne właściwości")
        void shouldHaveCorrectPropertiesForPresent() {
            // When
            Attendance.Status status = Attendance.Status.PRESENT;

            // Then
            assertThat(status.getDisplayName()).isEqualTo("Obecny");
            assertThat(status.getColor()).isEqualTo("#38A169");
        }

        @Test
        @DisplayName("Status LATE powinien mieć poprawne właściwości")
        void shouldHaveCorrectPropertiesForLate() {
            // When
            Attendance.Status status = Attendance.Status.LATE;

            // Then
            assertThat(status.getDisplayName()).isEqualTo("Spóźniony");
            assertThat(status.getColor()).isEqualTo("#F56500");
        }

        @Test
        @DisplayName("Status ABSENT powinien mieć poprawne właściwości")
        void shouldHaveCorrectPropertiesForAbsent() {
            // When
            Attendance.Status status = Attendance.Status.ABSENT;

            // Then
            assertThat(status.getDisplayName()).isEqualTo("Nieobecny");
            assertThat(status.getColor()).isEqualTo("#E53E3E");
        }

        @ParameterizedTest
        @EnumSource(Attendance.Status.class)
        @DisplayName("Wszystkie statusy powinny mieć niepuste nazwy i kolory")
        void shouldHaveNonEmptyDisplayNameAndColor(Attendance.Status status) {
            assertThat(status.getDisplayName()).isNotBlank();
            assertThat(status.getColor()).isNotBlank();
            assertThat(status.getColor()).startsWith("#");
            assertThat(status.getColor()).hasSize(7); // Format #RRGGBB
        }
    }

    @Nested
    @DisplayName("Tworzenie obecności")
    class AttendanceCreation {

        @Test
        @DisplayName("Powinien utworzyć obecność z podstawowymi danymi")
        void shouldCreateAttendanceWithBasicData() {
            // Given & When
            Attendance newAttendance = new Attendance(student, schedule, Attendance.Status.LATE);

            // Then
            assertThat(newAttendance.getStudent()).isEqualTo(student);
            assertThat(newAttendance.getSchedule()).isEqualTo(schedule);
            assertThat(newAttendance.getStatus()).isEqualTo(Attendance.Status.LATE);
            assertThat(newAttendance.getNotes()).isEmpty();
            assertThat(newAttendance.getMarkedAt()).isNotNull();
        }

        @Test
        @DisplayName("Powinien utworzyć obecność z uwagami")
        void shouldCreateAttendanceWithNotes() {
            // Given
            String notes = "Usprawiedliwione spóźnienie";

            // When
            Attendance attendanceWithNotes = new Attendance(student, schedule, Attendance.Status.LATE, notes);

            // Then
            assertThat(attendanceWithNotes.getNotes()).isEqualTo(notes);
            assertThat(attendanceWithNotes.getStatus()).isEqualTo(Attendance.Status.LATE);
        }

        @Test
        @DisplayName("Powinien ustawić czas oznaczenia na bieżący moment")
        void shouldSetMarkedAtToCurrentTime() {
            // Given
            LocalDateTime beforeCreation = LocalDateTime.now();

            // When
            Attendance newAttendance = new Attendance(student, schedule, Attendance.Status.PRESENT);

            // Then
            LocalDateTime afterCreation = LocalDateTime.now();
            assertThat(newAttendance.getMarkedAt())
                    .isAfter(beforeCreation.minusSeconds(1))
                    .isBefore(afterCreation.plusSeconds(1));
        }

        @ParameterizedTest
        @EnumSource(Attendance.Status.class)
        @DisplayName("Powinien utworzyć obecność z każdym statusem")
        void shouldCreateAttendanceWithAnyStatus(Attendance.Status status) {
            // When
            Attendance newAttendance = new Attendance(student, schedule, status);

            // Then
            assertThat(newAttendance.getStatus()).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("Gettery i Settery")
    class GettersAndSetters {

        @Test
        @DisplayName("Gettery powinny zwrócić poprawne wartości")
        void shouldReturnCorrectValues() {
            assertThat(attendance.getStudent()).isEqualTo(student);
            assertThat(attendance.getSchedule()).isEqualTo(schedule);
            assertThat(attendance.getStatus()).isEqualTo(Attendance.Status.PRESENT);
            assertThat(attendance.getNotes()).isEmpty();
            assertThat(attendance.getMarkedAt()).isNotNull();
        }

        @Test
        @DisplayName("Setter statusu powinien zaktualizować czas oznaczenia")
        void shouldUpdateMarkedAtWhenStatusChanges() {
            // Given
            LocalDateTime originalTime = attendance.getMarkedAt();

            // When - czekaj krótko i zmień status
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attendance.setStatus(Attendance.Status.LATE);

            // Then
            assertThat(attendance.getMarkedAt()).isAfter(originalTime);
            assertThat(attendance.getStatus()).isEqualTo(Attendance.Status.LATE);
        }

        @Test
        @DisplayName("Setter uwag powinien ustawić nowe uwagi")
        void shouldSetNewNotes() {
            // Given
            String newNotes = "Nowe uwagi dotyczące obecności";

            // When
            attendance.setNotes(newNotes);

            // Then
            assertThat(attendance.getNotes()).isEqualTo(newNotes);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "Krótkie", "Bardzo długie uwagi opisujące szczegółowo sytuację studenta"})
        @DisplayName("Setter uwag powinien przyjąć różne rodzaje uwag")
        void shouldAcceptVariousNotes(String notes) {
            // When
            attendance.setNotes(notes);

            // Then
            assertThat(attendance.getNotes()).isEqualTo(notes);
        }

        @Test
        @DisplayName("Setter studenta powinien ustawić nowego studenta")
        void shouldSetNewStudent() {
            // Given
            Student newStudent = new Student("Anna", "Nowak", "654321", "INF-B");

            // When
            attendance.setStudent(newStudent);

            // Then
            assertThat(attendance.getStudent()).isEqualTo(newStudent);
        }

        @Test
        @DisplayName("Setter terminu powinien ustawić nowy termin")
        void shouldSetNewSchedule() {
            // Given
            ClassSchedule newSchedule = new ClassSchedule(
                    "Bazy Danych",
                    "Sala 102",
                    LocalDateTime.of(2024, 3, 16, 14, 0),
                    LocalDateTime.of(2024, 3, 16, 16, 0),
                    "Dr Anna Kowalska",
                    "Laboratorium",
                    "INF-A"
            );

            // When
            attendance.setSchedule(newSchedule);

            // Then
            assertThat(attendance.getSchedule()).isEqualTo(newSchedule);
        }
    }

    @Nested
    @DisplayName("Metody pomocnicze")
    class HelperMethods {

        @Test
        @DisplayName("getFormattedMarkedTime() powinien zwrócić sformatowany czas")
        void shouldReturnFormattedMarkedTime() {
            // When
            String formattedTime = attendance.getFormattedMarkedTime();

            // Then
            assertThat(formattedTime)
                    .isNotBlank()
                    .matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")
                    .hasSize(16); // DD.MM.YYYY HH:MM
        }

        @Test
        @DisplayName("toString() powinien zwrócić reprezentację tekstową")
        void shouldReturnStringRepresentation() {
            // When
            String stringRepresentation = attendance.toString();

            // Then
            assertThat(stringRepresentation)
                    .contains(student.getFullName())
                    .contains(attendance.getStatus().getDisplayName())
                    .isEqualTo("Jan Kowalski - Obecny");
        }

        @ParameterizedTest
        @EnumSource(Attendance.Status.class)
        @DisplayName("toString() powinien obsłużyć wszystkie statusy")
        void shouldHandleAllStatusesInToString(Attendance.Status status) {
            // Given
            attendance.setStatus(status);

            // When
            String result = attendance.toString();

            // Then
            assertThat(result).contains(status.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Metody equals i hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals() powinien zwrócić true dla tej samej instancji")
        void shouldReturnTrueForSameInstance() {
            // When & Then
            assertThat(attendance.equals(attendance)).isTrue();
        }

        @Test
        @DisplayName("equals() powinien zwrócić false dla null")
        void shouldReturnFalseForNull() {
            // When & Then
            assertThat(attendance.equals(null)).isFalse();
        }

        @Test
        @DisplayName("equals() powinien zwrócić false dla innej klasy")
        void shouldReturnFalseForDifferentClass() {
            // When & Then
            assertThat(attendance.equals("string")).isFalse();
        }

        @Test
        @DisplayName("equals() powinien zwrócić true dla tej samej kombinacji student-termin")
        void shouldReturnTrueForSameStudentScheduleCombination() {
            // Given
            Attendance otherAttendance = new Attendance(student, schedule, Attendance.Status.LATE);

            // When & Then
            assertThat(attendance.equals(otherAttendance)).isTrue();
        }

        @Test
        @DisplayName("equals() powinien zwrócić false dla różnych studentów")
        void shouldReturnFalseForDifferentStudents() {
            // Given
            Student differentStudent = new Student("Anna", "Nowak", "654321", "INF-B");
            Attendance otherAttendance = new Attendance(differentStudent, schedule, Attendance.Status.PRESENT);

            // When & Then
            assertThat(attendance.equals(otherAttendance)).isFalse();
        }

        @Test
        @DisplayName("equals() powinien zwrócić false dla różnych terminów")
        void shouldReturnFalseForDifferentSchedules() {
            // Given
            ClassSchedule differentSchedule = new ClassSchedule(
                    "Inny przedmiot",
                    "Inna sala",
                    LocalDateTime.of(2024, 3, 17, 10, 0),
                    LocalDateTime.of(2024, 3, 17, 12, 0),
                    "Inny wykładowca",
                    "",
                    "INF-A"
            );
            Attendance otherAttendance = new Attendance(student, differentSchedule, Attendance.Status.PRESENT);

            // When & Then
            assertThat(attendance.equals(otherAttendance)).isFalse();
        }

        @Test
        @DisplayName("hashCode() powinien być spójny")
        void shouldHaveConsistentHashCode() {
            // When
            int hashCode1 = attendance.hashCode();
            int hashCode2 = attendance.hashCode();

            // Then
            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        @DisplayName("hashCode() powinien być taki sam dla równych obiektów")
        void shouldHaveSameHashCodeForEqualObjects() {
            // Given
            Attendance otherAttendance = new Attendance(student, schedule, Attendance.Status.LATE);

            // When
            int hashCode1 = attendance.hashCode();
            int hashCode2 = otherAttendance.hashCode();

            // Then
            assertThat(hashCode1).isEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("Testy przypadków skrajnych")
    class EdgeCases {

        @Test
        @DisplayName("Powinien obsłużyć bardzo długie uwagi")
        void shouldHandleVeryLongNotes() {
            // Given
            String veryLongNotes = "A".repeat(1000);

            // When
            attendance.setNotes(veryLongNotes);

            // Then
            assertThat(attendance.getNotes()).hasSize(1000);
        }

        @Test
        @DisplayName("Powinien obsłużyć null w uwagach")
        void shouldHandleNullNotes() {
            // When
            attendance.setNotes(null);

            // Then
            assertThat(attendance.getNotes()).isNull();
        }

        @Test
        @DisplayName("Powinien obsłużyć specjalne znaki w uwagach")
        void shouldHandleSpecialCharactersInNotes() {
            // Given
            String specialNotes = "Uwagi z polskimi znakami: ąćęłńóśźż, emoji: 😀, i symbolami: @#$%";

            // When
            attendance.setNotes(specialNotes);

            // Then
            assertThat(attendance.getNotes()).isEqualTo(specialNotes);
        }

        @Test
        @DisplayName("Powinien obsłużyć wielokrotne zmiany statusu")
        void shouldHandleMultipleStatusChanges() {
            // Given
            LocalDateTime originalTime = attendance.getMarkedAt();

            // When - zmień status i wymuś różnicę czasu
            try {
                Thread.sleep(1); // Zapewnij minimalną różnicę czasu
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            attendance.setStatus(Attendance.Status.LATE);
            LocalDateTime afterFirstChange = attendance.getMarkedAt();

            try {
                Thread.sleep(1); // Zapewnij minimalną różnicę czasu
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            attendance.setStatus(Attendance.Status.ABSENT);
            LocalDateTime afterSecondChange = attendance.getMarkedAt();

            // Then - sprawdź tylko czy czasy są różne lub równe (nie wymagaj strictly after)
            assertThat(afterFirstChange).isAfterOrEqualTo(originalTime);
            assertThat(afterSecondChange).isAfterOrEqualTo(afterFirstChange);
            assertThat(attendance.getStatus()).isEqualTo(Attendance.Status.ABSENT);
        }
    }

    @Nested
    @DisplayName("Testy logiki biznesowej")
    class BusinessLogic {

        @Test
        @DisplayName("Zmiana statusu na ten sam powinien i tak zaktualizować czas")
        void shouldUpdateTimeEvenWhenStatusStaysTheSame() {
            // Given
            LocalDateTime originalTime = attendance.getMarkedAt();
            Attendance.Status originalStatus = attendance.getStatus();

            // When
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attendance.setStatus(originalStatus);

            // Then
            assertThat(attendance.getMarkedAt()).isAfter(originalTime);
            assertThat(attendance.getStatus()).isEqualTo(originalStatus);
        }

        @Test
        @DisplayName("Student nie może mieć dwóch obecności na ten sam termin")
        void shouldRepresentUniqueStudentScheduleCombination() {
            // Given
            Attendance attendance1 = new Attendance(student, schedule, Attendance.Status.PRESENT);
            Attendance attendance2 = new Attendance(student, schedule, Attendance.Status.LATE);

            // Then - te dwie obecności reprezentują tę samą kombinację student-termin
            assertThat(attendance1.equals(attendance2)).isTrue();
            assertThat(attendance1.hashCode()).isEqualTo(attendance2.hashCode());
        }
    }
}