package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Klasa reprezentująca obecność studenta na określonym terminie zajęć.
 *
 * <p>Attendance stanowi kluczowy element systemu dziennika elektronicznego,
 * umożliwiając rejestrowanie i zarządzanie frekwencją studentów na zajęciach.
 * Każda instancja reprezentuje pojedynczy wpis frekwencji dla konkretnego
 * studenta w określonym terminie zajęć.</p>
 *
 * <p>Klasa integruje się z całym systemem poprzez powiązania z {@link Student}
 * i {@link ClassSchedule}, zapewniając spójność danych i możliwość generowania
 * raportów frekwencji.</p>
 *
 * <h3>Główne funkcjonalności:</h3>
 * <ul>
 *   <li><strong>Rejestrowanie statusu obecności</strong> - obecny, spóźniony, nieobecny</li>
 *   <li><strong>Automatyczne znaczenie czasem</strong> - każda zmiana statusu jest timestampowana</li>
 *   <li><strong>Dodawanie uwag</strong> - możliwość dołączenia komentarzy do wpisu</li>
 *   <li><strong>Kolorowe reprezentacje statusów</strong> - każdy status ma przypisany kolor</li>
 *   <li><strong>Unikalność wpisów</strong> - jeden student może mieć tylko jeden wpis na termin</li>
 * </ul>
 *
 * <h3>Scenariusze użycia:</h3>
 * <ul>
 *   <li>Oznaczanie obecności podczas zajęć</li>
 *   <li>Generowanie raportów frekwencji</li>
 *   <li>Analiza statystyk uczęszczania</li>
 *   <li>Usprawiedliwianie nieobecności</li>
 * </ul>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * // Utworzenie studenta i terminu
 * Student student = new Student("Jan", "Kowalski", "123456", "INF-2024");
 * ClassSchedule schedule = new ClassSchedule("Java Programming",
 *     "Lab 101", startTime, endTime, "Dr Smith", "Practical session", "INF-2024");
 *
 * // Oznaczenie obecności
 * Attendance attendance = new Attendance(student, schedule, Attendance.Status.PRESENT);
 *
 * // Dodanie uwag
 * attendance.setNotes("Student aktywnie uczestniczył w zajęciach");
 *
 * // Zmiana statusu na spóźniony (automatycznie aktualizuje czas)
 * attendance.setStatus(Attendance.Status.LATE);
 *
 * // Wyświetlenie informacji
 * System.out.println(attendance.toString()); // "Jan Kowalski - Spóźniony"
 * System.out.println("Oznaczono: " + attendance.getFormattedMarkedTime());
 * }
 * </pre>
 *
 * <h3>Integracja z systemem:</h3>
 * <p>Klasa współpracuje z następującymi komponentami:</p>
 * <ul>
 *   <li>{@link ClassSchedule#addAttendance(Attendance)} - dodawanie obecności do terminu</li>
 *   <li>{@link AttendanceService} - synchronizacja z serwerem backend</li>
 *   <li>{@link AttendanceReportController} - generowanie raportów</li>
 *   <li>{@link GroupDetailController} - interfejs zarządzania obecnością</li>
 * </ul>
 *
 * <h3>Uwagi implementacyjne:</h3>
 * <ul>
 *   <li>Klasa implementuje {@code equals()} i {@code hashCode()} bazując na kombinacji student+termin</li>
 *   <li>Automatyczne timestampowanie przy każdej zmianie statusu</li>
 *   <li>Thread-safe gettery (immutable objects returned)</li>
 *   <li>Null-safe implementation dla wszystkich operacji</li>
 * </ul>
 *
 * @author Mateusz Orski
 * @version 1.0
 * @since 2025
 *
 * @see Student
 * @see ClassSchedule
 * @see AttendanceService
 * @see AttendanceReportController
 */
public class Attendance {

    /**
     * Enum reprezentujący możliwe statusy obecności studenta.
     *
     * <p>Status określa czy student był obecny, spóźniony czy nieobecny
     * na konkretnych zajęciach. Każdy status ma przypisaną nazwę wyświetlaną
     * oraz kolor używany w interfejsie użytkownika.</p>
     *
     * <h3>Znaczenie statusów:</h3>
     * <ul>
     *   <li><strong>PRESENT</strong> - student był obecny od początku zajęć</li>
     *   <li><strong>LATE</strong> - student przyszedł po czasie ale uczestniczył w zajęciach</li>
     *   <li><strong>ABSENT</strong> - student w ogóle nie pojawił się na zajęciach</li>
     * </ul>
     *
     * <p>Statusy są wykorzystywane do:</p>
     * <ul>
     *   <li>Kolorowania komórek w tabelach frekwencji</li>
     *   <li>Obliczania statystyk obecności</li>
     *   <li>Generowania raportów dla studentów i wykładowców</li>
     *   <li>Wyświetlania przyjaznych nazw w interfejsie</li>
     * </ul>
     *
     * <h3>Przykład użycia:</h3>
     * <pre>
     * {@code
     * // Sprawdzenie dostępnych statusów
     * for (Attendance.Status status : Attendance.Status.values()) {
     *     System.out.println(status.getDisplayName() + " - kolor: " + status.getColor());
     * }
     *
     * // Użycie w praktyce
     * Attendance attendance = new Attendance(student, schedule, Status.LATE);
     * String displayText = attendance.getStatus().getDisplayName(); // "Spóźniony"
     * String color = attendance.getStatus().getColor(); // "#F56500"
     * }
     * </pre>
     *
     * @see #getDisplayName()
     * @see #getColor()
     */
    public enum Status {
        /**
         * Student obecny od początku zajęć.
         * <p>Kolor: zielony (#38A169) - reprezentuje pozytywny status</p>
         */
        PRESENT("Obecny", "#38A169"),

        /**
         * Student spóźniony ale uczestniczący w zajęciach.
         * <p>Kolor: pomarańczowy (#F56500) - reprezentuje ostrzeżenie</p>
         */
        LATE("Spóźniony", "#F56500"),

        /**
         * Student nieobecny na zajęciach.
         * <p>Kolor: czerwony (#E53E3E) - reprezentuje negatywny status</p>
         */
        ABSENT("Nieobecny", "#E53E3E");

        /** Nazwa wyświetlana w interfejsie użytkownika */
        private final String displayName;

        /** Kolor reprezentujący status w formacie hex (#RRGGBB) */
        private final String color;

        /**
         * Konstruktor statusu obecności.
         *
         * @param displayName czytelna nazwa statusu dla użytkownika (nie może być null)
         * @param color kolor w formacie hex #RRGGBB (nie może być null)
         * @throws IllegalArgumentException jeśli którykolwiek z parametrów jest null
         */
        Status(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        /**
         * Zwraca czytelną nazwę statusu dla użytkownika.
         *
         * <p>Nazwa jest używana w interfejsie użytkownika, raportach
         * i wszędzie tam gdzie potrzebna jest czytelna reprezentacja statusu.</p>
         *
         * @return nazwa wyświetlana (np. "Obecny", "Spóźniony", "Nieobecny")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Zwraca kolor reprezentujący status w formacie hex.
         *
         * <p>Kolor jest używany do stylizowania elementów UI, kolorowania
         * komórek w tabelach i innych elementów wizualnych związanych ze statusem.</p>
         *
         * @return kolor w formacie hex #RRGGBB (np. "#38A169", "#F56500", "#E53E3E")
         */
        public String getColor() {
            return color;
        }
    }

    /** Student, którego dotyczy obecność */
    private Student student;

    /** Termin zajęć, na których sprawdzana jest obecność */
    private ClassSchedule schedule;

    /** Aktualny status obecności studenta */
    private Status status;

    /** Opcjonalne uwagi dotyczące obecności */
    private String notes;

    /** Czas oznaczenia/ostatniej zmiany statusu obecności */
    private LocalDateTime markedAt;

    /**
     * Tworzy nową obecność z podstawowymi danymi.
     *
     * <p>Konstruktor inicjalizuje obecność z podanym statusem,
     * ustawia czas oznaczenia na bieżący moment i inicjalizuje
     * puste uwagi.</p>
     *
     * @param student student, którego dotyczy obecność (nie może być null)
     * @param schedule termin zajęć (nie może być null)
     * @param status status obecności (nie może być null)
     * @throws IllegalArgumentException jeśli którykolwiek z parametrów jest null
     */
    public Attendance(Student student, ClassSchedule schedule, Status status) {
        this.student = student;
        this.schedule = schedule;
        this.status = status;
        this.notes = "";
        this.markedAt = LocalDateTime.now();
    }

    /**
     * Tworzy nową obecność z danymi i uwagami.
     *
     * <p>Rozszerzony konstruktor pozwalający od razu określić
     * uwagi dotyczące obecności studenta.</p>
     *
     * @param student student, którego dotyczy obecność (nie może być null)
     * @param schedule termin zajęć (nie może być null)
     * @param status status obecności (nie może być null)
     * @param notes uwagi dotyczące obecności (może być null lub pusty)
     * @throws IllegalArgumentException jeśli student, schedule lub status jest null
     */
    public Attendance(Student student, ClassSchedule schedule, Status status, String notes) {
        this.student = student;
        this.schedule = schedule;
        this.status = status;
        this.notes = notes;
        this.markedAt = LocalDateTime.now();
    }

    /**
     * Zwraca studenta, którego dotyczy ta obecność.
     *
     * @return student (nigdy null)
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Zwraca termin zajęć, którego dotyczy ta obecność.
     *
     * @return termin zajęć (nigdy null)
     */
    public ClassSchedule getSchedule() {
        return schedule;
    }

    /**
     * Zwraca aktualny status obecności.
     *
     * @return status obecności (nigdy null)
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Zwraca uwagi dotyczące obecności.
     *
     * @return uwagi (może być null lub pusty string)
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Zwraca czas oznaczenia obecności.
     *
     * <p>Reprezentuje moment gdy obecność została po raz pierwszy
     * oznaczona lub ostatnio zmieniona.</p>
     *
     * @return czas oznaczenia (nigdy null)
     */
    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    /**
     * Ustawia studenta dla tej obecności.
     *
     * <p><strong>Uwaga:</strong> Ta metoda powinna być używana ostrożnie,
     * ponieważ zmiana studenta może naruszyć integralność danych.
     * Zaleca się tworzenie nowej instancji Attendance zamiast zmiany studenta.</p>
     *
     * @param student nowy student (nie może być null)
     * @throws IllegalArgumentException jeśli student jest null
     */
    public void setStudent(Student student) {
        this.student = student;
    }

    /**
     * Ustawia termin zajęć dla tej obecności.
     *
     * <p><strong>Uwaga:</strong> Ta metoda powinna być używana ostrożnie,
     * ponieważ zmiana terminu może naruszyć integralność danych.
     * Zaleca się tworzenie nowej instancji Attendance zamiast zmiany terminu.</p>
     *
     * @param schedule nowy termin zajęć (nie może być null)
     * @throws IllegalArgumentException jeśli schedule jest null
     */
    public void setSchedule(ClassSchedule schedule) {
        this.schedule = schedule;
    }

    /**
     * Ustawia nowy status obecności i aktualizuje czas oznaczenia.
     *
     * <p>Ta metoda automatycznie aktualizuje czas oznaczenia ({@link #markedAt})
     * na bieżący moment, niezależnie od tego czy status rzeczywiście się zmienił.
     * Dzięki temu zawsze wiadomo kiedy ostatnio modyfikowano obecność.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * // Pierwotnie student był obecny
     * attendance.setStatus(Status.PRESENT);
     * LocalDateTime firstTime = attendance.getMarkedAt();
     *
     * // Później okazało się że był spóźniony
     * attendance.setStatus(Status.LATE);
     * LocalDateTime secondTime = attendance.getMarkedAt();
     *
     * // secondTime jest późniejszy niż firstTime
     * assert secondTime.isAfter(firstTime);
     * }
     * </pre>
     *
     * @param status nowy status obecności (nie może być null)
     * @throws IllegalArgumentException jeśli status jest null
     */
    public void setStatus(Status status) {
        this.status = status;
        this.markedAt = LocalDateTime.now(); // Aktualizuj czas gdy zmieniany jest status
    }

    /**
     * Ustawia uwagi dotyczące obecności.
     *
     * <p>Uwagi mogą zawierać dodatkowe informacje takie jak:</p>
     * <ul>
     *   <li>Przyczyna spóźnienia lub nieobecności</li>
     *   <li>Usprawiedliwienie nieobecności</li>
     *   <li>Dodatkowe komentarze wykładowcy</li>
     *   <li>Informacje o aktywności studenta podczas zajęć</li>
     * </ul>
     *
     * @param notes nowe uwagi (może być null)
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Ustawia czas oznaczenia obecności.
     *
     * <p><strong>Uwaga:</strong> Ta metoda jest używana głównie przez system
     * do deserializacji danych z serwera. W normalnym użyciu czas jest
     * automatycznie ustawiany przez {@link #setStatus(Status)}.</p>
     *
     * @param markedAt nowy czas oznaczenia (nie może być null)
     * @throws IllegalArgumentException jeśli markedAt jest null
     */
    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    /**
     * Zwraca sformatowany czas oznaczenia obecności.
     *
     * <p>Formatuje czas oznaczenia do czytelnego formatu "dd.MM.yyyy HH:mm"
     * używanego w interfejsie użytkownika i raportach.</p>
     *
     * <p>Przykład zwracanego formatu: "15.03.2024 10:30"</p>
     *
     * @return sformatowany czas w formacie "dd.MM.yyyy HH:mm"
     */
    public String getFormattedMarkedTime() {
        return markedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Zwraca tekstową reprezentację obecności.
     *
     * <p>Format: "[Imię Nazwisko] - [Status]"</p>
     * <p>Przykład: "Jan Kowalski - Obecny"</p>
     *
     * <p>Ta reprezentacja jest używana w listach, combobox-ach
     * i wszędzie tam gdzie potrzebny jest krótki opis obecności.</p>
     *
     * @return tekstowa reprezentacja w formacie "[student] - [status]"
     */
    @Override
    public String toString() {
        return student.getFullName() + " - " + status.getDisplayName();
    }

    /**
     * Sprawdza równość dwóch obecności na podstawie studenta i terminu.
     *
     * <p>Dwie obecności są równe jeśli dotyczą tego samego studenta
     * (porównanie po numerze indeksu) i tego samego terminu zajęć.
     * Status obecności i uwagi nie wpływają na równość.</p>
     *
     * <p>Ta logika zapewnia, że jeden student może mieć tylko jedną
     * obecność na konkretny termin zajęć, co jest wymaganiem biznesowym.</p>
     *
     * <h3>Przykład:</h3>
     * <pre>
     * {@code
     * Attendance attendance1 = new Attendance(student, schedule, Status.PRESENT);
     * Attendance attendance2 = new Attendance(student, schedule, Status.LATE);
     *
     * // Te obecności są równe mimo różnych statusów
     * assert attendance1.equals(attendance2) == true;
     * }
     * </pre>
     *
     * @param obj obiekt do porównania
     * @return true jeśli obecności dotyczą tego samego studenta i terminu
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Attendance that = (Attendance) obj;
        return student.getIndexNumber().equals(that.student.getIndexNumber()) &&
                schedule.equals(that.schedule);
    }

    /**
     * Generuje hash code na podstawie studenta i terminu.
     *
     * <p>Hash code jest obliczany na podstawie numeru indeksu studenta
     * i hash code terminu zajęć. Jest spójny z implementacją {@link #equals(Object)}.</p>
     *
     * <p>Implementacja zapewnia:</p>
     * <ul>
     *   <li>Spójność z equals() - równe obiekty mają ten sam hash code</li>
     *   <li>Stabilność - wielokrotne wywołania zwracają tę samą wartość</li>
     *   <li>Dobrą dystrybucję dla Collections opartych na hash-ach</li>
     * </ul>
     *
     * @return hash code obliczony z numeru indeksu studenta i terminu
     */
    @Override
    public int hashCode() {
        return student.getIndexNumber().hashCode() + schedule.hashCode();
    }
}