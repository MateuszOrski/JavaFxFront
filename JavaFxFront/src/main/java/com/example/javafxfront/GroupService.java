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
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroupService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String GROUPS_ENDPOINT = BASE_URL + "/groups";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // KLUCZOWE - Ignoruj nieznane właściwości globalnie
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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
                    throw new RuntimeException("Server responded with status: " + response.statusCode()
                            + " Body: " + response.body());
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch groups from server: " + e.getMessage(), e);
            }
        });
    }

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
                } else if (response.statusCode() == 409) {
                    throw new GroupAlreadyExistsException("Grupa o nazwie '" + group.getName() + "' już istnieje w systemie!");
                } else {
                    throw new RuntimeException("Server responded with status: " + response.statusCode()
                            + " Body: " + response.body());
                }

            } catch (GroupAlreadyExistsException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to add group to server: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> deleteGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Sprawdź różne warianty URL-a
                String encodedName = java.net.URLEncoder.encode(groupName, "UTF-8");

                // Lista możliwych endpointów do sprawdzenia
                String[] possibleUrls = {
                        GROUPS_ENDPOINT + "/" + encodedName,           // /api/groups/nazwa
                        GROUPS_ENDPOINT + "/delete/" + encodedName,    // /api/groups/delete/nazwa
                        GROUPS_ENDPOINT + "?name=" + encodedName       // /api/groups?name=nazwa
                };

                System.out.println("=== PRÓBA USUWANIA GRUPY: " + groupName + " ===");

                for (String url : possibleUrls) {
                    System.out.println("Próbuję URL: " + url);

                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .timeout(Duration.ofSeconds(30))
                                .DELETE()
                                .build();

                        HttpResponse<String> response = httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());

                        System.out.println("Status odpowiedzi: " + response.statusCode());
                        System.out.println("Treść odpowiedzi: '" + response.body() + "'");

                        // ========== POPRAWIONA LOGIKA STATUSÓW ==========
                        if (response.statusCode() == 200 || response.statusCode() == 204) {
                            // 200 OK - usunięto pomyślnie
                            // 204 No Content - usunięto pomyślnie (bez treści odpowiedzi)
                            System.out.println("✅ Grupa została usunięta pomyślnie (status: " + response.statusCode() + ")");
                            return true;

                        } else if (response.statusCode() == 404) {
                            // 404 Not Found - endpoint nie istnieje LUB grupa nie istnieje
                            System.out.println("❌ 404 - Endpoint nie istnieje lub grupa nie znaleziona na serwerze");
                            // NIE TRAKTUJ JAKO SUKCES! Kontynuuj próby z innymi URL-ami

                        } else if (response.statusCode() == 405) {
                            // 405 Method Not Allowed - serwer nie obsługuje DELETE na tym endpoincie
                            System.out.println("❌ 405 - Metoda DELETE nie jest obsługiwana na: " + url);

                        } else if (response.statusCode() >= 400 && response.statusCode() < 500) {
                            // 4xx - błąd klienta
                            System.out.println("❌ Błąd klienta " + response.statusCode() + " dla URL: " + url);

                        } else if (response.statusCode() >= 500) {
                            // 5xx - błąd serwera
                            System.out.println("❌ Błąd serwera " + response.statusCode() + " dla URL: " + url);

                        } else {
                            System.out.println("❓ Nieoczekiwany status " + response.statusCode() + " dla URL: " + url);
                        }

                    } catch (Exception urlException) {
                        System.err.println("❌ Wyjątek dla URL " + url + ": " + urlException.getMessage());
                    }
                }

                // Jeśli żaden URL nie zadziałał
                System.err.println("❌ WSZYSTKIE URL-e niepomyślne - grupa nie została usunięta");
                return false;

            } catch (Exception e) {
                System.err.println("❌ Ogólny błąd usuwania grupy: " + e.getMessage());
                throw new RuntimeException("Failed to delete group from server: " + e.getMessage(), e);
            }
        });
    }
    public CompletableFuture<String> checkAvailableEndpoints(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("=== SPRAWDZANIE DOSTĘPNYCH ENDPOINTÓW DLA GRUPY: " + groupName + " ===");

            try {
                String encodedName = java.net.URLEncoder.encode(groupName, "UTF-8");

                // Testuj różne metody HTTP i URL-e
                String[] testUrls = {
                        GROUPS_ENDPOINT + "/" + encodedName,
                        GROUPS_ENDPOINT + "/delete/" + encodedName,
                        GROUPS_ENDPOINT + "?name=" + encodedName,
                        GROUPS_ENDPOINT + "/remove/" + encodedName,
                        BASE_URL + "/group/" + encodedName,
                        BASE_URL + "/deleteGroup/" + encodedName
                };

                String[] methods = {"DELETE", "POST", "PUT"};

                StringBuilder results = new StringBuilder();

                for (String url : testUrls) {
                    for (String method : methods) {
                        try {
                            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .header("Content-Type", "application/json")
                                    .header("Accept", "application/json")
                                    .timeout(Duration.ofSeconds(10));

                            // Ustaw metodę HTTP
                            switch (method) {
                                case "DELETE":
                                    requestBuilder.DELETE();
                                    break;
                                case "POST":
                                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString("{}"));
                                    break;
                                case "PUT":
                                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString("{}"));
                                    break;
                            }

                            HttpRequest request = requestBuilder.build();
                            HttpResponse<String> response = httpClient.send(request,
                                    HttpResponse.BodyHandlers.ofString());

                            String result = method + " " + url + " -> " + response.statusCode();
                            System.out.println(result);
                            results.append(result).append("\n");

                            // Jeśli status nie jest 404/405, endpoint może istnieć
                            if (response.statusCode() != 404 && response.statusCode() != 405) {
                                System.out.println("⭐ POTENCJALNIE DZIAŁAJĄCY ENDPOINT: " + method + " " + url);
                            }

                        } catch (Exception e) {
                            String error = method + " " + url + " -> BŁĄD: " + e.getMessage();
                            System.out.println(error);
                            results.append(error).append("\n");
                        }
                    }
                }

                return results.toString();

            } catch (Exception e) {
                return "Błąd testowania endpointów: " + e.getMessage();
            }
        });
    }

    public CompletableFuture<Boolean> checkServerConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GROUPS_ENDPOINT + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200;

            } catch (Exception e) {
                return false;
            }
        });
    }


    // === METODY PRYWATNE DO PARSOWANIA JSON ===

    private List<Group> parseGroupsFromJson(String json) {
        try {
            List<GroupFromServer> serverGroups = objectMapper.readValue(json, new TypeReference<List<GroupFromServer>>() {});

            return serverGroups.stream()
                    .map(this::convertToGroup)
                    .toList();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse groups JSON: " + e.getMessage(), e);
        }
    }

    private Group parseGroupFromJson(String json) {
        try {
            GroupFromServer serverGroup = objectMapper.readValue(json, GroupFromServer.class);
            return convertToGroup(serverGroup);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse group JSON: " + e.getMessage(), e);
        }
    }

    private String groupToJson(Group group) {
        try {
            GroupToServer groupToServer = new GroupToServer();
            groupToServer.name = group.getName();
            groupToServer.specialization = group.getSpecialization();

            return objectMapper.writeValueAsString(groupToServer);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert group to JSON: " + e.getMessage(), e);
        }
    }

    private Group convertToGroup(GroupFromServer serverGroup) {
        Group group = new Group(serverGroup.name, serverGroup.specialization);
        return group;
    }

    // === KLASY POMOCNICZE DO SERIALIZACJI - BEZ ADNOTACJI ===

    private static class GroupFromServer {
        public Long id;
        public String name;
        public String specialization;
        public LocalDateTime createdDate;
        public Boolean active;
    }

    private static class GroupToServer {
        public String name;
        public String specialization;
    }

    // === CUSTOM EXCEPTION ===
    public static class GroupAlreadyExistsException extends RuntimeException {
        public GroupAlreadyExistsException(String message) {
            super(message);
        }
    }
}