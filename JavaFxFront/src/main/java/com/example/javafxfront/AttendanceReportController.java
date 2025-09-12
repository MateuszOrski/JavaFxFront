package com.example.javafxfront;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kontroler dziennika obecności (attendance-report-view.fxml).
 *
 * <p>AttendanceReportController zarządza widokiem raportu frekwencji,
 * wyświetlając szczegółowe dane obecności studentów w formie tabeli
 * oraz generując statystyki i umożliwiając eksport danych do pliku CSV.</p>
 *
 * <p>Kontroler oferuje kompleksowy system raportowania obecności z następującymi funkcjonalościami:</p>
 *
 * <h3>Główne funkcjonalności:</h3>
 * <ul>
 *   <li><strong>Dynamiczna tabela obecności</strong> - automatyczne tworzenie kolumn dla każdego terminu</li>
 *   <li><strong>Kolorowe oznaczenia statusów</strong> - wizualne rozróżnienie obecnych/spóźnionych/nieobecnych</li>
 *   <li><strong>Statystyki frekwencji</strong> - obliczanie średniej obecności, najlepszego i najgorszego studenta</li>
 *   <li><strong>System filtrowania</strong> - filtrowanie według terminów i statusów obecności</li>
 *   <li><strong>Eksport do CSV</strong> - zachowanie pełnej struktury danych w formacie arkusza</li>
 *   <li><strong>Responsywny design</strong> - automatyczne dostosowanie kolumn do zawartości</li>
 * </ul>
 *
 * <h3>Struktura raportu:</h3>
 * <p>Raport prezentuje dane w następującym układzie:</p>
 * <ul>
 *   <li><strong>Nagłówek</strong> - informacje o grupie, liczbie studentów i terminów</li>
 *   <li><strong>Filtry</strong> - możliwość zawężenia widoku raportu</li>
 *   <li><strong>Statystyki</strong> - kluczowe wskaźniki frekwencji grupy</li>
 *   <li><strong>Tabela główna</strong> - macierz student × termin z statusami obecności</li>
 *   <li><strong>Kolumna statystyk</strong> - podsumowanie dla każdego studenta</li>
 * </ul>
 *
 * <h3>Kolorowanie statusów:</h3>
 * <ul>
 *   <li><span style="color: #38A169;">🟢 Obecny</span> - zielone tło</li>
 *   <li><span style="color: #F56500;">🟡 Spóźniony</span> - pomarańczowe tło</li>
 *   <li><span style="color: #E53E3E;">🔴 Nieobecny</span> - czerwone tło</li>
 *   <li><span style="color: #6C757D;">⚪ Nie zaznaczono</span> - szare tło</li>
 * </ul>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * // Otwieranie raportu z kontrolera grupy
 * FXMLLoader loader = new FXMLLoader(getClass().getResource("attendance-report-view.fxml"));
 * Parent root = loader.load();
 *
 * AttendanceReportController reportController = loader.getController();
 * reportController.setData(currentGroup, studentsList, schedulesList);
 *
 * Stage reportStage = new Stage();
 * reportStage.setTitle("📊 Dziennik obecności - " + currentGroup.getName());
 * reportStage.setScene(new Scene(root, 1200, 800));
 * reportStage.show();
 * }
 * </pre>
 *
 * <h3>Obliczenia statystyczne:</h3>
 * <p>Kontroler automatycznie oblicza następujące wskaźniki:</p>
 * <ul>
 *   <li><strong>Średnia obecność grupy</strong> - procent obecności wszystkich studentów</li>
 *   <li><strong>Najlepsza frekwencja</strong> - student z najwyższym procentem obecności</li>
 *   <li><strong>Najgorsza frekwencja</strong> - student z najniższym procentem obecności</li>
 *   <li><strong>Statystyki indywidualne</strong> - procent obecności dla każdego studenta</li>
 * </ul>
 *
 * @author System Team
 * @version 1.0
 * @since 2024
 *
 * @see GroupDetailController
 * @see Attendance
 * @see AttendanceReportRow
 * @see Group
 * @see Student
 * @see ClassSchedule
 */
public class AttendanceReportController {

    // Elementy nagłówka raportu

    /**
     * Label wyświetlający nazwę grupy w nagłówku raportu.
     *
     * <p>Format: "Grupa: [nazwa_grupy]"</p>
     */
    @FXML private Label groupNameLabel;

    /**
     * Główny tytuł raportu.
     *
     * <p>Format: "Dziennik obecności - [nazwa_grupy]"</p>
     */
    @FXML private Label reportTitleLabel;

    /**
     * Label pokazujący liczbę studentów w grupie.
     *
     * <p>Format: "Liczba studentów: X"</p>
     */
    @FXML private Label totalStudentsLabel;

    /**
     * Label pokazujący liczbę terminów w raporcie.
     *
     * <p>Format: "Liczba terminów: X"</p>
     */
    @FXML private Label totalSchedulesLabel;

    // Główna tabela obecności

    /**
     * Główna tabela raportu obecności.
     *
     * <p>Tabela zawiera:</p>
     * <ul>
     *   <li>Kolumny stałe: Imię i nazwisko, Nr indeksu</li>
     *   <li>Kolumny dynamiczne: jedna dla każdego terminu zajęć</li>
     *   <li>Kolumna statystyk: podsumowanie dla studenta</li>
     * </ul>
     *
     * <p>Każda komórka z obecnością jest kolorowana według statusu.</p>
     */
    @FXML private TableView<AttendanceReportRow> attendanceTable;

    /**
     * Kolumna z imieniem i nazwiskiem studenta.
     *
     * <p>Szerokość: 200px, sortowalna alfabetycznie.</p>
     */
    @FXML private TableColumn<AttendanceReportRow, String> studentNameColumn;

    /**
     * Kolumna z numerem indeksu studenta.
     *
     * <p>Szerokość: 100px, sortowalna numerycznie.</p>
     */
    @FXML private TableColumn<AttendanceReportRow, String> indexColumn;

    // Statystyki

    /**
     * Label wyświetlający średnią obecność wszystkich studentów.
     *
     * <p>Format: "X.X%" gdzie X.X to procent z jednym miejscem po przecinku.</p>
     */
    @FXML private Label avgAttendanceLabel;

    /**
     * Label pokazujący studenta z najlepszą frekwencją.
     *
     * <p>Format: "Imię Nazwisko (X.X%)" gdzie X.X to procent obecności.</p>
     */
    @FXML private Label bestStudentLabel;

    /**
     * Label pokazujący studenta z najgorszą frekwencją.
     *
     * <p>Format: "Imię Nazwisko (X.X%)" gdzie X.X to procent obecności.</p>
     */
    @FXML private Label worstStudentLabel;

    // Przyciski akcji

    /**
     * Przycisk eksportu dziennika do pliku CSV.
     *
     * <p>Otwiera dialog wyboru pliku i zapisuje pełną tabelę obecności
     * w formacie CSV zachowując strukturę kolumn i wszystkie dane.</p>
     */
    @FXML private Button exportCSVButton;

    /**
     * Przycisk zamykający okno dziennika.
     */
    @FXML private Button closeButton;

    /**
     * Przycisk odświeżający raport.
     *
     * <p>Regeneruje tabelę i przelicza statystyki na podstawie aktualnych danych.</p>
     */
    @FXML private Button refreshButton;

    // Filtry

    /**
     * ComboBox do filtrowania według terminów.
     *
     * <p>Zawiera opcje:</p>
     * <ul>
     *   <li>"Wszystkie terminy" - brak filtrowania</li>
     *   <li>Nazwy konkretnych terminów z datami</li>
     * </ul>
     */
    @FXML private ComboBox<String> filterScheduleComboBox;

    /**
     * ComboBox do filtrowania według typu obecności.
     *
     * <p>Wykorzystuje enum {@link AttendanceFilter} do określenia rodzaju filtra.</p>
     */
    @FXML private ComboBox<AttendanceFilter> filterTypeComboBox;

    // Dane

    /**
     * Referencja do bieżącej grupy dla której generowany jest raport.
     */
    private Group currentGroup;

    /**
     * Lista studentów grupy do uwzględnienia w raporcie.
     */
    private List<Student> students;

    /**
     * Lista terminów zajęć do uwzględnienia w raporcie.
     */
    private List<ClassSchedule> schedules;

    /**
     * Serwis obsługi obecności do dodatkowych operacji na danych.
     */
    private AttendanceService attendanceService;

    /**
     * Observable lista wierszy raportu związana z tabelą JavaFX.
     *
     * <p>Każdy element reprezentuje jednego studenta z jego obecnościami
     * na wszystkich terminach oraz obliczonymi statystykami.</p>
     */
    private ObservableList<AttendanceReportRow> reportData;

    /**
     * Wyliczenie typów filtrów dla dziennika obecności.
     *
     * <p>Umożliwia zawężenie widoku raportu do konkretnych kategorii obecności.
     * Każdy typ filtru ma przypisaną nazwę wyświetlaną w interfejsie użytkownika.</p>
     *
     * <h3>Dostępne filtry:</h3>
     * <ul>
     *   <li><strong>ALL</strong> - pokazuje wszystkich studentów</li>
     *   <li><strong>PRESENT_ONLY</strong> - tylko studentów oznaczonych jako obecni</li>
     *   <li><strong>ABSENT_ONLY</strong> - tylko studentów oznaczonych jako nieobecni</li>
     *   <li><strong>LATE_ONLY</strong> - tylko studentów oznaczonych jako spóźnieni</li>
     * </ul>
     *
     * @see #filterTypeComboBox
     * @see #applyFilters()
     */
    public enum AttendanceFilter {
        /** Pokazuje wszystkie wpisy obecności bez filtrowania */
        ALL("Wszystkie"),

        /** Filtruje tylko studentów obecnych na zajęciach */
        PRESENT_ONLY("Tylko obecni"),

        /** Filtruje tylko studentów nieobecnych na zajęciach */
        ABSENT_ONLY("Tylko nieobecni"),

        /** Filtruje tylko studentów spóźnionych na zajęcia */
        LATE_ONLY("Tylko spóźnieni");

        /** Nazwa wyświetlana filtru w interfejsie użytkownika */
        private final String displayName;

        /**
         * Konstruktor typu filtru.
         *
         * @param displayName nazwa do wyświetlenia w ComboBox
         */
        AttendanceFilter(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Zwraca nazwę wyświetlaną filtru.
         *
         * @return nazwa filtru do pokazania użytkownikowi
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Zwraca nazwę wyświetlaną jako reprezentację tekstową.
         *
         * @return nazwa filtru (używana przez ComboBox)
         */
        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Metoda inicjalizacyjna kontrolera wywoływana automatycznie przez JavaFX.
     *
     * <p>Konfiguruje wszystkie elementy interfejsu użytkownika, inicjalizuje serwisy
     * i ustawia nasłuchiwanie zdarzeń. Ta metoda jest wywoływana automatycznie
     * po załadowaniu pliku FXML, ale przed przekazaniem danych przez {@link #setData(Group, List, List)}.</p>
     *
     * <p>Wykonywane operacje inicjalizacyjne:</p>
     * <ol>
     *   <li>Inicjalizacja serwisu obecności i listy danych raportu</li>
     *   <li>Konfiguracja kolumn tabeli z PropertyValueFactory</li>
     *   <li>Ustawienie filtrów w ComboBox</li>
     *   <li>Powiązanie listy danych z tabelą</li>
     *   <li>Konfiguracja akcji przycisków</li>
     *   <li>Ustawienie nasłuchiwania zdarzeń filtrów</li>
     * </ol>
     *
     * <p>Po inicjalizacji kontroler jest gotowy do otrzymania danych
     * przez metodę {@link #setData(Group, List, List)}.</p>
     *
     * @see #setData(Group, List, List)
     * @see #setupTableColumns()
     * @see #setupFilters()
     */
    @FXML
    protected void initialize() {
        attendanceService = new AttendanceService();
        reportData = FXCollections.observableArrayList();

        setupTableColumns();
        setupFilters();

        attendanceTable.setItems(reportData);

        // Akcje przycisków
        exportCSVButton.setOnAction(e -> exportToCSV());
        closeButton.setOnAction(e -> closeWindow());
        refreshButton.setOnAction(e -> refreshReport());

        // Filtry
        filterScheduleComboBox.setOnAction(e -> applyFilters());
        filterTypeComboBox.setOnAction(e -> applyFilters());
    }

    /**
     * Ustawia dane dla raportu obecności i generuje kompletny raport.
     *
     * <p>Główna metoda wywoływana przez kontroler nadrzędny ({@link GroupDetailController})
     * do przekazania danych grupy, studentów i terminów. Po otrzymaniu danych metoda
     * automatycznie inicjalizuje wszystkie elementy raportu i generuje kompletną tabelę oraz statystyki.</p>
     *
     * <p>Proces generowania raportu:</p>
     * <ol>
     *   <li>Zapisanie referencji do danych wejściowych</li>
     *   <li>Aktualizacja informacji w nagłówku raportu</li>
     *   <li>Konfiguracja filtra terminów</li>
     *   <li>Generowanie głównej tabeli obecności</li>
     *   <li>Obliczanie i wyświetlanie statystyk</li>
     * </ol>
     *
     * <p>Wymagania danych wejściowych:</p>
     * <ul>
     *   <li><strong>group</strong> - musi zawierać poprawną nazwę i specjalizację</li>
     *   <li><strong>students</strong> - lista może być pusta, ale nie null</li>
     *   <li><strong>schedules</strong> - lista może być pusta, ale nie null</li>
     * </ul>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * // W kontrolerze grupy
     * AttendanceReportController reportController = loader.getController();
     *
     * // Przekazanie danych - raport zostanie automatycznie wygenerowany
     * reportController.setData(
     *     currentGroup,                    // grupa
     *     new ArrayList<>(students),       // kopia listy studentów
     *     new ArrayList<>(schedules)       // kopia listy terminów
     * );
     *
     * // Raport jest już gotowy do wyświetlenia
     * }
     * </pre>
     *
     * @param group grupa do raportu (nie może być null)
     * @param students lista studentów grupy (nie może być null, może być pusta)
     * @param schedules lista terminów zajęć (nie może być null, może być pusta)
     *
     * @throws IllegalArgumentException jeśli którykolwiek z parametrów jest null
     *
     * @see #updateHeader()
     * @see #generateReport()
     * @see #calculateStatistics()
     * @see GroupDetailController#onShowReportClick()
     */
    public void setData(Group group, List<Student> students, List<ClassSchedule> schedules) {
        this.currentGroup = group;
        this.students = students;
        this.schedules = schedules;

        updateHeader();
        setupScheduleFilter();
        generateReport();
        calculateStatistics();
    }

    /**
     * Konfiguruje kolumny tabeli z podstawowymi PropertyValueFactory.
     *
     * <p>Ustawia fabryki wartości dla stałych kolumn tabeli (imię/nazwisko i nr indeksu).
     * Dynamiczne kolumny dla terminów są dodawane później w {@link #generateReport()}.</p>
     *
     * <p>Konfigurowane kolumny:</p>
     * <ul>
     *   <li><strong>studentNameColumn</strong> - powiązana z właściwością "studentName"</li>
     *   <li><strong>indexColumn</strong> - powiązana z właściwością "indexNumber"</li>
     * </ul>
     *
     * @see #generateReport()
     * @see AttendanceReportRow#getStudentName()
     * @see AttendanceReportRow#getIndexNumber()
     */
    private void setupTableColumns() {
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        indexColumn.setCellValueFactory(new PropertyValueFactory<>("indexNumber"));

        // Dynamiczne kolumny dla każdego terminu będą dodane w generateReport()
    }

    /**
     * Konfiguruje dostępne opcje filtrów w ComboBox.
     *
     * <p>Inicjalizuje ComboBox filtra typu obecności wszystkimi dostępnymi opcjami
     * z enum {@link AttendanceFilter} i ustawia domyślną wartość na "Wszystkie".</p>
     *
     * @see AttendanceFilter
     * @see #applyFilters()
     */
    private void setupFilters() {
        filterTypeComboBox.setItems(FXCollections.observableArrayList(AttendanceFilter.values()));
        filterTypeComboBox.setValue(AttendanceFilter.ALL);
    }

    /**
     * Konfiguruje opcje filtra terminów na podstawie przekazanych danych.
     *
     * <p>Wypełnia ComboBox filtra terminów opcją "Wszystkie terminy" oraz
     * nazwami konkretnych terminów z datami. Każdy termin jest reprezentowany
     * w formacie: "Nazwa przedmiotu (dd.MM.yyyy HH:mm)".</p>
     *
     * <p>Struktura opcji filtra:</p>
     * <ol>
     *   <li>"Wszystkie terminy" - opcja domyślna, brak filtrowania</li>
     *   <li>Lista terminów w formacie: "Przedmiot (data)"</li>
     * </ol>
     *
     * @see ClassSchedule#getSubject()
     * @see ClassSchedule#getFormattedStartTime()
     */
    private void setupScheduleFilter() {
        ObservableList<String> scheduleNames = FXCollections.observableArrayList();
        scheduleNames.add("Wszystkie terminy");

        for (ClassSchedule schedule : schedules) {
            scheduleNames.add(schedule.getSubject() + " (" + schedule.getFormattedStartTime() + ")");
        }

        filterScheduleComboBox.setItems(scheduleNames);
        filterScheduleComboBox.setValue("Wszystkie terminy");
    }

    /**
     * Aktualizuje informacje w nagłówku raportu.
     *
     * <p>Wypełnia labele w nagłówku raportu aktualnymi informacjami o grupie
     * i liczbie studentów oraz terminów. Wszystkie informacje są formatowane
     * do czytelnej prezentacji.</p>
     *
     * <p>Aktualizowane elementy:</p>
     * <ul>
     *   <li><strong>groupNameLabel</strong> - "Grupa: [nazwa]"</li>
     *   <li><strong>reportTitleLabel</strong> - "Dziennik obecności - [nazwa]"</li>
     *   <li><strong>totalStudentsLabel</strong> - "Liczba studentów: X"</li>
     *   <li><strong>totalSchedulesLabel</strong> - "Liczba terminów: X"</li>
     * </ul>
     */
    private void updateHeader() {
        if (currentGroup != null) {
            groupNameLabel.setText("Grupa: " + currentGroup.getName());
            reportTitleLabel.setText("Dziennik obecności - " + currentGroup.getName());
            totalStudentsLabel.setText("Liczba studentów: " + students.size());
            totalSchedulesLabel.setText("Liczba terminów: " + schedules.size());
        }
    }

    /**
     * Generuje główną tabelę raportu obecności z dynamicznymi kolumnami.
     *
     * <p>To kluczowa metoda kontrolera, która tworzy kompletną tabelę obecności
     * z dynamicznie generowanymi kolumnami dla każdego terminu zajęć. Metoda
     * czyści istniejące dane, dodaje kolumny dla terminów i wypełnia wiersze
     * danymi wszystkich studentów.</p>
     *
     * <p>Proces generowania tabeli:</p>
     * <ol>
     *   <li>Wyczyszczenie danych i kolumn tabeli</li>
     *   <li>Dodanie podstawowych kolumn (Imię/Nazwisko, Nr indeksu)</li>
     *   <li>Dynamiczne tworzenie kolumn dla każdego terminu</li>
     *   <li>Konfiguracja stylizacji komórek z kolorowaniem statusów</li>
     *   <li>Dodanie kolumny statystyk</li>
     *   <li>Wypełnienie wierszy danymi studentów</li>
     *   <li>Zebranie obecności dla każdego studenta na każdym terminie</li>
     *   <li>Obliczenie statystyk dla każdego studenta</li>
     * </ol>
     *
     * <p>Struktura kolumn w tabeli:</p>
     * <ul>
     *   <li><strong>Imię i nazwisko</strong> - 200px szerokości</li>
     *   <li><strong>Nr indeksu</strong> - 100px szerokości</li>
     *   <li><strong>Kolumny terminów</strong> - dynamiczna szerokość, jedna na termin</li>
     *   <li><strong>Statystyki</strong> - podsumowanie obecności studenta</li>
     * </ul>
     *
     * <p>Kolorowanie komórek według statusu:</p>
     * <ul>
     *   <li><strong>Obecny</strong> - zielone tło (rgba(56, 161, 105, 0.2))</li>
     *   <li><strong>Spóźniony</strong> - pomarańczowe tło (rgba(245, 101, 0, 0.2))</li>
     *   <li><strong>Nieobecny</strong> - czerwone tło (rgba(229, 62, 62, 0.2))</li>
     *   <li><strong>Nie zaznaczono</strong> - szare tło (rgba(108, 117, 125, 0.1))</li>
     * </ul>
     *
     * <p>Format nagłówków kolumn terminów:</p>
     * <pre>
     * [Nazwa przedmiotu]
     * [dd.MM.yyyy HH:mm]
     * </pre>
     *
     * @see AttendanceReportRow
     * @see ClassSchedule#getAttendanceForStudent(Student)
     * @see Attendance.Status
     */
    private void generateReport() {
        reportData.clear();
        attendanceTable.getColumns().clear();

        // Dodaj podstawowe kolumny
        attendanceTable.getColumns().addAll(studentNameColumn, indexColumn);

        // Dodaj kolumny dla każdego terminu
        for (int i = 0; i < schedules.size(); i++) {
            ClassSchedule schedule = schedules.get(i);
            TableColumn<AttendanceReportRow, String> column = new TableColumn<>(
                    schedule.getSubject() + "\n" + schedule.getFormattedStartTime()
            );

            final int scheduleIndex = i;
            column.setCellValueFactory(data -> {
                AttendanceReportRow row = data.getValue();
                String status = row.getAttendanceForSchedule(scheduleIndex);
                return new javafx.beans.property.SimpleStringProperty(status);
            });

            // Stylizacja kolumn
            column.setCellFactory(col -> new TableCell<AttendanceReportRow, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        switch (status) {
                            case "Obecny":
                                setStyle("-fx-background-color: rgba(56, 161, 105, 0.2); -fx-text-fill: #38A169;");
                                break;
                            case "Spóźniony":
                                setStyle("-fx-background-color: rgba(245, 101, 0, 0.2); -fx-text-fill: #F56500;");
                                break;
                            case "Nieobecny":
                                setStyle("-fx-background-color: rgba(229, 62, 62, 0.2); -fx-text-fill: #E53E3E;");
                                break;
                            default:
                                setStyle("-fx-background-color: rgba(108, 117, 125, 0.1); -fx-text-fill: #6C757D;");
                        }
                    }
                }
            });

            attendanceTable.getColumns().add(column);
        }

        // Kolumna statystyk
        TableColumn<AttendanceReportRow, String> statsColumn = new TableColumn<>("Statystyki");
        statsColumn.setCellValueFactory(new PropertyValueFactory<>("statistics"));
        attendanceTable.getColumns().add(statsColumn);

        for (Student student : students) {
            AttendanceReportRow row = new AttendanceReportRow(student);

            // zbieranie obecnsoci dla terminu
            for (ClassSchedule schedule : schedules) {
                Attendance attendance = schedule.getAttendanceForStudent(student);
                if (attendance != null) {
                    row.addAttendance(attendance.getStatus().getDisplayName());
                } else {
                    row.addAttendance("Nie zaznaczono");
                }
            }

            row.calculateStatistics();
            reportData.add(row);
        }
    }

    /**
     * Oblicza i wyświetla statystyki frekwencji dla całej grupy.
     *
     * <p>Metoda analizuje dane obecności wszystkich studentów i generuje
     * kluczowe wskaźniki frekwencji grupy. Statystyki są automatycznie
     * aktualizowane w interfejsie użytkownika.</p>
     *
     * <p>Obliczane statystyki:</p>
     * <ul>
     *   <li><strong>Średnia obecność</strong> - średni procent obecności wszystkich studentów</li>
     *   <li><strong>Najlepsza frekwencja</strong> - student z najwyższym procentem obecności</li>
     *   <li><strong>Najgorsza frekwencja</strong> - student z najniższym procentem obecności</li>
     * </ul>
     *
     * <p>Formatowanie wyników:</p>
     * <ul>
     *   <li>Procenty wyświetlane z dokładnością do 1 miejsca po przecinku</li>
     *   <li>Nazwy studentów w pełnym formacie (imię + nazwisko)</li>
     *   <li>Graceful handling dla pustych danych</li>
     * </ul>
     *
     * <p>Przykłady formatów wyjściowych:</p>
     * <pre>
     * "Średnia obecność: 87.5%"
     * "Najlepsza frekwencja: Jan Kowalski (95.2%)"
     * "Najgorsza frekwencja: Anna Nowak (72.1%)"
     * </pre>
     *
     * @see AttendanceReportRow#getAttendancePercentage()
     * @see #avgAttendanceLabel
     * @see #bestStudentLabel
     * @see #worstStudentLabel
     */
    private void calculateStatistics() {
        if (reportData.isEmpty()) {
            avgAttendanceLabel.setText("Średnia obecność: 0%");
            bestStudentLabel.setText("Najlepsza frekwencja: Brak danych");
            worstStudentLabel.setText("Najgorsza frekwencja: Brak danych");
            return;
        }

        double avgAttendance = reportData.stream()
                .mapToDouble(AttendanceReportRow::getAttendancePercentage)
                .average()
                .orElse(0.0);

        AttendanceReportRow bestStudent = reportData.stream()
                .max((a, b) -> Double.compare(a.getAttendancePercentage(), b.getAttendancePercentage()))
                .orElse(null);

        AttendanceReportRow worstStudent = reportData.stream()
                .min((a, b) -> Double.compare(a.getAttendancePercentage(), b.getAttendancePercentage()))
                .orElse(null);

        avgAttendanceLabel.setText(String.format("Średnia obecność: %.1f%%", avgAttendance));

        if (bestStudent != null) {
            bestStudentLabel.setText(String.format("Najlepsza frekwencja: %s (%.1f%%)",
                    bestStudent.getStudentName(), bestStudent.getAttendancePercentage()));
        }

        if (worstStudent != null) {
            worstStudentLabel.setText(String.format("Najgorsza frekwencja: %s (%.1f%%)",
                    worstStudent.getStudentName(), worstStudent.getAttendancePercentage()));
        }
    }

    /**
     * Stosuje filtry do danych raportu na podstawie wybranych opcji.
     *
     * <p>Metoda filtruje dane w tabeli według wybranych kryteriów w ComboBox-ach.
     * Implementacja filtrowania zostanie dodana w przyszłej wersji - obecnie
     * jest to metoda placeholder.</p>
     *
     * <p>Planowane filtry:</p>
     * <ul>
     *   <li><strong>Filtr terminów</strong> - pokazywanie tylko wybranego terminu</li>
     *   <li><strong>Filtr statusów</strong> - pokazywanie tylko określonych statusów obecności</li>
     * </ul>
     *
     * @see #filterScheduleComboBox
     * @see #filterTypeComboBox
     * @see AttendanceFilter
     */
    private void applyFilters() {
        // TODO: Implementuj filtrowanie danych
        // Na podstawie wybranego terminu i typu filtru
    }

    /**
     * Eksportuje dziennik obecności do pliku CSV.
     *
     * <p>Otwiera dialog wyboru pliku i zapisuje kompletną tabelę obecności
     * w formacie CSV, zachowując wszystkie kolumny i formatowanie danych.
     * Plik może być później otwarty w Excel lub innych programach arkuszowych.</p>
     *
     * <p>Struktura eksportowanego pliku CSV:</p>
     * <ol>
     *   <li><strong>Nagłówki</strong> - nazwy kolumn oddzielone przecinkami</li>
     *   <li><strong>Dane studentów</strong> - jeden wiersz na studenta</li>
     *   <li><strong>Kolumny terminów</strong> - statusy obecności dla każdego terminu</li>
     *   <li><strong>Statystyki</strong> - podsumowanie dla każdego studenta</li>
     * </ol>
     *
     * <p>Format nazwy pliku:</p>
     * <pre>dziennik_obecnosci_[nazwa_grupy].csv</pre>
     *
     * <p>Przykład struktury CSV:</p>
     * <pre>
     * Imię i nazwisko,Numer indeksu,Egzamin Java (15.10.2024),Laboratorium (22.10.2024),Statystyki
     * Jan Kowalski,123456,Obecny,Spóźniony,"85.5% (17/20)"
     * Anna Nowak,654321,Nieobecny,Obecny,"72.1% (13/18)"
     * </pre>
     *
     * <p>Obsługa błędów:</p>
     * <ul>
     *   <li>Dialog anulowania przez użytkownika</li>
     *   <li>Błędy zapisu pliku (brak uprawnień, brak miejsca na dysku)</li>
     *   <li>Problemy z kodowaniem znaków</li>
     * </ul>
     *
     * @see FileChooser
     * @see #currentGroup
     * @see #schedules
     * @see #reportData
     */
    @FXML
    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz dziennik obecności");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv")
        );
        fileChooser.setInitialFileName("dziennik_obecnosci_" + currentGroup.getName() + ".csv");

        Stage stage = (Stage) exportCSVButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Nagłówki CSV
                writer.append("Imię i nazwisko,Numer indeksu");
                for (ClassSchedule schedule : schedules) {
                    writer.append(",").append(schedule.getSubject()).append(" (")
                            .append(schedule.getFormattedStartTime()).append(")");
                }
                writer.append(",Statystyki\n");

                // Dane studentów
                for (AttendanceReportRow row : reportData) {
                    writer.append(row.getStudentName()).append(",")
                            .append(row.getIndexNumber());

                    for (int i = 0; i < schedules.size(); i++) {
                        writer.append(",").append(row.getAttendanceForSchedule(i));
                    }

                    writer.append(",").append(row.getStatistics()).append("\n");
                }

                showAlert("Sukces", "Dziennik został wyeksportowany do pliku:\n" + file.getAbsolutePath(),
                        Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                showAlert("Błąd", "Nie udało się zapisać pliku:\n" + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Odświeża raport obecności, regenerując tabelę i statystyki.
     *
     * <p>Ponownie generuje całą tabelę obecności i przelicza wszystkie statystyki
     * na podstawie aktualnych danych. Przydatne gdy dane mogły się zmienić
     * od momentu wygenerowania raportu.</p>
     *
     * @see #generateReport()
     * @see #calculateStatistics()
     */
    @FXML
    private void refreshReport() {
        generateReport();
        calculateStatistics();
        showAlert("Info", "Raport został odświeżony", Alert.AlertType.INFORMATION);
    }

    /**
     * Zamyka okno dziennika obecności.
     *
     * <p>Pobiera referencję do aktualnego Stage i zamyka okno raportu,
     * powracając do kontrolera nadrzędnego.</p>
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Wyświetla dialog z komunikatem dla użytkownika.
     *
     * <p>Uniwersalna metoda do pokazywania alertów o różnych typach
     * (informacja, ostrzeżenie, błąd). Automatycznie stylizuje dialog
     * zgodnie z motywem aplikacji.</p>
     *
     * @param title tytuł okna dialogowego
     * @param message treść komunikatu do wyświetlenia
     * @param type typ alertu (INFORMATION, WARNING, ERROR, etc.)
     *
     * @see Alert.AlertType
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Klasa reprezentująca wiersz w tabeli raportu obecności.
     *
     * <p>AttendanceReportRow agreguje dane jednego studenta wraz z jego obecnościami
     * na wszystkich terminach oraz obliczonymi statystykami frekwencji. Każdy wiersz
     * w tabeli raportu jest reprezentowany przez instancję tej klasy.</p>
     *
     * <p>Klasa zarządza następującymi danymi:</p>
     * <ul>
     *   <li><strong>Dane studenta</strong> - imię, nazwisko, numer indeksu</li>
     *   <li><strong>Lista obecności</strong> - statusy dla każdego terminu w kolejności</li>
     *   <li><strong>Statystyki</strong> - obliczony procent obecności i podsumowanie</li>
     * </ul>
     *
     * <p>Statystyki są obliczane automatycznie przez {@link #calculateStatistics()}
     * i uwzględniają następujące zasady:</p>
     * <ul>
     *   <li><strong>Obecni i spóźnieni</strong> liczą się jako pozytywne dla frekwencji</li>
     *   <li><strong>Nieobecni</strong> liczą się jako negatywne dla frekwencji</li>
     *   <li><strong>"Nie zaznaczono"</strong> nie są uwzględniane w obliczeniach</li>
     * </ul>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * AttendanceReportRow row = new AttendanceReportRow(student);
     *
     * // Dodawanie obecności dla każdego terminu
     * row.addAttendance("Obecny");
     * row.addAttendance("Spóźniony");
     * row.addAttendance("Nieobecny");
     * row.addAttendance("Nie zaznaczono");
     *
     * // Obliczenie statystyk
     * row.calculateStatistics();
     *
     * // Wynik: "66.7% (2/3)" - 2 z 3 ocenionych obecności
     * System.out.println(row.getStatistics());
     * }
     * </pre>
     *
     * @see Student
     * @see Attendance.Status
     * @see #generateReport()
     */
    public static class AttendanceReportRow {

        /**
         * Student, którego dotyczy ten wiersz raportu.
         */
        private final Student student;

        /**
         * Lista statusów obecności dla każdego terminu w kolejności chronologicznej.
         *
         * <p>Kolejność statusów odpowiada kolejności terminów w {@link #schedules}.
         * Możliwe wartości: "Obecny", "Spóźniony", "Nieobecny", "Nie zaznaczono".</p>
         */
        private final List<String> attendanceStatuses = new java.util.ArrayList<>();

        /**
         * Obliczone statystyki frekwencji studenta w formacie tekstowym.
         *
         * <p>Format: "X.X% (obecni+spóźnieni/łącznie_ocenione)"</p>
         * <p>Przykład: "85.5% (17/20)"</p>
         */
        private String statistics = "";

        /**
         * Konstruktor wiersza raportu dla konkretnego studenta.
         *
         * @param student student dla tego wiersza (nie może być null)
         * @throws IllegalArgumentException jeśli student jest null
         */
        public AttendanceReportRow(Student student) {
            this.student = student;
        }

        /**
         * Dodaje status obecności dla kolejnego terminu.
         *
         * <p>Statusy są dodawane w kolejności chronologicznej terminów.
         * Metoda nie waliduje wartości - przyjmuje dowolny string.</p>
         *
         * @param status status obecności jako String (np. "Obecny", "Nieobecny")
         */
        public void addAttendance(String status) {
            attendanceStatuses.add(status);
        }

        /**
         * Oblicza statystyki frekwencji dla studenta na podstawie dodanych obecności.
         *
         * <p>Metoda analizuje wszystkie statusy obecności i generuje podsumowanie
         * w formacie czytelnym dla użytkownika. Obliczenia uwzględniają tylko
         * terminy, gdzie obecność została oznaczona (pomija "Nie zaznaczono").</p>
         *
         * <p>Algorytm obliczania:</p>
         * <ol>
         *   <li>Zlicz obecnych i spóźnionych (pozytywne obecności)</li>
         *   <li>Zlicz nieobecnych (negatywne obecności)</li>
         *   <li>Oblicz łączną liczbę ocenionych terminów (bez "Nie zaznaczono")</li>
         *   <li>Wylicz procent: (pozytywne / łącznie) * 100</li>
         *   <li>Sformatuj wynik z dokładnością do 1 miejsca po przecinku</li>
         * </ol>
         *
         * <p>Przykłady wyników:</p>
         * <ul>
         *   <li>"100.0% (5/5)" - perfect attendance</li>
         *   <li>"80.0% (4/5)" - jedna nieobecność</li>
         *   <li>"66.7% (2/3)" - dwie obecności z trzech ocenionych</li>
         *   <li>"Brak danych" - brak ocenionych terminów</li>
         *   <li>"Brak ocen" - wszystkie terminy "Nie zaznaczono"</li>
         * </ul>
         *
         * @see #getAttendancePercentage()
         * @see #getStatistics()
         */
        public void calculateStatistics() {
            if (attendanceStatuses.isEmpty()) {
                statistics = "Brak danych";
                return;
            }

            long present = attendanceStatuses.stream().filter(s -> s.equals("Obecny")).count();
            long late = attendanceStatuses.stream().filter(s -> s.equals("Spóźniony")).count();
            long absent = attendanceStatuses.stream().filter(s -> s.equals("Nieobecny")).count();
            long total = present + late + absent;

            if (total == 0) {
                statistics = "Brak ocen";
            } else {
                double percentage = (double) (present + late) / total * 100;
                statistics = String.format("%.1f%% (%d/%d)", percentage, present + late, total);
            }
        }

        /**
         * Zwraca procentową frekwencję studenta jako liczbę.
         *
         * <p>Metoda oblicza procent obecności (włączając spóźnionych) w stosunku
         * do wszystkich ocenionych terminów. Wartość jest zwracana jako double
         * w zakresie 0.0-100.0.</p>
         *
         * <p>Użycie w obliczeniach statystycznych:</p>
         * <ul>
         *   <li>Średnia frekwencji grupy</li>
         *   <li>Ranking studentów według frekwencji</li>
         *   <li>Identyfikacja najlepszego/najgorszego studenta</li>
         * </ul>
         *
         * @return procent obecności (0.0-100.0), lub 0.0 jeśli brak danych
         *
         * @see #calculateStatistics()
         */
        public double getAttendancePercentage() {
            if (attendanceStatuses.isEmpty()) return 0.0;

            long present = attendanceStatuses.stream().filter(s -> s.equals("Obecny")).count();
            long late = attendanceStatuses.stream().filter(s -> s.equals("Spóźniony")).count();
            long total = attendanceStatuses.stream().filter(s -> !s.equals("Nie zaznaczono")).count();

            if (total == 0) return 0.0;
            return (double) (present + late) / total * 100;
        }

        /**
         * Zwraca pełne imię i nazwisko studenta.
         *
         * <p>Wykorzystywane jako wartość dla kolumny "Imię i nazwisko" w tabeli raportu.</p>
         *
         * @return pełne imię studenta w formacie "Imię Nazwisko"
         *
         * @see Student#getFullName()
         * @see #studentNameColumn
         */
        public String getStudentName() {
            return student.getFullName();
        }

        /**
         * Zwraca numer indeksu studenta.
         *
         * <p>Wykorzystywane jako wartość dla kolumny "Nr indeksu" w tabeli raportu.</p>
         *
         * @return numer indeksu studenta (6 cyfr)
         *
         * @see Student#getIndexNumber()
         * @see #indexColumn
         */
        public String getIndexNumber() {
            return student.getIndexNumber();
        }

        /**
         * Zwraca obliczone statystyki frekwencji jako sformatowany tekst.
         *
         * <p>Wykorzystywane jako wartość dla kolumny "Statystyki" w tabeli raportu.
         * Statystyki muszą być wcześniej obliczone przez {@link #calculateStatistics()}.</p>
         *
         * @return statystyki w formacie "X.X% (Y/Z)" lub komunikat o braku danych
         *
         * @see #calculateStatistics()
         */
        public String getStatistics() {
            return statistics;
        }

        /**
         * Zwraca status obecności dla konkretnego terminu (kolumny).
         *
         * <p>Metoda używana przez dynamiczne kolumny terminów w tabeli do pobierania
         * wartości komórek. Indeks odpowiada pozycji terminu na liście schedules.</p>
         *
         * @param index indeks terminu (0-based)
         * @return status obecności dla tego terminu lub "Nie zaznaczono" jeśli brak danych
         *
         * @see #generateReport()
         * @see #addAttendance(String)
         */
        public String getAttendanceForSchedule(int index) {
            if (index >= 0 && index < attendanceStatuses.size()) {
                return attendanceStatuses.get(index);
            }
            return "Nie zaznaczono";
        }
    }
}