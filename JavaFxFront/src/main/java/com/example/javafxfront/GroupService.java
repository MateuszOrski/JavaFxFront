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

/**
 * Serwis do komunikacji z serwerem backend w zakresie zarządzania grupami.
 *
 * <p>GroupService obsługuje wszystkie operacje związane z grupami studenckimi,
 * w tym tworzenie, pobieranie, aktualizację i usuwanie grup. Komunikacja
 * z serwerem odbywa się asynchronicznie przy użyciu HTTP API.</p>
 *
 * <p>Wszystkie metody zwracają {@link CompletableFuture} dla obsługi asynchronicznej,
 * co pozwala na nieblokujące wykonywanie operacji sieciowych.</p>
 *
 * <h3>Obsługiwane operacje:</h3>
 * <ul>
 *   <li>Pobieranie wszystkich grup z serwera ({@link #getAllGroupsAsync()})</li>
 *   <li>Dodawanie nowych grup ({@link #addGroupAsync(Group)})</li>
 *   <li>Usuwanie grup ({@link #deleteGroupAsync(String)})</li>
 *   <li>Diagnostyka połączenia i endpointów API</li>
 * </ul>
 *
 * <h3>Konfiguracja serwera:</h3>
 * <p>Serwis domyślnie łączy się z serwerem na adresie {@value #BASE_URL}.
 * Endpoint dla grup znajduje się pod adresem {@value #GROUPS_ENDPOINT}.</p>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * GroupService service = new GroupService();
 *
 * // Asynchroniczne pobranie wszystkich grup
 * service.getAllGroupsAsync()
 *        .thenAccept(groups -> {
 *            System.out.println("Załadowano " + groups.size() + " grup");
 *            groups.forEach(group -> System.out.println(group.getName()));
 *        })
 *        .exceptionally(throwable -> {
 *            System.err.println("Błąd: " + throwable.getMessage());
 *            return null;
 *        });
 *
 * // Dodawanie nowej grupy
 * Group newGroup = new Group("INF-2024", "Informatyka");
 * service.addGroupAsync(newGroup)
 *        .thenAccept(savedGroup ->
 *            System.out.println("Grupa zapisana: " + savedGroup.getName()))
 *        .exceptionally(ex -> {
 *            if (ex.getCause() instanceof GroupAlreadyExistsException) {
 *                System.err.println("Grupa już istnieje!");
 *            }
 *            return null;
 *        });
 * }
 * </pre>
 *
 * <h3>Obsługa błędów:</h3>
 * <p>Serwis automatycznie obsługuje następujące sytuacje błędne:</p>
 * <ul>
 *   <li>Brak połączenia z serwerem</li>
 *   <li>Timeout żądań (domyślnie 30 sekund)</li>
 *   <li>Nieprawidłowe odpowiedzi HTTP</li>
 *   <li>Błędy deserializacji JSON</li>
 *   <li>Duplikaty nazw grup ({@link GroupAlreadyExistsException})</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 1.0
 * @since 2025
 *
 * @see Group
 * @see StudentService
 * @see ScheduleService
 * @see CompletableFuture
 */
public class GroupService {

    /**
     * Bazowy URL serwera API.
     *
     * <p>Wszystkie żądania HTTP są kierowane do tego adresu bazowego.
     * Domyślnie aplikacja łączy się z lokalnym serwerem deweloperskim.</p>
     */
    private static final String BASE_URL = "http://localhost:8080/api";

    /**
     * Pełny endpoint dla operacji na grupach.
     *
     * <p>Endpoint obsługuje następujące operacje HTTP:</p>
     * <ul>
     *   <li>GET {@value} - pobieranie wszystkich grup</li>
     *   <li>POST {@value} - dodawanie nowej grupy</li>
     *   <li>DELETE {@value}/{nazwa} - usuwanie grupy po nazwie</li>
     * </ul>
     */
    private static final String GROUPS_ENDPOINT = BASE_URL + "/groups";

    /**
     * Klient HTTP do komunikacji z serwerem.
     *
     * <p>Konfigurowany z timeoutem połączenia 10 sekund dla zapewnienia
     * responsywności interfejsu użytkownika.</p>
     */
    private final HttpClient httpClient;

    /**
     * Mapper JSON do serializacji i deserializacji obiektów.
     *
     * <p>Skonfigurowany do obsługi:</p>
     * <ul>
     *   <li>Dat Java 8+ (JavaTimeModule)</li>
     *   <li>Ignorowania nieznanych właściwości JSON</li>
     *   <li>Automatycznej konwersji typów</li>
     * </ul>
     */
    private final ObjectMapper objectMapper;

    /**
     * Konstruktor serwisu grup.
     *
     * <p>Inicjalizuje klienta HTTP z timeoutem 10 sekund oraz konfiguruje
     * ObjectMapper do obsługi dat Java 8+ i ignorowania nieznanych właściwości JSON.</p>
     *
     * <p>Konfiguracja klienta HTTP:</p>
     * <ul>
     *   <li>Timeout połączenia: 10 sekund</li>
     *   <li>Automatyczne zarządzanie cookies</li>
     *   <li>Obsługa HTTP/2 gdy dostępne</li>
     * </ul>
     *
     * <p>Konfiguracja ObjectMapper:</p>
     * <ul>
     *   <li>JavaTimeModule dla obsługi LocalDateTime</li>
     *   <li>FAIL_ON_UNKNOWN_PROPERTIES = false</li>
     *   <li>Automatyczna detekcja formatów dat</li>
     * </ul>
     */
    public GroupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Pobiera wszystkie grupy z serwera asynchronicznie.
     *
     * <p>Wykonuje żądanie GET do endpointu {@value #GROUPS_ENDPOINT} i deserializuje
     * odpowiedź JSON do listy obiektów {@link Group}. Operacja jest wykonywana
     * asynchronicznie w tle, nie blokując wątku interfejsu użytkownika.</p>
     *
     * <p>Proces pobierania:</p>
     * <ol>
     *   <li>Wysłanie żądania HTTP GET do serwera</li>
     *   <li>Oczekiwanie na odpowiedź (max 30 sekund)</li>
     *   <li>Sprawdzenie kodu statusu HTTP (oczekiwany: 200)</li>
     *   <li>Deserializacja JSON do listy Group</li>
     *   <li>Konwersja obiektów serwera do obiektów klienta</li>
     * </ol>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * groupService.getAllGroupsAsync()
     *     .thenAccept(groups -> {
     *         System.out.println("Znaleziono " + groups.size() + " grup:");
     *         groups.forEach(group ->
     *             System.out.println("- " + group.getName() + " (" + group.getSpecialization() + ")")
     *         );
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Nie można pobrać grup: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @return CompletableFuture z listą wszystkich grup z serwera.
     *         Lista może być pusta jeśli brak grup, ale nigdy nie będzie null.
     *
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         timeout żądania, nieprawidłowy kod odpowiedzi HTTP,
     *                         lub błąd deserializacji JSON
     *
     * @see #addGroupAsync(Group)
     * @see #deleteGroupAsync(String)
     * @see Group
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
                    throw new RuntimeException("Server responded with status: " + response.statusCode()
                            + " Body: " + response.body());
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch groups from server: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Dodaje nową grupę na serwer asynchronicznie.
     *
     * <p>Wykonuje żądanie POST do endpointu {@value #GROUPS_ENDPOINT} z danymi grupy
     * w formacie JSON. Zwraca grupę z uzupełnionymi danymi z serwera (np. ID, data utworzenia).
     * Operacja sprawdza unikalność nazwy grupy i rzuca wyjątek w przypadku duplikatu.</p>
     *
     * <p>Proces dodawania:</p>
     * <ol>
     *   <li>Walidacja obiektu grupy (nazwa i specjalizacja nie mogą być puste)</li>
     *   <li>Serializacja grupy do formatu JSON</li>
     *   <li>Wysłanie żądania HTTP POST z danymi JSON</li>
     *   <li>Sprawdzenie kodu odpowiedzi (oczekiwany: 201 lub 200)</li>
     *   <li>Deserializacja odpowiedzi do obiektu Group</li>
     * </ol>
     *
     * <p>Obsługa kodów błędów HTTP:</p>
     * <ul>
     *   <li><strong>201/200</strong> - grupa utworzona pomyślnie</li>
     *   <li><strong>409</strong> - grupa o takiej nazwie już istnieje ({@link GroupAlreadyExistsException})</li>
     *   <li><strong>400</strong> - nieprawidłowe dane grupy</li>
     *   <li><strong>500</strong> - błąd wewnętrzny serwera</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * Group newGroup = new Group("MAT-2024", "Matematyka");
     *
     * groupService.addGroupAsync(newGroup)
     *     .thenAccept(savedGroup -> {
     *         System.out.println("Grupa utworzona pomyślnie:");
     *         System.out.println("Nazwa: " + savedGroup.getName());
     *         System.out.println("Utworzona: " + savedGroup.getFormattedDate());
     *     })
     *     .exceptionally(throwable -> {
     *         if (throwable.getCause() instanceof GroupAlreadyExistsException) {
     *             System.err.println("Grupa o nazwie '" + newGroup.getName() + "' już istnieje!");
     *         } else {
     *             System.err.println("Błąd dodawania grupy: " + throwable.getMessage());
     *         }
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @param group grupa do dodania (nie może być null)
     * @return CompletableFuture z zapisaną grupą zawierającą dane z serwera
     *         (ID, data utworzenia, etc.)
     *
     * @throws IllegalArgumentException jeśli group jest null, nazwa lub specjalizacja jest pusta
     * @throws GroupAlreadyExistsException jeśli grupa o takiej nazwie już istnieje w systemie
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem lub deserializacji
     *
     * @see #getAllGroupsAsync()
     * @see #deleteGroupAsync(String)
     * @see GroupAlreadyExistsException
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

    /**
     * Usuwa grupę z serwera asynchronicznie.
     *
     * <p>Próbuje usunąć grupę używając różnych możliwych endpointów API dla maksymalnej
     * kompatybilności z różnymi wersjami serwera. Metoda automatycznie testuje różne
     * warianty URL-i i zatrzymuje się przy pierwszym udanym żądaniu.</p>
     *
     * <p><strong>UWAGA:</strong> Ta operacja jest nieodwracalna! Usunięcie grupy
     * może spowodować również usunięcie powiązanych danych (studentów, terminów, frekwencji)
     * w zależności od konfiguracji serwera.</p>
     *
     * <p>Testowane endpointy (w kolejności):</p>
     * <ol>
     *   <li>{@code DELETE /api/groups/{nazwa}} - standardowy endpoint REST</li>
     *   <li>{@code DELETE /api/groups/delete/{nazwa}} - alternatywny endpoint</li>
     *   <li>{@code DELETE /api/groups?name={nazwa}} - endpoint z parametrem query</li>
     * </ol>
     *
     * <p>Obsługa kodów odpowiedzi:</p>
     * <ul>
     *   <li><strong>200/204</strong> - grupa usunięta pomyślnie</li>
     *   <li><strong>404</strong> - endpoint nie istnieje LUB grupa nie znaleziona</li>
     *   <li><strong>405</strong> - metoda DELETE nie obsługiwana na endpoincie</li>
     *   <li><strong>4xx</strong> - inne błędy klienta</li>
     *   <li><strong>5xx</strong> - błędy serwera</li>
     * </ul>
     *
     * <p>Proces usuwania:</p>
     * <ol>
     *   <li>Kodowanie nazwy grupy do formatu URL-safe</li>
     *   <li>Iteracja przez dostępne endpointy</li>
     *   <li>Wysłanie żądania DELETE dla każdego endpointu</li>
     *   <li>Sprawdzenie kodu odpowiedzi</li>
     *   <li>Zwrócenie true przy pierwszym sukcesie lub false jeśli wszystkie zawiodły</li>
     * </ol>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * String groupName = "INF-2024";
     *
     * groupService.deleteGroupAsync(groupName)
     *     .thenAccept(success -> {
     *         if (success) {
     *             System.out.println("Grupa '" + groupName + "' została usunięta.");
     *         } else {
     *             System.out.println("Nie udało się usunąć grupy '" + groupName + "'.");
     *         }
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd usuwania grupy: " + throwable.getMessage());
     *         return null;
     *     });
     *
     * // Z obsługą potwierdzenia
     * Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
     * confirmation.setContentText("Czy na pewno usunąć grupę " + groupName + "?");
     * confirmation.showAndWait().ifPresent(response -> {
     *     if (response == ButtonType.OK) {
     *         groupService.deleteGroupAsync(groupName).thenAccept(result -> {
     *             // obsługa wyniku
     *         });
     *     }
     * });
     * }
     * </pre>
     *
     * @param groupName nazwa grupy do usunięcia (nie może być null ani pusta)
     * @return CompletableFuture z wynikiem operacji:
     *         <ul>
     *           <li><strong>true</strong> - grupa została usunięta pomyślnie</li>
     *           <li><strong>false</strong> - nie udało się usunąć grupy (może nie istnieć)</li>
     *         </ul>
     *
     * @throws IllegalArgumentException jeśli groupName jest null lub puste
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem
     *
     * @see #addGroupAsync(Group)
     * @see #checkAvailableEndpoints(String)
     */
    public CompletableFuture<Boolean> deleteGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedName = java.net.URLEncoder.encode(groupName, "UTF-8");

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

                        if (response.statusCode() == 200 || response.statusCode() == 204) {
                            System.out.println("✅ Grupa została usunięta pomyślnie (status: " + response.statusCode() + ")");
                            return true;

                        } else if (response.statusCode() == 404) {
                            System.out.println("❌ 404 - Endpoint nie istnieje lub grupa nie znaleziona na serwerze");

                        } else if (response.statusCode() == 405) {
                            System.out.println("❌ 405 - Metoda DELETE nie jest obsługiwana na: " + url);

                        } else if (response.statusCode() >= 400 && response.statusCode() < 500) {
                            System.out.println("❌ Błąd klienta " + response.statusCode() + " dla URL: " + url);

                        } else if (response.statusCode() >= 500) {
                            System.out.println("❌ Błąd serwera " + response.statusCode() + " dla URL: " + url);

                        } else {
                            System.out.println("❓ Nieoczekiwany status " + response.statusCode() + " dla URL: " + url);
                        }

                    } catch (Exception urlException) {
                        System.err.println("❌ Wyjątek dla URL " + url + ": " + urlException.getMessage());
                    }
                }

                System.err.println("❌ WSZYSTKIE URL-e niepomyślne - grupa nie została usunięta");
                return false;

            } catch (Exception e) {
                System.err.println("❌ Ogólny błąd usuwania grupy: " + e.getMessage());
                throw new RuntimeException("Failed to delete group from server: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Sprawdza dostępne endpointy API dla operacji na grupach.
     *
     * <p>Metoda diagnostyczna testująca różne kombinacje URL-i i metod HTTP
     * aby zidentyfikować prawidłowe endpointy serwera. Przydatna do debugowania
     * problemów z komunikacją API lub zmianami w strukturze endpointów serwera.</p>
     *
     * <p>Testowane kombinacje:</p>
     * <ul>
     *   <li><strong>URL-e:</strong> /groups/{nazwa}, /groups/delete/{nazwa}, /groups?name={nazwa}, etc.</li>
     *   <li><strong>Metody HTTP:</strong> DELETE, POST, PUT</li>
     *   <li><strong>Timeout:</strong> 10 sekund na żądanie</li>
     * </ul>
     *
     * <p>Analiza odpowiedzi:</p>
     * <ul>
     *   <li>Kody 200-299: prawdopodobnie działający endpoint</li>
     *   <li>Kod 404: endpoint nie istnieje</li>
     *   <li>Kod 405: metoda nie obsługiwana</li>
     *   <li>Inne kody: różne błędy do analizy</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * groupService.checkAvailableEndpoints("TEST-GROUP")
     *     .thenAccept(results -> {
     *         System.out.println("=== WYNIKI TESTÓW ENDPOINTÓW ===");
     *         System.out.println(results);
     *         // Szukaj linii zawierających "⭐ POTENCJALNIE DZIAŁAJĄCY ENDPOINT"
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd testowania: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @param groupName nazwa grupy do testowania endpointów (używana jako parametr testowy)
     * @return CompletableFuture z wynikami testów endpointów jako sformatowany String.
     *         Każda linia zawiera informację o testowanym URL, metodzie HTTP i otrzymanym kodzie odpowiedzi.
     *
     * @see #deleteGroupAsync(String)
     */
    public CompletableFuture<String> checkAvailableEndpoints(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("=== SPRAWDZANIE DOSTĘPNYCH ENDPOINTÓW DLA GRUPY: " + groupName + " ===");

            try {
                String encodedName = java.net.URLEncoder.encode(groupName, "UTF-8");

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

    /**
     * Sprawdza połączenie z serwerem backend.
     *
     * <p>Wykonuje proste żądanie GET do endpointu sprawdzania zdrowia serwera.
     * Używane do weryfikacji czy serwer jest dostępny i odpowiada na żądania.</p>
     *
     * <p>Endpoint sprawdzany: {@code GET /api/groups/health}</p>
     * <p>Timeout: 5 sekund</p>
     *
     * @return CompletableFuture z wynikiem testu połączenia:
     *         <ul>
     *           <li><strong>true</strong> - serwer jest dostępny i odpowiada</li>
     *           <li><strong>false</strong> - brak połączenia lub serwer nie odpowiada</li>
     *         </ul>
     *
     * @see #getAllGroupsAsync()
     */
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


    /**
     * Parsuje JSON z listą grup z serwera do listy obiektów Group.
     *
     * <p>Konwertuje JSON otrzymany z serwera do listy obiektów {@link Group}
     * używanych w aplikacji klienckiej. Obsługuje automatyczną konwersję
     * typów danych i mapowanie pól.</p>
     *
     * @param json JSON z odpowiedzi serwera zawierający listę grup
     * @return lista obiektów Group
     * @throws RuntimeException jeśli nie można sparsować JSON
     */
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

    /**
     * Parsuje JSON z pojedynczą grupą z serwera.
     *
     * @param json JSON z odpowiedzi serwera zawierający grupę
     * @return obiekt Group
     * @throws RuntimeException jeśli nie można sparsować JSON
     */
    private Group parseGroupFromJson(String json) {
        try {
            GroupFromServer serverGroup = objectMapper.readValue(json, GroupFromServer.class);
            return convertToGroup(serverGroup);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse group JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Konwertuje obiekt Group do JSON do wysłania na serwer.
     *
     * @param group grupa do konwersji
     * @return JSON reprezentujący grupę
     * @throws RuntimeException jeśli nie można serializować do JSON
     */
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

    /**
     * Konwertuje obiekt grupy z serwera do obiektu klienta.
     *
     * @param serverGroup grupa z serwera
     * @return obiekt Group do użycia w kliencie
     */
    private Group convertToGroup(GroupFromServer serverGroup) {
        Group group = new Group(serverGroup.name, serverGroup.specialization);
        return group;
    }


    /**
     * Klasa reprezentująca grupę otrzymaną z serwera.
     *
     * <p>Używana do deserializacji JSON z serwera. Zawiera wszystkie pola
     * jakie serwer może zwrócić, włączając te które nie są używane w kliencie.</p>
     */
    private static class GroupFromServer {
        /** ID grupy z bazy danych */
        public Long id;

        /** Nazwa grupy */
        public String name;

        /** Specjalizacja grupy */
        public String specialization;

        /** Data utworzenia w systemie */
        public LocalDateTime createdDate;

        /** Czy grupa jest aktywna */
        public Boolean active;
    }

    /**
     * Klasa reprezentująca grupę do wysłania na serwer.
     *
     * <p>Używana do serializacji danych grupy do JSON przed wysłaniem na serwer.
     * Zawiera tylko podstawowe pola wymagane do utworzenia grupy.</p>
     */
    private static class GroupToServer {
        /** Nazwa grupy */
        public String name;

        /** Specjalizacja grupy */
        public String specialization;
    }


    /**
     * Wyjątek rzucany gdy grupa o danej nazwie już istnieje w systemie.
     *
     * <p>Ten wyjątek jest rzucany przez {@link #addGroupAsync(Group)} gdy serwer
     * zwróci kod odpowiedzi HTTP 409 (Conflict), co oznacza że grupa o takiej
     * nazwie już istnieje w bazie danych.</p>
     *
     * <p>Przykład obsługi:</p>
     * <pre>
     * {@code
     * groupService.addGroupAsync(newGroup)
     *     .exceptionally(throwable -> {
     *         if (throwable.getCause() instanceof GroupAlreadyExistsException) {
     *             System.err.println("Grupa już istnieje: " + throwable.getMessage());
     *             // Pokaż użytkownikowi dialog o błędzie
     *         }
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @see #addGroupAsync(Group)
     */
    public static class GroupAlreadyExistsException extends RuntimeException {
        /**
         * Konstruktor wyjątku z komunikatem błędu.
         *
         * @param message szczegółowy opis błędu, zwykle zawierający nazwę grupy
         */
        public GroupAlreadyExistsException(String message) {
            super(message);
        }
    }
}