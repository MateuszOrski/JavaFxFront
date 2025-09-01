package com.example.javafxfront;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AttendanceService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String ATTENDANCE_ENDPOINT = BASE_URL + "/attendance";

    private final HttpClient httpClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public AttendanceService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/mark-student"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 201 || response.statusCode() == 200;

            } catch (Exception e) {
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
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ATTENDANCE_ENDPOINT + "/schedule/" + scheduleId))
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
     * Parsuje pojedynczy obiekt Attendance z JSON
     * TYMCZASOWA IMPLEMENTACJA - DOSTOSUJ DO SWOJEGO FORMATU
     */
    private Attendance parseAttendanceFromJson(String json) {
        // PRZYKŁAD OCZEKIWANEGO FORMATU Z SERWERA:
        /*
        {
            "id": 1,
            "student": {
                "firstName": "Jan",
                "lastName": "Kowalski",
                "indexNumber": "123456",
                "groupName": "Grupa INF-A"
            },
            "scheduleId": 5,
            "status": "PRESENT",
            "notes": "Przybyła na czas",
            "markedAt": "2024-06-15T10:30:00"
        }
        */

        // TYMCZASOWO - zwracamy null
        // Tu dodaj prawdziwe parsowanie JSON używając Jackson/Gson
        return null;
    }

    /**
     * Parsuje listę obecności z JSON
     * TYMCZASOWA IMPLEMENTACJA - DOSTOSUJ DO SWOJEGO FORMATU
     */
    private List<Attendance> parseAttendanceListFromJson(String json) {
        // PRZYKŁAD OCZEKIWANEGO FORMATU Z SERWERA:
        /*
        [
            {
                "id": 1,
                "student": {...},
                "scheduleId": 5,
                "status": "PRESENT",
                "notes": "",
                "markedAt": "2024-06-15T10:30:00"
            },
            ...
        ]
        */

        // TYMCZASOWO - zwracamy pustą listę
        java.util.List<Attendance> attendances = new java.util.ArrayList<>();

        // Tu dodaj prawdziwe parsowanie JSON używając Jackson/Gson
        // ObjectMapper mapper = new ObjectMapper();
        // mapper.registerModule(new JavaTimeModule());
        // return mapper.readValue(json, new TypeReference<List<Attendance>>(){});

        return attendances;
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
}