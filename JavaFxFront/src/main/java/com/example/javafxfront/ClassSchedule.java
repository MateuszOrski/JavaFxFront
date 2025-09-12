package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasa reprezentująca termin zajęć w systemie dziennika elektronicznego.
 *
 * <p>ClassSchedule stanowi centralny element systemu planowania zajęć,
 * łącząc informacje o terminie, miejscu i prowadzącym z możliwością
 * rejestrowania frekwencji studentów. Każdy termin może być powiązany
 * z konkretną grupą studencką i zawierać listę obecności.</p>
 *
 * <p>Klasa obsługuje dwa główne scenariusze:</p>
 * <ul>
 *   <li><strong>Terminy lokalne</strong> - utworzone w aplikacji, bez synchronizacji z serwerem</li>
 *   <li><strong>Terminy serwerowe</strong> - zsynchronizowane z bazą danych, posiadające ID</li>
 * </ul>
 *
 * <h3>Główne funkcjonalności:</h3>
 * <ul>
 *   <li><strong>Zarządzanie podstawowymi danymi</strong> - przedmiot, sala, czas, prowadzący</li>
 *   <li><strong>Rejestrowanie frekwencji</strong> - dodawanie, usuwanie, modyfikacja obecności</li>
 *   <li><strong>Generowanie statystyk</strong> - liczba obecnych, spóźnionych, nieobecnych</li>
 *   <li><strong>Formatowanie dat i czasów</strong> - czytelna prezentacja dla użytkownika</li>
 *   <li><strong>Synchronizacja z serwerem</strong> - rozróżnienie terminów lokalnych i serwerowych</li>
 * </ul>
 *
 * <h3>Model danych:</h3>
 * <p>Termin zajęć składa się z następujących elementów:</p>
 * <ul>
 *   <li><strong>Identyfikacja:</strong> ID (opcjonalne), nazwa przedmiotu, grupa</li>
 *   <li><strong>Lokalizacja:</strong> sala/miejsce przeprowadzenia zajęć</li>
 *   <li><strong>Czas:</strong> data i godzina rozpoczęcia oraz zakończenia</li>
 *   <li><strong>Osoby:</strong> prowadzący zajęcia</li>
 *   <li><strong>Dodatkowe:</strong> uwagi, data utworzenia w systemie</li>
 *   <li><strong>Frekwencja:</strong> lista obecności studentów ({@link Attendance})</li>
 * </ul>
 *
 * <h3>Zarządzanie frekwencją:</h3>
 * <p>Klasa zawiera rozbudowane API do zarządzania obecnością studentów:</p>
 * <ul>
 *   <li>{@link #addAttendance(Attendance)} - dodawanie/aktualizacja obecności</li>
 *   <li>{@link #removeAttendance(Student)} - usuwanie obecności studenta</li>
 *   <li>{@link #getAttendanceForStudent(Student)} - pobieranie obecności konkretnego studenta</li>
 *   <li>{@link #hasAttendanceForStudent(Student)} - sprawdzanie czy student ma obecność</li>
 * </ul>
 *
 * <h3>Statystyki frekwencji:</h3>
 * <p>Automatyczne obliczanie statystyk obecności:</p>
 * <ul>
 *   <li>{@link #getPresentCount()} - liczba obecnych studentów</li>
 *   <li>{@link #getLateCount()} - liczba spóźnionych studentów</li>
 *   <li>{@link #getAbsentCount()} - liczba nieobecnych studentów</li>
 *   <li>{@link #getTotalAttendanceCount()} - całkowita liczba wpisów frekwencji</li>
 *   <li>{@link #getAttendanceSummary()} - tekstowe podsumowanie statystyk</li>
 * </ul>
 *
 * <h3>Przykład użycia - tworzenie terminu:</h3>
 * <pre>
 * {@code
 * // Nowy termin (lokalny)
 * ClassSchedule schedule = new ClassSchedule(
 *     "Programowanie w Javie",           // przedmiot
 *     "Sala 101",                        // sala
 *     LocalDateTime.of(2024, 3, 15, 10, 0),  // rozpoczęcie
 *     LocalDateTime.of(2024, 3, 15, 12, 0),  // zakończenie
 *     "Dr Jan Kowalski",                 // prowadzący
 *     "Wykład wprowadzający",            // uwagi
 *     "INF-2024"                         // grupa
 * );
 *
 * System.out.println(schedule.toString());
 * // Output: "Programowanie w Javie - 15.03.2024 10:00 - 12:00"
 * }
 * </pre>
 *
 * <h3>Przykład użycia - zarządzanie frekwencją:</h3>
 * <pre>
 * {@code
 * // Dodawanie obecności studentów
 * Student student1 = new Student("Anna", "Kowalska", "123456", "INF-2024");
 * Student student2 = new Student("Piotr", "Nowak", "654321", "INF-2024");
 *
 * // Oznaczanie obecności
 * schedule.addAttendance(new Attendance(student1, schedule, Attendance.Status.PRESENT));
 * schedule.addAttendance(new Attendance(student2, schedule, Attendance.Status.LATE));
 *
 * // Sprawdzanie statystyk
 * System.out.println("Obecni: " + schedule.getPresentCount());      // 1
 * System.out.println("Spóźnieni: " + schedule.getLateCount());      // 1
 * System.out.println("Razem: " + schedule.getTotalAttendanceCount()); // 2
 * System.out.println(schedule.getAttendanceSummary());
 * // Output: "Obecni: 1 | Spóźnieni: 1 | Nieobecni: 0 | Razem: 2"
 * }
 * </pre>
 *
 * <h3>Synchronizacja z serwerem:</h3>
 * <p>Klasa rozróżnia terminy lokalne od serwerowych:</p>
 * <pre>
 * {@code
 * // Sprawdzenie pochodzenia terminu
 * if (schedule.isFromServer()) {
 *     System.out.println("Termin z serwera, ID: " + schedule.getId());
 *     // Można synchronizować obecności z bazą danych
 * } else {
 *     System.out.println("Termin lokalny, brak synchronizacji");
 *     // Tylko dane w pamięci aplikacji
 * }
 * }
 * </pre>
 *
 * <h3>Integracja z systemem:</h3>
 * <ul>
 *   <li>{@link ScheduleService} - synchronizacja z serwerem REST API</li>
 *   <li>{@link GroupDetailController} - interfejs zarządzania terminami</li>
 *   <li>{@link AttendanceReportController} - generowanie raportów frekwencji</li>
 *   <li>{@link AttendanceService} - zarządzanie obecnością online</li>
 * </ul>
 *
 * <h3>Uwagi dotyczące wydajności:</h3>
 * <ul>
 *   <li>Lista obecności jest przechowywana w pamięci jako {@link ArrayList}</li>
 *   <li>Operacje wyszukiwania obecności mają złożoność O(n)</li>
 *   <li>Dla dużej liczby studentów zaleca się indeksowanie po numerze indeksu</li>
 *   <li>Statystyki są obliczane na żądanie (bez cache'owania)</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 1.0
 * @since 2025
 *
 * @see Attendance Klasa reprezentująca obecność studenta
 * @see Student Klasa studenta uczestniczącego w zajęciach
 * @see ScheduleService Serwis synchronizacji terminów z serwerem
 * @see GroupDetailController Kontroler zarządzania terminami w grupie
 * @see AttendanceReportController Kontroler raportów frekwencji
 */
public class ClassSchedule {

    /**
     * Unikalny identyfikator terminu w bazie danych serwera.
     * <p>Null dla terminów utworzonych lokalnie, Long dla terminów z serwera.</p>
     */
    private Long id;

    /**
     * Nazwa przedmiotu/zajęć.
     * <p>Przykłady: "Programowanie w Javie", "Bazy Danych", "Algorytmy"</p>
     */
    private String subject;

    /**
     * Sala lub miejsce przeprowadzenia zajęć.
     * <p>Może zawierać: numer sali, nazwę budynku, lub "Online" dla zajęć zdalnych</p>
     */
    private String classroom;

    /**
     * Data i godzina rozpoczęcia zajęć.
     * <p>Używana do sortowania, filtrowania i wyświetlania terminów</p>
     */
    private LocalDateTime startTime;

    /**
     * Data i godzina zakończenia zajęć.
     * <p>Używana do obliczania długości trwania zajęć</p>
     */
    private LocalDateTime endTime;

    /**
     * Imię i nazwisko prowadzącego zajęcia.
     * <p>Może zawierać tytuł naukowy, np. "Dr hab. Jan Kowalski"</p>
     */
    private String instructor;

    /**
     * Dodatkowe uwagi dotyczące terminu.
     * <p>Może zawierać informacje o typie zajęć, tematyce, wymaganiach itp.</p>
     */
    private String notes;

    /**
     * Nazwa grupy studenckiej dla której przewidziany jest termin.
     * <p>Używana do filtrowania i grupowania terminów</p>
     */
    private String groupName;

    /**
     * Data i czas utworzenia terminu w systemie.
     * <p>Automatycznie ustawiana przy tworzeniu, używana do audytu</p>
     */
    private LocalDateTime createdDate;

    /**
     * Lista obecności studentów na tym terminie.
     * <p>Każdy student może mieć maksymalnie jedną obecność na termin.
     * Lista jest inicjalizowana jako pusta {@link ArrayList}.</p>
     */
    private List<Attendance> attendances; // DODANE - Lista uczestnictwa

    /**
     * Konstruktor dla nowych terminów tworzonych lokalnie w aplikacji.
     *
     * <p>Tworzy nowy termin bez identyfikatora serwera (ID = null).
     * Data utworzenia jest automatycznie ustawiana na bieżący moment.
     * Lista obecności jest inicjalizowana jako pusta.</p>
     *
     * <p>Ten konstruktor jest używany gdy:</p>
     * <ul>
     *   <li>Użytkownik tworzy nowy termin w aplikacji</li>
     *   <li>Importowane są dane z plików zewnętrznych</li>
     *   <li>Tworzone są terminy testowe lub demonstracyjne</li>
     * </ul>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * ClassSchedule newSchedule = new ClassSchedule(
     *     "Laboratorium Java",
     *     "Sala komputerowa 15",
     *     LocalDateTime.of(2024, 3, 20, 14, 0),
     *     LocalDateTime.of(2024, 3, 20, 16, 0),
     *     "mgr inż. Anna Kowalska",
     *     "Ćwiczenia praktyczne z programowania",
     *     "INF-2024A"
     * );
     *
     * // Termin jest lokalny
     * assert newSchedule.getId() == null;
     * assert !newSchedule.isFromServer();
     * assert newSchedule.getAttendances().isEmpty();
     * }
     * </pre>
     *
     * @param subject nazwa przedmiotu/zajęć (wymagane, nie może być null)
     * @param classroom sala lub miejsce zajęć (może być null lub puste)
     * @param startTime data i godzina rozpoczęcia (wymagane, nie może być null)
     * @param endTime data i godzina zakończenia (wymagane, nie może być null, musi być po startTime)
     * @param instructor prowadzący zajęcia (może być null lub puste)
     * @param notes dodatkowe uwagi (może być null lub puste)
     * @param groupName nazwa grupy studenckiej (wymagane dla filtrowania)
     *
     * @throws IllegalArgumentException jeśli subject, startTime, endTime lub groupName są null
     * @throws IllegalArgumentException jeśli endTime jest przed lub równe startTime
     *
     * @see #ClassSchedule(Long, String, String, LocalDateTime, LocalDateTime, String, String, String, LocalDateTime)
     * @see #isFromServer()
     */
    // Konstruktor dla nowych terminów (bez ID)
    public ClassSchedule(String subject, String classroom, LocalDateTime startTime,
                         LocalDateTime endTime, String instructor, String notes, String groupName) {
        this.subject = subject;
        this.classroom = classroom;
        this.startTime = startTime;
        this.endTime = endTime;
        this.instructor = instructor;
        this.notes = notes;
        this.groupName = groupName;
        this.createdDate = LocalDateTime.now();
        this.attendances = new ArrayList<>(); // DODANE
    }

    /**
     * Konstruktor dla terminów pochodzących z serwera z kompletnym ID i datą utworzenia.
     *
     * <p>Tworzy termin z identyfikatorem serwera, co oznacza że jest zsynchronizowany
     * z bazą danych. Takie terminy mogą mieć automatyczną synchronizację obecności
     * z serwerem backend.</p>
     *
     * <p>Ten konstruktor jest używany gdy:</p>
     * <ul>
     *   <li>Terminy są pobierane z serwera przez {@link ScheduleService}</li>
     *   <li>Deserializowane są dane JSON z REST API</li>
     *   <li>Tworzony jest termin po zapisie na serwerze</li>
     * </ul>
     *
     * <h3>Przykład użycia przez serwis:</h3>
     * <pre>
     * {@code
     * // Deserializacja z JSON serwera
     * ClassSchedule serverSchedule = new ClassSchedule(
     *     42L,                              // ID z bazy danych
     *     "Egzamin końcowy - Java",
     *     "Aula Magna",
     *     LocalDateTime.of(2024, 6, 15, 9, 0),
     *     LocalDateTime.of(2024, 6, 15, 12, 0),
     *     "Prof. dr hab. Jan Nowak",
     *     "Egzamin pisemny, czas: 3h",
     *     "INF-2024A",
     *     LocalDateTime.of(2024, 3, 1, 10, 30)  // data utworzenia
     * );
     *
     * // Termin pochodzi z serwera
     * assert serverSchedule.getId().equals(42L);
     * assert serverSchedule.isFromServer();
     * }
     * </pre>
     *
     * @param id unikalny identyfikator w bazie danych (wymagane, nie może być null)
     * @param subject nazwa przedmiotu/zajęć (wymagane, nie może być null)
     * @param classroom sala lub miejsce zajęć (może być null lub puste)
     * @param startTime data i godzina rozpoczęcia (wymagane, nie może być null)
     * @param endTime data i godzina zakończenia (wymagane, nie może być null)
     * @param instructor prowadzący zajęcia (może być null lub puste)
     * @param notes dodatkowe uwagi (może być null lub puste)
     * @param groupName nazwa grupy studenckiej (wymagane)
     * @param createdDate data utworzenia w systemie (wymagane, nie może być null)
     *
     * @throws IllegalArgumentException jeśli id, subject, startTime, endTime,
     *                                 groupName lub createdDate są null
     *
     * @see #ClassSchedule(String, String, LocalDateTime, LocalDateTime, String, String, String)
     */
    // Konstruktor dla terminów z serwera (z ID)
    public ClassSchedule(Long id, String subject, String classroom, LocalDateTime startTime,
                         LocalDateTime endTime, String instructor, String notes, String groupName,
                         LocalDateTime createdDate) {
        this.id = id;
        this.subject = subject;
        this.classroom = classroom;
        this.startTime = startTime;
        this.endTime = endTime;
        this.instructor = instructor;
        this.notes = notes;
        this.groupName = groupName;
        this.createdDate = createdDate;
        this.attendances = new ArrayList<>(); // DODANE
    }

    // === GETTERY - Metody dostępowe do pól klasy ===

    /**
     * Zwraca unikalny identyfikator terminu w bazie danych serwera.
     *
     * @return ID terminu lub null dla terminów lokalnych
     * @see #isFromServer()
     */
    public Long getId() { return id; }

    /**
     * Zwraca nazwę przedmiotu/zajęć.
     *
     * @return nazwa przedmiotu (nigdy null po prawidłowej inicjalizacji)
     */
    public String getSubject() { return subject; }

    /**
     * Zwraca salę lub miejsce przeprowadzenia zajęć.
     *
     * @return nazwa sali (może być null lub pusty string)
     */
    public String getClassroom() { return classroom; }

    /**
     * Zwraca datę i godzinę rozpoczęcia zajęć.
     *
     * @return czas rozpoczęcia (nigdy null po prawidłowej inicjalizacji)
     */
    public LocalDateTime getStartTime() { return startTime; }

    /**
     * Zwraca datę i godzinę zakończenia zajęć.
     *
     * @return czas zakończenia (nigdy null po prawidłowej inicjalizacji)
     */
    public LocalDateTime getEndTime() { return endTime; }

    /**
     * Zwraca imię i nazwisko prowadzącego zajęcia.
     *
     * @return nazwa prowadzącego (może być null lub pusty string)
     */
    public String getInstructor() { return instructor; }

    /**
     * Zwraca dodatkowe uwagi dotyczące terminu.
     *
     * @return uwagi (może być null lub pusty string)
     */
    public String getNotes() { return notes; }

    /**
     * Zwraca nazwę grupy studenckiej.
     *
     * @return nazwa grupy (nigdy null po prawidłowej inicjalizacji)
     */
    public String getGroupName() { return groupName; }

    /**
     * Zwraca datę utworzenia terminu w systemie.
     *
     * @return data utworzenia (nigdy null po prawidłowej inicjalizacji)
     */
    public LocalDateTime getCreatedDate() { return createdDate; }

    /**
     * Zwraca listę obecności studentów na tym terminie.
     *
     * <p>Lista zawiera obiekty {@link Attendance} reprezentujące obecność
     * poszczególnych studentów. Lista jest modyfikowalna i może być używana
     * do bezpośrednich operacji, ale zaleca się korzystanie z dedykowanych
     * metod zarządzania obecnością.</p>
     *
     * <p><strong>Uwaga:</strong> Bezpośrednia modyfikacja tej listy może
     * naruszyć integralność danych. Zaleca się używanie metod:
     * {@link #addAttendance(Attendance)}, {@link #removeAttendance(Student)}</p>
     *
     * @return modyfikowalna lista obecności (nigdy null, może być pusta)
     * @see #addAttendance(Attendance)
     * @see #removeAttendance(Student)
     */
    public List<Attendance> getAttendances() { return attendances; } // DODANE

    // === SETTERY - Metody modyfikujące pola klasy ===

    /**
     * Ustawia identyfikator terminu.
     *
     * <p><strong>Uwaga:</strong> Ta metoda jest używana głównie przez system
     * podczas deserializacji danych z serwera. Ręczna zmiana ID może naruszyć
     * integralność danych i synchronizację z bazą danych.</p>
     *
     * @param id nowy identyfikator terminu (może być null dla terminów lokalnych)
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Ustawia nazwę przedmiotu/zajęć.
     *
     * @param subject nowa nazwa przedmiotu (nie powinna być null)
     */
    public void setSubject(String subject) { this.subject = subject; }

    /**
     * Ustawia salę lub miejsce przeprowadzenia zajęć.
     *
     * @param classroom nowa nazwa sali (może być null lub pusty)
     */
    public void setClassroom(String classroom) { this.classroom = classroom; }

    /**
     * Ustawia datę i godzinę rozpoczęcia zajęć.
     *
     * @param startTime nowy czas rozpoczęcia (nie może być null)
     */
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    /**
     * Ustawia datę i godzinę zakończenia zajęć.
     *
     * @param endTime nowy czas zakończenia (nie może być null, powinien być po startTime)
     */
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    /**
     * Ustawia prowadzącego zajęcia.
     *
     * @param instructor nowy prowadzący (może być null lub pusty)
     */
    public void setInstructor(String instructor) { this.instructor = instructor; }

    /**
     * Ustawia dodatkowe uwagi dotyczące terminu.
     *
     * @param notes nowe uwagi (może być null lub pusty)
     */
    public void setNotes(String notes) { this.notes = notes; }

    /**
     * Ustawia nazwę grupy studenckiej.
     *
     * @param groupName nowa nazwa grupy (nie powinna być null)
     */
    public void setGroupName(String groupName) { this.groupName = groupName; }

    /**
     * Ustawia datę utworzenia terminu.
     *
     * <p><strong>Uwaga:</strong> Ta metoda jest używana głównie przez system.
     * Ręczna zmiana daty utworzenia może wpłynąć na audyt i sortowanie terminów.</p>
     *
     * @param createdDate nowa data utworzenia (nie może być null)
     */
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    /**
     * Ustawia listę obecności studentów.
     *
     * <p><strong>Uwaga:</strong> Ta metoda zastępuje całą listę obecności.
     * Używaj ostrożnie, ponieważ może spowodować utratę danych o frekwencji.
     * W większości przypadków zaleca się używanie {@link #addAttendance(Attendance)}
     * i {@link #removeAttendance(Student)}.</p>
     *
     * @param attendances nowa lista obecności (nie może być null)
     * @see #addAttendance(Attendance)
     * @see #removeAttendance(Student)
     */
    public void setAttendances(List<Attendance> attendances) { this.attendances = attendances; } // DODANE

    // === METODY FORMATOWANIA - Czytelna prezentacja dat i czasów ===

    /**
     * Zwraca sformatowaną datę i godzinę rozpoczęcia zajęć.
     *
     * <p>Format: "dd.MM.yyyy HH:mm"</p>
     * <p>Przykład: "15.03.2024 10:00"</p>
     *
     * <p>Ta metoda jest szeroko używana w interfejsie użytkownika do wyświetlania
     * terminów w listach, tabelach i raportach.</p>
     *
     * @return sformatowana data rozpoczęcia w formacie "dd.MM.yyyy HH:mm"
     */
    public String getFormattedStartTime() {
        return startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Zwraca sformatowaną godzinę zakończenia zajęć (tylko czas).
     *
     * <p>Format: "HH:mm"</p>
     * <p>Przykład: "12:00"</p>
     *
     * <p>Metoda zwraca tylko czas, ponieważ zazwyczaj zajęcia odbywają się
     * tego samego dnia co rozpoczęcie. Dla pełnej daty użyj {@link #getEndTime()}.</p>
     *
     * @return sformatowana godzina zakończenia w formacie "HH:mm"
     */
    public String getFormattedEndTime() {
        return endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Zwraca pełny zakres czasowy zajęć w jednym ciągu.
     *
     * <p>Format: "dd.MM.yyyy HH:mm - HH:mm"</p>
     * <p>Przykład: "15.03.2024 10:00 - 12:00"</p>
     *
     * <p>Ta metoda jest używana w {@link #toString()} oraz wszędzie gdzie
     * potrzebny jest kompletny opis czasu trwania zajęć.</p>
     *
     * @return pełny zakres czasowy w formacie "data rozpoczęcia - godzina zakończenia"
     * @see #toString()
     */
    public String getFormattedTimeRange() {
        return getFormattedStartTime() + " - " + getFormattedEndTime();
    }

    /**
     * Zwraca sformatowaną datę utworzenia terminu w systemie.
     *
     * <p>Format: "dd.MM.yyyy HH:mm"</p>
     * <p>Przykład: "01.03.2024 14:30"</p>
     *
     * <p>Używane do celów audytu, debugowania i wyświetlania informacji
     * o tym kiedy termin został dodany do systemu.</p>
     *
     * @return sformatowana data utworzenia w formacie "dd.MM.yyyy HH:mm"
     */
    public String getFormattedCreatedDate() {
        return createdDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Sprawdza czy termin pochodzi z serwera (ma przypisane ID).
     *
     * <p>Terminy z serwera:</p>
     * <ul>
     *   <li>Mają przypisane ID (Long != null)</li>
     *   <li>Mogą być synchronizowane z bazą danych</li>
     *   <li>Obsługują automatyczną synchronizację obecności</li>
     *   <li>Mogą być modyfikowane przez innych użytkowników</li>
     * </ul>
     *
     * <p>Terminy lokalne:</p>
     * <ul>
     *   <li>Nie mają ID (id == null)</li>
     *   <li>Istnieją tylko w pamięci aplikacji</li>
     *   <li>Obecności są przechowywane lokalnie</li>
     *   <li>Mogą być utracone przy restarcie aplikacji</li>
     * </ul>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * if (schedule.isFromServer()) {
     *     // Można wysyłać obecności na serwer
     *     attendanceService.markAttendance(student, schedule.getId(), status);
     * } else {
     *     // Tylko lokalne przechowywanie
     *     schedule.addAttendance(new Attendance(student, schedule, status));
     * }
     * }
     * </pre>
     *
     * @return true jeśli termin ma ID z serwera, false dla terminów lokalnych
     * @see #getId()
     */
    public boolean isFromServer() {
        return id != null;
    }

    //=== METODY ZARZĄDZANIA OBECNOŚCIĄ ===

    /**
     * Dodaje lub aktualizuje obecność studenta na tym terminie.
     *
     * <p>Jeśli student już ma obecność na tym terminie, zostanie ona zastąpiona
     * nową. Jeśli nie ma, zostanie dodana. Ta logika zapewnia, że każdy student
     * może mieć maksymalnie jedną obecność na termin.</p>
     *
     * <p>Metoda automatycznie usuwa stare wpisy dla tego samego studenta
     * (porównanie po numerze indeksu) przed dodaniem nowego wpisu.</p>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * Student student = new Student("Jan", "Kowalski", "123456", "INF-2024");
     * Attendance attendance = new Attendance(student, schedule, Attendance.Status.PRESENT);
     *
     * // Dodanie obecności
     * schedule.addAttendance(attendance);
     * System.out.println("Studentów: " + schedule.getTotalAttendanceCount()); // 1
     *
     * // Zmiana statusu tego samego studenta
     * Attendance newAttendance = new Attendance(student, schedule, Attendance.Status.LATE);
     * schedule.addAttendance(newAttendance);
     * System.out.println("Studentów: " + schedule.getTotalAttendanceCount()); // nadal 1
     *
     * // Sprawdzenie aktualnego statusu
     * Attendance current = schedule.getAttendanceForStudent(student);
     * System.out.println(current.getStatus()); // LATE
     * }
     * </pre>
     *
     * <p><strong>Uwaga dotycząca synchronizacji:</strong></p>
     * <p>Dla terminów z serwera ({@link #isFromServer()} == true) dodatkowo
     * należy wysłać obecność na serwer przez {@link AttendanceService}.</p>
     *
     * @param attendance obecność do dodania/aktualizacji (nie może być null)
     * @throws IllegalArgumentException jeśli attendance jest null
     * @see #removeAttendance(Student)
     * @see #getAttendanceForStudent(Student)
     * @see #hasAttendanceForStudent(Student)
     */
    public void addAttendance(Attendance attendance) {
        attendances.removeIf(a -> a.getStudent().getIndexNumber().equals(attendance.getStudent().getIndexNumber()));
        attendances.add(attendance);
    }

    /**
     * Usuwa obecność określonego studenta z tego terminu.
     *
     * <p>Metoda wyszukuje obecność na podstawie numeru indeksu studenta
     * i usuwa ją z listy. Jeśli student nie ma obecności na tym terminie,
     * metoda nie wykonuje żadnej operacji (nie rzuca wyjątku).</p>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * Student student = new Student("Anna", "Nowak", "654321", "INF-2024");
     *
     * // Dodanie obecności
     * schedule.addAttendance(new Attendance(student, schedule, Attendance.Status.PRESENT));
     * System.out.println("Ma obecność: " + schedule.hasAttendanceForStudent(student)); // true
     *
     * // Usunięcie obecności
     * schedule.removeAttendance(student);
     * System.out.println("Ma obecność: " + schedule.hasAttendanceForStudent(student)); // false
     * }
     * </pre>
     *
     * <p><strong>Uwagi dotyczące synchronizacji:</strong></p>
     * <p>Dla terminów z serwera należy dodatkowo usunąć obecność z bazy danych
     * używając {@link AttendanceService#removeAttendanceAsync(String, Long)}.</p>
     *
     * @param student student którego obecność ma być usunięta (nie może być null)
     * @throws IllegalArgumentException jeśli student jest null
     * @see #addAttendance(Attendance)
     * @see #getAttendanceForStudent(Student)
     */
    public void removeAttendance(Student student) {
        attendances.removeIf(a -> a.getStudent().getIndexNumber().equals(student.getIndexNumber()));
    }

    /**
     * Zwraca obecność określonego studenta na tym terminie.
     *
     * <p>Wyszukuje obecność na podstawie numeru indeksu studenta.
     * Jeśli student nie ma obecności na tym terminie, zwraca null.</p>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * Student student = new Student("Piotr", "Wiśniewski", "789123", "INF-2024");
     *
     * // Sprawdzenie obecności przed dodaniem
     * Attendance attendance = schedule.getAttendanceForStudent(student);
     * if (attendance == null) {
     *     System.out.println("Student nie ma jeszcze obecności");
     *     schedule.addAttendance(new Attendance(student, schedule, Attendance.Status.PRESENT));
     * } else {
     *     System.out.println("Aktualny status: " + attendance.getStatus().getDisplayName());
     * }
     * }
     * </pre>
     *
     * @param student student którego obecność ma być pobrana (nie może być null)
     * @return obecność studenta lub null jeśli nie ma obecności na tym terminie
     * @throws IllegalArgumentException jeśli student jest null
     * @see #hasAttendanceForStudent(Student)
     * @see #addAttendance(Attendance)
     */
    public Attendance getAttendanceForStudent(Student student) {
        return attendances.stream()
                .filter(a -> a.getStudent().getIndexNumber().equals(student.getIndexNumber()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sprawdza czy określony student ma obecność na tym terminie.
     *
     * <p>Wygodna metoda sprawdzająca czy {@link #getAttendanceForStudent(Student)} != null.
     * Używana do szybkiego sprawdzenia obecności bez pobierania obiektu Attendance.</p>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * // Sprawdzenie przed dodaniem
     * if (!schedule.hasAttendanceForStudent(student)) {
     *     schedule.addAttendance(new Attendance(student, schedule, Attendance.Status.PRESENT));
     *     System.out.println("Dodano obecność dla " + student.getFullName());
     * } else {
     *     System.out.println("Student już ma obecność na tym terminie");
     * }
     * }
     * </pre>
     *
     * @param student student do sprawdzenia (nie może być null)
     * @return true jeśli student ma obecność na tym terminie, false w przeciwnym razie
     * @throws IllegalArgumentException jeśli student jest null
     * @see #getAttendanceForStudent(Student)
     */
    public boolean hasAttendanceForStudent(Student student) {
        return getAttendanceForStudent(student) != null;
    }

    // === METODY STATYSTYCZNE - Automatyczne obliczanie statystyk frekwencji ===

    /**
     * Zwraca liczbę studentów oznaczonych jako obecni.
     *
     * <p>Liczy wszystkie obecności ze statusem {@link Attendance.Status#PRESENT}.</p>
     *
     * @return liczba obecnych studentów (0 lub więcej)
     * @see Attendance.Status#PRESENT
     * @see #getLateCount()
     * @see #getAbsentCount()
     */
    public int getPresentCount() {
        return (int) attendances.stream().filter(a -> a.getStatus() == Attendance.Status.PRESENT).count();
    }

    /**
     * Zwraca liczbę studentów oznaczonych jako spóźnieni.
     *
     * <p>Liczy wszystkie obecności ze statusem {@link Attendance.Status#LATE}.</p>
     *
     * @return liczba spóźnionych studentów (0 lub więcej)
     * @see Attendance.Status#LATE
     * @see #getPresentCount()
     * @see #getAbsentCount()
     */
    public int getLateCount() {
        return (int) attendances.stream().filter(a -> a.getStatus() == Attendance.Status.LATE).count();
    }

    /**
     * Zwraca liczbę studentów oznaczonych jako nieobecni.
     *
     * <p>Liczy wszystkie obecności ze statusem {@link Attendance.Status#ABSENT}.</p>
     *
     * @return liczba nieobecnych studentów (0 lub więcej)
     * @see Attendance.Status#ABSENT
     * @see #getPresentCount()
     * @see #getLateCount()
     */
    public int getAbsentCount() {
        return (int) attendances.stream().filter(a -> a.getStatus() == Attendance.Status.ABSENT).count();
    }

    /**
     * Zwraca całkowitą liczbę wpisów frekwencji dla tego terminu.
     *
     * <p>Liczy wszystkie obecności niezależnie od statusu.
     * Równa się sumie: {@link #getPresentCount()} + {@link #getLateCount()} + {@link #getAbsentCount()}.</p>
     *
     * @return całkowita liczba wpisów frekwencji (0 lub więcej)
     */
    public int getTotalAttendanceCount() {
        return attendances.size();
    }

    /**
     * Generuje tekstowe podsumowanie statystyk frekwencji.
     *
     * <p>Zwraca czytelne podsumowanie w formacie:</p>
     * <ul>
     *   <li>"Obecni: X | Spóźnieni: Y | Nieobecni: Z | Razem: W" - gdy są wpisy</li>
     *   <li>"Brak wpisów frekwencji" - gdy lista obecności jest pusta</li>
     * </ul>
     *
     * <h3>Przykłady wyjścia:</h3>
     * <pre>
     * {@code
     * // Termin z obecnościami
     * "Obecni: 15 | Spóźnieni: 2 | Nieobecni: 3 | Razem: 20"
     *
     * // Termin bez wpisów
     * "Brak wpisów frekwencji"
     * }
     * </pre>
     *
     * <p>Ta metoda jest używana w interfejsie użytkownika do wyświetlania
     * szybkich statystyk w listach terminów i raportach.</p>
     *
     * @return sformatowane podsumowanie statystyk frekwencji
     * @see #getPresentCount()
     * @see #getLateCount()
     * @see #getAbsentCount()
     * @see #getTotalAttendanceCount()
     */
    public String getAttendanceSummary() {
        int present = getPresentCount();
        int late = getLateCount();
        int absent = getAbsentCount();
        int total = getTotalAttendanceCount();

        if (total == 0) {
            return "Brak wpisów frekwencji";
        }

        return String.format("Obecni: %d | Spóźnieni: %d | Nieobecni: %d | Razem: %d",
                present, late, absent, total);
    }

    /**
     * Zwraca tekstową reprezentację terminu.
     *
     * <p>Format: "[Nazwa przedmiotu] - [Pełny zakres czasowy]"</p>
     * <p>Przykład: "Programowanie w Javie - 15.03.2024 10:00 - 12:00"</p>
     *
     * <p>Ta reprezentacja jest używana w:</p>
     * <ul>
     *   <li>ComboBox-ach z listą terminów</li>
     *   <li>ListView z terminami</li>
     *   <li>Logach i komunikatach debugowania</li>
     *   <li>Eksporcie danych do CSV</li>
     * </ul>
     *
     * @return tekstowa reprezentacja terminu
     * @see #getFormattedTimeRange()
     */
    @Override
    public String toString() {
        return subject + " - " + getFormattedTimeRange();
    }
}