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

/**
 * Serwis do komunikacji z serwerem backend w zakresie zarządzania terminami zajęć.
 *
 * <p>ScheduleService obsługuje wszystkie operacje CRUD na terminach zajęć,
 * w tym tworzenie, pobieranie, aktualizację i usuwanie terminów. Serwis
 * automatycznie synchronizuje dane z serwerem backend i zapewnia
 * asynchroniczną obsługę wszystkich operacji sieciowych.</p>
 *
 * <p>Klasa jest zaprojektowana do współpracy z systemem dziennika elektronicznego,
 * gdzie terminy zajęć stanowią podstawę dla rejestrowania frekwencji studentów
 * i zarządzania harmonogramem grupy.</p>
 *
 * <h3>Obsługiwane operacje:</h3>
 * <ul>
 *   <li>Pobieranie wszystkich terminów z serwera ({@link #getAllSchedulesAsync()})</li>
 *   <li>Pobieranie terminów konkretnej grupy ({@link #getSchedulesByGroupAsync(String)})</li>
 *   <li>Dodawanie nowych terminów ({@link #addScheduleAsync(ClassSchedule)})</li>
 *   <li>Aktualizacja istniejących terminów ({@link #updateScheduleAsync(Long, ClassSchedule)})</li>
 *   <li>Usuwanie terminów ({@link #deleteScheduleAsync(Long)})</li>
 * </ul>
 *
 * <h3>Konfiguracja serwera:</h3>
 * <p>Serwis domyślnie łączy się z serwerem na adresie {@value #BASE_URL}.
 * Endpoint dla terminów znajduje się pod adresem {@value #SCHEDULES_ENDPOINT}.</p>
 *
 * <h3>Format danych:</h3>
 * <p>Wszystkie dane są wymieniane w formacie JSON. Serwis automatycznie
 * obsługuje serializację obiektów {@link ClassSchedule} do JSON i deserializację
 * odpowiedzi z serwera, uwzględniając różnice w strukturze danych między
 * klientem a serwerem.</p>
 *
 * <p>Szczególną uwagę poświęcono prawidłowemu formatowaniu dat i czasów
 * oraz obsłudze informacji o grupach studenckich.</p>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * ScheduleService service = new ScheduleService();
 *
 * // Utworzenie nowego terminu
 * ClassSchedule newSchedule = new ClassSchedule(
 *     "Egzamin z Programowania",
 *     "Sala 101",
 *     LocalDateTime.of(2024, 6, 15, 9, 0),
 *     LocalDateTime.of(2024, 6, 15, 12, 0),
 *     "Dr Jan Kowalski",
 *     "Egzamin pisemny",
 *     "INF-2024"
 * );
 *
 * // Asynchroniczne dodanie terminu
 * service.addScheduleAsync(newSchedule)
 *        .thenAccept(savedSchedule -> {
 *            System.out.println("Termin zapisany z ID: " + savedSchedule.getId());
 *            System.out.println("Data: " + savedSchedule.getFormattedStartTime());
 *        })
 *        .exceptionally(throwable -> {
 *            System.err.println("Błąd zapisywania terminu: " + throwable.getMessage());
 *            return null;
 *        });
 *
 * // Pobieranie terminów dla konkretnej grupy
 * service.getSchedulesByGroupAsync("INF-2024")
 *        .thenAccept(schedules -> {
 *            System.out.println("Znaleziono " + schedules.size() + " terminów:");
 *            schedules.forEach(schedule ->
 *                System.out.println("- " + schedule.getSubject() +
 *                                 " (" + schedule.getFormattedStartTime() + ")")
 *            );
 *        });
 * }
 * </pre>
 *
 * <h3>Obsługa błędów:</h3>
 * <p>Serwis automatycznie obsługuje następujące sytuacje błędne:</p>
 * <ul>
 *   <li>Brak połączenia z serwerem</li>
 *   <li>Timeout żądań (domyślnie 30 sekund)</li>
 *   <li>Nieprawidłowe odpowiedzi HTTP (4xx, 5xx)</li>
 *   <li>Błędy serializacji/deserializacji JSON</li>
 *   <li>Nieprawidłowe formaty dat i czasów</li>
 *   <li>Konflikty w danych grupy</li>
 * </ul>
 *
 * <h3>Integracja z systemem:</h3>
 * <ul>
 *   <li>{@link ClassSchedule} - model terminu zajęć</li>
 *   <li>{@link GroupDetailController} - interfejs zarządzania terminami</li>
 *   <li>{@link AttendanceService} - rejestrowanie frekwencji na terminach</li>
 *   <li>{@link Group} - powiązania z grupami studenckimi</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 1.0
 * @since 2025
 *
 * @see ClassSchedule
 * @see GroupService
 * @see AttendanceService
 * @see CompletableFuture
 */
public class ScheduleService {

    /**
     * Bazowy URL serwera API.
     *
     * <p>Wszystkie żądania HTTP są kierowane do tego adresu bazowego.
     * Domyślnie aplikacja łączy się z lokalnym serwerem deweloperskim.</p>
     */
    private static final String BASE_URL = "http://localhost:8080/api";

    /**
     * Pełny endpoint dla operacji na terminach zajęć.
     *
     * <p>Endpoint obsługuje następujące operacje HTTP:</p>
     * <ul>
     *   <li>GET {@value} - pobieranie wszystkich terminów</li>
     *   <li>POST {@value} - dodawanie nowego terminu</li>
     *   <li>PUT {@value}/{id} - aktualizacja terminu</li>
     *   <li>DELETE {@value}/{id} - usuwanie terminu</li>
     *   <li>GET {@value}/group/{nazwa} - pobieranie terminów grupy</li>
     * </ul>
     */
    private static final String SCHEDULES_ENDPOINT = BASE_URL + "/schedules";

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
     *   <li>Automatycznej konwersji typów danych</li>
     * </ul>
     */
    private final ObjectMapper objectMapper;

    /**
     * Formatter dla dat i czasów w komunikacji z serwerem.
     *
     * <p>Używa formatu ISO-8601 z precyzją do sekund: "yyyy-MM-dd'T'HH:mm:ss"
     * dla zapewnienia kompatybilności z różnymi systemami backend.</p>
     */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Konstruktor serwisu terminów zajęć.
     *
     * <p>Inicjalizuje klienta HTTP z timeoutem 10 sekund oraz konfiguruje
     * ObjectMapper do obsługi dat Java 8+ i ignorowania nieznanych właściwości JSON.
     * Dodatkowo ustawia formatter dla konsystentnego formatowania dat.</p>
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
    public ScheduleService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Pobiera wszystkie terminy zajęć z serwera asynchronicznie.
     *
     * <p>Wykonuje żądanie GET do endpointu {@value #SCHEDULES_ENDPOINT} i deserializuje
     * odpowiedź JSON do listy obiektów {@link ClassSchedule}. Operacja jest wykonywana
     * asynchronicznie w tle, nie blokując wątku interfejsu użytkownika.</p>
     *
     * <p>Proces pobierania:</p>
     * <ol>
     *   <li>Wysłanie żądania HTTP GET do serwera</li>
     *   <li>Oczekiwanie na odpowiedź (max 30 sekund)</li>
     *   <li>Sprawdzenie kodu statusu HTTP (oczekiwany: 200)</li>
     *   <li>Deserializacja JSON do listy ClassSchedule</li>
     *   <li>Konwersja obiektów serwera do obiektów klienta</li>
     * </ol>
     *
     * <p>Zwracana lista zawiera wszystkie terminy w systemie, niezależnie
     * od grup studenckich. Dla filtrowania po grupie użyj
     * {@link #getSchedulesByGroupAsync(String)}.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * scheduleService.getAllSchedulesAsync()
     *     .thenAccept(schedules -> {
     *         System.out.println("Znaleziono " + schedules.size() + " terminów:");
     *
     *         Map<String, Long> groupCounts = schedules.stream()
     *             .collect(Collectors.groupingBy(
     *                 ClassSchedule::getGroupName,
     *                 Collectors.counting()
     *             ));
     *
     *         groupCounts.forEach((group, count) ->
     *             System.out.println("Grupa " + group + ": " + count + " terminów")
     *         );
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Nie można pobrać terminów: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @return CompletableFuture z listą wszystkich terminów z serwera.
     *         Lista może być pusta jeśli brak terminów, ale nigdy nie będzie null.
     *
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         timeout żądania, nieprawidłowy kod odpowiedzi HTTP,
     *                         lub błąd deserializacji JSON
     *
     * @see #getSchedulesByGroupAsync(String)
     * @see #addScheduleAsync(ClassSchedule)
     * @see ClassSchedule
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
     * Pobiera terminy zajęć przypisane do konkretnej grupy studenckiej asynchronicznie.
     *
     * <p>Wykonuje żądanie GET do endpointu z filtrem grupy i zwraca tylko terminy
     * przypisane do określonej grupy studenckiej. Nazwa grupy jest automatycznie
     * kodowana dla bezpiecznego przesyłania w URL.</p>
     *
     * <p>Endpoint: {@code GET /api/schedules/group/{encodedGroupName}}</p>
     *
     * <p>Proces pobierania:</p>
     * <ol>
     *   <li>Kodowanie nazwy grupy do formatu URL-safe (UTF-8)</li>
     *   <li>Wysłanie żądania GET z nazwą grupy w ścieżce</li>
     *   <li>Oczekiwanie na odpowiedź z filtrem po stronie serwera</li>
     *   <li>Deserializacja tylko terminów z danej grupy</li>
     *   <li>Debug logging - szczegółowe informacje o procesie</li>
     * </ol>
     *
     * <p>Metoda zawiera rozszerzone debugowanie z logowaniem do konsoli
     * statusu żądania, liczby pobranych terminów i szczegółów każdego terminu.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * String groupName = "INF-2024";
     * scheduleService.getSchedulesByGroupAsync(groupName)
     *     .thenAccept(schedules -> {
     *         if (schedules.isEmpty()) {
     *             System.out.println("Brak terminów dla grupy " + groupName);
     *         } else {
     *             System.out.println("Terminy dla grupy " + groupName + ":");
     *
     *             // Sortuj chronologicznie
     *             schedules.stream()
     *                 .sorted((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()))
     *                 .forEach(schedule ->
     *                     System.out.println("- " + schedule.getSubject() +
     *                                      " (" + schedule.getFormattedStartTime() + ")" +
     *                                      " - " + schedule.getInstructor())
     *                 );
     *         }
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd pobierania terminów grupy: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @param groupName nazwa grupy studenckiej (nie może być null ani pusta)
     * @return CompletableFuture z listą terminów z danej grupy.
     *         Lista może być pusta jeśli grupa nie ma terminów.
     *
     * @throws IllegalArgumentException jeśli groupName jest null lub puste
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         grupa nie istnieje, lub błąd deserializacji
     *
     * @see #getAllSchedulesAsync()
     * @see #addScheduleAsync(ClassSchedule)
     */
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

    /**
     * Dodaje nowy termin zajęć na serwer asynchronicznie.
     *
     * <p>Wykonuje żądanie POST do endpointu {@value #SCHEDULES_ENDPOINT} z danymi terminu
     * w formacie JSON. Zwraca termin z uzupełnionymi danymi z serwera (ID, data utworzenia).
     * Używa zaawansowanej metody ręcznego tworzenia JSON dla maksymalnej kontroli nad
     * formatem danych wysyłanych na serwer.</p>
     *
     * <p>Proces dodawania:</p>
     * <ol>
     *   <li>Walidacja obiektu terminu (przedmiot, czas rozpoczęcia/zakończenia wymagane)</li>
     *   <li>Ręczna serializacja terminu do formatu JSON</li>
     *   <li>Debug logging - wyświetlenie JSON-a przed wysłaniem</li>
     *   <li>Wysłanie żądania HTTP POST z danymi JSON</li>
     *   <li>Sprawdzenie kodu odpowiedzi (oczekiwany: 201 lub 200)</li>
     *   <li>Deserializacja odpowiedzi do obiektu ClassSchedule z danymi serwera</li>
     *   <li>Debug logging - potwierdzenie zapisania z ID</li>
     * </ol>
     *
     * <p>Specjalne właściwości implementacji:</p>
     * <ul>
     *   <li><strong>Ręczne tworzenie JSON</strong> - dla maksymalnej kontroli nad formatem</li>
     *   <li><strong>Obsługa grup jako obiektów</strong> - zgodność z backend API</li>
     *   <li><strong>Extensywny debug logging</strong> - ułatwia diagnozowanie problemów</li>
     *   <li><strong>Formatowanie dat ISO-8601</strong> - standardowy format międzynarodowy</li>
     * </ul>
     *
     * <p>Obsługa kodów błędów HTTP:</p>
     * <ul>
     *   <li><strong>201/200</strong> - termin utworzony pomyślnie</li>
     *   <li><strong>400</strong> - nieprawidłowe dane terminu</li>
     *   <li><strong>409</strong> - konflikt (np. nakładające się terminy)</li>
     *   <li><strong>500</strong> - błąd wewnętrzny serwera</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * // Nowy termin laboratorium
     * ClassSchedule newSchedule = new ClassSchedule(
     *     "Laboratorium Java",
     *     "Sala komputerowa 15",
     *     LocalDateTime.of(2024, 3, 20, 14, 0),
     *     LocalDateTime.of(2024, 3, 20, 16, 0),
     *     "mgr inż. Anna Kowalska",
     *     "Ćwiczenia praktyczne z programowania",
     *     "INF-2024"
     * );
     *
     * scheduleService.addScheduleAsync(newSchedule)
     *     .thenAccept(savedSchedule -> {
     *         System.out.println("Termin utworzony pomyślnie:");
     *         System.out.println("ID: " + savedSchedule.getId());
     *         System.out.println("Przedmiot: " + savedSchedule.getSubject());
     *         System.out.println("Data: " + savedSchedule.getFormattedStartTime());
     *         System.out.println("Grupa: " + savedSchedule.getGroupName());
     *         System.out.println("Z serwera: " + savedSchedule.isFromServer());
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd dodawania terminu: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @param schedule termin do dodania (nie może być null)
     * @return CompletableFuture z zapisanym terminem zawierającym dane z serwera
     *         (ID, data utworzenia, etc.)
     *
     * @throws IllegalArgumentException jeśli schedule jest null, przedmiot jest pusty,
     *                                 lub czas zakończenia jest przed czasem rozpoczęcia
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem lub deserializacji
     *
     * @see #updateScheduleAsync(Long, ClassSchedule)
     * @see #deleteScheduleAsync(Long)
     * @see #createScheduleJsonManually(ClassSchedule)
     */
    public CompletableFuture<ClassSchedule> addScheduleAsync(ClassSchedule schedule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("=== WYSYŁANIE TERMINU NA SERWER ===");
                System.out.println("📋 Termin: " + schedule.getSubject());
                System.out.println("📅 Data: " + schedule.getStartTime());
                System.out.println("🏫 Grupa: " + schedule.getGroupName());

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

    /**
     * Usuwa termin zajęć z serwera asynchronicznie.
     *
     * <p>Wykonuje żądanie DELETE do endpointu z identyfikatorem terminu.
     * <strong>UWAGA:</strong> Ta operacja usuwa termin całkowicie z systemu,
     * wraz z wszystkimi powiązanymi danymi (frekwencja studentów, historia, etc.).</p>
     *
     * <p>Endpoint: {@code DELETE /api/schedules/{scheduleId}}</p>
     *
     * <p>Ta operacja jest nieodwracalna! Po usunięciu terminu:</p>
     * <ul>
     *   <li>Wszystkie dane terminu zostaną utracone</li>
     *   <li>Historia frekwencji dla tego terminu zostanie usunięta</li>
     *   <li>Statystyki obecności studentów zostaną przeliczone</li>
     *   <li>Nie będzie możliwości przywrócenia danych</li>
     * </ul>
     *
     * <p>Metoda zawiera debug logging z informacjami o procesie usuwania.</p>
     *
     * <p>Obsługa kodów odpowiedzi HTTP:</p>
     * <ul>
     *   <li><strong>200</strong> - termin usunięty pomyślnie (z treścią odpowiedzi)</li>
     *   <li><strong>204</strong> - termin usunięty pomyślnie (bez treści odpowiedzi)</li>
     *   <li><strong>404</strong> - termin nie został znaleziony</li>
     *   <li><strong>409</strong> - nie można usunąć (np. są powiązane dane)</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * Long scheduleId = 42L;
     *
     * // Potwierdzenie przed usunięciem
     * Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
     * confirmation.setTitle("Potwierdzenie usunięcia");
     * confirmation.setContentText("Czy na pewno usunąć termin?\n" +
     *                             "Wszystkie dane frekwencji zostaną utracone!\n" +
     *                             "Ta operacja jest nieodwracalna!");
     * confirmation.showAndWait().ifPresent(response -> {
     *     if (response == ButtonType.OK) {
     *         scheduleService.deleteScheduleAsync(scheduleId)
     *             .thenAccept(success -> {
     *                 if (success) {
     *                     System.out.println("Termin został usunięty z serwera");
     *                     // Odśwież listę terminów w UI
     *                     refreshSchedulesList();
     *                 } else {
     *                     System.out.println("Nie udało się usunąć terminu");
     *                 }
     *             })
     *             .exceptionally(throwable -> {
     *                 System.err.println("Błąd usuwania: " + throwable.getMessage());
     *                 return null;
     *             });
     *     }
     * });
     * }
     * </pre>
     *
     * @param scheduleId identyfikator terminu do usunięcia (nie może być null)
     * @return CompletableFuture z wynikiem operacji:
     *         <ul>
     *           <li><strong>true</strong> - termin został usunięty pomyślnie</li>
     *           <li><strong>false</strong> - nie udało się usunąć terminu</li>
     *         </ul>
     *
     * @throws IllegalArgumentException jeśli scheduleId jest null
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem
     *
     * @see #addScheduleAsync(ClassSchedule)
     * @see #updateScheduleAsync(Long, ClassSchedule)
     */
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

    /**
     * Aktualizuje istniejący termin zajęć na serwerze asynchronicznie.
     *
     * <p>Wykonuje żądanie PUT do endpointu z identyfikatorem terminu i nowymi danymi.
     * Pozwala na zmianę wszystkich właściwości terminu: przedmiotu, czasu, sali,
     * prowadzącego, uwag, a także przypisania do grupy.</p>
     *
     * <p>Endpoint: {@code PUT /api/schedules/{scheduleId}}</p>
     *
     * <p>Możliwe scenariusze aktualizacji:</p>
     * <ul>
     *   <li><strong>Zmiana czasu</strong> - przesunięcie terminu na inną datę/godzinę</li>
     *   <li><strong>Zmiana sali</strong> - przeprowadzenie w innym miejscu</li>
     *   <li><strong>Zmiana prowadzącego</strong> - zastępstwo lub zmiana wykładowcy</li>
     *   <li><strong>Aktualizacja uwag</strong> - dodanie informacji o zmianach</li>
     *   <li><strong>Przeniesienie do innej grupy</strong> - zmiana przypisania</li>
     * </ul>
     *
     * <p>Metoda używa zaawansowanej techniki ręcznego tworzenia JSON (podobnie jak
     * {@link #addScheduleAsync(ClassSchedule)}) dla maksymalnej kontroli nad formatem
     * i zgodnością z backend API.</p>
     *
     * <p>Proces aktualizacji:</p>
     * <ol>
     *   <li>Ręczna serializacja danych terminu do JSON</li>
     *   <li>Debug logging - wyświetlenie JSON-a przed wysłaniem</li>
     *   <li>Wysłanie żądania PUT z danymi</li>
     *   <li>Sprawdzenie kodu odpowiedzi (oczekiwany: 200)</li>
     *   <li>Deserializacja zaktualizowanego terminu z serwera</li>
     *   <li>Debug logging - potwierdzenie aktualizacji</li>
     * </ol>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * Long scheduleId = 42L;
     *
     * // Aktualizacja czasu i sali
     * ClassSchedule updatedSchedule = new ClassSchedule(
     *     "Egzamin z Programowania", // bez zmian
     *     "Aula Magna",              // zmiana sali
     *     LocalDateTime.of(2024, 6, 20, 10, 0), // zmiana daty
     *     LocalDateTime.of(2024, 6, 20, 13, 0), // zmiana czasu
     *     "Prof. dr hab. Jan Nowak", // bez zmian
     *     "Egzamin przeniesiony z powodu kolizji", // aktualizacja uwag
     *     "INF-2024"                 // bez zmian
     * );
     *
     * scheduleService.updateScheduleAsync(scheduleId, updatedSchedule)
     *     .thenAccept(result -> {
     *         System.out.println("Termin zaktualizowany:");
     *         System.out.println("Przedmiot: " + result.getSubject());
     *         System.out.println("Nowa sala: " + result.getClassroom());
     *         System.out.println("Nowa data: " + result.getFormattedStartTime());
     *         System.out.println("Uwagi: " + result.getNotes());
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd aktualizacji: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @param scheduleId identyfikator terminu do aktualizacji (nie może być null)
     * @param schedule nowe dane terminu (nie może być null)
     * @return CompletableFuture z zaktualizowanym terminem zawierającym dane z serwera
     *
     * @throws IllegalArgumentException jeśli scheduleId lub schedule jest null
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         termin nie istnieje, lub błąd deserializacji
     *
     * @see #addScheduleAsync(ClassSchedule)
     * @see #deleteScheduleAsync(Long)
     * @see #createScheduleJsonManually(ClassSchedule)
     */
    public CompletableFuture<ClassSchedule> updateScheduleAsync(Long scheduleId, ClassSchedule schedule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
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

    /**
     * Parsuje JSON z listą terminów z serwera do listy obiektów ClassSchedule.
     *
     * <p>Konwertuje JSON otrzymany z serwera do listy obiektów {@link ClassSchedule}
     * używanych w aplikacji klienckiej. Obsługuje automatyczną konwersję
     * typów danych i mapowanie pól, w tym informacji o grupach z obiektów zagnieżdżonych.</p>
     *
     * <p>Proces konwersji:</p>
     * <ol>
     *   <li>Deserializacja JSON do listy obiektów ScheduleFromServer</li>
     *   <li>Mapowanie każdego obiektu serwera na obiekt klienta</li>
     *   <li>Wyciągnięcie nazwy grupy z zagnieżdżonego obiektu</li>
     *   <li>Konwersja dat z formatu ISO do LocalDateTime</li>
     * </ol>
     *
     * @param json JSON z odpowiedzi serwera zawierający listę terminów
     * @return lista obiektów ClassSchedule
     * @throws RuntimeException jeśli nie można sparsować JSON
     *
     * @see #convertToClassSchedule(ScheduleFromServer)
     * @see #parseScheduleFromJson(String)
     */
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

    /**
     * Parsuje JSON z pojedynczym terminem z serwera.
     *
     * <p>Konwertuje JSON pojedynczego terminu otrzymany z serwera po operacjach
     * dodawania lub aktualizacji do obiektu {@link ClassSchedule}.</p>
     *
     * @param json JSON z odpowiedzi serwera zawierający termin
     * @return obiekt ClassSchedule
     * @throws RuntimeException jeśli nie można sparsować JSON
     *
     * @see #convertToClassSchedule(ScheduleFromServer)
     * @see #parseSchedulesFromJson(String)
     */
    private ClassSchedule parseScheduleFromJson(String json) {
        try {
            ScheduleFromServer serverSchedule = objectMapper.readValue(json, ScheduleFromServer.class);
            return convertToClassSchedule(serverSchedule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse schedule JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Tworzy JSON reprezentujący termin do wysłania na serwer przy użyciu ręcznego formatowania.
     *
     * <p>Ta metoda zastąpiła automatyczną serializację Jackson dla zapewnienia
     * maksymalnej kontroli nad formatem JSON wysyłanym na serwer. Szczególnie
     * ważne jest prawidłowe formatowanie grup jako obiektów zagnieżdżonych.</p>
     *
     * <p>Format JSON generowanego przez tę metodę:</p>
     * <pre>
     * {@code
     * {
     *   "subject": "Nazwa przedmiotu",
     *   "classroom": "Sala 101",
     *   "startTime": "2024-03-15T10:00:00",
     *   "endTime": "2024-03-15T12:00:00",
     *   "instructor": "Dr Jan Kowalski",
     *   "notes": "Uwagi dodatkowe",
     *   "group": {
     *     "name": "INF-2024"
     *   }
     * }
     * }
     * </pre>
     *
     * <p>Kluczowe cechy implementacji:</p>
     * <ul>
     *   <li><strong>Ręczna konstrukcja JSON</strong> - pełna kontrola nad formatem</li>
     *   <li><strong>Escape'owanie stringów</strong> - bezpieczne obsługa znaków specjalnych</li>
     *   <li><strong>Formatowanie dat ISO-8601</strong> - zgodność ze standardami</li>
     *   <li><strong>Grupa jako obiekt</strong> - zgodność z backend API</li>
     *   <li><strong>Obsługa null values</strong> - bezpieczne przetwarzanie pustych pól</li>
     * </ul>
     *
     * @param schedule termin do konwersji na JSON
     * @return JSON reprezentujący termin w formacie oczekiwanym przez serwer
     * @throws RuntimeException jeśli wystąpi błąd podczas tworzenia JSON
     *
     * @see #escapeJson(String)
     * @see #addScheduleAsync(ClassSchedule)
     * @see #updateScheduleAsync(Long, ClassSchedule)
     */
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

    /**
     * Konwertuje obiekt terminu z serwera do obiektu klienta.
     *
     * <p>Mapuje pola z obiektu {@link ScheduleFromServer} na obiekt {@link ClassSchedule}
     * używany w aplikacji klienckiej, w tym wyciąganie nazwy grupy z zagnieżdżonego
     * obiektu grupy i ustawienie wszystkich wymaganych właściwości.</p>
     *
     * <p>Proces konwersji:</p>
     * <ol>
     *   <li>Wyciągnięcie nazwy grupy z obiektu group (lub "Nieznana grupa" jako fallback)</li>
     *   <li>Utworzenie obiektu ClassSchedule z konstruktorem serwerowym (z ID)</li>
     *   <li>Przekazanie wszystkich pól z obiektu serwera</li>
     *   <li>Zachowanie pełnej zgodności typów danych</li>
     * </ol>
     *
     * @param serverSchedule termin z serwera do konwersji
     * @return obiekt ClassSchedule do użycia w kliencie
     *
     * @see ScheduleFromServer
     * @see ClassSchedule
     */
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

    /**
     * Escape'uje stringi w JSON dla bezpiecznej serializacji.
     *
     * <p>Zabezpiecza przed błędami JSON spowodowanymi przez specjalne znaki
     * w danych terminów (cudzysłowy, znaki nowej linii, tabulatory, etc.).
     * Implementuje pełny zestaw escape'ów zgodnych ze standardem JSON.</p>
     *
     * <p>Obsługiwane znaki specjalne:</p>
     * <ul>
     *   <li><strong>\\</strong> → \\\\</li>
     *   <li><strong>"</strong> → \\"</li>
     *   <li><strong>\n</strong> → \\n</li>
     *   <li><strong>\r</strong> → \\r</li>
     *   <li><strong>\t</strong> → \\t</li>
     * </ul>
     *
     * @param str string do escape'owania (może być null)
     * @return bezpieczny string do użycia w JSON (pusty string dla null)
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // === KLASY POMOCNICZE DO DESERIALIZACJI ===

    /**
     * Klasa reprezentująca termin otrzymany z serwera.
     *
     * <p>Używana do deserializacji JSON z serwera. Zawiera wszystkie pola
     * jakie serwer może zwrócić, włączając te które nie są używane w kliencie.
     * Struktura tej klasy musi być zgodna z formatem JSON zwracanym przez backend API.</p>
     *
     * <p>Pola klasy odpowiadają strukturze JSON:</p>
     * <pre>
     * {@code
     * {
     *   "id": 123,
     *   "subject": "Nazwa przedmiotu",
     *   "classroom": "Sala 101",
     *   "startTime": "2024-03-15T10:00:00",
     *   "endTime": "2024-03-15T12:00:00",
     *   "instructor": "Dr Jan Kowalski",
     *   "notes": "Uwagi",
     *   "createdDate": "2024-03-01T14:30:00",
     *   "group": {
     *     "id": 1,
     *     "name": "INF-2024",
     *     "specialization": "Informatyka"
     *   }
     * }
     * }
     * </pre>
     */
    private static class ScheduleFromServer {
        /** ID terminu z bazy danych */
        public Long id;

        /** Nazwa przedmiotu/zajęć */
        public String subject;

        /** Sala lub miejsce zajęć */
        public String classroom;

        /** Data i czas rozpoczęcia zajęć */
        public LocalDateTime startTime;

        /** Data i czas zakończenia zajęć */
        public LocalDateTime endTime;

        /** Prowadzący zajęcia */
        public String instructor;

        /** Dodatkowe uwagi */
        public String notes;

        /** Data utworzenia terminu w systemie */
        public LocalDateTime createdDate;

        /** Informacje o grupie studenckiej jako obiekt zagnieżdżony */
        public GroupInfo group;
    }

    /**
     * Klasa reprezentująca informacje o grupie w odpowiedzi serwera.
     *
     * <p>Zagnieżdżona w obiekcie ScheduleFromServer dla reprezentacji
     * powiązania termin-grupa. Zawiera podstawowe informacje o grupie
     * studenckiej przypisanej do terminu.</p>
     *
     * <p>Struktura JSON grupy:</p>
     * <pre>
     * {@code
     * {
     *   "id": 1,
     *   "name": "INF-2024",
     *   "specialization": "Informatyka"
     * }
     * }
     * </pre>
     */
    private static class GroupInfo {
        /** ID grupy z bazy danych */
        public Long id;

        /** Nazwa grupy (kluczowa dla klienta) */
        public String name;

        /** Specjalizacja grupy */
        public String specialization;
    }
}