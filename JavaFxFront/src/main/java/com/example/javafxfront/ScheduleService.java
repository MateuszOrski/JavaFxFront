package com.example.javafxfront;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ScheduleService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String SCHEDULES_ENDPOINT = BASE_URL + "/schedules";

    private final HttpClient httpClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ScheduleService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Pobiera wszystkie terminy z serwera
     */
    public CompletableFuture<List<ClassSchedule>> getAllSchedulesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SCHEDULES_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseSchedulesFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac terminow z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Pobiera terminy dla konkretnej grupy
     */
    public CompletableFuture<List<ClassSchedule>> getSchedulesByGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = SCHEDULES_ENDPOINT + "/group/" + java.net.URLEncoder.encode(groupName, "UTF-8");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseSchedulesFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac terminow grupy z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Dodaje termin na serwer
     */
    public CompletableFuture<ClassSchedule> addScheduleAsync(ClassSchedule schedule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = scheduleToJson(schedule);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SCHEDULES_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    return parseScheduleFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie dodac terminu na serwer: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Usuwa termin z serwera
     */
    public CompletableFuture<Boolean> deleteScheduleAsync(Long scheduleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SCHEDULES_ENDPOINT + "/" + scheduleId))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200 || response.statusCode() == 204;

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie usunac terminu z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Aktualizuje termin na serwerze
     */
    public CompletableFuture<ClassSchedule> updateScheduleAsync(Long scheduleId, ClassSchedule schedule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = scheduleToJson(schedule);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SCHEDULES_ENDPOINT + "/" + scheduleId))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseScheduleFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie zaktualizowac terminu na serwerze: " + e.getMessage(), e);
            }
        });
    }

    // === METODY PRYWATNE DO PARSOWANIA JSON ===

    private List<ClassSchedule> parseSchedulesFromJson(String json) {
        // TYMCZASOWA IMPLEMENTACJA - DOSTOSUJ DO SWOJEGO FORMATU JSON
        // Tu dodaj prawdziwe parsowanie JSON używając Jackson, Gson, itp.

        /* Przykładowy format JSON z serwera:
        [
            {
                "id": 1,
                "subject": "Egzamin z Javy",
                "classroom": "Sala 101",
                "startTime": "2024-06-15T10:30:00",
                "endTime": "2024-06-15T12:30:00",
                "instructor": "Dr Kowalski",
                "notes": "Przyniesć długopis",
                "groupName": "Grupa INF-A",
                "createdDate": "2024-01-15T10:30:00"
            }
        ]
        */

        java.util.List<ClassSchedule> schedules = new java.util.ArrayList<>();

        // Tutaj dodaj kod parsujący JSON z Twojego serwera
        // Na przykład używając Jackson ObjectMapper:
        // ObjectMapper mapper = new ObjectMapper();
        // mapper.registerModule(new JavaTimeModule());
        // return mapper.readValue(json, new TypeReference<List<ClassSchedule>>(){});

        return schedules;
    }

    private ClassSchedule parseScheduleFromJson(String json) {
        // TYMCZASOWA IMPLEMENTACJA - DOSTOSUJ DO SWOJEGO FORMATU
        // Parsuj pojedynczy termin z JSON response

        // Tu możesz zwrócić null lub rzucić wyjątek - dostosuj do potrzeb
        return null; // Zamień na prawdziwe parsowanie
    }

    private String scheduleToJson(ClassSchedule schedule) {
        // TYMCZASOWA IMPLEMENTACJA - DOSTOSUJ DO SWOJEGO FORMATU
        // Konwertuj termin do JSON dla wysłania na serwer

        /* Przykładowy format dla serwera:
        {
            "subject": "Egzamin z Javy",
            "classroom": "Sala 101",
            "startTime": "2024-06-15T10:30:00",
            "endTime": "2024-06-15T12:30:00",
            "instructor": "Dr Kowalski",
            "notes": "Przyniesć długopis",
            "groupName": "Grupa INF-A"
        }
        */

        return String.format(
                "{\"subject\":\"%s\",\"classroom\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"instructor\":\"%s\",\"notes\":\"%s\",\"groupName\":\"%s\"}",
                escapeJson(schedule.getSubject()),
                escapeJson(schedule.getClassroom()),
                schedule.getStartTime().format(formatter),
                schedule.getEndTime().format(formatter),
                escapeJson(schedule.getInstructor()),
                escapeJson(schedule.getNotes()),
                escapeJson(schedule.getGroupName())
        );
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