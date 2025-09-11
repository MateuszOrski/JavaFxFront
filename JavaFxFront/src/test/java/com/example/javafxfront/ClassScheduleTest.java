package com.example.javafxfront;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Testy jednostkowe dla klasy ClassSchedule
 */
@DisplayName("Testy klasy ClassSchedule")
class ClassScheduleTest {

    private ClassSchedule schedule;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.of(2024, 3, 15, 10, 0);
        endTime = LocalDateTime.of(2024, 3, 15, 12, 0);

        schedule = new ClassSchedule(
                "Programowanie w Javie",
                "Sala 101",
                startTime,
                endTime,
                "Dr Jan Nowak",
                "Wykład wprowadzający",
                "INF-A"
        );
    }

    @Nested
    @DisplayName("Tworzenie terminów")
    class ScheduleCreation {

        @Test
        @DisplayName("Powinien utworzyć termin bez ID (nowy)")
        void shouldCreateNewScheduleWithoutId() {
            // When
            ClassSchedule newSchedule = new ClassSchedule(
                    "Bazy Danych",
                    "Sala 102",
                    startTime,
                    endTime,
                    "Dr Anna Kowalska",
                    "Laboratorium",
                    "INF-B"
            );

            // Then
            assertThat(newSchedule.getId()).isNull();
            assertThat(newSchedule.getSubject()).isEqualTo("Bazy Danych");
            assertThat(newSchedule.getClassroom()).isEqualTo("Sala 102");
            assertThat(newSchedule.getGroupName()).isEqualTo("INF-B");
            assertThat(newSchedule.isFromServer()).isFalse();
            assertThat(newSchedule.getAttendances()).isEmpty();
        }

        @Test
        @DisplayName("Powinien utworzyć termin z ID (z serwera)")
        void shouldCreateScheduleFromServer() {
            // When
            ClassSchedule serverSchedule = new ClassSchedule(
                    1L,
                    "Algorytmy",
                    "Sala 103",
                    startTime,
                    endTime,
                    "Prof. Maria Nowak",
                    "Wykład",
                    "INF-C",
                    LocalDateTime.now()
            );

            // Then
            assertThat(serverSchedule.getId()).isEqualTo(1L);
            assertThat(serverSchedule.isFromServer()).isTrue();
            assertThat(serverSchedule.getCreatedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Formatowanie dat i czasów")
    class DateTimeFormatting {

        @Test
        @DisplayName("getFormattedStartTime() powinien zwrócić poprawny format")
        void shouldFormatStartTimeCorrectly() {
            // When
            String formatted = schedule.getFormattedStartTime();

            // Then
            assertThat(formatted).isEqualTo("15.03.2024 10:00");
        }

        @Test
        @DisplayName("getFormattedEndTime() powinien zwrócić tylko godzinę")
        void shouldFormatEndTimeCorrectly() {
            // When
            String formatted = schedule.getFormattedEndTime();

            // Then
            assertThat(formatted).isEqualTo("12:00");
        }

        @Test
        @DisplayName("getFormattedTimeRange() powinien zwrócić pełny zakres")
        void shouldFormatTimeRangeCorrectly() {
            // When
            String formatted = schedule.getFormattedTimeRange();

            // Then
            assertThat(formatted).isEqualTo("15.03.2024 10:00 - 12:00");
        }

        @Test
        @DisplayName("getFormattedCreatedDate() powinien zwrócić sformatowaną datę utworzenia")
        void shouldFormatCreatedDateCorrectly() {
            // When
            String formatted = schedule.getFormattedCreatedDate();

            // Then
            assertThat(formatted)
                    .matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")
                    .hasSize(16);
        }
    }

    @Nested
    @DisplayName("Zarządzanie obecnością")
    class AttendanceManagement {

        private Student student1;
        private Student student2;
        private Student student3;

        @BeforeEach
        void setUpStudents() {
            student1 = new Student("Jan", "Kowalski", "123456", "INF-A");
            student2 = new Student("Anna", "Nowak", "654321", "INF-A");
            student3 = new Student("Piotr", "Wiśniewski", "789123", "INF-A");
        }

        @Test
        @DisplayName("Powinien dodać obecność studenta")
        void shouldAddStudentAttendance() {
            // Given
            Attendance attendance = new Attendance(student1, schedule, Attendance.Status.PRESENT);

            // When
            schedule.addAttendance(attendance);

            // Then
            assertThat(schedule.getAttendances()).hasSize(1);
            assertThat(schedule.hasAttendanceForStudent(student1)).isTrue();
            assertThat(schedule.getAttendanceForStudent(student1)).isEqualTo(attendance);
        }

        @Test
        @DisplayName("Powinien zastąpić istniejącą obecność studenta")
        void shouldReplaceExistingAttendance() {
            // Given
            Attendance originalAttendance = new Attendance(student1, schedule, Attendance.Status.PRESENT);
            Attendance newAttendance = new Attendance(student1, schedule, Attendance.Status.LATE);

            // When
            schedule.addAttendance(originalAttendance);
            schedule.addAttendance(newAttendance);

            // Then
            assertThat(schedule.getAttendances()).hasSize(1);
            assertThat(schedule.getAttendanceForStudent(student1).getStatus()).isEqualTo(Attendance.Status.LATE);
        }

        @Test
        @DisplayName("Powinien usunąć obecność studenta")
        void shouldRemoveStudentAttendance() {
            // Given
            Attendance attendance = new Attendance(student1, schedule, Attendance.Status.PRESENT);
            schedule.addAttendance(attendance);

            // When
            schedule.removeAttendance(student1);

            // Then
            assertThat(schedule.getAttendances()).isEmpty();
            assertThat(schedule.hasAttendanceForStudent(student1)).isFalse();
            assertThat(schedule.getAttendanceForStudent(student1)).isNull();
        }

        @Test
        @DisplayName("Powinien zwrócić null dla nieistniejącej obecności")
        void shouldReturnNullForNonExistentAttendance() {
            // When & Then
            assertThat(schedule.getAttendanceForStudent(student1)).isNull();
            assertThat(schedule.hasAttendanceForStudent(student1)).isFalse();
        }
    }

    @Nested
    @DisplayName("Statystyki obecności")
    class AttendanceStatistics {

        @BeforeEach
        void setUpAttendances() {
            Student student1 = new Student("Jan", "Kowalski", "123456", "INF-A");
            Student student2 = new Student("Anna", "Nowak", "654321", "INF-A");
            Student student3 = new Student("Piotr", "Wiśniewski", "789123", "INF-A");
            Student student4 = new Student("Maria", "Kowalczyk", "111222", "INF-A");

            schedule.addAttendance(new Attendance(student1, schedule, Attendance.Status.PRESENT));
            schedule.addAttendance(new Attendance(student2, schedule, Attendance.Status.PRESENT));
            schedule.addAttendance(new Attendance(student3, schedule, Attendance.Status.LATE));
            schedule.addAttendance(new Attendance(student4, schedule, Attendance.Status.ABSENT));
        }

        @Test
        @DisplayName("Powinien liczyć obecnych studentów")
        void shouldCountPresentStudents() {
            assertThat(schedule.getPresentCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Powinien liczyć spóźnionych studentów")
        void shouldCountLateStudents() {
            assertThat(schedule.getLateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Powinien liczyć nieobecnych studentów")
        void shouldCountAbsentStudents() {
            assertThat(schedule.getAbsentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Powinien liczyć wszystkich studentów z frekwencją")
        void shouldCountTotalAttendance() {
            assertThat(schedule.getTotalAttendanceCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("Powinien generować podsumowanie obecności")
        void shouldGenerateAttendanceSummary() {
            // When
            String summary = schedule.getAttendanceSummary();

            // Then
            assertThat(summary)
                    .contains("Obecni: 2")
                    .contains("Spóźnieni: 1")
                    .contains("Nieobecni: 1")
                    .contains("Razem: 4");
        }

        @Test
        @DisplayName("Powinien obsłużyć brak wpisów frekwencji")
        void shouldHandleNoAttendanceEntries() {
            // Given
            ClassSchedule emptySchedule = new ClassSchedule(
                    "Empty", "", startTime, endTime, "", "", "INF-A"
            );

            // When
            String summary = emptySchedule.getAttendanceSummary();

            // Then
            assertThat(summary).isEqualTo("Brak wpisów frekwencji");
            assertThat(emptySchedule.getTotalAttendanceCount()).isZero();
            assertThat(emptySchedule.getPresentCount()).isZero();
            assertThat(emptySchedule.getLateCount()).isZero();
            assertThat(emptySchedule.getAbsentCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Walidacja danych")
    class DataValidation {

        @ParameterizedTest
        @ValueSource(strings = {
                "Programowanie w Javie",
                "Bazy Danych",
                "Algorytmy i Struktury Danych",
                "Inżynieria Oprogramowania",
                "Sztuczna Inteligencja"
        })
        @DisplayName("Powinien zaakceptować różne nazwy przedmiotów")
        void shouldAcceptVariousSubjectNames(String subject) {
            // When
            schedule.setSubject(subject);

            // Then
            assertThat(schedule.getSubject()).isEqualTo(subject);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Sala 101", "A-123", "Lab 01", "Aula Magna",
                "Sala konferencyjna", "Online", "Teams"
        })
        @DisplayName("Powinien zaakceptować różne sale")
        void shouldAcceptVariousClassrooms(String classroom) {
            // When
            schedule.setClassroom(classroom);

            // Then
            assertThat(schedule.getClassroom()).isEqualTo(classroom);
        }

        @Test
        @DisplayName("Powinien obsłużyć puste i null wartości")
        void shouldHandleEmptyAndNullValues() {
            // When
            schedule.setClassroom(null);
            schedule.setInstructor("");
            schedule.setNotes(null);

            // Then
            assertThat(schedule.getClassroom()).isNull();
            assertThat(schedule.getInstructor()).isEmpty();
            assertThat(schedule.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("toString i reprezentacja tekstowa")
    class StringRepresentation {

        @Test
        @DisplayName("toString() powinien zwrócić reprezentację z przedmiotem i czasem")
        void shouldReturnSubjectAndTimeInToString() {
            // When
            String result = schedule.toString();

            // Then
            assertThat(result).isEqualTo("Programowanie w Javie - 15.03.2024 10:00 - 12:00");
        }

        @Test
        @DisplayName("toString() powinien obsłużyć krótkie nazwy")
        void shouldHandleShortNamesInToString() {
            // Given
            schedule.setSubject("Java");

            // When
            String result = schedule.toString();

            // Then
            assertThat(result).contains("Java");
            assertThat(result).contains("15.03.2024 10:00 - 12:00");
        }
    }

    @Nested
    @DisplayName("Testy przypadków skrajnych")
    class EdgeCases {

        @Test
        @DisplayName("Powinien obsłużyć bardzo długie nazwy")
        void shouldHandleVeryLongNames() {
            // Given
            String veryLongSubject = "A".repeat(200);
            String veryLongInstructor = "B".repeat(100);

            // When
            schedule.setSubject(veryLongSubject);
            schedule.setInstructor(veryLongInstructor);

            // Then
            assertThat(schedule.getSubject()).hasSize(200);
            assertThat(schedule.getInstructor()).hasSize(100);
        }

        @Test
        @DisplayName("Powinien obsłużyć terminsy w przeszłości i przyszłości")
        void shouldHandlePastAndFutureSchedules() {
            // Given
            LocalDateTime pastTime = LocalDateTime.of(2020, 1, 1, 10, 0);
            LocalDateTime futureTime = LocalDateTime.of(2030, 12, 31, 15, 0);

            ClassSchedule pastSchedule = new ClassSchedule(
                    "History", "", pastTime, pastTime.plusHours(2), "", "", "INF-A"
            );

            ClassSchedule futureSchedule = new ClassSchedule(
                    "Future", "", futureTime, futureTime.plusHours(2), "", "", "INF-A"
            );

            // Then
            assertThat(pastSchedule.getStartTime()).isBefore(LocalDateTime.now());
            assertThat(futureSchedule.getStartTime()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Powinien obsłużyć terminy jednominutowe i wielodniowe")
        void shouldHandleVaryingDurations() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 3, 15, 10, 0);

            ClassSchedule shortSchedule = new ClassSchedule(
                    "Short", "", start, start.plusMinutes(1), "", "", "INF-A"
            );

            ClassSchedule longSchedule = new ClassSchedule(
                    "Long", "", start, start.plusDays(7), "", "", "INF-A"
            );

            // Then
            assertThat(shortSchedule.getStartTime()).isEqualTo(start);
            assertThat(shortSchedule.getEndTime()).isEqualTo(start.plusMinutes(1));
            assertThat(longSchedule.getEndTime()).isEqualTo(start.plusDays(7));
        }
    }

    @Nested
    @DisplayName("Testy integracji z obecnością")
    class AttendanceIntegration {

        @Test
        @DisplayName("Dodanie i usunięcie wielu obecności powinno działać płynnie")
        void shouldHandleMultipleAttendanceOperations() {
            // Given
            List<Student> students = List.of(
                    new Student("Student1", "Last1", "111111", "INF-A"),
                    new Student("Student2", "Last2", "222222", "INF-A"),
                    new Student("Student3", "Last3", "333333", "INF-A"),
                    new Student("Student4", "Last4", "444444", "INF-A"),
                    new Student("Student5", "Last5", "555555", "INF-A")
            );

            // When - dodaj wszystkich
            for (Student student : students) {
                schedule.addAttendance(new Attendance(student, schedule, Attendance.Status.PRESENT));
            }

            // Then
            assertThat(schedule.getTotalAttendanceCount()).isEqualTo(5);
            assertThat(schedule.getPresentCount()).isEqualTo(5);

            // When - usuń co drugiego
            schedule.removeAttendance(students.get(1));
            schedule.removeAttendance(students.get(3));

            // Then
            assertThat(schedule.getTotalAttendanceCount()).isEqualTo(3);
            assertThat(schedule.hasAttendanceForStudent(students.get(0))).isTrue();
            assertThat(schedule.hasAttendanceForStudent(students.get(1))).isFalse();
        }

        @Test
        @DisplayName("Zmiana statusu obecności powinna aktualizować statystyki")
        void shouldUpdateStatisticsWhenAttendanceStatusChanges() {
            // Given
            Student student = new Student("Jan", "Kowalski", "123456", "INF-A");
            Attendance attendance = new Attendance(student, schedule, Attendance.Status.PRESENT);
            schedule.addAttendance(attendance);

            // When - zmień status na spóźniony
            attendance.setStatus(Attendance.Status.LATE);
            schedule.addAttendance(attendance); // Re-add to update

            // Then
            assertThat(schedule.getPresentCount()).isEqualTo(0);
            assertThat(schedule.getLateCount()).isEqualTo(1);
            assertThat(schedule.getAbsentCount()).isEqualTo(0);
        }
    }
}