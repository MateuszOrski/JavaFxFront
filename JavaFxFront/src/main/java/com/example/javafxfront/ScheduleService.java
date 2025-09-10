package com.example.javafxfront;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ScheduleService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String SCHEDULES_ENDPOINT = BASE_URL + "/schedules";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ScheduleService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // KLUCZOWE - Ignoruj nieznane właściwości globalnie
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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

    public CompletableFuture<List<ClassSchedule>> getSchedulesByGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("=== FRONTEND: getSchedulesByGroupAsync ===");
                System.out.println("🔗 Grupa: '" + groupName + "'");

                String encodedGroupName = java.net.URLEncoder.encode(groupName, "UTF-8");
                String url = SCHEDULES_ENDPOINT + "/group/" + encodedGroupName;

                System.out.println("🔗 Wywołuję URL: " + url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi: " + response.statusCode());
                System.out.println("📄 Treść odpowiedzi: " + response.body());

                if (response.statusCode() == 200) {
                    List<ClassSchedule> schedules = parseSchedulesFromJson(response.body());
                    System.out.println("✅ Sparsowano " + schedules.size() + " terminów");

                    // Debug - wypisz szczegóły każdego terminu
                    for (int i = 0; i < schedules.size(); i++) {
                        ClassSchedule s = schedules.get(i);
                        System.out.println("  " + (i+1) + ". " + s.getSubject() +
                                " (ID: " + s.getId() + ", grupa: " + s.getGroupName() + ")");
                    }

                    return schedules;
                } else {
                    System.err.println("❌ Serwer odpowiedział statusem: " + response.statusCode());
                    System.err.println("❌ Treść błędu: " + response.body());
                    throw new RuntimeException("Serwer odpowiedział statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd getSchedulesByGroupAsync: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Nie udało się pobrać terminów grupy z serwera: " + e.getMessage(), e);
            }
        });
    }

    // ================================
    // 🔧 GŁÓWNA POPRAWKA - addScheduleAsync
    // ================================
    public CompletableFuture<ClassSchedule> addScheduleAsync(ClassSchedule schedule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("=== WYSYŁANIE TERMINU NA SERWER ===");
                System.out.println("📋 Termin: " + schedule.getSubject());
                System.out.println("📅 Data: " + schedule.getStartTime());
                System.out.println("🏫 Grupa: " + schedule.getGroupName());

                // 🔧 POPRAWIONY JSON - używamy ręcznego tworzenia dla większej kontroli
                String jsonBody = createScheduleJsonManually(schedule);

                System.out.println("📤 Wysyłam JSON: " + jsonBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SCHEDULES_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi: " + response.statusCode());
                System.out.println("📄 Treść odpowiedzi: " + response.body());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    ClassSchedule savedSchedule = parseScheduleFromJson(response.body());
                    System.out.println("✅ Termin zapisany na serwerze z ID: " + savedSchedule.getId());
                    return savedSchedule;
                } else {
                    System.err.println("❌ Serwer odpowiedział błędem: " + response.statusCode());
                    System.err.println("❌ Treść błędu: " + response.body());
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Szczegóły: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd wysyłania terminu na serwer: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Nie udalo sie dodac terminu na serwer: " + e.getMessage(), e);
            }
        });
    }

    // ================================
    // 🔧 NOWA METODA - Ręczne tworzenie JSON
    // ================================
    private String createScheduleJsonManually(ClassSchedule schedule) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{");

            // Podstawowe pola terminu
            json.append("\"subject\":\"").append(escapeJson(schedule.getSubject())).append("\",");
            json.append("\"classroom\":\"").append(escapeJson(schedule.getClassroom() != null ? schedule.getClassroom() : "")).append("\",");
            json.append("\"startTime\":\"").append(schedule.getStartTime().format(formatter)).append("\",");
            json.append("\"endTime\":\"").append(schedule.getEndTime().format(formatter)).append("\",");
            json.append("\"instructor\":\"").append(escapeJson(schedule.getInstructor() != null ? schedule.getInstructor() : "")).append("\",");
            json.append("\"notes\":\"").append(escapeJson(schedule.getNotes() != null ? schedule.getNotes() : "")).append("\"");

            // 🔧 KLUCZOWE - Dodaj grupę jako obiekt (zgodnie z backendem)
            if (schedule.getGroupName() != null && !schedule.getGroupName().trim().isEmpty()) {
                json.append(",\"group\":{\"name\":\"").append(escapeJson(schedule.getGroupName())).append("\"}");
            }

            json.append("}");
            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create schedule JSON manually: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<Boolean> deleteScheduleAsync(Long scheduleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🗑️ Usuwam termin z serwera ID: " + scheduleId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SCHEDULES_ENDPOINT + "/" + scheduleId))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status usuwania: " + response.statusCode());

                boolean success = response.statusCode() == 200 || response.statusCode() == 204;
                if (success) {
                    System.out.println("✅ Termin usunięty z serwera");
                } else {
                    System.err.println("❌ Nie udało się usunąć terminu: " + response.body());
                }

                return success;

            } catch (Exception e) {
                System.err.println("❌ Błąd usuwania terminu: " + e.getMessage());
                throw new RuntimeException("Nie udalo sie usunac terminu z serwera: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<ClassSchedule> updateScheduleAsync(Long scheduleId, ClassSchedule schedule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 🔧 POPRAWKA - używaj nowej metody tworzenia JSON
                String jsonBody = createScheduleJsonManually(schedule);

                System.out.println("🔄 Aktualizuję termin ID: " + scheduleId);
                System.out.println("📤 JSON: " + jsonBody);

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
                    System.out.println("✅ Termin zaktualizowany na serwerze");
                    return parseScheduleFromJson(response.body());
                } else {
                    System.err.println("❌ Błąd aktualizacji: " + response.statusCode() + " - " + response.body());
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd aktualizacji terminu: " + e.getMessage());
                throw new RuntimeException("Nie udalo sie zaktualizowac terminu na serwerze: " + e.getMessage(), e);
            }
        });
    }

    // === METODY PRYWATNE DO PARSOWANIA JSON ===

    private List<ClassSchedule> parseSchedulesFromJson(String json) {
        try {
            List<ScheduleFromServer> serverSchedules = objectMapper.readValue(json, new TypeReference<List<ScheduleFromServer>>() {});

            return serverSchedules.stream()
                    .map(this::convertToClassSchedule)
                    .toList();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse schedules JSON: " + e.getMessage(), e);
        }
    }

    private ClassSchedule parseScheduleFromJson(String json) {
        try {
            ScheduleFromServer serverSchedule = objectMapper.readValue(json, ScheduleFromServer.class);
            return convertToClassSchedule(serverSchedule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse schedule JSON: " + e.getMessage(), e);
        }
    }

    // ================================
    // 🗑️ USUNIĘTA STARA METODA scheduleToJson
    // Zastąpiona przez createScheduleJsonManually
    // ================================

    private ClassSchedule convertToClassSchedule(ScheduleFromServer serverSchedule) {
        String groupName = serverSchedule.group != null ? serverSchedule.group.name : "Nieznana grupa";

        ClassSchedule schedule = new ClassSchedule(
                serverSchedule.id,
                serverSchedule.subject,
                serverSchedule.classroom,
                serverSchedule.startTime,
                serverSchedule.endTime,
                serverSchedule.instructor,
                serverSchedule.notes,
                groupName,
                serverSchedule.createdDate
        );

        return schedule;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    private static class ScheduleFromServer {
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

    // ================================
    // 🗑️ USUNIĘTA KLASA ScheduleToServer
    // Nie jest już potrzebna - używamy ręcznego tworzenia JSON
    // ================================
}