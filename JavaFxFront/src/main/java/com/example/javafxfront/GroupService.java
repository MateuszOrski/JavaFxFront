package com.example.javafxfront;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroupService {

    // KONFIGURACJA SERWERA - ZMIEŃ NA SWÓJ ENDPOINT
    private static final String BASE_URL = "http://localhost:8080/api"; // Twój serwer
    private static final String GROUPS_ENDPOINT = BASE_URL + "/groups";

    private final HttpClient httpClient;

    public GroupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Pobiera wszystkie grupy z serwera (asynchronicznie)
     * @return CompletableFuture z listą grup
     */
    public CompletableFuture<List<Group>> getAllGroupsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GROUPS_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseGroupsFromJson(response.body());
                } else {
                    throw new RuntimeException("Server responded with status: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch groups from server: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Dodaje nową grupę na serwerze (asynchronicznie)
     * @param group Grupa do dodania
     * @return CompletableFuture z dodaną grupą
     */
    public CompletableFuture<Group> addGroupAsync(Group group) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = groupToJson(group);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GROUPS_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    return parseGroupFromJson(response.body());
                } else {
                    throw new RuntimeException("Server responded with status: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to add group to server: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Usuwa grupę z serwera (asynchronicznie)
     * @param groupId ID grupy do usunięcia
     * @return CompletableFuture<Boolean> - true jeśli usunięto pomyślnie
     */
    public CompletableFuture<Boolean> deleteGroupAsync(String groupId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GROUPS_ENDPOINT + "/" + groupId))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200 || response.statusCode() == 204;

            } catch (Exception e) {
                throw new RuntimeException("Failed to delete group from server: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Sprawdza połączenie z serwerem
     * @return CompletableFuture<Boolean> - true jeśli serwer jest dostępny
     */
    public CompletableFuture<Boolean> checkServerConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/health")) // endpoint health check
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200;

            } catch (Exception e) {
                return false; // Serwer niedostępny
            }
        });
    }

    // === METODY PRYWATNE DO PARSOWANIA JSON ===

    private List<Group> parseGroupsFromJson(String json) {
        // PRZYKŁAD - Dostosuj do swojego formatu JSON
        // Tutaj możesz użyć biblioteki jak Jackson, Gson, lub parsować ręcznie

        /* Przykładowy format JSON z serwera:
        [
            {
                "id": "1",
                "name": "Grupa INF-A",
                "specialization": "Informatyka",
                "createdDate": "2024-01-15T10:30:00"
            },
            {
                "id": "2",
                "name": "Grupa MAT-B",
                "specialization": "Matematyka",
                "createdDate": "2024-01-16T14:20:00"
            }
        ]
        */

        // TYMCZASOWA IMPLEMENTACJA - ZMIEŃ NA PRAWDZIWE PARSOWANIE
        java.util.List<Group> groups = new java.util.ArrayList<>();

        // Tu dodaj kod parsujący JSON z Twojego serwera
        // Na przykład używając Jackson ObjectMapper:
        // ObjectMapper mapper = new ObjectMapper();
        // return mapper.readValue(json, new TypeReference<List<Group>>(){});

        return groups;
    }

    private Group parseGroupFromJson(String json) {
        // TYMCZASOWA IMPLEMENTACJA - ZMIEŃ NA PRAWDZIWE PARSOWANIE
        // Parsuj pojedynczą grupę z JSON response

        return null; // Zamień na prawdziwe parsowanie
    }

    private String groupToJson(Group group) {
        // TYMCZASOWA IMPLEMENTACJA - ZMIEŃ NA PRAWDZIWE TWORZENIE JSON
        // Konwertuj grupę do JSON dla wysłania na serwer

        /* Przykładowy format:
        {
            "name": "Grupa INF-A",
            "specialization": "Informatyka"
        }
        */

        return String.format(
                "{\"name\":\"%s\",\"specialization\":\"%s\"}",
                group.getName(),
                group.getSpecialization()
        );
    }
}