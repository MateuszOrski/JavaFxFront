package com.example.javafxfront;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// DODANE IMPORTY
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class AttendanceService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String ATTENDANCE_ENDPOINT = BASE_URL + "/attendance";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper; // DODANE
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public AttendanceService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // DODANE - Konfiguracja ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Wysyła pojedynczą obecność na serwer
     * @param attendance Obiekt obecności do wysłania
     * @return CompletableFuture z wysłanym obiektem obecności
     */
    public CompletableFuture<Attendance> markAttendanceAsync(Attendance attendance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = attendanceToJson(attendance);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/mark"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    return parseAttendanceFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedział statusem: " + response.statusCode() +
                            " Body: " + response.body());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udało się wysłać obecności na serwer: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Wysyła obecność studenta z podstawowymi danymi (bez pełnego obiektu Attendance)
     * @param student Student
     * @param scheduleId ID terminu
     * @param status Status obecności
     * @param notes Opcjonalne uwagi
     * @return CompletableFuture z potwierdzeniem
     */
    public CompletableFuture<Boolean> markStudentAttendanceAsync(Student student, Long scheduleId,
                                                                 Attendance.Status status, String notes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = createAttendanceJson(student, scheduleId, status, notes);

                System.out.println("📤 Wysyłam obecność JSON: " + jsonBody); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/mark-student"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi: " + response.statusCode()); // DEBUG
                System.out.println("📄 Treść odpowiedzi: " + response.body()); // DEBUG

                return response.statusCode() == 201 || response.statusCode() == 200;

            } catch (Exception e) {
                System.err.println("❌ Błąd wysyłania obecności: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udało się wysłać obecności studenta na serwer: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Pobiera obecności dla konkretnego terminu
     * @param scheduleId ID terminu
     * @return Lista obecności
     */
    public CompletableFuture<List<Attendance>> getAttendancesByScheduleAsync(Long scheduleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🔍 Pobieranie obecności dla terminu ID: " + scheduleId); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/schedule/" + scheduleId))
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status: " + response.statusCode()); // DEBUG
                System.out.println("📄 JSON: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    List<Attendance> attendances = parseAttendanceListFromJson(response.body());
                    System.out.println("✅ Sparsowano " + attendances.size() + " obecności"); // DEBUG
                    return attendances;
                } else {
                    throw new RuntimeException("Serwer odpowiedział statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd pobierania obecności: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udało się pobrać obecności z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Pobiera obecności dla konkretnego studenta
     * @param studentIndexNumber Numer indeksu studenta
     * @return Lista obecności studenta
     */
    public CompletableFuture<List<Attendance>> getAttendancesByStudentAsync(String studentIndexNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedIndex = java.net.URLEncoder.encode(studentIndexNumber, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/student/" + encodedIndex))
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseAttendanceListFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedział statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udało się pobrać obecności studenta z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Usuwa oznaczenie obecności
     * @param studentIndexNumber Numer indeksu studenta
     * @param scheduleId ID terminu
     * @return CompletableFuture<Boolean> - true jeśli usunięto pomyślnie
     */
    public CompletableFuture<Boolean> removeAttendanceAsync(String studentIndexNumber, Long scheduleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedIndex = java.net.URLEncoder.encode(studentIndexNumber, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/remove/" + encodedIndex + "/" + scheduleId))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200 || response.statusCode() == 204;

            } catch (Exception e) {
                throw new RuntimeException("Nie udało się usunąć obecności z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Pobiera statystyki obecności dla grupy
     * @param groupName Nazwa grupy
     * @return CompletableFuture ze statystykami
     */
    public CompletableFuture<String> getGroupAttendanceStatsAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedGroup = java.net.URLEncoder.encode(groupName, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/stats/group/" + encodedGroup))
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return response.body(); // Zwraca JSON ze statystykami
                } else {
                    throw new RuntimeException("Serwer odpowiedział statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udało się pobrać statystyk obecności z serwera: " + e.getMessage(), e);
            }
        });
    }

    // === METODY PRYWATNE DO PARSOWANIA I TWORZENIA JSON ===

    /**
     * Konwertuje obiekt Attendance do JSON
     */
    private String attendanceToJson(Attendance attendance) {
        return String.format(
                "{"
                        + "\"student\": {"
                        + "\"firstName\": \"%s\","
                        + "\"lastName\": \"%s\","
                        + "\"indexNumber\": \"%s\","
                        + "\"groupName\": \"%s\""
                        + "},"
                        + "\"scheduleId\": %d,"
                        + "\"status\": \"%s\","
                        + "\"notes\": \"%s\","
                        + "\"markedAt\": \"%s\""
                        + "}",
                escapeJson(attendance.getStudent().getFirstName()),
                escapeJson(attendance.getStudent().getLastName()),
                escapeJson(attendance.getStudent().getIndexNumber()),
                escapeJson(attendance.getStudent().getGroupName()),
                attendance.getSchedule().getId(),
                attendance.getStatus().name(), // PRESENT, LATE, ABSENT
                escapeJson(attendance.getNotes()),
                attendance.getMarkedAt().format(formatter)
        );
    }

    /**
     * Tworzy JSON dla oznaczenia obecności studenta (uproszczona wersja)
     */
    private String createAttendanceJson(Student student, Long scheduleId,
                                        Attendance.Status status, String notes) {
        return String.format(
                "{"
                        + "\"firstName\": \"%s\","
                        + "\"lastName\": \"%s\","
                        + "\"indexNumber\": \"%s\","
                        + "\"groupName\": \"%s\","
                        + "\"scheduleId\": %d,"
                        + "\"status\": \"%s\","
                        + "\"notes\": \"%s\""
                        + "}",
                escapeJson(student.getFirstName()),
                escapeJson(student.getLastName()),
                escapeJson(student.getIndexNumber()),
                escapeJson(student.getGroupName() != null ? student.getGroupName() : ""),
                scheduleId,
                status.name(), // PRESENT, LATE, ABSENT
                escapeJson(notes != null ? notes : "")
        );
    }

    /**
     * NOWA IMPLEMENTACJA - Parsuje pojedynczy obiekt Attendance z JSON
     */
    private Attendance parseAttendanceFromJson(String json) {
        try {
            AttendanceFromServer serverAttendance = objectMapper.readValue(json, AttendanceFromServer.class);
            return convertToAttendance(serverAttendance);
        } catch (JsonProcessingException e) {
            System.err.println("❌ Błąd parsowania JSON attendance: " + e.getMessage());
            System.err.println("JSON: " + json);
            throw new RuntimeException("Failed to parse attendance JSON: " + e.getMessage(), e);
        }
    }

    /**
     * NOWA IMPLEMENTACJA - Parsuje listę obecności z JSON
     */
    private List<Attendance> parseAttendanceListFromJson(String json) {
        try {
            List<AttendanceFromServer> serverAttendances = objectMapper.readValue(json, new TypeReference<List<AttendanceFromServer>>() {});

            return serverAttendances.stream()
                    .map(this::convertToAttendance)
                    .toList();
        } catch (JsonProcessingException e) {
            System.err.println("❌ Błąd parsowania JSON attendance list: " + e.getMessage());
            System.err.println("JSON: " + json);
            throw new RuntimeException("Failed to parse attendance list JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Konwertuje AttendanceFromServer na Attendance
     */
    private Attendance convertToAttendance(AttendanceFromServer serverAttendance) {
        // Utwórz studenta na podstawie danych z serwera
        Student student = new Student(
                serverAttendance.student.firstName,
                serverAttendance.student.lastName,
                serverAttendance.student.indexNumber,
                serverAttendance.student.group != null ? serverAttendance.student.group.name : null
        );

        // Utwórz termin na podstawie danych z serwera
        ClassSchedule schedule = new ClassSchedule(
                serverAttendance.schedule.id,
                serverAttendance.schedule.subject,
                serverAttendance.schedule.classroom,
                serverAttendance.schedule.startTime,
                serverAttendance.schedule.endTime,
                serverAttendance.schedule.instructor,
                serverAttendance.schedule.notes,
                serverAttendance.schedule.group != null ? serverAttendance.schedule.group.name : "Nieznana grupa",
                serverAttendance.schedule.createdDate
        );

        // Utwórz obecność
        Attendance.Status status;
        switch (serverAttendance.status) {
            case "PRESENT": status = Attendance.Status.PRESENT; break;
            case "LATE": status = Attendance.Status.LATE; break;
            case "ABSENT": status = Attendance.Status.ABSENT; break;
            default: status = Attendance.Status.ABSENT;
        }

        Attendance attendance = new Attendance(student, schedule, status, serverAttendance.notes);
        attendance.setMarkedAt(serverAttendance.markedAt);

        return attendance;
    }

    /**
     * Pomocnicza metoda do escape'owania stringów w JSON
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // === KLASY POMOCNICZE DO DESERIALIZACJI ===

    private static class AttendanceFromServer {
        public Long id;
        public StudentInfo student;
        public ScheduleInfo schedule;
        public String status;
        public String notes;
        public LocalDateTime markedAt;
        public Boolean justified;
    }

    private static class StudentInfo {
        public Long id;
        public String firstName;
        public String lastName;
        public String indexNumber;
        public GroupInfo group;
    }

    private static class ScheduleInfo {
        public Long id;
        public String subject;
        public String classroom;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String instructor;
        public String notes;
        public LocalDateTime createdDate;
        public GroupInfo group;
    }

    private static class GroupInfo {
        public Long id;
        public String name;
        public String specialization;
    }
}