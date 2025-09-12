package com.example.javafxfront;

import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Optional;

// ========== DODANE IMPORTY ==========
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.geometry.Insets;
import java.util.List;
// ====================================

/**
 * Główny kontroler interfejsu użytkownika aplikacji Dziennik Online.
 *
 * <p>ModernController zarządza głównym widokiem aplikacji (modern-view.fxml),
 * który stanowi dashboard do zarządzania grupami studenckimi i studentami.
 * Kontroler oferuje kompleksowy system zarządzania danymi z możliwością
 * synchronizacji z serwerem backend oraz zaawansowanymi funkcjonalnościami
 * wyszukiwania i usuwania.</p>
 *
 * <p>Kontroler implementuje wzorzec MVC (Model-View-Controller) i integruje się
 * z następującymi serwisami:</p>
 * <ul>
 *   <li>{@link GroupService} - zarządzanie grupami studenckimi</li>
 *   <li>{@link StudentService} - zarządzanie studentami</li>
 *   <li>Synchronizacja asynchroniczna z serwerem REST API</li>
 *   <li>Diagnostyka połączeń i endpointów API</li>
 * </ul>
 *
 * <h3>Główne funkcjonalności:</h3>
 * <ul>
 *   <li><strong>Zarządzanie grupami</strong>
 *       <ul>
 *         <li>Dodawanie nowych grup z walidacją duplikatów</li>
 *         <li>Wyświetlanie listy grup z informacjami szczegółowymi</li>
 *         <li>Usuwanie grup z potwierdzeniem i diagnostyką</li>
 *         <li>Otwieranie szczegółowego widoku grupy</li>
 *         <li>Synchronizacja z serwerem i obsługa trybu offline</li>
 *       </ul>
 *   </li>
 *   <li><strong>Zarządzanie studentami</strong>
 *       <ul>
 *         <li>Dodawanie studentów bez przypisanej grupy</li>
 *         <li>Wyszukiwanie studentów po numerze indeksu</li>
 *         <li>Wyświetlanie ostatnio dodanych studentów</li>
 *         <li>Zaawansowane usuwanie z dodatkowymi polami (powód, uwagi)</li>
 *         <li>Monitorowanie liczby studentów na serwerze</li>
 *       </ul>
 *   </li>
 *   <li><strong>Diagnostyka systemu</strong>
 *       <ul>
 *         <li>Sprawdzanie połączenia z serwerem</li>
 *         <li>Testowanie dostępnych endpointów API</li>
 *         <li>Diagnostyka problemów z usuwaniem danych</li>
 *         <li>Wyświetlanie statusów operacji sieciowych</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Struktura interfejsu użytkownika:</h3>
 * <p>Główny widok składa się z następujących sekcji:</p>
 * <ol>
 *   <li><strong>Header</strong> - tytuł aplikacji i opis funkcjonalności</li>
 *   <li><strong>Karty funkcjonalne</strong>
 *       <ul>
 *         <li>Karta "Dodaj nową grupę" - formularz tworzenia grup</li>
 *         <li>Karta "Dodaj studenta" - formularz dodawania studentów</li>
 *         <li>Karta "Zarządzaj studentami" - wyszukiwanie i zaawansowane operacje</li>
 *         <li>Karta "Lista grup" - przegląd i zarządzanie grupami</li>
 *       </ul>
 *   </li>
 *   <li><strong>Sekcja instrukcji</strong> - przewodnik użytkownika</li>
 *   <li><strong>Footer</strong> - informacje o aplikacji</li>
 * </ol>
 *
 * <h3>Zarządzanie danymi:</h3>
 * <p>Kontroler implementuje elastyczny system zarządzania danymi:</p>
 * <ul>
 *   <li><strong>Synchronizacja dwukierunkowa</strong> - dane są automatycznie
 *       synchronizowane z serwerem, z możliwością pracy offline</li>
 *   <li><strong>Obsługa błędów</strong> - graceful degradation przy problemach z siecią</li>
 *   <li><strong>Walidacja danych</strong> - sprawdzanie poprawności przed wysłaniem</li>
 *   <li><strong>Powiadomienia użytkownika</strong> - alerty o statusie operacji</li>
 * </ul>
 *
 * <h3>Zaawansowane funkcjonalności:</h3>
 * <p>Nowatorskie funkcje wprowadzone w tej wersji:</p>
 * <ul>
 *   <li><strong>Wyszukiwanie studentów</strong> - znajdowanie po numerze indeksu</li>
 *   <li><strong>Zaawansowane usuwanie</strong> - dialog z dodatkowymi polami:
 *       <ul>
 *         <li>ComboBox z predefiniowanymi powodami usunięcia</li>
 *         <li>Pole tekstowe na dodatkowe uwagi</li>
 *         <li>Checkbox potwierdzenia operacji</li>
 *         <li>Informacje o konsekwencjach usunięcia</li>
 *         <li>Automatyczne logowanie operacji</li>
 *       </ul>
 *   </li>
 *   <li><strong>Diagnostyka API</strong> - testowanie różnych endpointów
 *       <ul>
 *         <li>Sprawdzanie dostępności serwera</li>
 *         <li>Testowanie różnych kombinacji URL i metod HTTP</li>
 *         <li>Identyfikacja działających endpointów</li>
 *         <li>Szczegółowe logowanie do konsoli</li>
 *       </ul>
 *   </li>
 *   <li><strong>Monitoring w czasie rzeczywistym</strong>
 *       <ul>
 *         <li>Liczniki studentów z automatycznym odświeżaniem</li>
 *         <li>Status połączenia z serwerem</li>
 *         <li>Lista ostatnio dodanych studentów</li>
 *         <li>Statystyki grup (z grupą/bez grupy)</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * // Kontroler jest automatycznie inicjalizowany przez JavaFX FXML Loader
 * // Po załadowaniu widoku, użytkownik może:
 *
 * // 1. Dodać nową grupę
 * // - Wypełnić pola "Nazwa grupy" i "Specjalizacja"
 * // - Kliknąć "Dodaj grupę" - dane zostaną wysłane na serwer
 *
 * // 2. Dodać studenta
 * // - Wypełnić pola studenta (bez grupy)
 * // - Student zostanie dodany do puli dostępnej do przypisania
 *
 * // 3. Wyszukać studenta
 * // - Wpisać numer indeksu w polu wyszukiwania
 * // - System wyświetli szczegóły znalezionego studenta
 * // - Możliwość edycji lub zaawansowanego usunięcia
 *
 * // 4. Zarządzać grupami
 * // - Wybrać grupę z listy
 * // - Wejść do szczegółów lub usunąć grupę
 * // - Uruchomić diagnostykę API przy problemach
 * }
 * </pre>
 *
 * <h3>Obsługa błędów i diagnostyka:</h3>
 * <p>Kontroler zawiera rozbudowany system diagnostyczny:</p>
 * <ul>
 *   <li><strong>Automatyczna detekcja problemów</strong> - rozpoznawanie błędów sieci</li>
 *   <li><strong>Alternatywne scenariusze</strong> - działanie offline przy braku serwera</li>
 *   <li><strong>Szczegółowe logowanie</strong> - informacje debugowe w konsoli</li>
 *   <li><strong>Przyjazne komunikaty</strong> - zrozumiałe alerty dla użytkownika</li>
 *   <li><strong>Diagnostyka endpointów</strong> - testowanie różnych URL API</li>
 * </ul>
 *
 * <h3>Integracja z innymi komponentami:</h3>
 * <ul>
 *   <li>{@link GroupDetailController} - szczegółowy widok grupy</li>
 *   <li>{@link HelloApplication} - główna klasa aplikacji</li>
 *   <li>{@link Group} - model danych grup</li>
 *   <li>{@link Student} - model danych studentów</li>
 *   <li>Plik FXML: modern-view.fxml</li>
 *   <li>Arkusz stylów: styles.css</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 2.0
 * @since 2025
 *
 * @see GroupService
 * @see StudentService
 * @see GroupDetailController
 * @see HelloApplication
 * @see Group
 * @see Student
 */
public class ModernController {


    /**
     * Główny tytuł aplikacji wyświetlany w headerze.
     * <p>Domyślny tekst: "Witamy w dzienniku online"</p>
     */
    @FXML private Label titleLabel;

    /**
     * Podtytuł aplikacji opisujący główną funkcjonalność.
     * <p>Domyślny tekst: "Zarządzanie grupami studenckimi"</p>
     */
    @FXML private Label subtitleLabel;


    /**
     * Pole tekstowe do wprowadzania nazwy nowej grupy.
     * <p>Placeholder: "np. Grupa INF-A"</p>
     * <p>Walidacja: sprawdzanie duplikatów nazw</p>
     */
    @FXML private TextField groupNameField;

    /**
     * Pole tekstowe do wprowadzania specjalizacji grupy.
     * <p>Placeholder: "np. Informatyka"</p>
     */
    @FXML private TextField specializationField;

    /**
     * Przycisk dodawania nowej grupy na serwer.
     * <p>Wykonuje walidację, wysyła dane na serwer i aktualizuje listę grup.</p>
     * <p>Podczas operacji zmienia tekst na "Dodawanie..." i jest wyłączony.</p>
     */
    @FXML private Button addGroupButton;

    // ========== ELEMENTY INTERFEJSU - DODAWANIE STUDENTÓW ==========

    /**
     * Pole tekstowe do wprowadzania imienia studenta.
     * <p>Placeholder: "Imię studenta"</p>
     */
    @FXML private TextField studentFirstNameField;

    /**
     * Pole tekstowe do wprowadzania nazwiska studenta.
     * <p>Placeholder: "Nazwisko studenta"</p>
     */
    @FXML private TextField studentLastNameField;

    /**
     * Pole tekstowe do wprowadzania numeru indeksu studenta.
     * <p>Placeholder: "6 cyfr (np. 123456)"</p>
     * <p>Walidacja: automatyczne filtrowanie - tylko cyfry, maksymalnie 6 znaków</p>
     */
    @FXML private TextField studentIndexField;

    /**
     * Przycisk dodawania studenta bez przypisanej grupy.
     * <p>Student zostanie dodany do puli dostępnej do późniejszego przypisania.</p>
     */
    @FXML private Button addStudentGlobalButton;

    /**
     * Kontener karty dodawania studentów.
     * <p>Zawiera wszystkie elementy formularza studenta.</p>
     */
    @FXML private VBox addStudentCard;

    // ========== ELEMENTY INTERFEJSU - LISTA GRUP ==========

    /**
     * ListView wyświetlająca wszystkie grupy w systemie.
     * <p>Każdy element listy jest renderowany przez {@link GroupListCell}.</p>
     * <p>Obsługuje selekcję pojedynczą z aktywacją przycisków akcji.</p>
     */
    @FXML private ListView<Group> groupsListView;

    /**
     * Przycisk otwierania szczegółowego widoku wybranej grupy.
     * <p>Uruchamia {@link GroupDetailController} w nowym oknie.</p>
     * <p>Aktywny tylko gdy grupa jest wybrana.</p>
     */
    @FXML private Button enterGroupButton;

    /**
     * Przycisk usuwania wybranej grupy z systemu.
     * <p>Wyświetla dialog potwierdzenia przed usunięciem.</p>
     * <p>Aktywny tylko gdy grupa jest wybrana.</p>
     */
    @FXML private Button deleteGroupButton;

    /**
     * Kontener karty dodawania grup.
     * <p>Zawiera wszystkie elementy formularza grupy.</p>
     */
    @FXML private VBox addGroupCard;

    /**
     * Kontener karty z listą grup.
     * <p>Zawiera ListView i przyciski zarządzania grupami.</p>
     */
    @FXML private VBox groupsCard;

    /**
     * Label wyświetlająca aktualną liczbę grup w systemie.
     * <p>Format: "Liczba grup: X"</p>
     * <p>Automatycznie aktualizowana przy dodawaniu/usuwaniu grup.</p>
     */
    @FXML private Label groupCountLabel;

    /**
     * Przycisk odświeżania listy grup z serwera.
     * <p>Pobiera aktualne dane z serwera i zastępuje lokalną listę.</p>
     * <p>Podczas operacji zmienia tekst na "Ładowanie..."</p>
     */
    @FXML private Button refreshButton;

    /**
     * Label wyświetlająca status połączenia z serwerem.
     * <p>Możliwe stany:</p>
     * <ul>
     *   <li>"🟢 Połączony z serverem" - kolor zielony</li>
     *   <li>"🔴 Serwer niedostępny" - kolor czerwony</li>
     *   <li>"🔄 Sprawdzanie serwera..." - podczas testowania</li>
     * </ul>
     */
    @FXML private Label serverStatusLabel;

    // ========== ELEMENTY INTERFEJSU - LICZNIKI STUDENTÓW ==========

    /**
     * Przycisk odświeżania licznika studentów na serwerze.
     * <p>Aktualizuje {@link #studentCountLabel} najnowszymi danymi.</p>
     */
    @FXML private Button refreshStudentsGlobalButton;

    /**
     * Label wyświetlająca liczbę studentów na serwerze.
     * <p>Format: "Studentów na serwerze: X (bez grupy: Y)"</p>
     * <p>Pokazuje łączną liczbę i liczbę studentów dostępnych do przypisania.</p>
     */
    @FXML private Label studentCountLabel;

    // ========== NOWE ELEMENTY - ZAAWANSOWANE ZARZĄDZANIE STUDENTAMI ==========

    /**
     * Kontener karty zaawansowanego zarządzania studentami.
     * <p>Zawiera funkcjonalności wyszukiwania, edycji i usuwania studentów.</p>
     */
    @FXML private VBox studentManagementCard;

    /**
     * Pole tekstowe do wyszukiwania studenta po numerze indeksu.
     * <p>Placeholder: "Wpisz nr indeksu (6 cyfr)"</p>
     * <p>Walidacja: automatyczne filtrowanie - tylko cyfry, maksymalnie 6 znaków</p>
     */
    @FXML private TextField searchStudentField;

    /**
     * Przycisk wykonywania wyszukiwania studenta.
     * <p>Wyszukuje studenta na serwerze i wyświetla jego szczegóły.</p>
     * <p>Podczas wyszukiwania zmienia tekst na "Szukam..."</p>
     */
    @FXML private Button searchStudentButton;

    /**
     * Przycisk odświeżania wszystkich danych studentów.
     * <p>Aktualizuje liczniki i listę ostatnio dodanych studentów.</p>
     */
    @FXML private Button refreshAllStudentsButton;

    /**
     * Label wyświetlająca szczegółowe statystyki wszystkich studentów.
     * <p>Format: "Wszystkich studentów: X (z grupą: Y, bez grupy: Z)"</p>
     */
    @FXML private Label allStudentsCountLabel;

    // ========== ELEMENTY WYŚWIETLANIA ZNALEZIONEGO STUDENTA ==========

    /**
     * Kontener informacji o znalezionym studencie.
     * <p>Widoczny tylko gdy student zostanie znaleziony przez wyszukiwanie.</p>
     * <p>Zawiera szczegóły studenta i przyciski akcji.</p>
     */
    @FXML private VBox foundStudentInfo;

    /**
     * Label wyświetlająca imię i nazwisko znalezionego studenta.
     * <p>Format: "👤 [Imię Nazwisko]"</p>
     */
    @FXML private Label foundStudentNameLabel;

    /**
     * Label wyświetlająca grupę znalezionego studenta.
     * <p>Format: "🏫 Grupa: [nazwa grupy]" lub "🏫 Grupa: Brak"</p>
     */
    @FXML private Label foundStudentGroupLabel;

    /**
     * Label wyświetlająca datę dodania studenta do systemu.
     * <p>Format: "📅 Dodano: [dd.MM.yyyy HH:mm]"</p>
     */
    @FXML private Label foundStudentDateLabel;

    /**
     * Przycisk edycji znalezionego studenta.
     * <p>Obecnie otwiera dialog informacyjny o przyszłej implementacji.</p>
     */
    @FXML private Button editFoundStudentButton;

    /**
     * Przycisk zaawansowanego usuwania znalezionego studenta.
     * <p>Otwiera dialog z dodatkowymi polami: powód, uwagi, potwierdzenie.</p>
     */
    @FXML private Button removeFoundStudentButton;

    // ========== ELEMENTY LISTY OSTATNICH STUDENTÓW ==========

    /**
     * ListView wyświetlająca listę ostatnio dodanych studentów.
     * <p>Pokazuje maksymalnie 5 najnowszych studentów z serwera.</p>
     * <p>Każdy element jest renderowany przez {@link RecentStudentListCell}.</p>
     */
    @FXML private ListView<Student> recentStudentsListView;

    /**
     * Przycisk otwierania pełnego panelu zarządzania studentami.
     * <p>Obecnie otwiera dialog informacyjny o przyszłej implementacji.</p>
     */
    @FXML private Button manageAllStudentsButton;

    // ========== POLA DANYCH ==========

    /**
     * Obecnie znaleziony student przez funkcję wyszukiwania.
     * <p>Null gdy żaden student nie jest aktualnie wyświetlany.</p>
     * <p>Używany przez przyciski akcji dla znalezionego studenta.</p>
     */
    private Student currentFoundStudent;

    /**
     * Observable lista ostatnio dodanych studentów.
     * <p>Związana z {@link #recentStudentsListView}.</p>
     * <p>Automatycznie aktualizowana z serwera.</p>
     */
    private ObservableList<Student> recentStudents;

    // ========== PODSTAWOWE POLA DANYCH ==========

    /**
     * Observable lista grup wyświetlana w interfejsie użytkownika.
     * <p>Związana z {@link #groupsListView}.</p>
     * <p>Synchronizowana z serwerem przy użyciu {@link GroupService}.</p>
     */
    private ObservableList<Group> groups;

    /**
     * Serwis zarządzania grupami - komunikacja z API backend.
     * <p>Obsługuje operacje CRUD na grupach:</p>
     * <ul>
     *   <li>{@link GroupService#getAllGroupsAsync()} - pobieranie grup</li>
     *   <li>{@link GroupService#addGroupAsync(Group)} - dodawanie grup</li>
     *   <li>{@link GroupService#deleteGroupAsync(String)} - usuwanie grup</li>
     * </ul>
     */
    private GroupService groupService;

    /**
     * Serwis zarządzania studentami - komunikacja z API backend.
     * <p>Obsługuje operacje CRUD na studentach:</p>
     * <ul>
     *   <li>{@link StudentService#getAllStudentsAsync()} - pobieranie studentów</li>
     *   <li>{@link StudentService#addStudentAsync(Student)} - dodawanie studentów</li>
     *   <li>{@link StudentService#deleteStudentAsync(String)} - usuwanie studentów</li>
     *   <li>{@link StudentService#getStudentsWithoutGroupAsync()} - studenci bez grup</li>
     * </ul>
     */
    private StudentService studentService;

    /**
     * Metoda inicjalizacyjna kontrolera wywoływana automatycznie przez JavaFX.
     *
     * <p>Ta metoda jest wywoływana automatycznie po załadowaniu pliku FXML
     * i przed wyświetleniem widoku użytkownikowi. Konfiguruje wszystkie elementy
     * interfejsu, inicjalizuje serwisy i ustawia nasłuchiwanie zdarzeń.</p>
     *
     * <p>Wykonywane operacje inicjalizacyjne:</p>
     * <ol>
     *   <li><strong>Inicjalizacja kolekcji danych</strong>
     *       <ul>
     *         <li>Utworzenie Observable listy grup</li>
     *         <li>Powiązanie listy z ListView</li>
     *         <li>Inicjalizacja serwisów (GroupService, StudentService)</li>
     *       </ul>
     *   </li>
     *   <li><strong>Konfiguracja ListView grup</strong>
     *       <ul>
     *         <li>Ustawienie custom CellFactory ({@link GroupListCell})</li>
     *         <li>Nasłuchiwanie zmian selekcji</li>
     *         <li>Aktywacja/dezaktywacja przycisków akcji</li>
     *       </ul>
     *   </li>
     *   <li><strong>Konfiguracja walidacji pól</strong>
     *       <ul>
     *         <li>Walidacja numeru indeksu (tylko cyfry, max 6 znaków)</li>
     *         <li>Automatyczne filtrowanie nieprawidłowych znaków</li>
     *       </ul>
     *   </li>
     *   <li><strong>Inicjalizacja zaawansowanego zarządzania studentami</strong>
     *       <ul>
     *         <li>Konfiguracja wyszukiwania studentów</li>
     *         <li>Inicjalizacja listy ostatnich studentów</li>
     *         <li>Ustawienie walidacji pola wyszukiwania</li>
     *       </ul>
     *   </li>
     *   <li><strong>Ładowanie danych z serwera</strong>
     *       <ul>
     *         <li>Sprawdzenie połączenia z serwerem</li>
     *         <li>Załadowanie liczników studentów</li>
     *         <li>Aktualizacja statusu połączenia</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Stan przycisków po inicjalizacji:</p>
     * <ul>
     *   <li>{@link #enterGroupButton} - wyłączony (brak selekcji)</li>
     *   <li>{@link #deleteGroupButton} - wyłączony (brak selekcji)</li>
     *   <li>{@link #foundStudentInfo} - ukryty (brak wyszukiwania)</li>
     * </ul>
     *
     * <p>Przykład sekwencji inicjalizacji:</p>
     * <pre>
     * {@code
     * 1. groups = FXCollections.observableArrayList();
     * 2. groupsListView.setItems(groups);
     * 3. groupService = new GroupService();
     * 4. studentService = new StudentService();
     * 5. setupValidation();
     * 6. initializeStudentManagement();
     * 7. checkServerConnection();
     * 8. loadStudentCountFromServer();
     * }
     * </pre>
     *
     * @see #setupStudentIndexValidation()
     * @see #initializeStudentManagement()
     * @see #checkServerConnection()
     * @see #loadStudentCountFromServer()
     */
    @FXML
    protected void initialize() {
        groups = FXCollections.observableArrayList();
        groupsListView.setItems(groups);
        groupService = new GroupService();
        studentService = new StudentService();

        groupsListView.setCellFactory(listView -> new GroupListCell());

        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            enterGroupButton.setDisable(!hasSelection);
            deleteGroupButton.setDisable(!hasSelection);
        });

        enterGroupButton.setDisable(true);
        deleteGroupButton.setDisable(true);

        updateGroupCount();
        checkServerConnection();
        setupStudentIndexValidation();
        loadStudentCountFromServer();

        initializeStudentManagement();
    }


    /**
     * Inicjalizuje system zaawansowanego zarządzania studentami.
     *
     * <p>Metoda konfiguruje wszystkie elementy związane z nową funkcjonalnością
     * wyszukiwania, wyświetlania i zarządzania studentami. Obejmuje inicjalizację
     * list, walidację pól, ukrywanie elementów interfejsu i ładowanie danych.</p>
     *
     * <p>Wykonywane operacje:</p>
     * <ol>
     *   <li><strong>Inicjalizacja listy ostatnich studentów</strong>
     *       <ul>
     *         <li>Utworzenie Observable listy {@link #recentStudents}</li>
     *         <li>Powiązanie z {@link #recentStudentsListView}</li>
     *         <li>Ustawienie custom CellFactory ({@link RecentStudentListCell})</li>
     *       </ul>
     *   </li>
     *   <li><strong>Ukrycie informacji o znalezionym studencie</strong>
     *       <ul>
     *         <li>Ustawienie {@link #foundStudentInfo} jako niewidoczny</li>
     *         <li>Ustawienie managed=false dla optymalizacji layoutu</li>
     *       </ul>
     *   </li>
     *   <li><strong>Konfiguracja walidacji wyszukiwania</strong>
     *       <ul>
     *         <li>Wywołanie {@link #setupSearchValidation()}</li>
     *         <li>Filtrowanie tylko cyfr, maksymalnie 6 znaków</li>
     *         <li>Ukrywanie informacji przy zmianie tekstu wyszukiwania</li>
     *       </ul>
     *   </li>
     *   <li><strong>Ładowanie danych z serwera</strong>
     *       <ul>
     *         <li>Wywołanie {@link #loadRecentStudents()}</li>
     *         <li>Wywołanie {@link #loadAllStudentsCount()}</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Stan po inicjalizacji:</p>
     * <ul>
     *   <li>{@link #recentStudents} - pusta lista gotowa na dane</li>
     *   <li>{@link #foundStudentInfo} - ukryty kontener</li>
     *   <li>{@link #currentFoundStudent} - null</li>
     *   <li>Pole wyszukiwania z aktywną walidacją</li>
     * </ul>
     *
     * @see #setupSearchValidation()
     * @see #loadRecentStudents()
     * @see #loadAllStudentsCount()
     * @see RecentStudentListCell
     */
    private void initializeStudentManagement() {
        recentStudents = FXCollections.observableArrayList();
        if (recentStudentsListView != null) {
            recentStudentsListView.setItems(recentStudents);
            recentStudentsListView.setCellFactory(listView -> new RecentStudentListCell());
        }

        if (foundStudentInfo != null) {
            foundStudentInfo.setVisible(false);
            foundStudentInfo.setManaged(false);
        }

        setupSearchValidation();

        loadRecentStudents();
        loadAllStudentsCount();
    }

    /**
     * Konfiguruje walidację i zachowanie pola wyszukiwania studentów.
     *
     * <p>Metoda ustawia listener na pole {@link #searchStudentField}, który:</p>
     * <ul>
     *   <li><strong>Filtruje znaki</strong> - pozostawia tylko cyfry (0-9)</li>
     *   <li><strong>Ogranicza długość</strong> - maksymalnie 6 znaków</li>
     *   <li><strong>Ukrywa poprzednie wyniki</strong> - przy zmianie tekstu</li>
     *   <li><strong>Automatycznie formatuje</strong> - usuwa nieprawidłowe znaki</li>
     * </ul>
     *
     * <p>Algorytm walidacji:</p>
     * <ol>
     *   <li>Pobranie nowej wartości pola tekstowego</li>
     *   <li>Usunięcie wszystkich znaków oprócz cyfr: {@code replaceAll("[^0-9]", "")}</li>
     *   <li>Obcięcie do maksymalnie 6 znaków: {@code substring(0, 6)}</li>
     *   <li>Aktualizacja pola jeśli wartość się zmieniła</li>
     *   <li>Ukrycie informacji o poprzednio znalezionym studencie</li>
     * </ol>
     *
     * <p>Przykład działania:</p>
     * <pre>
     * {@code
     * Użytkownik wpisuje: "12a3b45c67"
     * System filtruje do: "123456"
     * Pole wyświetla: "123456"
     *
     * Użytkownik wpisuje: "1234567890"
     * System obcina do: "123456"
     * Pole wyświetla: "123456"
     * }
     * </pre>
     *
     * @see #hideFoundStudentInfo()
     * @see #onSearchStudentClick()
     */
    private void setupSearchValidation() {
        if (searchStudentField != null) {
            searchStudentField.textProperty().addListener((observable, oldValue, newValue) -> {
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                if (digitsOnly.length() > 6) {
                    digitsOnly = digitsOnly.substring(0, 6);
                }
                if (!digitsOnly.equals(newValue)) {
                    searchStudentField.setText(digitsOnly);
                }

                hideFoundStudentInfo();
            });
        }
    }

    /**
     * Obsługuje kliknięcie przycisku wyszukiwania studenta.
     *
     * <p>Metoda wykonuje wyszukiwanie studenta na serwerze na podstawie numeru indeksu
     * wprowadzonego w polu {@link #searchStudentField}. Proces obejmuje walidację danych,
     * komunikację z serwerem, przetwarzanie wyników i aktualizację interfejsu użytkownika.</p>
     *
     * <p>Proces wyszukiwania:</p>
     * <ol>
     *   <li><strong>Walidacja danych wejściowych</strong>
     *       <ul>
     *         <li>Sprawdzenie czy pole nie jest puste</li>
     *         <li>Weryfikacja formatu (dokładnie 6 cyfr)</li>
     *         <li>Wyświetlenie alertu w przypadku błędnych danych</li>
     *       </ul>
     *   </li>
     *   <li><strong>Aktualizacja stanu przycisków</strong>
     *       <ul>
     *         <li>Zmiana tekstu na "Szukam..."</li>
     *         <li>Wyłączenie przycisku {@link #searchStudentButton}</li>
     *       </ul>
     *   </li>
     *   <li><strong>Komunikacja z serwerem</strong>
     *       <ul>
     *         <li>Wywołanie {@link StudentService#getAllStudentsAsync()}</li>
     *         <li>Filtrowanie wyników po numerze indeksu</li>
     *         <li>Asynchroniczne przetwarzanie odpowiedzi</li>
     *       </ul>
     *   </li>
     *   <li><strong>Przetwarzanie wyników</strong>
     *       <ul>
     *         <li>Student znaleziony: wywołanie {@link #showFoundStudent(Student)}</li>
     *         <li>Student nie znaleziony: wyświetlenie odpowiedniego komunikatu</li>
     *         <li>Błąd serwera: wyświetlenie alertu z informacją o błędzie</li>
     *       </ul>
     *   </li>
     *   <li><strong>Przywrócenie stanu interfejsu</strong>
     *       <ul>
     *         <li>Przywrócenie oryginalnego tekstu przycisku</li>
     *         <li>Ponowne włączenie przycisku</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Obsługiwane scenariusze:</p>
     * <ul>
     *   <li><strong>Sukces</strong> - student znaleziony i wyświetlony</li>
     *   <li><strong>Brak wyników</strong> - student o podanym indeksie nie istnieje</li>
     *   <li><strong>Błąd sieci</strong> - problem z połączeniem do serwera</li>
     *   <li><strong>Błąd serwera</strong> - problem po stronie backend API</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * // Użytkownik wpisuje "123456" i klika "Znajdź studenta"
     * // System:
     * // 1. Waliduje format (6 cyfr) ✓
     * // 2. Wyłącza przycisk i zmienia tekst na "Szukam..."
     * // 3. Wysyła żądanie na serwer
     * // 4. Jeśli znaleziono - wywołuje showFoundStudent()
     * // 5. Przywraca stan przycisku
     * }
     * </pre>
     *
     * @see #showFoundStudent(Student)
     * @see #hideFoundStudentInfo()
     * @see StudentService#getAllStudentsAsync()
     */
    @FXML
    protected void onSearchStudentClick() {
        String indexNumber = searchStudentField.getText().trim();

        if (indexNumber.isEmpty()) {
            showAlert("Błąd", "Wpisz numer indeksu studenta!", Alert.AlertType.WARNING);
            return;
        }

        if (!indexNumber.matches("\\d{6}")) {
            showAlert("Błąd", "Numer indeksu musi składać się z 6 cyfr!", Alert.AlertType.WARNING);
            return;
        }

        searchStudentButton.setText("Szukam...");
        searchStudentButton.setDisable(true);

        studentService.getAllStudentsAsync()
                .thenAccept(allStudents -> {
                    javafx.application.Platform.runLater(() -> {
                        searchStudentButton.setText("🔍 Znajdź studenta");
                        searchStudentButton.setDisable(false);

                        Student foundStudent = allStudents.stream()
                                .filter(s -> s.getIndexNumber().equals(indexNumber))
                                .findFirst()
                                .orElse(null);

                        if (foundStudent != null) {
                            showFoundStudent(foundStudent);
                        } else {
                            hideFoundStudentInfo();
                            showAlert("Student nie znaleziony",
                                    "Nie znaleziono studenta o numerze indeksu: " + indexNumber,
                                    Alert.AlertType.INFORMATION);
                        }
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        searchStudentButton.setText("🔍 Znajdź studenta");
                        searchStudentButton.setDisable(false);
                        hideFoundStudentInfo();
                        showAlert("Błąd", "Nie udało się wyszukać studenta: " + throwable.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Wyświetla szczegóły znalezionego studenta w interfejsie użytkownika.
     *
     * <p>Metoda aktywuje i wypełnia sekcję {@link #foundStudentInfo} danymi
     * znalezionego studenta. Ustawia także referencję {@link #currentFoundStudent}
     * używaną przez przyciski akcji (edycja, usuwanie).</p>
     *
     * <p>Wyświetlane informacje:</p>
     * <ul>
     *   <li><strong>Imię i nazwisko</strong> - {@link #foundStudentNameLabel}
     *       <ul><li>Format: "👤 [Imię Nazwisko]"</li></ul>
     *   </li>
     *   <li><strong>Grupa</strong> - {@link #foundStudentGroupLabel}
     *       <ul>
     *         <li>Format z grupą: "🏫 Grupa: [nazwa_grupy]"</li>
     *         <li>Format bez grupy: "🏫 Grupa: Brak"</li>
     *       </ul>
     *   </li>
     *   <li><strong>Data dodania</strong> - {@link #foundStudentDateLabel}
     *       <ul><li>Format: "📅 Dodano: [dd.MM.yyyy HH:mm]"</li></ul>
     *   </li>
     * </ul>
     *
     * <p>Operacje wykonywane przez metodę:</p>
     * <ol>
     *   <li>Zapisanie referencji studenta w {@link #currentFoundStudent}</li>
     *   <li>Wypełnienie etykiet danymi studenta</li>
     *   <li>Ustawienie kontenerem jako widoczny (visible=true, managed=true)</li>
     *   <li>Aktywacja przycisków akcji ({@link #editFoundStudentButton}, {@link #removeFoundStudentButton})</li>
     * </ol>
     *
     * <p>Przykład wyświetlanych danych:</p>
     * <pre>
     * {@code
     * 👤 Jan Kowalski
     * 🏫 Grupa: INF-2024
     * 📅 Dodano: 15.03.2024 14:30
     * [Edytuj] [Usuń]
     * }
     * </pre>
     *
     * @param student znaleziony student do wyświetlenia (nie może być null)
     *
     * @see #hideFoundStudentInfo()
     * @see #onEditFoundStudentClick()
     * @see #onRemoveFoundStudentClick()
     * @see Student#getFullName()
     * @see Student#getGroupName()
     * @see Student#getFormattedDate()
     */
    private void showFoundStudent(Student student) {
        currentFoundStudent = student;

        if (foundStudentInfo != null) {
            foundStudentNameLabel.setText("👤 " + student.getFullName());
            foundStudentGroupLabel.setText("🏫 Grupa: " + (student.getGroupName() != null ? student.getGroupName() : "Brak"));
            foundStudentDateLabel.setText("📅 Dodano: " + student.getFormattedDate());

            foundStudentInfo.setVisible(true);
            foundStudentInfo.setManaged(true);
        }
    }

    /**
     * Ukrywa sekcję informacji o znalezionym studencie.
     *
     * <p>Metoda czyści referencję {@link #currentFoundStudent} i ukrywa
     * kontener {@link #foundStudentInfo} w interfejsie użytkownika.
     * Wywoływana gdy użytkownik rozpoczyna nowe wyszukiwanie lub
     * gdy nie znaleziono studenta.</p>
     *
     * <p>Operacje wykonywane:</p>
     * <ul>
     *   <li>Ustawienie {@link #currentFoundStudent} na null</li>
     *   <li>Ustawienie kontenera jako niewidoczny (visible=false)</li>
     *   <li>Wyłączenie zarządzania layoutem (managed=false)</li>
     *   <li>Dezaktywacja przycisków akcji</li>
     * </ul>
     *
     * <p>Metoda jest wywoływana w następujących sytuacjach:</p>
     * <ul>
     *   <li>Użytkownik zmienia tekst w polu wyszukiwania</li>
     *   <li>Nie znaleziono studenta o podanym indeksie</li>
     *   <li>Wystąpił błąd podczas wyszukiwania</li>
     *   <li>Student został usunięty z systemu</li>
     * </ul>
     *
     * @see #showFoundStudent(Student)
     * @see #setupSearchValidation()
     */
    private void hideFoundStudentInfo() {
        currentFoundStudent = null;
        if (foundStudentInfo != null) {
            foundStudentInfo.setVisible(false);
            foundStudentInfo.setManaged(false);
        }
    }

    /**
     * Obsługuje kliknięcie przycisku edycji znalezionego studenta.
     *
     * <p>Obecnie wyświetla dialog informacyjny o planowanej implementacji
     * funkcjonalności edycji studentów w przyszłej wersji aplikacji.</p>
     *
     * <p><strong>Planowana funkcjonalność:</strong></p>
     * <ul>
     *   <li>Dialog edycji z polami: imię, nazwisko, grupa</li>
     *   <li>Walidacja zmian</li>
     *   <li>Aktualizacja danych na serwerze</li>
     *   <li>Odświeżenie wyświetlanych informacji</li>
     * </ul>
     *
     * <p>Metoda jest aktywna tylko gdy {@link #currentFoundStudent} != null.</p>
     *
     * @see #showFoundStudent(Student)
     * @see #currentFoundStudent
     */
    @FXML
    protected void onEditFoundStudentClick() {
        if (currentFoundStudent != null) {
            // Otwórz dialog edycji studenta
            openEditStudentDialog(currentFoundStudent);
        }
    }

    /**
     * Obsługuje kliknięcie przycisku usuwania znalezionego studenta.
     *
     * <p>Uruchamia zaawansowany dialog usuwania studenta z dodatkowymi polami
     * do wprowadzenia powodu i uwag. Metoda wywołuje {@link #performAdvancedStudentRemoval(Student)}
     * dla aktualnie wybranego studenta.</p>
     *
     * <p>Funkcjonalność dostępna tylko gdy {@link #currentFoundStudent} != null.</p>
     *
     * @see #performAdvancedStudentRemoval(Student)
     * @see #currentFoundStudent
     */
    @FXML
    protected void onRemoveFoundStudentClick() {
        if (currentFoundStudent != null) {
            // *** TU UŻYWAMY NOWEJ FUNKCJI USUWANIA Z DODATKOWYMI POLAMI ***
            performAdvancedStudentRemoval(currentFoundStudent);
        }
    }

    /**
     * Obsługuje kliknięcie przycisku odświeżania wszystkich danych studentów.
     *
     * <p>Aktualizuje statystyki studentów i listę ostatnio dodanych studentów
     * danymi pobranymi z serwera. Wywoływana przez użytkownika lub automatycznie
     * po zmianach w danych.</p>
     *
     * <p>Operacje wykonywane:</p>
     * <ul>
     *   <li>Wywołanie {@link #loadAllStudentsCount()} - aktualizacja liczników</li>
     *   <li>Wywołanie {@link #loadRecentStudents()} - odświeżenie listy ostatnich</li>
     * </ul>
     *
     * @see #loadAllStudentsCount()
     * @see #loadRecentStudents()
     */
    @FXML
    protected void onRefreshAllStudentsClick() {
        loadAllStudentsCount();
        loadRecentStudents();
    }

    /**
     * Obsługuje kliknięcie przycisku zarządzania wszystkimi studentami.
     *
     * <p>Obecnie wyświetla dialog informacyjny o planowanej implementacji
     * pełnego panelu zarządzania studentami w przyszłej wersji aplikacji.</p>
     *
     * <p><strong>Planowana funkcjonalność:</strong></p>
     * <ul>
     *   <li>Tabela ze wszystkimi studentami w systemie</li>
     *   <li>Zaawansowane filtrowanie i sortowanie</li>
     *   <li>Masowe operacje (przypisywanie do grup, eksport)</li>
     *   <li>Szczegółowy widok historii studenta</li>
     * </ul>
     */
    @FXML
    protected void onManageAllStudentsClick() {
        openFullStudentManagementWindow();
    }

    /**
     * Ładuje i wyświetla szczegółowe statystyki wszystkich studentów z serwera.
     *
     * <p>Metoda pobiera wszystkich studentów z serwera, oblicza statystyki
     * dotyczące przypisania do grup i aktualizuje {@link #allStudentsCountLabel}.</p>
     *
     * <p>Obliczane statystyki:</p>
     * <ul>
     *   <li><strong>Łączna liczba studentów</strong> - wszystkie rekordy w systemie</li>
     *   <li><strong>Studenci z grupą</strong> - mają przypisaną grupę (!= null)</li>
     *   <li><strong>Studenci bez grupy</strong> - dostępni do przypisania (== null)</li>
     * </ul>
     *
     * <p>Format wyświetlanego tekstu:</p>
     * <pre>
     * "Wszystkich studentów: X (z grupą: Y, bez grupy: Z)"
     * </pre>
     *
     * <p>Obsługa błędów:</p>
     * <ul>
     *   <li><strong>Sukces</strong> - zielony kolor tekstu, normalne statystyki</li>
     *   <li><strong>Błąd</strong> - czerwony kolor, komunikat "Błąd ładowania liczby studentów"</li>
     * </ul>
     *
     * <p>Przykład działania:</p>
     * <pre>
     * {@code
     * // Serwer zwraca 50 studentów
     * // 35 ma przypisaną grupę, 15 bez grupy
     * // Wyświetli: "Wszystkich studentów: 50 (z grupą: 35, bez grupy: 15)"
     * }
     * </pre>
     *
     * @see StudentService#getAllStudentsAsync()
     * @see #allStudentsCountLabel
     */
    private void loadAllStudentsCount() {
        studentService.getAllStudentsAsync()
                .thenAccept(allStudents -> {
                    javafx.application.Platform.runLater(() -> {
                        if (allStudentsCountLabel != null) {
                            long withGroup = allStudents.stream().filter(s -> s.getGroupName() != null && !s.getGroupName().trim().isEmpty()).count();
                            long withoutGroup = allStudents.size() - withGroup;

                            allStudentsCountLabel.setText(String.format("Wszystkich studentów: %d (z grupą: %d, bez grupy: %d)",
                                    allStudents.size(), withGroup, withoutGroup));
                            allStudentsCountLabel.setStyle("-fx-text-fill: #38A169;");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        if (allStudentsCountLabel != null) {
                            allStudentsCountLabel.setText("Błąd ładowania liczby studentów");
                            allStudentsCountLabel.setStyle("-fx-text-fill: #E53E3E;");
                        }
                    });
                    return null;
                });
    }

    /**
     * Ładuje i wyświetla listę ostatnio dodanych studentów.
     *
     * <p>Metoda pobiera wszystkich studentów z serwera, sortuje ich według daty
     * dodania (od najnowszych) i wyświetla maksymalnie 5 ostatnich w liście
     * {@link #recentStudentsListView}.</p>
     *
     * <p>Proces ładowania:</p>
     * <ol>
     *   <li>Pobranie wszystkich studentów przez {@link StudentService#getAllStudentsAsync()}</li>
     *   <li>Sortowanie według {@link Student#getAddedDate()} (DESC)</li>
     *   <li>Ograniczenie do 5 najnowszych rekordów</li>
     *   <li>Aktualizacja listy {@link #recentStudents}</li>
     *   <li>Automatyczne odświeżenie ListView</li>
     * </ol>
     *
     * <p>Każdy element listy jest wyświetlany przez {@link RecentStudentListCell}
     * zawierającą:</p>
     * <ul>
     *   <li>Imię i nazwisko studenta</li>
     *   <li>Numer indeksu</li>
     *   <li>Nazwę grupy lub "Brak grupy"</li>
     * </ul>
     *
     * <p>Obsługa błędów:</p>
     * <ul>
     *   <li>W przypadku błędu sieciowego, lista pozostaje pusta</li>
     *   <li>Błędy są logowane do konsoli</li>
     *   <li>Brak wyświetlania alertów (operacja w tle)</li>
     * </ul>
     *
     * @see StudentService#getAllStudentsAsync()
     * @see Student#getAddedDate()
     * @see RecentStudentListCell
     * @see #recentStudentsListView
     */
    private void loadRecentStudents() {
        studentService.getAllStudentsAsync()
                .thenAccept(allStudents -> {
                    javafx.application.Platform.runLater(() -> {
                        // Posortuj po dacie dodania i weź 5 ostatnich
                        List<Student> recent = allStudents.stream()
                                .sorted((s1, s2) -> s2.getAddedDate().compareTo(s1.getAddedDate()))
                                .limit(5)
                                .collect(java.util.stream.Collectors.toList());

                        recentStudents.clear();
                        recentStudents.addAll(recent);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("Błąd ładowania ostatnich studentów: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Wykonuje zaawansowane usuwanie studenta z dodatkowymi polami informacyjnymi.
     *
     * <p>Główna metoda implementująca zaawansowany dialog usuwania studenta z systemu.
     * Dialog zawiera dodatkowe pola do wprowadzenia powodu usunięcia, uwag oraz
     * checkbox potwierdzenia. Metoda jest dostosowana do użycia z głównego ekranu
     * aplikacji.</p>
     *
     * <p>Struktura dialogu:</p>
     * <ol>
     *   <li><strong>Header</strong> - tytuł "Usuwanie studenta z systemu"</li>
     *   <li><strong>Informacje o studencie</strong> - imię, nazwisko, indeks, grupa</li>
     *   <li><strong>Sekcja powodów</strong>
     *       <ul>
     *         <li>ComboBox z predefiniowanymi powodami usunięcia</li>
     *         <li>TextArea na dodatkowe uwagi</li>
     *       </ul>
     *   </li>
     *   <li><strong>Checkbox potwierdzenia</strong> - wymagany do aktywacji przycisku</li>
     *   <li><strong>Ostrzeżenia</strong> - informacje o konsekwencjach usunięcia</li>
     * </ol>
     *
     * <p>Predefiniowane powody usunięcia:</p>
     * <ul>
     *   <li>"Zakończenie studiów" - naturalny koniec nauki</li>
     *   <li>"Rezygnacja" - student zrezygnował ze studiów</li>
     *   <li>"Przeniesienie na inne uczelnie" - transfer zewnętrzny</li>
     *   <li>"Błąd w systemie" - nieprawidłowe dane</li>
     *   <li>"Duplikat" - duplikowanie rekordów</li>
     *   <li>"Nieaktywność" - długotrwała nieaktywność</li>
     *   <li>"Inne" - inne powody nie wymienione powyżej</li>
     * </ul>
     *
     * <p>Konsekwencje usunięcia (wyświetlane w ostrzeżeniu):</p>
     * <ul>
     *   <li>Student zostanie usunięty ze WSZYSTKICH grup</li>
     *   <li>WSZYSTKIE dane frekwencji zostaną utracone</li>
     *   <li>Operacja jest NIEODWRACALNA</li>
     * </ul>
     *
     * <p>Walidacja dialogu:</p>
     * <ul>
     *   <li>Przycisk "Usuń całkowicie" jest aktywny tylko gdy checkbox jest zaznaczony</li>
     *   <li>Powód i uwagi są opcjonalne</li>
     *   <li>Anulowanie nie wykonuje żadnych operacji</li>
     * </ul>
     *
     * <p>Po potwierdzeniu wywoływana jest metoda {@link #executeStudentRemovalFromSystem(Student, String, String)}.</p>
     *
     * @param student student do usunięcia (nie może być null)
     *
     * @see #executeStudentRemovalFromSystem(Student, String, String)
     * @see #onRemoveFoundStudentClick()
     */
    private void performAdvancedStudentRemoval(Student student) {
        Dialog<ButtonType> confirmDialog = new Dialog<>();
        confirmDialog.setTitle("Usuwanie studenta z systemu");
        confirmDialog.setHeaderText("Czy na pewno chcesz usunąć studenta " + student.getFullName() + " z całego systemu?");

        ButtonType removeButtonType = new ButtonType("Usuń całkowicie", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getDialogPane().getButtonTypes().addAll(removeButtonType, cancelButtonType);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        VBox studentInfo = new VBox(8);
        studentInfo.setStyle("-fx-background-color: rgba(220, 20, 60, 0.05); " +
                "-fx-padding: 15; -fx-background-radius: 10; " +
                "-fx-border-color: rgba(220, 20, 60, 0.2); " +
                "-fx-border-width: 1; -fx-border-radius: 10;");

        Label nameLabel = new Label("👤 Student: " + student.getFullName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #DC143C;");

        Label indexLabel = new Label("🆔 Nr indeksu: " + student.getIndexNumber());
        indexLabel.setStyle("-fx-font-size: 12px;");

        Label groupLabel = new Label("🏫 Grupa: " + (student.getGroupName() != null ? student.getGroupName() : "Brak"));
        groupLabel.setStyle("-fx-font-size: 12px;");

        studentInfo.getChildren().addAll(nameLabel, indexLabel, groupLabel);

        VBox reasonSection = new VBox(8);
        Label reasonLabel = new Label("📝 Powód usunięcia:");
        reasonLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll(
                "Zakończenie studiów", "Rezygnacja", "Przeniesienie na inne uczelnie",
                "Błąd w systemie", "Duplikat", "Nieaktywność", "Inne"
        );
        reasonCombo.setPromptText("Wybierz powód...");
        reasonCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Dodatkowe uwagi...");
        notesArea.setPrefRowCount(2);
        notesArea.setMaxHeight(60);

        reasonSection.getChildren().addAll(reasonLabel, reasonCombo,
                new Label("💬 Uwagi:"), notesArea);

        CheckBox confirmBox = new CheckBox("Potwierdzam całkowite usunięcie studenta z systemu");
        confirmBox.setStyle("-fx-font-weight: bold;");

        VBox warningBox = new VBox(5);
        warningBox.setStyle("-fx-background-color: rgba(229, 62, 62, 0.1); " +
                "-fx-padding: 12; -fx-background-radius: 8; " +
                "-fx-border-color: rgba(229, 62, 62, 0.3); " +
                "-fx-border-width: 1; -fx-border-radius: 8;");

        Label warningTitle = new Label("⚠️ OSTRZEŻENIE:");
        warningTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #E53E3E;");

        Label warning1 = new Label("• Student zostanie usunięty ze WSZYSTKICH grup");
        Label warning2 = new Label("• WSZYSTKIE dane frekwencji zostaną utracone");
        Label warning3 = new Label("• Ta operacja jest NIEODWRACALNA!");

        warningBox.getChildren().addAll(warningTitle, warning1, warning2, warning3);

        content.getChildren().addAll(studentInfo, reasonSection, confirmBox, warningBox);
        confirmDialog.getDialogPane().setContent(content);

        confirmDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm());
        confirmDialog.getDialogPane().getStyleClass().add("alert-dialog");

        javafx.scene.Node removeButton = confirmDialog.getDialogPane().lookupButton(removeButtonType);
        removeButton.setDisable(true);
        confirmBox.selectedProperty().addListener((obs, was, is) -> removeButton.setDisable(!is));

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == removeButtonType) {
            String reason = reasonCombo.getValue();
            String notes = notesArea.getText().trim();

            executeStudentRemovalFromSystem(student, reason, notes);
        }
    }

    /**
     * Wykonuje właściwe usunięcie studenta z systemu po potwierdzeniu w dialogu.
     *
     * <p>Metoda realizuje ostateczną operację usunięcia studenta z serwera
     * oraz aktualizuje interfejs użytkownika. Dodatkowo loguje operację
     * z wprowadzonymi powodami i uwagami.</p>
     *
     * <p>Proces usuwania:</p>
     * <ol>
     *   <li><strong>Wywołanie serwera</strong> - {@link StudentService#deleteStudentAsync(String)}</li>
     *   <li><strong>Aktualizacja interfejsu</strong> przy sukcesie:
     *       <ul>
     *         <li>Odświeżenie liczników studentów</li>
     *         <li>Odświeżenie listy ostatnich studentów</li>
     *         <li>Ukrycie informacji o usuniętym studencie</li>
     *         <li>Wyczyszczenie pola wyszukiwania</li>
     *       </ul>
     *   </li>
     *   <li><strong>Wyświetlenie potwierdzenia</strong> z podanymi powodami</li>
     *   <li><strong>Logowanie operacji</strong> do konsoli z pełnymi szczegółami</li>
     * </ol>
     *
     * <p>Obsługa błędów:</p>
     * <ul>
     *   <li>Przy błędzie serwera nadal aktualizuje interfejs lokalnie</li>
     *   <li>Wyświetla ostrzeżenie o problemie z serwerem</li>
     *   <li>Kontynuuje odświeżanie danych mimo błędu</li>
     * </ul>
     *
     * <p>Format komunikatu sukcesu:</p>
     * <pre>
     * {@code
     * "✅ Student [imię nazwisko] został usunięty z systemu!
     * 📝 Powód: [wybrany powód]
     * 💬 Uwagi: [wprowadzone uwagi]"
     * }
     * </pre>
     *
     * <p>Logowanie do konsoli zawiera:</p>
     * <ul>
     *   <li>Nagłówek "USUNIĘCIE STUDENTA Z GŁÓWNEGO EKRANU"</li>
     *   <li>Pełne dane studenta (imię, nazwisko, indeks)</li>
     *   <li>Powód usunięcia (jeśli podany)</li>
     *   <li>Uwagi (jeśli podane)</li>
     *   <li>Dokładną datę i czas operacji</li>
     * </ul>
     *
     * @param student student do usunięcia z systemu
     * @param reason powód usunięcia (może być null)
     * @param notes dodatkowe uwagi (może być null lub pusty)
     *
     * @see StudentService#deleteStudentAsync(String)
     * @see #loadAllStudentsCount()
     * @see #loadRecentStudents()
     * @see #hideFoundStudentInfo()
     */
    private void executeStudentRemovalFromSystem(Student student, String reason, String notes) {
        studentService.deleteStudentAsync(student.getIndexNumber())
                .thenAccept(success -> {
                    javafx.application.Platform.runLater(() -> {
                        // Odśwież listy
                        loadAllStudentsCount();
                        loadRecentStudents();
                        hideFoundStudentInfo();
                        searchStudentField.clear();

                        StringBuilder message = new StringBuilder();
                        message.append("✅ Student ").append(student.getFullName()).append(" został usunięty z systemu!");
                        if (reason != null) message.append("\n📝 Powód: ").append(reason);
                        if (!notes.isEmpty()) message.append("\n💬 Uwagi: ").append(notes);

                        showAlert("Student usunięty", message.toString(), Alert.AlertType.INFORMATION);

                        // Log
                        System.out.println("=== USUNIĘCIE STUDENTA Z GŁÓWNEGO EKRANU ===");
                        System.out.println("Student: " + student.getFullName());
                        System.out.println("Nr indeksu: " + student.getIndexNumber());
                        if (reason != null) System.out.println("Powód: " + reason);
                        if (!notes.isEmpty()) System.out.println("Uwagi: " + notes);
                        System.out.println("Data: " + java.time.LocalDateTime.now());
                        System.out.println("============================================");
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        showAlert("Błąd", "Nie udało się usunąć studenta: " + throwable.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Placeholder dla funkcjonalności edycji studenta (przyszła implementacja).
     *
     * <p>Obecnie wyświetla komunikat informacyjny o planowanej implementacji.</p>
     * <p>W przyszłości otworzy dialog edycji z możliwością zmiany danych studenta.</p>
     *
     * @param student student do edycji
     */
    private void openEditStudentDialog(Student student) {
        showAlert("Info", "Funkcja edycji studenta będzie dostępna w przyszłej wersji.", Alert.AlertType.INFORMATION);
    }

    /**
     * Placeholder dla pełnego panelu zarządzania studentami (przyszła implementacja).
     *
     * <p>Obecnie wyświetla komunikat informacyjny o planowanej implementacji.</p>
     * <p>W przyszłości otworzy okno z pełną tabelą wszystkich studentów.</p>
     */
    private void openFullStudentManagementWindow() {
        showAlert("Info", "Pełny panel zarządzania studentami będzie dostępny w przyszłej wersji.", Alert.AlertType.INFORMATION);
    }

    /**
     * Komórka listy do wyświetlania ostatnio dodanych studentów.
     *
     * <p>Custom ListCell implementująca wyświetlanie studenta w kompaktowym formacie
     * w liście {@link #recentStudentsListView}. Każda komórka zawiera:</p>
     * <ul>
     *   <li>Ikonę 👤 i pełne imię i nazwisko (pogrubione, czerwone)</li>
     *   <li>Numer indeksu i grupę lub "Brak grupy" (szare, mniejsze)</li>
     * </ul>
     *
     * <p>Stylizacja:</p>
     * <ul>
     *   <li>Nazwa studenta: font-weight: bold, kolor: #DC143C, rozmiar: 12px</li>
     *   <li>Szczegóły: kolor: #6C757D, rozmiar: 10px</li>
     *   <li>Odstępy między elementami: 2px</li>
     * </ul>
     *
     * @see #recentStudentsListView
     * @see #loadRecentStudents()
     */
    private class RecentStudentListCell extends ListCell<Student> {
        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);
            if (empty || student == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox content = new VBox(2);

                Label nameLabel = new Label("👤 " + student.getFullName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #DC143C;");

                Label detailsLabel = new Label("🆔 " + student.getIndexNumber() +
                        " | 🏫 " + (student.getGroupName() != null ? student.getGroupName() : "Brak grupy"));
                detailsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6C757D;");

                content.getChildren().addAll(nameLabel, detailsLabel);
                setGraphic(content);
                setText(null);
            }
        }
    }


    /**
     * Obsługuje kliknięcie przycisku dodawania nowej grupy.
     *
     * <p>Metoda wykonuje pełny proces dodawania grupy: walidację danych,
     * komunikację z serwerem, aktualizację interfejsu i obsługę błędów.
     * Implementuje mechanizm graceful degradation - przy problemach z serwerem
     * grupa jest dodawana lokalnie z odpowiednim ostrzeżeniem.</p>
     *
     * <p>Proces dodawania grupy:</p>
     * <ol>
     *   <li><strong>Walidacja danych wejściowych</strong>
     *       <ul>
     *         <li>Sprawdzenie czy pola nazwy i specjalizacji nie są puste</li>
     *         <li>Weryfikacja unikalności nazwy grupy w lokalnej liście</li>
     *       </ul>
     *   </li>
     *   <li><strong>Przygotowanie obiektu grupy</strong>
     *       <ul>
     *         <li>Utworzenie instancji {@link Group} z wprowadzonymi danymi</li>
     *         <li>Automatyczne ustawienie daty utworzenia</li>
     *       </ul>
     *   </li>
     *   <li><strong>Komunikacja z serwerem</strong>
     *       <ul>
     *         <li>Wyłączenie przycisku i zmiana tekstu na "Dodawanie..."</li>
     *         <li>Wywołanie {@link GroupService#addGroupAsync(Group)}</li>
     *         <li>Asynchroniczne przetwarzanie odpowiedzi</li>
     *       </ul>
     *   </li>
     *   <li><strong>Obsługa rezultatu</strong>
     *       <ul>
     *         <li>Sukces: dodanie do lokalnej listy, animacja, czyszczenie pól</li>
     *         <li>Błąd duplikatu: wyświetlenie odpowiedniego ostrzeżenia</li>
     *         <li>Inny błąd: dodanie lokalne z ostrzeżeniem o problemie z serwerem</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Walidacja danych:</p>
     * <ul>
     *   <li>Nazwa grupy nie może być pusta</li>
     *   <li>Specjalizacja nie może być pusta</li>
     *   <li>Nazwa musi być unikalna w lokalnej liście (case-insensitive)</li>
     * </ul>
     *
     * <p>Obsługiwane błędy:</p>
     * <ul>
     *   <li>{@link GroupService.GroupAlreadyExistsException} - grupa o takiej nazwie już istnieje na serwerze</li>
     *   <li>Błędy sieciowe - problemy z połączeniem</li>
     *   <li>Błędy serwera - problemy po stronie backend</li>
     * </ul>
     *
     * <p>Aktualizacja interfejsu po sukcesie:</p>
     * <ul>
     *   <li>Dodanie grupy do {@link #groups} (automatyczne odświeżenie ListView)</li>
     *   <li>Animacja przycisku przez {@link #animateButton(Button)}</li>
     *   <li>Wyczyszczenie pól formularza</li>
     *   <li>Aktualizacja licznika grup</li>
     *   <li>Wyświetlenie komunikatu sukcesu</li>
     * </ul>
     *
     * @see GroupService#addGroupAsync(Group)
     * @see GroupService.GroupAlreadyExistsException
     * @see #animateButton(Button)
     * @see #updateGroupCount()
     */
    @FXML
    protected void onAddGroupClick() {
        String groupName = groupNameField.getText().trim();
        String specialization = specializationField.getText().trim();

        if (groupName.isEmpty() || specialization.isEmpty()) {
            showAlert("Błąd", "Wszystkie pola muszą być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        boolean groupExists = groups.stream().anyMatch(g -> g.getName().equalsIgnoreCase(groupName));
        if (groupExists) {
            showAlert("Błąd", "Grupa o nazwie '" + groupName + "' już istnieje w lokalnej liście!", Alert.AlertType.WARNING);
            return;
        }

        Group newGroup = new Group(groupName, specialization);

        addGroupButton.setDisable(true);
        addGroupButton.setText("Dodawanie...");

        groupService.addGroupAsync(newGroup)
                .thenAccept(savedGroup -> {
                    javafx.application.Platform.runLater(() -> {
                        addGroupButton.setDisable(false);
                        addGroupButton.setText("Dodaj grupę");

                        groups.add(newGroup);
                        animateButton(addGroupButton);

                        groupNameField.clear();
                        specializationField.clear();

                        updateGroupCount();

                        showAlert("Sukces", "Grupa '" + groupName + "' została dodana na serwer!", Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        addGroupButton.setDisable(false);
                        addGroupButton.setText("Dodaj grupę");

                        if (throwable.getCause() instanceof GroupService.GroupAlreadyExistsException) {
                            showAlert("Grupa już istnieje",
                                    throwable.getCause().getMessage() +
                                            "\nSprawdź nazwę grupy i spróbuj ponownie z inną nazwą.",
                                    Alert.AlertType.WARNING);
                        } else {
                            groups.add(newGroup);
                            animateButton(addGroupButton);

                            groupNameField.clear();
                            specializationField.clear();

                            updateGroupCount();

                            showAlert("Ostrzeżenie",
                                    "Grupa '" + groupName + "' została dodana lokalnie, ale nie udało się wysłać na serwer:\n" +
                                            throwable.getMessage(),
                                    Alert.AlertType.WARNING);
                        }
                    });
                    return null;
                });
    }

    /**
     * Obsługuje kliknięcie przycisku wejścia do szczegółów wybranej grupy.
     *
     * <p>Metoda otwiera nowe okno z szczegółowym widokiem grupy obsługiwanym
     * przez {@link GroupDetailController}. Okno umożliwia zarządzanie studentami
     * grupy, terminami zajęć oraz frekwencją.</p>
     *
     * <p>Proces otwierania widoku grupy:</p>
     * <ol>
     *   <li>Pobranie wybranej grupy z {@link #groupsListView}</li>
     *   <li>Walidacja selekcji (nie może być null)</li>
     *   <li>Animacja przycisku dla feedbacku użytkownika</li>
     *   <li>Wywołanie {@link #openGroupDetailWindow(Group)}</li>
     * </ol>
     *
     * <p>Przycisk jest aktywny tylko gdy grupa jest wybrana w ListView.</p>
     *
     * @see #openGroupDetailWindow(Group)
     * @see GroupDetailController
     * @see #animateButton(Button)
     */
    @FXML
    protected void onEnterGroupClick() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            animateButton(enterGroupButton);

            // Otwórz nowe okno z detalami grupy
            openGroupDetailWindow(selectedGroup);
        }
    }

    /**
     * Obsługuje kliknięcie przycisku usuwania wybranej grupy.
     *
     * <p>Metoda implementuje bezpieczne usuwanie grupy z dialogiem potwierdzenia
     * i strategią "usuń lokalnie najpierw, potem spróbuj na serwerze". Zapewnia
     * to lepsze doświadczenie użytkownika przy problemach z siecią.</p>
     *
     * <p>Proces usuwania grupy:</p>
     * <ol>
     *   <li><strong>Walidacja selekcji</strong> - sprawdzenie czy grupa jest wybrana</li>
     *   <li><strong>Dialog potwierdzenia</strong>
     *       <ul>
     *         <li>Wyświetlenie szczegółów grupy (nazwa, specjalizacja)</li>
     *         <li>Ostrzeżenie o nieodwracalności operacji</li>
     *         <li>Stylizacja zgodna z motywem aplikacji</li>
     *       </ul>
     *   </li>
     *   <li><strong>Usunięcie lokalne</strong> (po potwierdzeniu)
     *       <ul>
     *         <li>Natychmiastowe usunięcie z {@link #groups}</li>
     *         <li>Aktualizacja licznika grup</li>
     *         <li>Animacja przycisku</li>
     *       </ul>
     *   </li>
     *   <li><strong>Próba usunięcia z serwera</strong> (asynchronicznie w tle)
     *       <ul>
     *         <li>Wywołanie {@link GroupService#deleteGroupAsync(String)}</li>
     *         <li>Wyświetlenie odpowiedniego komunikatu o wyniku</li>
     *         <li>Przywrócenie stanu przycisku</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Strategia "local-first":</p>
     * <ul>
     *   <li>Grupa jest usuwana z lokalnej listy natychmiast po potwierdzeniu</li>
     *   <li>Użytkownik widzi natychmiastowy efekt w interfejsie</li>
     *   <li>Operacja serwera wykonuje się w tle</li>
     *   <li>Komunikaty informują o statusie synchronizacji z serwerem</li>
     * </ul>
     *
     * <p>Obsługiwane scenariusze wyniku:</p>
     * <ul>
     *   <li><strong>Sukces serwera</strong> - "Grupa została usunięta z serwera"</li>
     *   <li><strong>Błąd serwera</strong> - ostrzeżenie z sugestią odświeżenia</li>
     *   <li><strong>Wyjątek</strong> - ostrzeżenie z informacją o problemie</li>
     * </ul>
     *
     * @see GroupService#deleteGroupAsync(String)
     * @see #showAlert(String, String, Alert.AlertType)
     * @see #animateButton(Button)
     * @see #updateGroupCount()
     */
    @FXML
    protected void onDeleteGroupClick() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            // Potwierdzenie usunięcia
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Potwierdzenie usunięcia");
            confirmAlert.setHeaderText("Czy na pewno chcesz usunąć grupę?");
            confirmAlert.setContentText("Grupa: " + selectedGroup.getName() +
                    "\nSpecjalizacja: " + selectedGroup.getSpecialization() +
                    "\n\nTa operacja jest nieodwracalna!");

            confirmAlert.getDialogPane().getStylesheets().add(
                    getClass().getResource("styles.css").toExternalForm());
            confirmAlert.getDialogPane().getStyleClass().add("alert-dialog");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                deleteGroupButton.setDisable(true);
                deleteGroupButton.setText("Usuwanie...");

                groups.remove(selectedGroup);
                updateGroupCount();
                animateButton(deleteGroupButton);


                groupService.deleteGroupAsync(selectedGroup.getName())
                        .thenAccept(success -> {
                            javafx.application.Platform.runLater(() -> {
                                deleteGroupButton.setDisable(false);
                                deleteGroupButton.setText("Usuń grupę");

                                if (success) {

                                    showAlert("Sukces",
                                            "Grupa '" + selectedGroup.getName() + "' została usunięta z serwera.",
                                            Alert.AlertType.INFORMATION);
                                } else {
                                    showAlert("Ostrzeżenie",
                                            "Grupa '" + selectedGroup.getName() + "' została usunięta lokalnie, " +
                                                    "ale może nadal istnieć na serwerze. Odśwież listę aby sprawdzić.",
                                            Alert.AlertType.WARNING);
                                }
                            });
                        })
                        .exceptionally(throwable -> {
                            javafx.application.Platform.runLater(() -> {
                                deleteGroupButton.setDisable(false);
                                deleteGroupButton.setText("Usuń grupę");

                                showAlert("Ostrzeżenie",
                                        "Grupa '" + selectedGroup.getName() + "' została usunięta lokalnie, " +
                                                "ale wystąpił problem z serwerem:\n" + throwable.getMessage() +
                                                "\n\nOdśwież listę aby sprawdzić stan na serwerze.",
                                        Alert.AlertType.WARNING);
                            });
                            return null;
                        });
            }
        }
    }

    /**
     * Metoda diagnostyczna do analizy API zarządzania grupami.
     *
     * <p>Zaawansowana funkcja diagnostyczna testująca różne aspekty komunikacji
     * z serwerem API. Wykonuje serie testów sprawdzających połączenie, pobieranie
     * danych oraz możliwości usuwania grup. Wyniki są wyświetlane w konsoli
     * z szczegółowymi informacjami diagnostycznymi.</p>
     *
     * <p>Przeprowadzane testy:</p>
     * <ol>
     *   <li><strong>Test połączenia</strong>
     *       <ul>
     *         <li>Wywołanie {@link GroupService#checkServerConnection()}</li>
     *         <li>Weryfikacja dostępności serwera</li>
     *       </ul>
     *   </li>
     *   <li><strong>Test pobierania grup</strong> (jeśli połączenie OK)
     *       <ul>
     *         <li>Wywołanie {@link GroupService#getAllGroupsAsync()}</li>
     *         <li>Wyświetlenie listy dostępnych grup</li>
     *         <li>Uruchomienie testów usuwania dla każdej grupy</li>
     *       </ul>
     *   </li>
     *   <li><strong>Testy usuwania</strong>
     *       <ul>
     *         <li>Dla każdej grupy: wywołanie {@link #testGroupDeletion(String)}</li>
     *         <li>Analiza odpowiedzi różnych endpointów usuwania</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Informacje w konsoli zawierają:</p>
     * <ul>
     *   <li>Status połączenia z serwerem (OK/BŁĄD)</li>
     *   <li>Liczbę grup dostępnych na serwerze</li>
     *   <li>Listę wszystkich grup z nazwami i specjalizacjami</li>
     *   <li>Wyniki testów usuwania dla każdej grupy</li>
     *   <li>Szczegóły błędów w przypadku problemów</li>
     * </ul>
     *
     * <p>Przykład wyjścia do konsoli:</p>
     * <pre>
     * {@code
     * === DIAGNOSTYKA API GRUP ===
     * Połączenie z serwerem: OK
     * Liczba grup na serwerze: 3
     * - Grupa: INF-2024 (Informatyka)
     * - Grupa: MAT-2024 (Matematyka)
     * - Grupa: FIZ-2024 (Fizyka)
     * Testowanie usuwania grupy: INF-2024
     * Test usuwania grupy 'INF-2024': SUKCES
     * ...
     * }
     * </pre>
     *
     * <p>Metoda jest użyteczna do:</p>
     * <ul>
     *   <li>Debugowania problemów z komunikacją API</li>
     *   <li>Weryfikacji dostępności endpointów serwera</li>
     *   <li>Testowania różnych scenariuszy usuwania</li>
     *   <li>Identyfikacji problemów z konfiguracją serwera</li>
     * </ul>
     *
     * @see GroupService#checkServerConnection()
     * @see GroupService#getAllGroupsAsync()
     * @see #testGroupDeletion(String)
     */
    @FXML
    protected void onDiagnoseGroupAPI() {
        System.out.println("=== DIAGNOSTYKA API GRUP ===");

        groupService.checkServerConnection()
                .thenAccept(isConnected -> {
                    System.out.println("Połączenie z serwerem: " + (isConnected ? "OK" : "BŁĄD"));

                    if (isConnected) {
                        groupService.getAllGroupsAsync()
                                .thenAccept(serverGroups -> {
                                    System.out.println("Liczba grup na serwerze: " + serverGroups.size());

                                    for (Group group : serverGroups) {
                                        System.out.println("- Grupa: " + group.getName() + " (" + group.getSpecialization() + ")");

                                        testGroupDeletion(group.getName());
                                    }
                                })
                                .exceptionally(listThrowable -> {
                                    System.err.println("BŁĄD pobierania listy grup: " + listThrowable.getMessage());
                                    return null;
                                });
                    }
                })
                .exceptionally(connectionThrowable -> {
                    System.err.println("BŁĄD połączenia z serwerem: " + connectionThrowable.getMessage());
                    return null;
                });
    }

    /**
     * Testuje możliwość usunięcia konkretnej grupy przez API.
     *
     * <p>Metoda pomocnicza wywoływana przez {@link #onDiagnoseGroupAPI()}
     * do testowania operacji usuwania dla konkretnej grupy. Wykonuje próbę
     * usunięcia i analizuje odpowiedź serwera w celach diagnostycznych.</p>
     *
     * <p><strong>UWAGA:</strong> Ta metoda wykonuje rzeczywiste żądanie usunięcia
     * do serwera. W środowisku produkcyjnym może spowodować faktyczne usunięcie danych!</p>
     *
     * <p>Proces testowania:</p>
     * <ol>
     *   <li>Wywołanie {@link GroupService#deleteGroupAsync(String)}</li>
     *   <li>Analiza wyniku operacji (sukces/niepowodzenie)</li>
     *   <li>Wyświetlenie szczegółów w konsoli</li>
     *   <li>Obsługa i analiza błędów</li>
     * </ol>
     *
     * <p>Informacje diagnostyczne w konsoli:</p>
     * <ul>
     *   <li>Nazwa testowanej grupy</li>
     *   <li>Wynik testu (SUKCES/NIEPOWODZENIE)</li>
     *   <li>Szczegóły błędów jeśli wystąpiły</li>
     *   <li>Informacje o przyczynie błędu</li>
     * </ul>
     *
     * <p>Przykład wyjścia:</p>
     * <pre>
     * {@code
     * Testowanie usuwania grupy: INF-2024
     * Test usuwania grupy 'INF-2024': SUKCES
     *
     * // lub w przypadku błędu:
     * Testowanie usuwania grupy: TEST-GROUP
     * BŁĄD testowania usuwania grupy 'TEST-GROUP': Connection timeout
     * Przyczyna: java.net.SocketTimeoutException: timeout
     * }
     * </pre>
     *
     * @param groupName nazwa grupy do przetestowania (nie może być null)
     *
     * @see #onDiagnoseGroupAPI()
     * @see GroupService#deleteGroupAsync(String)
     */
    private void testGroupDeletion(String groupName) {
        System.out.println("Testowanie usuwania grupy: " + groupName);

        groupService.deleteGroupAsync(groupName)
                .thenAccept(success -> {
                    System.out.println("Test usuwania grupy '" + groupName + "': " + (success ? "SUKCES" : "NIEPOWODZENIE"));
                })
                .exceptionally(throwable -> {
                    System.err.println("BŁĄD testowania usuwania grupy '" + groupName + "': " + throwable.getMessage());

                    // Wyświetl szczegóły błędu
                    if (throwable.getCause() != null) {
                        System.err.println("Przyczyna: " + throwable.getCause().getMessage());
                    }

                    return null;
                });
    }

    /**
     * Obsługuje kliknięcie przycisku odświeżania listy grup z serwera.
     *
     * <p>Metoda pobiera aktualną listę grup z serwera i zastępuje lokalną listę.
     * Operacja jest wykonywana asynchronicznie z odpowiednim feedbackiem dla użytkownika.</p>
     *
     * <p>Proces odświeżania:</p>
     * <ol>
     *   <li>Aktualizacja stanu przycisku (tekst: "Ładowanie...", wyłączony)</li>
     *   <li>Wywołanie {@link GroupService#getAllGroupsAsync()}</li>
     *   <li>Wyczyszczenie i aktualizacja lokalnej listy {@link #groups}</li>
     *   <li>Aktualizacja licznika grup</li>
     *   <li>Przywrócenie stanu przycisku</li>
     *   <li>Wyświetlenie komunikatu o wyniku</li>
     * </ol>
     *
     * <p>Obsługa wyników:</p>
     * <ul>
     *   <li><strong>Sukces</strong> - komunikat o liczbie załadowanych grup</li>
     *   <li><strong>Błąd</strong> - komunikat o problemie z serwerem</li>
     * </ul>
     *
     * @see GroupService#getAllGroupsAsync()
     * @see #loadGroupsFromServer()
     */
    @FXML
    protected void onRefreshClick() {
        loadGroupsFromServer();
    }

    /**
     * Obsługuje kliknięcie przycisku odświeżania licznika studentów.
     *
     * <p>Aktualizuje wyświetlacz {@link #studentCountLabel} najnowszymi danymi
     * z serwera dotyczącymi liczby studentów w systemie.</p>
     *
     * <p>Proces odświeżania:</p>
     * <ol>
     *   <li>Aktualizacja stanu przycisku ("Ładowanie...", wyłączony)</li>
     *   <li>Wywołanie {@link #loadStudentCountFromServer()}</li>
     *   <li>Przywrócenie stanu przycisku po zakończeniu</li>
     * </ol>
     *
     * @see #loadStudentCountFromServer()
     */
    @FXML
    protected void onRefreshStudentsGlobalClick() {
        if (refreshStudentsGlobalButton != null) {
            refreshStudentsGlobalButton.setText("Ładowanie...");
            refreshStudentsGlobalButton.setDisable(true);
        }

        loadStudentCountFromServer();

        if (refreshStudentsGlobalButton != null) {
            javafx.application.Platform.runLater(() -> {
                refreshStudentsGlobalButton.setText("🔄");
                refreshStudentsGlobalButton.setDisable(false);
            });
        }
    }

    /**
     * Obsługuje kliknięcie przycisku dodawania studenta bez grupy.
     *
     * <p>Metoda dodaje nowego studenta do systemu bez przypisywania go do konkretnej grupy.
     * Student będzie dostępny do późniejszego przypisania do grup przez szczegółowe
     * widoki grup. Implementuje pełną walidację danych i komunikację z serwerem.</p>
     *
     * <p>Proces dodawania studenta:</p>
     * <ol>
     *   <li><strong>Walidacja danych wejściowych</strong>
     *       <ul>
     *         <li>Sprawdzenie kompletności pól (imię, nazwisko, indeks)</li>
     *         <li>Walidacja formatu numeru indeksu (dokładnie 6 cyfr)</li>
     *       </ul>
     *   </li>
     *   <li><strong>Przygotowanie obiektu studenta</strong>
     *       <ul>
     *         <li>Utworzenie {@link Student} z groupName = null</li>
     *         <li>Automatyczne ustawienie daty dodania</li>
     *       </ul>
     *   </li>
     *   <li><strong>Komunikacja z serwerem</strong>
     *       <ul>
     *         <li>Aktualizacja stanu przycisku</li>
     *         <li>Wywołanie {@link StudentService#addStudentAsync(Student)}</li>
     *       </ul>
     *   </li>
     *   <li><strong>Aktualizacja interfejsu</strong> (przy sukcesie)
     *       <ul>
     *         <li>Animacja przycisku</li>
     *         <li>Wyczyszczenie formularza</li>
     *         <li>Odświeżenie liczników studentów</li>
     *         <li>Aktualizacja listy ostatnio dodanych</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Walidacja danych:</p>
     * <ul>
     *   <li>Imię: wymagane, nie może być puste</li>
     *   <li>Nazwisko: wymagane, nie może być puste</li>
     *   <li>Numer indeksu: wymagany, dokładnie 6 cyfr</li>
     * </ul>
     *
     * <p>Obsługiwane błędy:</p>
     * <ul>
     *   <li>{@link StudentService.StudentAlreadyExistsException} - duplikat numeru indeksu</li>
     *   <li>Błędy sieciowe i serwera - wyświetlenie odpowiedniego komunikatu</li>
     * </ul>
     *
     * <p>Aktualizacje po dodaniu:</p>
     * <ul>
     *   <li>{@link #loadStudentCountFromServer()} - odświeżenie licznika</li>
     *   <li>{@link #loadAllStudentsCount()} - aktualizacja statystyk</li>
     *   <li>{@link #loadRecentStudents()} - odświeżenie listy ostatnich</li>
     * </ul>
     *
     * @see StudentService#addStudentAsync(Student)
     * @see StudentService.StudentAlreadyExistsException
     * @see #loadStudentCountFromServer()
     * @see #animateButton(Button)
     */
    @FXML
    protected void onAddStudentGlobalClick() {
        String firstName = studentFirstNameField.getText().trim();
        String lastName = studentLastNameField.getText().trim();
        String indexNumber = studentIndexField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || indexNumber.isEmpty()) {
            showAlert("Błąd", "Imię, nazwisko i numer indeksu muszą być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        if (!indexNumber.matches("\\d{6}")) {
            showAlert("Błąd", "Numer indeksu musi składać się z dokładnie 6 cyfr!", Alert.AlertType.WARNING);
            return;
        }

        Student newStudent = new Student(firstName, lastName, indexNumber, null);

        addStudentGlobalButton.setDisable(true);
        addStudentGlobalButton.setText("Dodawanie...");

        studentService.addStudentAsync(newStudent)
                .thenAccept(savedStudent -> {
                    javafx.application.Platform.runLater(() -> {
                        addStudentGlobalButton.setDisable(false);
                        addStudentGlobalButton.setText("Dodaj studenta");

                        animateButton(addStudentGlobalButton);
                        clearStudentGlobalForm();

                        loadStudentCountFromServer();
                        loadAllStudentsCount();
                        loadRecentStudents();

                        showAlert("Sukces",
                                "Student " + newStudent.getFullName() + " został dodany na serwer!" +
                                        "\n(Przypisanie do grupy możliwe w oknie szczegółów grupy)",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        addStudentGlobalButton.setDisable(false);
                        addStudentGlobalButton.setText("Dodaj studenta");

                        if (throwable.getCause() instanceof StudentService.StudentAlreadyExistsException) {
                            showAlert("Student już istnieje",
                                    throwable.getCause().getMessage() +
                                            "\nSprawdź numer indeksu i spróbuj ponownie z innym numerem.",
                                    Alert.AlertType.WARNING);
                        } else {
                            showAlert("Błąd serwera",
                                    "Nie udało się dodać studenta na serwer:\n" + throwable.getMessage(),
                                    Alert.AlertType.ERROR);
                        }
                    });
                    return null;
                });
    }

    /**
     * Otwiera okno szczegółów wybranej grupy.
     *
     * <p>Metoda ładuje widok zarządzania grupą (group-detail-view.fxml) obsługiwany
     * przez {@link GroupDetailController} w nowym oknie modalnym. Okno umożliwia
     * zarządzanie studentami grupy, terminami zajęć oraz systemem frekwencji.</p>
     *
     * <p>Proces otwierania okna:</p>
     * <ol>
     *   <li><strong>Ładowanie FXML</strong>
     *       <ul>
     *         <li>Utworzenie FXMLLoader dla group-detail-view.fxml</li>
     *         <li>Załadowanie hierarchii węzłów</li>
     *         <li>Pobranie referencji do kontrolera</li>
     *       </ul>
     *   </li>
     *   <li><strong>Konfiguracja kontrolera</strong>
     *       <ul>
     *         <li>Przekazanie obiektu grupy przez {@link GroupDetailController#setGroup(Group)}</li>
     *         <li>Automatyczna inicjalizacja danych grupy</li>
     *       </ul>
     *   </li>
     *   <li><strong>Konfiguracja okna</strong>
     *       <ul>
     *         <li>Tytuł: "Grupa: [nazwa_grupy]"</li>
     *         <li>Rozmiar: 1200x800 pikseli</li>
     *         <li>Minimalny rozmiar: 1000x700</li>
     *         <li>Załadowanie arkusza stylów</li>
     *       </ul>
     *   </li>
     *   <li><strong>Wyświetlenie</strong> - pokazanie okna użytkownikowi</li>
     * </ol>
     *
     * <p>Właściwości okna szczegółów grupy:</p>
     * <ul>
     *   <li>Niezależne od okna głównego (nie modalne)</li>
     *   <li>Możliwość otwierania wielu okien grup jednocześnie</li>
     *   <li>Automatyczne zamykanie przy zamknięciu aplikacji głównej</li>
     *   <li>Pełna funkcjonalność zarządzania grupą</li>
     * </ul>
     *
     * <p>Obsługa błędów:</p>
     * <ul>
     *   <li>IOException przy ładowaniu FXML - wyświetlenie alertu</li>
     *   <li>Problemy z kontrolerem - log do konsoli</li>
     *   <li>Błędy stylizacji - kontynuacja bez stylów</li>
     * </ul>
     *
     * @param group grupa do wyświetlenia w oknie szczegółów (nie może być null)
     *
     * @see GroupDetailController
     * @see GroupDetailController#setGroup(Group)
     */
    private void openGroupDetailWindow(Group group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("group-detail-view.fxml"));
            Parent root = loader.load();

            GroupDetailController controller = loader.getController();
            controller.setGroup(group);

            Stage stage = new Stage();
            stage.setTitle("Grupa: " + group.getName());
            stage.setScene(new Scene(root, 1200, 800));

            stage.getScene().getStylesheets().add(
                    getClass().getResource("styles.css").toExternalForm());

            stage.setMinWidth(1000);
            stage.setMinHeight(700);

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd", "Nie udało się otworzyć widoku grupy: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    /**
     * Ładuje listę grup z serwera i aktualizuje interfejs użytkownika.
     *
     * <p>Metoda wykonuje pełny proces pobierania grup z serwera z odpowiednim
     * feedbackiem dla użytkownika i obsługą błędów. Po pomyślnym załadowaniu
     * zastępuje lokalną listę grup danymi z serwera.</p>
     *
     * <p>Proces ładowania:</p>
     * <ol>
     *   <li><strong>Aktualizacja stanu UI</strong>
     *       <ul>
     *         <li>Zmiana tekstu przycisku na "Ładowanie..."</li>
     *         <li>Wyłączenie przycisku {@link #refreshButton}</li>
     *       </ul>
     *   </li>
     *   <li><strong>Komunikacja z serwerem</strong>
     *       <ul>
     *         <li>Wywołanie {@link GroupService#getAllGroupsAsync()}</li>
     *         <li>Asynchroniczne przetwarzanie odpowiedzi</li>
     *       </ul>
     *   </li>
     *   <li><strong>Aktualizacja danych</strong> (przy sukcesie)
     *       <ul>
     *         <li>Wyczyszczenie lokalnej listy {@link #groups}</li>
     *         <li>Dodanie wszystkich grup z serwera</li>
     *         <li>Aktualizacja licznika grup</li>
     *       </ul>
     *   </li>
     *   <li><strong>Przywrócenie stanu UI</strong>
     *       <ul>
     *         <li>Przywrócenie tekstu przycisku</li>
     *         <li>Ponowne włączenie przycisku</li>
     *         <li>Wyświetlenie komunikatu o wyniku</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Obsługiwane scenariusze:</p>
     * <ul>
     *   <li><strong>Sukces</strong> - komunikat "Załadowano X grup z serwera"</li>
     *   <li><strong>Błąd serwera</strong> - alert z szczegółami błędu</li>
     *   <li><strong>Brak połączenia</strong> - informacja o problemie z siecią</li>
     * </ul>
     *
     * <p>Metoda zapewnia, że interfejs użytkownika zawsze wróci do stanu funkcjonalnego,
     * niezależnie od wyniku operacji sieciowej.</p>
     *
     * @see GroupService#getAllGroupsAsync()
     * @see #updateGroupCount()
     */
    private void loadGroupsFromServer() {
        refreshButton.setText("Ładowanie...");
        refreshButton.setDisable(true);

        groupService.getAllGroupsAsync()
                .thenAccept(serverGroups -> {
                    javafx.application.Platform.runLater(() -> {
                        groups.clear();
                        groups.addAll(serverGroups);
                        updateGroupCount();

                        refreshButton.setText("Odśwież z serwera");
                        refreshButton.setDisable(false);

                        showAlert("Sukces", "Załadowano " + serverGroups.size() + " grup z serwera",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        refreshButton.setText("Odśwież z serwera");
                        refreshButton.setDisable(false);

                        showAlert("Błąd serwera",
                                "Nie udało się załadować grup z serwera:\n" + throwable.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Ładuje i wyświetla liczbę studentów z serwera.
     *
     * <p>Metoda pobiera wszystkich studentów z serwera, oblicza statystyki
     * dotyczące przypisania do grup i aktualizuje {@link #studentCountLabel}.</p>
     *
     * <p>Obliczane informacje:</p>
     * <ul>
     *   <li><strong>Łączna liczba studentów</strong> - wszystkie rekordy w systemie</li>
     *   <li><strong>Studenci bez grupy</strong> - dostępni do przypisania</li>
     * </ul>
     *
     * <p>Format wyświetlanego tekstu:</p>
     * <pre>
     * "Studentów na serwerze: X (bez grupy: Y)"
     * </pre>
     *
     * <p>Obsługa błędów:</p>
     * <ul>
     *   <li>Sukces: normalny tekst</li>
     *   <li>Błąd: czerwony tekst "Błąd ładowania liczby studentów"</li>
     * </ul>
     *
     * @see StudentService#getAllStudentsAsync()
     * @see #studentCountLabel
     */
    private void loadStudentCountFromServer() {
        studentService.getAllStudentsAsync()
                .thenAccept(serverStudents -> {
                    javafx.application.Platform.runLater(() -> {
                        if (studentCountLabel != null) {
                            long withoutGroup = serverStudents.stream()
                                    .filter(s -> s.getGroupName() == null || s.getGroupName().trim().isEmpty())
                                    .count();

                            studentCountLabel.setText("Studentów na serwerze: " + serverStudents.size() +
                                    " (bez grupy: " + withoutGroup + ")");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        if (studentCountLabel != null) {
                            studentCountLabel.setText("Błąd ładowania liczby studentów");
                            studentCountLabel.setStyle("-fx-text-fill: #E53E3E;");
                        }
                    });
                    return null;
                });
    }

    /**
     * Sprawdza i wyświetla status połączenia z serwerem.
     *
     * <p>Metoda testuje dostępność serwera i aktualizuje {@link #serverStatusLabel}
     * odpowiednim komunikatem i kolorem. Wykonywana automatycznie przy inicjalizacji
     * kontrolera oraz na żądanie użytkownika.</p>
     *
     * <p>Możliwe stany połączenia:</p>
     * <ul>
     *   <li><strong>Połączony</strong> - "🟢 Połączony z serverem" (kolor zielony #38A169)</li>
     *   <li><strong>Niedostępny</strong> - "🔴 Serwer niedostępny" (kolor czerwony #E53E3E)</li>
     * </ul>
     *
     * <p>Test połączenia wykorzystuje {@link GroupService#checkServerConnection()}
     * który wykonuje proste żądanie GET do endpointu health check.</p>
     *
     * @see GroupService#checkServerConnection()
     * @see #serverStatusLabel
     */
    private void checkServerConnection() {
        groupService.checkServerConnection()
                .thenAccept(isConnected -> {
                    javafx.application.Platform.runLater(() -> {
                        if (isConnected) {
                            serverStatusLabel.setText("🟢 Połączony z serverem");
                            serverStatusLabel.setStyle("-fx-text-fill: #38A169;");
                        } else {
                            serverStatusLabel.setText("🔴 Serwer niedostępny");
                            serverStatusLabel.setStyle("-fx-text-fill: #E53E3E;");
                        }
                    });
                });
    }

    /**
     * Czyści pola formularza dodawania studenta.
     *
     * <p>Metoda wywoływana po pomyślnym dodaniu studenta lub w innych sytuacjach
     * wymagających wyczyszczenia formularza.</p>
     *
     * <p>Czyszczone pola:</p>
     * <ul>
     *   <li>{@link #studentFirstNameField} - imię studenta</li>
     *   <li>{@link #studentLastNameField} - nazwisko studenta</li>
     *   <li>{@link #studentIndexField} - numer indeksu</li>
     * </ul>
     */
    private void clearStudentGlobalForm() {
        studentFirstNameField.clear();
        studentLastNameField.clear();
        studentIndexField.clear();
    }

    /**
     * Konfiguruje walidację pola numeru indeksu studenta.
     *
     * <p>Ustawia listener na pole {@link #studentIndexField}, który automatycznie
     * filtruje wprowadzane znaki, pozostawiając tylko cyfry i ograniczając długość
     * do maksymalnie 6 znaków.</p>
     *
     * <p>Reguły walidacji:</p>
     * <ul>
     *   <li>Tylko cyfry (0-9) są dozwolone</li>
     *   <li>Maksymalna długość: 6 znaków</li>
     *   <li>Automatyczne usuwanie nieprawidłowych znaków</li>
     *   <li>Natychmiastowa aktualizacja pola przy każdej zmianie</li>
     * </ul>
     *
     * <p>Przykład działania:</p>
     * <pre>
     * {@code
     * Użytkownik wpisuje: "1a2b3c4d5e6f"
     * System filtruje do: "123456"
     * Pole wyświetla: "123456"
     * }
     * </pre>
     */
    private void setupStudentIndexValidation() {
        if (studentIndexField != null) {
            studentIndexField.textProperty().addListener((observable, oldValue, newValue) -> {
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                if (digitsOnly.length() > 6) {
                    digitsOnly = digitsOnly.substring(0, 6);
                }

                if (!digitsOnly.equals(newValue)) {
                    studentIndexField.setText(digitsOnly);
                }
            });
        }
    }

    private void updateGroupCount() {
        groupCountLabel.setText("Liczba grup: " + groups.size());
    }

    private void animateButton(Button button) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), button);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(0.95);
        scaleTransition.setToY(0.95);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);
        scaleTransition.play();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");

        alert.showAndWait();
    }

    private class GroupListCell extends ListCell<Group> {
        @Override
        protected void updateItem(Group group, boolean empty) {
            super.updateItem(group, empty);

            if (empty || group == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox cellContent = new VBox(5);
                cellContent.getStyleClass().add("group-cell");

                Label nameLabel = new Label(group.getName());
                nameLabel.getStyleClass().add("group-name");

                Label detailsLabel = new Label(group.getSpecialization());
                detailsLabel.getStyleClass().add("group-details");

                Label dateLabel = new Label("Utworzono: " + group.getFormattedDate());
                dateLabel.getStyleClass().add("group-date");

                cellContent.getChildren().addAll(nameLabel, detailsLabel, dateLabel);
                setGraphic(cellContent);
                setText(null);
            }
        }
    }
    @FXML
    protected void onTestEndpoints() {
        if (groups.isEmpty()) {
            showAlert("Info", "Najpierw dodaj jakąś grupę do przetestowania endpointów.",
                    Alert.AlertType.INFORMATION);
            return;
        }

        Group firstGroup = groups.get(0);
        System.out.println("=== TESTOWANIE ENDPOINTÓW DLA GRUPY: " + firstGroup.getName() + " ===");

        groupService.checkAvailableEndpoints(firstGroup.getName())
                .thenAccept(results -> {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("=== WYNIKI TESTÓW ENDPOINTÓW ===");
                        System.out.println(results);

                        showAlert("Wyniki testów",
                                "Sprawdź konsolę - wyświetlono wyniki testów wszystkich endpointów.\n\n" +
                                        "Szukaj linii z ⭐ POTENCJALNIE DZIAŁAJĄCY ENDPOINT",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("Błąd testowania endpointów: " + throwable.getMessage());
                    });
                    return null;
                });
    }
}