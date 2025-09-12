package com.example.javafxfront;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Główna klasa aplikacji JavaFX - Dziennik Online.
 *
 * <p>HelloApplication stanowi punkt wejścia (entry point) dla aplikacji
 * dziennika elektronicznego opartej na JavaFX. Klasa rozszerza {@link Application}
 * i implementuje kompletny cykl uruchamiania aplikacji wraz z konfiguracją
 * interfejsu użytkownika.</p>
 *
 * <p>Aplikacja "Dziennik Online" to kompleksowy system zarządzania grupami
 * studenckimi, który umożliwia:</p>
 * <ul>
 *   <li><strong>Zarządzanie grupami</strong> - tworzenie, edycja, usuwanie grup studenckich</li>
 *   <li><strong>Zarządzanie studentami</strong> - dodawanie, przypisywanie do grup, usuwanie</li>
 *   <li><strong>Planowanie zajęć</strong> - tworzenie terminów, harmonogramów</li>
 *   <li><strong>Rejestrowanie frekwencji</strong> - oznaczanie obecności, generowanie raportów</li>
 *   <li><strong>Synchronizacja z serwerem</strong> - wymiana danych z systemem backend</li>
 * </ul>
 *
 * <h3>Architektura aplikacji:</h3>
 * <p>Aplikacja została zbudowana w oparciu o wzorzec MVC (Model-View-Controller):</p>
 * <ul>
 *   <li><strong>Model</strong> - {@link Student}, {@link Group}, {@link ClassSchedule}, {@link Attendance}</li>
 *   <li><strong>View</strong> - pliki FXML (modern-view.fxml, group-detail-view.fxml, attendance-report-view.fxml)</li>
 *   <li><strong>Controller</strong> - {@link ModernController}, {@link GroupDetailController}, {@link AttendanceReportController}</li>
 *   <li><strong>Service</strong> - {@link GroupService}, {@link StudentService}, {@link ScheduleService}, {@link AttendanceService}</li>
 * </ul>
 *
 * <h3>Struktura interfejsu użytkownika:</h3>
 * <p>Aplikacja składa się z trzech głównych widoków:</p>
 * <ol>
 *   <li><strong>Widok główny (modern-view.fxml)</strong>
 *       <ul>
 *         <li>Dashboard z kartami zarządzania grupami i studentami</li>
 *         <li>Lista wszystkich grup w systemie</li>
 *         <li>Globalne dodawanie studentów</li>
 *         <li>Wyszukiwanie i zarządzanie studentami</li>
 *       </ul>
 *   </li>
 *   <li><strong>Widok szczegółów grupy (group-detail-view.fxml)</strong>
 *       <ul>
 *         <li>Zarządzanie studentami w konkretnej grupie</li>
 *         <li>Tworzenie i zarządzanie terminami zajęć</li>
 *         <li>Rejestrowanie frekwencji dla każdego terminu</li>
 *         <li>Generowanie dziennika obecności</li>
 *       </ul>
 *   </li>
 *   <li><strong>Widok dziennika obecności (attendance-report-view.fxml)</strong>
 *       <ul>
 *         <li>Tabela z kompletną frekwencją grupy</li>
 *         <li>Statystyki obecności i analiza danych</li>
 *         <li>Eksport do plików CSV</li>
 *         <li>Filtrowanie i sortowanie danych</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h3>Funkcjonalności systemu:</h3>
 * <p>System oferuje następujące kluczowe funkcjonalności:</p>
 * <ul>
 *   <li><strong>Zarządzanie danymi offline i online</strong> - możliwość pracy bez połączenia z serwerem</li>
 *   <li><strong>Synchronizacja dwukierunkowa</strong> - automatyczna synchronizacja danych z serwerem backend</li>
 *   <li><strong>Walidacja danych</strong> - sprawdzanie poprawności wprowadzanych informacji</li>
 *   <li><strong>Responsywny interfejs</strong> - automatyczne dostosowanie do rozmiaru okna</li>
 *   <li><strong>Kolorowe oznaczenia statusów</strong> - intuicyjne rozróżnienie stanów obecności</li>
 *   <li><strong>Eksport danych</strong> - możliwość eksportu raportów do formatów CSV</li>
 *   <li><strong>Diagnostyka połączeń</strong> - narzędzia do debugowania problemów z API</li>
 * </ul>
 *
 * <h3>Konfiguracja i wymagania:</h3>
 * <ul>
 *   <li><strong>Java 21+</strong> - aplikacja wymaga najnowszej wersji Java</li>
 *   <li><strong>JavaFX 21</strong> - biblioteka interfejsu użytkownika</li>
 *   <li><strong>Jackson</strong> - serializacja/deserializacja JSON</li>
 *   <li><strong>HTTP Client</strong> - komunikacja z serwerem REST API</li>
 *   <li><strong>Rozdzielczość ekranu</strong> - minimum 1024x768, zalecane 1400x800+</li>
 * </ul>
 *
 * <h3>Przykład uruchomienia:</h3>
 * <pre>
 * {@code
 * // Uruchomienie aplikacji z linii komend:
 * java -jar dziennik-online.jar
 *
 * // Lub poprzez IDE:
 * public static void main(String[] args) {
 *     HelloApplication.main(args);
 * }
 *
 * // Uruchomienie z parametrami JVM dla JavaFX:
 * java --module-path /path/to/javafx/lib
 *      --add-modules javafx.controls,javafx.fxml
 *      -jar dziennik-online.jar
 * }
 * </pre>
 *
 * <h3>Struktura stylów:</h3>
 * <p>Aplikacja wykorzystuje dedykowany plik CSS ({@code styles.css}) z motywem biało-czerwonym:</p>
 * <ul>
 *   <li>Kolor główny: #DC143C (Crimson Red)</li>
 *   <li>Kolory pomocnicze: zielony (#38A169), pomarańczowy (#F56500), czerwony (#E53E3E)</li>
 *   <li>Stylizowane karty, przyciski, pola tekstowe i tabele</li>
 *   <li>Animacje hover i efekty wizualne</li>
 *   <li>Responsywny design z media queries</li>
 * </ul>
 *
 * <h3>Integracja z serwerem:</h3>
 * <p>Aplikacja komunikuje się z serwerem backend poprzez REST API:</p>
 * <ul>
 *   <li><strong>Endpoint bazowy:</strong> {@code http://localhost:8080/api}</li>
 *   <li><strong>Endpointy:</strong> /groups, /students, /schedules, /attendance</li>
 *   <li><strong>Metody HTTP:</strong> GET, POST, PUT, DELETE</li>
 *   <li><strong>Format danych:</strong> JSON z automatycznym mapowaniem</li>
 *   <li><strong>Obsługa błędów:</strong> Graceful degradation przy braku połączenia</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 1.0.0
 * @since 2025
 *
 * @see ModernController Główny kontroler interfejsu użytkownika
 * @see GroupDetailController Kontroler zarządzania grupami
 * @see AttendanceReportController Kontroler dziennika obecności
 * @see GroupService Serwis zarządzania grupami
 * @see StudentService Serwis zarządzania studentami
 * @see ScheduleService Serwis zarządzania terminami
 * @see AttendanceService Serwis zarządzania frekwencją
 */
public class HelloApplication extends Application {

    /**
     * Główna metoda uruchamiająca aplikację JavaFX.
     *
     * <p>Ta metoda jest automatycznie wywoływana przez framework JavaFX
     * podczas uruchamiania aplikacji. Odpowiada za:</p>
     * <ul>
     *   <li><strong>Ładowanie głównego widoku FXML</strong> - wczytuje modern-view.fxml</li>
     *   <li><strong>Konfigurację głównej sceny</strong> - ustawia rozmiar 1400x800 pikseli</li>
     *   <li><strong>Ładowanie stylów CSS</strong> - aplikuje styles.css dla motywu wizualnego</li>
     *   <li><strong>Konfigurację okna głównego</strong> - tytuł, rozmiary, ograniczenia</li>
     *   <li><strong>Wyświetlenie aplikacji</strong> - pokazuje okno użytkownikowi</li>
     * </ul>
     *
     * <h3>Proces inicjalizacji:</h3>
     * <ol>
     *   <li>Tworzenie FXMLLoader dla głównego widoku</li>
     *   <li>Ładowanie hierarchii węzłów z pliku FXML</li>
     *   <li>Utworzenie sceny o rozmiarze 1400x800</li>
     *   <li>Załadowanie arkuszy stylów CSS</li>
     *   <li>Konfiguracja właściwości Stage (okna)</li>
     *   <li>Wyświetlenie okna użytkownikowi</li>
     * </ol>
     *
     * <h3>Konfiguracja okna:</h3>
     * <ul>
     *   <li><strong>Tytuł:</strong> "Dziennik Online - Zarządzanie Grupami"</li>
     *   <li><strong>Rozmiar początkowy:</strong> 1400x800 pikseli</li>
     *   <li><strong>Minimalny rozmiar:</strong> 900x600 pikseli</li>
     *   <li><strong>Arkusz stylów:</strong> styles.css z motywem biało-czerwonym</li>
     *   <li><strong>Domyślny widok:</strong> modern-view.fxml (dashboard główny)</li>
     * </ul>
     *
     * <h3>Obsługa błędów:</h3>
     * <p>W przypadku błędów podczas ładowania FXML lub CSS,
     * aplikacja rzuci {@link IOException} z odpowiednim opisem problemu.
     * Najczęstsze przyczyny błędów:</p>
     * <ul>
     *   <li>Brak pliku modern-view.fxml w resources</li>
     *   <li>Błędna składnia w pliku FXML</li>
     *   <li>Brak klasy kontrolera ({@link ModernController})</li>
     *   <li>Brak pliku styles.css</li>
     *   <li>Problemy z ładowaniem JavaFX Runtime</li>
     * </ul>
     *
     * <h3>Przykład hierarchii widoków:</h3>
     * <pre>
     * {@code
     * Stage (Okno główne)
     * └── Scene (1400x800)
     *     └── BorderPane (z modern-view.fxml)
     *         ├── Top: Header z tytułem
     *         ├── Center: ScrollPane
     *         │   └── VBox z kartami:
     *         │       ├── Karta "Dodaj grupę"
     *         │       ├── Karta "Dodaj studenta"
     *         │       ├── Karta "Zarządzaj studentami"
     *         │       └── Karta "Lista grup"
     *         └── Bottom: Footer z informacjami
     * }
     * </pre>
     *
     * @param stage główne okno aplikacji dostarczane przez JavaFX runtime
     * @throws IOException jeśli wystąpi błąd podczas ładowania pliku FXML,
     *                     stylów CSS, lub innych zasobów aplikacji
     *
     * @see FXMLLoader#load() Ładowanie hierarchii węzłów z FXML
     * @see Scene#getStylesheets() Ładowanie stylów CSS
     * @see Stage#setTitle(String) Ustawianie tytułu okna
     * @see Stage#setMinWidth(double) Ustawianie minimalnej szerokości
     * @see Stage#setMinHeight(double) Ustawianie minimalnej wysokości
     * @see Stage#show() Wyświetlanie okna użytkownikowi
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("modern-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 800);

        // Dodanie CSS stylów
        //Komentarz do commita o 3 pierwszych dzialajacyh punktach
        //Naprawa widzenia terminow
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        stage.setTitle("Dziennik Online - Zarządzanie Grupami");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    /**
     * Statyczna metoda main - punkt wejścia aplikacji Java.
     *
     * <p>Ta metoda jest standardowym punktem wejścia dla aplikacji Java.
     * Dla aplikacji JavaFX deleguje wykonanie do metody {@link Application#launch(String...)},
     * która inicjalizuje JavaFX Runtime i wywołuje metodę {@link #start(Stage)}.</p>
     *
     * <h3>Proces uruchamiania JavaFX:</h3>
     * <ol>
     *   <li><strong>Inicjalizacja JavaFX Runtime</strong> - ładowanie natywnych bibliotek</li>
     *   <li><strong>Utworzenie Application Thread</strong> - dedykowany wątek UI</li>
     *   <li><strong>Utworzenie głównego Stage</strong> - podstawowe okno aplikacji</li>
     *   <li><strong>Wywołanie init()</strong> - opcjonalna inicjalizacja (nie używane)</li>
     *   <li><strong>Wywołanie start(Stage)</strong> - główna logika uruchamiania</li>
     *   <li><strong>Uruchomienie pętli zdarzeń</strong> - obsługa interakcji użytkownika</li>
     * </ol>
     *
     * <h3>Wymagania systemowe:</h3>
     * <ul>
     *   <li><strong>Java 21+</strong> z obsługą modułów</li>
     *   <li><strong>JavaFX Runtime</strong> w classpath lub module-path</li>
     *   <li><strong>Moduły JavaFX:</strong> javafx.controls, javafx.fxml</li>
     *   <li><strong>System graficzny:</strong> X11, Windows, macOS z GUI</li>
     *   <li><strong>Pamięć RAM:</strong> minimum 512MB dostępnej pamięci</li>
     * </ul>
     *
     * <h3>Parametry uruchomieniowe:</h3>
     * <p>Aplikacja może być uruchamiana z następującymi parametrami JVM:</p>
     * <pre>
     * {@code
     * // Podstawowe uruchomienie:
     * java -jar dziennik-online.jar
     *
     * // Z określeniem ścieżki do JavaFX:
     * java --module-path /path/to/javafx/lib \
     *      --add-modules javafx.controls,javafx.fxml \
     *      -jar dziennik-online.jar
     *
     * // Z dodatkowymi opcjami pamięci:
     * java -Xmx1024m -Xms512m \
     *      --add-modules javafx.controls,javafx.fxml \
     *      -jar dziennik-online.jar
     *
     * // Z debugowaniem:
     * java -Dprism.verbose=true \
     *      -Djavafx.verbose=true \
     *      -jar dziennik-online.jar
     * }
     * </pre>
     *
     * <h3>Obsługa błędów uruchamiania:</h3>
     * <p>Potencjalne problemy podczas uruchamiania:</p>
     * <ul>
     *   <li><strong>NoClassDefFoundError:</strong> Brak JavaFX w classpath</li>
     *   <li><strong>UnsupportedOperationException:</strong> Headless environment</li>
     *   <li><strong>IllegalStateException:</strong> JavaFX już uruchomiony</li>
     *   <li><strong>RuntimeException:</strong> Błąd ładowania natywnych bibliotek</li>
     * </ul>
     *
     * @param args argumenty wiersza poleceń przekazane do aplikacji.
     *             Obecnie nie są wykorzystywane, ale są przekazane do JavaFX
     *             dla ewentualnego wykorzystania przez framework
     *
     * @see Application#launch(String...) Uruchamianie aplikacji JavaFX
     * @see #start(Stage) Główna metoda inicjalizacji interfejsu
     *
     * @implNote Ta metoda blokuje wykonanie do momentu zamknięcia aplikacji
     *           przez użytkownika lub wywołania {@link javafx.application.Platform#exit()}
     */
    public static void main(String[] args) {
        launch();
    }
}