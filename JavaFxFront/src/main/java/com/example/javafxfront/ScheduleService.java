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

    private String scheduleToJson(ClassSchedule schedule) {
        try {
            ScheduleToServer scheduleToServer = new ScheduleToServer();
            scheduleToServer.subject = schedule.getSubject();
            scheduleToServer.classroom = schedule.getClassroom() != null ? schedule.getClassroom() : "";
            scheduleToServer.startTime = schedule.getStartTime();
            scheduleToServer.endTime = schedule.getEndTime();
            scheduleToServer.instructor = schedule.getInstructor() != null ? schedule.getInstructor() : "";
            scheduleToServer.notes = schedule.getNotes() != null ? schedule.getNotes() : "";
            scheduleToServer.groupName = schedule.getGroupName();

            return objectMapper.writeValueAsString(scheduleToServer);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert schedule to JSON: " + e.getMessage(), e);
        }
    }

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

    // === KLASY POMOCNICZE DO SERIALIZACJI - BEZ ADNOTACJI ===

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

    private static class ScheduleToServer {
        public String subject;
        public String classroom;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String instructor;
        public String notes;
        public String groupName;
    }
}