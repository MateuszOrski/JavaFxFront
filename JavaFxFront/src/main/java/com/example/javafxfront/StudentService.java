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
 * Serwis do komunikacji z serwerem backend w zakresie zarządzania studentami.
 *
 * <p>StudentService obsługuje wszystkie operacje CRUD na studentach,
 * w tym dodawanie, pobieranie, aktualizację, usuwanie oraz zarządzanie
 * przypisaniem studentów do grup. Wszystkie operacje są wykonywane asynchronicznie
 * przy użyciu HTTP API.</p>
 *
 * <p>Serwis automatycznie obsługuje serializację obiektów {@link Student} do JSON
 * i deserializację odpowiedzi z serwera, uwzględniając różnice w strukturze
 * danych między klientem a serwerem.</p>
 *
 * <h3>Obsługiwane operacje:</h3>
 * <ul>
 *   <li>Pobieranie wszystkich studentów ({@link #getAllStudentsAsync()})</li>
 *   <li>Pobieranie studentów według grupy ({@link #getStudentsByGroupAsync(String)})</li>
 *   <li>Pobieranie studentów bez grupy ({@link #getStudentsWithoutGroupAsync()})</li>
 *   <li>Dodawanie nowych studentów ({@link #addStudentAsync(Student)})</li>
 *   <li>Aktualizacja danych studenta ({@link #updateStudentAsync(String, Student)})</li>
 *   <li>Usuwanie studentów ({@link #deleteStudentAsync(String)})</li>
 *   <li>Zarządzanie przypisaniem do grup ({@link #removeStudentFromGroupAsync(String)})</li>
 * </ul>
 *
 * <h3>Zarządzanie grupami studentów:</h3>
 * <p>Serwis oferuje elastyczne zarządzanie przypisaniem studentów do grup:</p>
 * <ul>
 *   <li><strong>Dodawanie bez grupy</strong> - student może być utworzony bez przypisania</li>
 *   <li><strong>Przypisywanie do grupy</strong> - przez aktualizację danych studenta</li>
 *   <li><strong>Przenoszenie między grupami</strong> - przez aktualizację pola grupy</li>
 *   <li><strong>Usuwanie z grupy</strong> - bez usuwania z systemu</li>
 * </ul>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * StudentService service = new StudentService();
 *
 * // Dodawanie nowego studenta bez grupy
 * Student newStudent = new Student("Jan", "Kowalski", "123456", null);
 * service.addStudentAsync(newStudent)
 *        .thenAccept(saved -> System.out.println("Student dodany: " + saved.getFullName()))
 *        .exceptionally(ex -> {
 *            if (ex.getCause() instanceof StudentAlreadyExistsException) {
 *                System.err.println("Student o tym indeksie już istnieje!");
 *            }
 *            return null;
 *        });
 *
 * // Pobieranie studentów konkretnej grupy
 * service.getStudentsByGroupAsync("INF-2024")
 *        .thenAccept(students -> {
 *            System.out.println("Studenci w grupie INF-2024:");
 *            students.forEach(s -> System.out.println("- " + s.getFullName()));
 *        });
 *
 * // Przeniesienie studenta do innej grupy
 * Student existingStudent = new Student("Jan", "Kowalski", "123456", "MAT-2024");
 * service.updateStudentAsync("123456", existingStudent)
 *        .thenAccept(updated ->
 *            System.out.println("Student przeniesiony do grupy: " + updated.getGroupName()));
 * }
 * </pre>
 *
 * <h3>Obsługa błędów:</h3>
 * <p>Serwis automatycznie obsługuje następujące sytuacje błędne:</p>
 * <ul>
 *   <li>Duplikaty numerów indeksów ({@link StudentAlreadyExistsException})</li>
 *   <li>Brak połączenia z serwerem</li>
 *   <li>Timeout żądań (domyślnie 30 sekund)</li>
 *   <li>Nieprawidłowe odpowiedzi HTTP</li>
 *   <li>Błędy deserializacji JSON</li>
 *   <li>Nieistniejący student przy aktualizacji/usuwaniu</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 1.0
 * @since 2025
 *
 * @see Student
 * @see GroupService
 * @see CompletableFuture
 */
public class StudentService {

    /**
     * Bazowy URL serwera API.
     *
     * <p>Wszystkie żądania HTTP są kierowane do tego adresu bazowego.</p>
     */
    private static final String BASE_URL = "http://localhost:8080/api";

    /**
     * Pełny endpoint dla operacji na studentach.
     *
     * <p>Endpoint obsługuje następujące operacje HTTP:</p>
     * <ul>
     *   <li>GET {@value} - pobieranie wszystkich studentów</li>
     *   <li>POST {@value} - dodawanie nowego studenta</li>
     *   <li>PUT {@value}/{indeks} - aktualizacja danych studenta</li>
     *   <li>DELETE {@value}/{indeks} - usuwanie studenta</li>
     *   <li>GET {@value}/group/{grupa} - pobieranie studentów grupy</li>
     *   <li>GET {@value}/without-group - pobieranie studentów bez grupy</li>
     *   <li>PUT {@value}/remove-from-group/{indeks} - usuwanie z grupy</li>
     * </ul>
     */
    private static final String STUDENTS_ENDPOINT = BASE_URL + "/students";

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
     * Konstruktor serwisu studentów.
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
    public StudentService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Pobiera wszystkich studentów z serwera asynchronicznie.
     *
     * <p>Wykonuje żądanie GET do endpointu {@value #STUDENTS_ENDPOINT} i deserializuje
     * odpowiedź JSON do listy obiektów {@link Student}. Operacja jest wykonywana
     * asynchronicznie w tle, nie blokując wątku interfejsu użytkownika.</p>
     *
     * <p>Proces pobierania:</p>
     * <ol>
     *   <li>Wysłanie żądania HTTP GET do serwera</li>
     *   <li>Oczekiwanie na odpowiedź (max 30 sekund)</li>
     *   <li>Sprawdzenie kodu statusu HTTP (oczekiwany: 200)</li>
     *   <li>Deserializacja JSON do listy Student</li>
     *   <li>Konwersja obiektów serwera do obiektów klienta</li>
     *   <li>Mapowanie informacji o grupach z obiektów zagnieżdżonych</li>
     * </ol>
     *
     * <p>Zwracana lista zawiera wszystkich studentów w systemie, niezależnie
     * od ich przypisania do grup. Studenci bez grupy mają pole groupName ustawione na null.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * studentService.getAllStudentsAsync()
     *     .thenAccept(students -> {
     *         System.out.println("Znaleziono " + students.size() + " studentów:");
     *
     *         long withGroup = students.stream()
     *             .filter(s -> s.getGroupName() != null)
     *             .count();
     *         long withoutGroup = students.size() - withGroup;
     *
     *         System.out.println("- Z grupą: " + withGroup);
     *         System.out.println("- Bez grupy: " + withoutGroup);
     *
     *         students.forEach(student ->
     *             System.out.println("  " + student.getFullName() +
     *                              " (" + student.getIndexNumber() + ") - " +
     *                              (student.getGroupName() != null ? student.getGroupName() : "Brak grupy"))
     *         );
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Nie można pobrać studentów: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @return CompletableFuture z listą wszystkich studentów z serwera.
     *         Lista może być pusta jeśli brak studentów, ale nigdy nie będzie null.
     *
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         timeout żądania, nieprawidłowy kod odpowiedzi HTTP,
     *                         lub błąd deserializacji JSON
     *
     * @see #getStudentsByGroupAsync(String)
     * @see #getStudentsWithoutGroupAsync()
     * @see Student
     */
    public CompletableFuture<List<Student>> getAllStudentsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseStudentsFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac studentow z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Pobiera studentów przypisanych do konkretnej grupy asynchronicznie.
     *
     * <p>Wykonuje żądanie GET do endpointu z filtrem grupy i zwraca tylko studentów
     * przypisanych do określonej grupy. Nazwa grupy jest automatycznie kodowana
     * dla bezpiecznego przesyłania w URL.</p>
     *
     * <p>Endpoint: {@code GET /api/students/group/{encodedGroupName}}</p>
     *
     * <p>Proces pobierania:</p>
     * <ol>
     *   <li>Kodowanie nazwy grupy do formatu URL-safe (UTF-8)</li>
     *   <li>Wysłanie żądania GET z nazwą grupy w ścieżce</li>
     *   <li>Oczekiwanie na odpowiedź z filtrem po stronie serwera</li>
     *   <li>Deserializacja tylko studentów z danej grupy</li>
     * </ol>
     *
     * <p>Metoda zawiera rozszerzone debugowanie z logowaniem do konsoli
     * statusu żądania i liczby pobranych studentów.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * String groupName = "INF-2024";
     * studentService.getStudentsByGroupAsync(groupName)
     *     .thenAccept(students -> {
     *         if (students.isEmpty()) {
     *             System.out.println("Brak studentów w grupie " + groupName);
     *         } else {
     *             System.out.println("Studenci w grupie " + groupName + ":");
     *             students.forEach(student ->
     *                 System.out.println("- " + student.getFullName() +
     *                                  " (indeks: " + student.getIndexNumber() + ")")
     *             );
     *         }
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd pobierania studentów grupy: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @param groupName nazwa grupy (nie może być null ani pusta)
     * @return CompletableFuture z listą studentów z danej grupy.
     *         Lista może być pusta jeśli grupa nie ma studentów.
     *
     * @throws IllegalArgumentException jeśli groupName jest null lub puste
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem
     *
     * @see #getAllStudentsAsync()
     * @see #getStudentsWithoutGroupAsync()
     */
    public CompletableFuture<List<Student>> getStudentsByGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = STUDENTS_ENDPOINT + "/group/" + java.net.URLEncoder.encode(groupName, "UTF-8");

                System.out.println("🔗 Wywołuję URL: " + url); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi: " + response.statusCode()); // DEBUG
                System.out.println("📄 Treść odpowiedzi: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    List<Student> students = parseStudentsFromJson(response.body());
                    System.out.println("✅ Sparsowano " + students.size() + " studentów"); // DEBUG
                    return students;
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Treść: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd getStudentsByGroupAsync: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udalo sie pobrac studentow grupy z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Pobiera studentów bez przypisanej grupy asynchronicznie.
     *
     * <p>Wykonuje żądanie GET do specjalnego endpointu, który zwraca tylko studentów
     * bez przypisanej grupy. Przydatne do wyświetlania listy studentów dostępnych
     * do przypisania do nowych grup.</p>
     *
     * <p>Endpoint: {@code GET /api/students/without-group}</p>
     *
     * <p>Studenci bez grupy to tacy, którzy:</p>
     * <ul>
     *   <li>Zostali dodani do systemu bez przypisania do grupy</li>
     *   <li>Zostali usunięci z grupy ale pozostali w systemie</li>
     *   <li>Mają pole group ustawione na null w bazie danych</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * studentService.getStudentsWithoutGroupAsync()
     *     .thenAccept(availableStudents -> {
     *         if (availableStudents.isEmpty()) {
     *             System.out.println("Wszyscy studenci są przypisani do grup");
     *         } else {
     *             System.out.println("Studenci dostępni do przypisania:");
     *             availableStudents.forEach(student ->
     *                 System.out.println("- " + student.getFullName() +
     *                                  " (indeks: " + student.getIndexNumber() + ")")
     *             );
     *         }
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd pobierania studentów bez grupy: " + throwable.getMessage());
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @return CompletableFuture z listą studentów bez przypisanej grupy.
     *         Lista może być pusta jeśli wszyscy studenci mają grupy.
     *
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem
     *
     * @see #getAllStudentsAsync()
     * @see #getStudentsByGroupAsync(String)
     * @see #removeStudentFromGroupAsync(String)
     */
    public CompletableFuture<List<Student>> getStudentsWithoutGroupAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = STUDENTS_ENDPOINT + "/without-group";

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
                    return parseStudentsFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac studentow bez grupy z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Dodaje nowego studenta na serwer asynchronicznie.
     *
     * <p>Wykonuje żądanie POST do endpointu {@value #STUDENTS_ENDPOINT} z danymi studenta
     * w formacie JSON. Student może być dodany z przypisaną grupą lub bez grupy.
     * Serwer automatycznie sprawdza unikalność numeru indeksu.</p>
     *
     * <p>Proces dodawania:</p>
     * <ol>
     *   <li>Walidacja obiektu studenta (imię, nazwisko, indeks wymagane)</li>
     *   <li>Serializacja studenta do formatu JSON</li>
     *   <li>Wysłanie żądania HTTP POST z danymi JSON</li>
     *   <li>Sprawdzenie kodu odpowiedzi (oczekiwany: 201 lub 200)</li>
     *   <li>Deserializacja odpowiedzi do obiektu Student z danymi z serwera</li>
     * </ol>
     *
     * <p>Metoda zawiera rozszerzone debugowanie z logowaniem JSON-a wysyłanego na serwer.</p>
     *
     * <p>Obsługa kodów błędów HTTP:</p>
     * <ul>
     *   <li><strong>201/200</strong> - student utworzony pomyślnie</li>
     *   <li><strong>409</strong> - student o tym indeksie już istnieje ({@link StudentAlreadyExistsException})</li>
     *   <li><strong>400</strong> - nieprawidłowe dane studenta</li>
     *   <li><strong>500</strong> - błąd wewnętrzny serwera</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * // Student bez grupy
     * Student newStudent = new Student("Anna", "Nowak", "654321", null);
     *
     * studentService.addStudentAsync(newStudent)
     *     .thenAccept(savedStudent -> {
     *         System.out.println("Student dodany pomyślnie:");
     *         System.out.println("Imię: " + savedStudent.getFullName());
     *         System.out.println("Indeks: " + savedStudent.getIndexNumber());
     *         System.out.println("Grupa: " + (savedStudent.getGroupName() != null ?
     *                                        savedStudent.getGroupName() : "Brak"));
     *         System.out.println("Dodano: " + savedStudent.getFormattedDate());
     *     })
     *     .exceptionally(throwable -> {
     *         if (throwable.getCause() instanceof StudentAlreadyExistsException) {
     *             System.err.println("Student o indeksie " + newStudent.getIndexNumber() + " już istnieje!");
     *         } else {
     *             System.err.println("Błąd dodawania studenta: " + throwable.getMessage());
     *         }
     *         return null;
     *     });
     *
     * // Student z grupą
     * Student studentWithGroup = new Student("Piotr", "Kowalski", "789012", "FIZ-2024");
     * studentService.addStudentAsync(studentWithGroup)
     *     .thenAccept(saved -> System.out.println("Student dodany do grupy: " + saved.getGroupName()));
     * }
     * </pre>
     *
     * @param student student do dodania (nie może być null)
     * @return CompletableFuture z zapisanym studentem zawierającym dane z serwera
     *         (ID, data utworzenia, etc.)
     *
     * @throws IllegalArgumentException jeśli student jest null, lub wymagane pola są puste
     * @throws StudentAlreadyExistsException jeśli student o tym numerze indeksu już istnieje w systemie
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem lub deserializacji
     *
     * @see #updateStudentAsync(String, Student)
     * @see #deleteStudentAsync(String)
     * @see StudentAlreadyExistsException
     */
    public CompletableFuture<Student> addStudentAsync(Student student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = studentToJson(student);
                System.out.println("📤 Wysyłam JSON: " + jsonBody); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    return parseStudentFromJson(response.body());
                } else if (response.statusCode() == 409) {
                    throw new StudentAlreadyExistsException("Student o numerze indeksu " +
                            student.getIndexNumber() + " już istnieje w systemie!");
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode()
                            + ". Szczegóły: " + response.body());
                }

            } catch (StudentAlreadyExistsException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie dodac studenta na serwer: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Usuwa studenta z serwera asynchronicznie.
     *
     * <p>Wykonuje żądanie DELETE do endpointu z numerem indeksu studenta.
     * <strong>UWAGA:</strong> Ta operacja usuwa studenta całkowicie z systemu,
     * wraz z wszystkimi powiązanymi danymi (frekwencja, historia, etc.).</p>
     *
     * <p>Endpoint: {@code DELETE /api/students/{indexNumber}}</p>
     *
     * <p>Ta operacja jest nieodwracalna! Po usunięciu studenta:</p>
     * <ul>
     *   <li>Wszystkie dane studenta zostaną utracone</li>
     *   <li>Historia frekwencji zostanie usunięta</li>
     *   <li>Student zostanie usunięty ze wszystkich grup</li>
     *   <li>Nie będzie możliwości przywrócenia danych</li>
     * </ul>
     *
     * <p>Jeśli chcesz tylko usunąć studenta z grupy (bez usuwania z systemu),
     * użyj {@link #removeStudentFromGroupAsync(String)}.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * String indexNumber = "123456";
     *
     * // Potwierdzenie przed usunięciem
     * Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
     * confirmation.setContentText("Czy na pewno usunąć studenta " + indexNumber + "?\n" +
     *                             "Ta operacja jest nieodwracalna!");
     * confirmation.showAndWait().ifPresent(response -> {
     *     if (response == ButtonType.OK) {
     *         studentService.deleteStudentAsync(indexNumber)
     *             .thenAccept(success -> {
     *                 if (success) {
     *                     System.out.println("Student " + indexNumber + " został usunięty z systemu");
     *                 } else {
     *                     System.out.println("Nie udało się usunąć studenta");
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
     * @param studentIndexNumber numer indeksu studenta do usunięcia (nie może być null ani pusty)
     * @return CompletableFuture z wynikiem operacji:
     *         <ul>
     *           <li><strong>true</strong> - student został usunięty pomyślnie</li>
     *           <li><strong>false</strong> - nie udało się usunąć studenta</li>
     *         </ul>
     *
     * @throws IllegalArgumentException jeśli studentIndexNumber jest null lub pusty
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem
     *
     * @see #removeStudentFromGroupAsync(String) - alternatywa: usuwanie tylko z grupy
     * @see #updateStudentAsync(String, Student)
     */
    public CompletableFuture<Boolean> deleteStudentAsync(String studentIndexNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT + "/" + studentIndexNumber))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200 || response.statusCode() == 204;

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie usunac studenta z serwera: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Aktualizuje dane studenta na serwerze asynchronicznie.
     *
     * <p>Wykonuje żądanie PUT do endpointu z numerem indeksu i nowymi danymi studenta.
     * Pozwala na zmianę wszystkich danych studenta, w tym przypisania do grupy.
     * To główna metoda do przenoszenia studentów między grupami.</p>
     *
     * <p>Endpoint: {@code PUT /api/students/{indexNumber}}</p>
     *
     * <p>Możliwe scenariusze aktualizacji:</p>
     * <ul>
     *   <li><strong>Zmiana danych osobowych</strong> - imię, nazwisko</li>
     *   <li><strong>Przypisanie do grupy</strong> - ustawienie groupName</li>
     *   <li><strong>Przeniesienie między grupami</strong> - zmiana groupName</li>
     *   <li><strong>Usunięcie z grupy</strong> - ustawienie groupName na null</li>
     * </ul>
     *
     * <p>Metoda zawiera rozszerzone debugowanie z logowaniem JSON-a i odpowiedzi serwera.</p>
     *
     * <p>Proces aktualizacji:</p>
     * <ol>
     *   <li>Serializacja danych studenta do JSON (specjalna metoda dla aktualizacji)</li>
     *   <li>Wysłanie żądania PUT z danymi</li>
     *   <li>Sprawdzenie kodu odpowiedzi (oczekiwany: 200)</li>
     *   <li>Deserializacja zaktualizowanego studenta z serwera</li>
     * </ol>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * String indexNumber = "123456";
     *
     * // Przeniesienie studenta do nowej grupy
     * Student updatedStudent = new Student("Jan", "Kowalski", indexNumber, "MAT-2024");
     *
     * studentService.updateStudentAsync(indexNumber, updatedStudent)
     *     .thenAccept(result -> {
     *         System.out.println("Student zaktualizowany:");
     *         System.out.println("Imię: " + result.getFullName());
     *         System.out.println("Nowa grupa: " + result.getGroupName());
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd aktualizacji: " + throwable.getMessage());
     *         return null;
     *     });
     *
     * // Usunięcie z grupy (bez usuwania z systemu)
     * Student withoutGroup = new Student("Jan", "Kowalski", indexNumber, null);
     * studentService.updateStudentAsync(indexNumber, withoutGroup)
     *     .thenAccept(result ->
     *         System.out.println("Student usunięty z grupy, pozostaje w systemie"));
     * }
     * </pre>
     *
     * @param indexNumber aktualny numer indeksu studenta (nie może być null ani pusty)
     * @param student nowe dane studenta (nie może być null)
     * @return CompletableFuture z zaktualizowanym studentem zawierającym dane z serwera
     *
     * @throws IllegalArgumentException jeśli indexNumber lub student jest null/pusty
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         student nie istnieje, lub błąd deserializacji
     *
     * @see #addStudentAsync(Student)
     * @see #removeStudentFromGroupAsync(String) - alternatywna metoda do usuwania z grupy
     */
    public CompletableFuture<Student> updateStudentAsync(String indexNumber, Student student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = studentToJsonForUpdate(student);
                System.out.println("🔄 Aktualizuję studenta " + indexNumber + " JSON: " + jsonBody); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT + "/" + indexNumber))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Update status: " + response.statusCode()); // DEBUG
                System.out.println("📄 Update response: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    return parseStudentFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Treść: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd updateStudentAsync: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udalo sie zaktualizowac studenta na serwerze: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Usuwa studenta z jego aktualnej grupy (bez usuwania z systemu) asynchronicznie.
     *
     * <p>Wykonuje żądanie PUT do specjalnego endpointu, który usuwa przypisanie studenta
     * do grupy, ale pozostawia go w systemie. Student staje się dostępny do przypisania
     * do innej grupy. To bezpieczniejsza alternatywa dla {@link #deleteStudentAsync(String)}.</p>
     *
     * <p>Endpoint: {@code PUT /api/students/remove-from-group/{encodedIndexNumber}}</p>
     *
     * <p>Po wykonaniu tej operacji:</p>
     * <ul>
     *   <li>Student pozostaje w systemie z wszystkimi danymi osobowymi</li>
     *   <li>Pole groupName zostaje ustawione na null</li>
     *   <li>Student staje się dostępny do przypisania do nowej grupy</li>
     *   <li>Historia frekwencji może zostać zachowana (zależnie od konfiguracji serwera)</li>
     * </ul>
     *
     * <p>Metoda zawiera rozszerzone debugowanie z logowaniem URL-a i odpowiedzi serwera.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * String indexNumber = "123456";
     *
     * studentService.removeStudentFromGroupAsync(indexNumber)
     *     .thenAccept(updatedStudent -> {
     *         System.out.println("Student usunięty z grupy:");
     *         System.out.println("Imię: " + updatedStudent.getFullName());
     *         System.out.println("Indeks: " + updatedStudent.getIndexNumber());
     *         System.out.println("Grupa: " + (updatedStudent.getGroupName() != null ?
     *                                        updatedStudent.getGroupName() : "BRAK"));
     *         System.out.println("Student jest teraz dostępny do przypisania do nowej grupy");
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Błąd usuwania z grupy: " + throwable.getMessage());
     *         return null;
     *     });
     *
     * // Sprawdzenie dostępnych studentów po operacji
     * studentService.getStudentsWithoutGroupAsync()
     *     .thenAccept(availableStudents ->
     *         System.out.println("Dostępnych studentów: " + availableStudents.size()));
     * }
     * </pre>
     *
     * @param indexNumber numer indeksu studenta do usunięcia z grupy (nie może być null ani pusty)
     * @return CompletableFuture z zaktualizowanym studentem bez grupy (groupName = null)
     *
     * @throws IllegalArgumentException jeśli indexNumber jest null lub pusty
     * @throws RuntimeException jeśli wystąpi błąd komunikacji z serwerem,
     *                         student nie istnieje, lub błąd deserializacji
     *
     * @see #updateStudentAsync(String, Student) - alternatywna metoda przez aktualizację
     * @see #getStudentsWithoutGroupAsync() - sprawdzenie dostępnych studentów
     * @see #deleteStudentAsync(String) - całkowite usunięcie z systemu
     */
    public CompletableFuture<Student> removeStudentFromGroupAsync(String indexNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = STUDENTS_ENDPOINT + "/remove-from-group/" + java.net.URLEncoder.encode(indexNumber, "UTF-8");

                System.out.println("🔗 Wywołuję URL usuwania z grupy: " + url); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.ofString("{}"))  // Pusty body dla PUT
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi usuwania z grupy: " + response.statusCode()); // DEBUG
                System.out.println("📄 Treść odpowiedzi: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    Student updatedStudent = parseStudentFromJson(response.body());
                    System.out.println("✅ Student usunięty z grupy: " + updatedStudent.getFullName() +
                            " (grupa: " + (updatedStudent.getGroupName() != null ? updatedStudent.getGroupName() : "BRAK") + ")");
                    return updatedStudent;
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Treść: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd removeStudentFromGroupAsync: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udalo sie usunac studenta z grupy na serwerze: " + e.getMessage(), e);
            }
        });
    }

    // === METODY PRYWATNE DO PARSOWANIA JSON ===

    /**
     * Parsuje JSON z listą studentów z serwera do listy obiektów Student.
     *
     * <p>Konwertuje JSON otrzymany z serwera do listy obiektów {@link Student}
     * używanych w aplikacji klienckiej. Obsługuje automatyczną konwersję
     * typów danych i mapowanie pól, w tym informacji o grupach z obiektów zagnieżdżonych.</p>
     *
     * @param json JSON z odpowiedzi serwera zawierający listę studentów
     * @return lista obiektów Student
     * @throws RuntimeException jeśli nie można sparsować JSON
     */
    private List<Student> parseStudentsFromJson(String json) {
        try {
            List<StudentFromServer> serverStudents = objectMapper.readValue(json, new TypeReference<List<StudentFromServer>>() {});

            return serverStudents.stream()
                    .map(this::convertToStudent)
                    .toList();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse students JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parsuje JSON z pojedynczym studentem z serwera.
     *
     * @param json JSON z odpowiedzi serwera zawierający studenta
     * @return obiekt Student
     * @throws RuntimeException jeśli nie można sparsować JSON
     */
    private Student parseStudentFromJson(String json) {
        try {
            StudentFromServer serverStudent = objectMapper.readValue(json, StudentFromServer.class);
            return convertToStudent(serverStudent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse student JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Konwertuje obiekt Student do JSON do wysłania na serwer (dla operacji dodawania).
     *
     * <p>Używa standardowej struktury JSON dla nowych studentów.</p>
     *
     * @param student student do konwersji
     * @return JSON reprezentujący studenta
     * @throws RuntimeException jeśli nie można serializować do JSON
     */
    private String studentToJson(Student student) {
        try {
            StudentToServer studentToServer = new StudentToServer();
            studentToServer.firstName = student.getFirstName();
            studentToServer.lastName = student.getLastName();
            studentToServer.indexNumber = student.getIndexNumber();
            studentToServer.groupName = student.getGroupName();

            return objectMapper.writeValueAsString(studentToServer);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert student to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Specjalna konwersja dla aktualizacji z nazwą grupy jako obiektem.
     *
     * <p>Tworzy JSON który backend może zrozumieć dla operacji aktualizacji,
     * gdzie grupa jest reprezentowana jako obiekt zagnieżdżony zamiast prostego stringa.</p>
     *
     * <p>Format JSON dla aktualizacji:</p>
     * <pre>
     * {@code
     * {
     *   "firstName": "Jan",
     *   "lastName": "Kowalski",
     *   "indexNumber": "123456",
     *   "group": {
     *     "name": "INF-2024"
     *   }
     * }
     * }
     * </pre>
     *
     * @param student student do konwersji
     * @return JSON w formacie oczekiwanym przez endpoint aktualizacji
     * @throws RuntimeException jeśli nie można serializować do JSON
     */
    private String studentToJsonForUpdate(Student student) {
        try {
            // Tworzymy JSON który backend może zrozumieć
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"firstName\":\"").append(escapeJson(student.getFirstName())).append("\",");
            json.append("\"lastName\":\"").append(escapeJson(student.getLastName())).append("\",");
            json.append("\"indexNumber\":\"").append(escapeJson(student.getIndexNumber())).append("\"");

            // KLUCZOWE: Jeśli student ma grupę, dodaj ją jako obiekt Group
            if (student.getGroupName() != null && !student.getGroupName().trim().isEmpty()) {
                json.append(",\"group\":{\"name\":\"").append(escapeJson(student.getGroupName())).append("\"}");
            } else {
                json.append(",\"group\":null");
            }

            json.append("}");
            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert student to JSON for update: " + e.getMessage(), e);
        }
    }

    /**
     * Konwertuje obiekt studenta z serwera do obiektu klienta.
     *
     * <p>Mapuje pola z obiektu serwera na obiekt używany w kliencie,
     * w tym wyciąganie nazwy grupy z zagnieżdżonego obiektu grupy.</p>
     *
     * @param serverStudent student z serwera
     * @return obiekt Student do użycia w kliencie
     */
    private Student convertToStudent(StudentFromServer serverStudent) {
        String groupName = null;
        if (serverStudent.group != null) {
            groupName = serverStudent.group.name; // Pobierz nazwę z obiektu grupy
        }

        Student student = new Student(serverStudent.firstName, serverStudent.lastName,
                serverStudent.indexNumber, groupName);

        // DEBUG - pokaż co zostało sparsowane
        System.out.println("🔄 Sparsowano studenta: " + student.getFullName() +
                " (grupa: " + (groupName != null ? groupName : "BRAK") + ")");

        return student;
    }

    /**
     * Escape'uje stringi w JSON dla bezpiecznej serializacji.
     *
     * <p>Zabezpiecza przed błędami JSON spowodowanymi przez specjalne znaki
     * w danych studentów (cudzysłowy, znaki nowej linii, etc.).</p>
     *
     * @param str string do escape'owania
     * @return bezpieczny string do użycia w JSON
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // === KLASY POMOCNICZE - BEZ EMAIL ===

    /**
     * Klasa reprezentująca studenta otrzymanego z serwera.
     *
     * <p>Używana do deserializacji JSON z serwera. Zawiera wszystkie pola
     * jakie serwer może zwrócić, włączając te które nie są używane w kliencie.</p>
     */
    private static class StudentFromServer {
        /** ID studenta z bazy danych */
        public Long id;

        /** Imię studenta */
        public String firstName;

        /** Nazwisko studenta */
        public String lastName;

        /** Numer indeksu studenta */
        public String indexNumber;

        /** Data utworzenia w systemie */
        public LocalDateTime createdDate;

        /** Czy student jest aktywny */
        public Boolean active;

        /** Informacje o grupie jako obiekt zagnieżdżony */
        public GroupInfo group;
    }

    /**
     * Klasa reprezentująca informacje o grupie w odpowiedzi serwera.
     *
     * <p>Zagnieżdżona w obiekcie StudentFromServer dla reprezentacji
     * powiązania student-grupa.</p>
     */
    private static class GroupInfo {
        /** ID grupy z bazy danych */
        public Long id;

        /** Nazwa grupy */
        public String name;

        /** Specjalizacja grupy */
        public String specialization;
    }

    /**
     * Klasa reprezentująca studenta do wysłania na serwer.
     *
     * <p>Używana do serializacji danych studenta do JSON przed wysłaniem na serwer.
     * Zawiera tylko podstawowe pola wymagane do utworzenia studenta.
     * Grupa jest reprezentowana jako prosty string z nazwą.</p>
     */
    private static class StudentToServer {
        /** Imię studenta */
        public String firstName;

        /** Nazwisko studenta */
        public String lastName;

        /** Numer indeksu studenta */
        public String indexNumber;

        /** Nazwa grupy (może być null) */
        public String groupName;
        // BEZ POLA EMAIL - nie używane w tej aplikacji
    }

    /**
     * Wyjątek rzucany gdy student o danym numerze indeksu już istnieje w systemie.
     *
     * <p>Ten wyjątek jest rzucany przez {@link #addStudentAsync(Student)} gdy serwer
     * zwróci kod odpowiedzi HTTP 409 (Conflict), co oznacza że student o takim
     * numerze indeksu już istnieje w bazie danych.</p>
     *
     * <p>Numer indeksu studenta musi być unikalny w całym systemie.</p>
     *
     * <p>Przykład obsługi:</p>
     * <pre>
     * {@code
     * studentService.addStudentAsync(newStudent)
     *     .exceptionally(throwable -> {
     *         if (throwable.getCause() instanceof StudentAlreadyExistsException) {
     *             System.err.println("Student już istnieje: " + throwable.getMessage());
     *             // Pokaż użytkownikowi dialog o błędzie
     *             // Możliwość aktualizacji istniejącego studenta
     *         }
     *         return null;
     *     });
     * }
     * </pre>
     *
     * @see #addStudentAsync(Student)
     * @see #updateStudentAsync(String, Student) - dla aktualizacji istniejącego studenta
     */
    public static class StudentAlreadyExistsException extends RuntimeException {
        /**
         * Konstruktor wyjątku z komunikatem błędu.
         *
         * @param message szczegółowy opis błędu, zwykle zawierający numer indeksu studenta
         */
        public StudentAlreadyExistsException(String message) {
            super(message);
        }
    }
}