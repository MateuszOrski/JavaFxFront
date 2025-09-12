package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Klasa reprezentująca studenta w systemie dziennika elektronicznego.
 *
 * <p>Student stanowi jedną z podstawowych encji systemu dziennika online,
 * reprezentując osobę uczęszczającą na zajęcia w ramach określonej grupy studenckiej.
 * Klasa przechowuje dane osobowe studenta, numer indeksu oraz informacje
 * o przynależności do grupy.</p>
 *
 * <p>Każdy student w systemie charakteryzuje się następującymi cechami:</p>
 * <ul>
 *   <li><strong>Dane osobowe</strong> - imię i nazwisko studenta</li>
 *   <li><strong>Numer indeksu</strong> - unikalny 6-cyfrowy identyfikator</li>
 *   <li><strong>Przypisanie do grupy</strong> - nazwa grupy studenckiej (opcjonalne)</li>
 *   <li><strong>Data dodania</strong> - automatyczny znacznik czasowy utworzenia</li>
 * </ul>
 *
 * <h3>Zarządzanie grupami:</h3>
 * <p>Student może znajdować się w jednym z następujących stanów przypisania:</p>
 * <ul>
 *   <li><strong>Bez grupy</strong> - groupName = null, student dostępny do przypisania</li>
 *   <li><strong>Z grupą</strong> - groupName zawiera nazwę grupy studenckiej</li>
 *   <li><strong>Przenoszony</strong> - tymczasowy stan podczas zmiany grup</li>
 * </ul>
 *
 * <h3>Unikalność i walidacja:</h3>
 * <p>System gwarantuje unikalność studentów na podstawie numeru indeksu.
 * Numer indeksu musi spełniać następujące wymagania:</p>
 * <ul>
 *   <li>Składa się z dokładnie 6 cyfr (np. "123456")</li>
 *   <li>Jest unikalny w całym systemie</li>
 *   <li>Nie może zostać zmieniony po utworzeniu studenta</li>
 * </ul>
 *
 * <h3>Integracja z systemem:</h3>
 * <p>Klasa Student współpracuje z następującymi komponentami:</p>
 * <ul>
 *   <li>{@link Group} - grupa do której student może być przypisany</li>
 *   <li>{@link Attendance} - obecności studenta na zajęciach</li>
 *   <li>{@link ClassSchedule} - terminy w których student może uczestniczyć</li>
 *   <li>{@link StudentService} - serwis zarządzania studentami przez API</li>
 * </ul>
 *
 * <h3>Przykłady użycia:</h3>
 * <pre>
 * {@code
 * // Utworzenie studenta bez grupy
 * Student student = new Student("Jan", "Kowalski", "123456", null);
 * System.out.println("Student: " + student.getFullName());
 * System.out.println("Indeks: " + student.getIndexNumber());
 * System.out.println("Grupa: " + (student.getGroupName() != null ? student.getGroupName() : "Brak"));
 * System.out.println("Dodany: " + student.getFormattedDate());
 *
 * // Przypisanie do grupy
 * student.setGroupName("INF-2024");
 * System.out.println("Przypisano do grupy: " + student.getGroupName());
 *
 * // Tekstowa reprezentacja
 * System.out.println(student.toString()); // "Jan Kowalski (123456) - INF-2024"
 * }
 * </pre>
 *
 * <h3>Scenariusze życiowe studenta w systemie:</h3>
 * <ol>
 *   <li><strong>Utworzenie</strong> - student dodawany globalnie bez grupy</li>
 *   <li><strong>Przypisanie</strong> - student przypisywany do konkretnej grupy</li>
 *   <li><strong>Uczestnictwo</strong> - rejestrowanie obecności na zajęciach</li>
 *   <li><strong>Przeniesienie</strong> - zmiana grupy przez aktualizację groupName</li>
 *   <li><strong>Usunięcie z grupy</strong> - ustawienie groupName na null</li>
 *   <li><strong>Usunięcie z systemu</strong> - całkowite usunięcie studenta</li>
 * </ol>
 *
 * <h3>Thread Safety:</h3>
 * <p>Klasa nie jest thread-safe. W środowisku wielowątkowym należy zapewnić
 * zewnętrzną synchronizację dostępu do obiektów Student, szczególnie podczas
 * modyfikacji pól (setGroupName, etc.).</p>
 *
 * <h3>Serializacja i komunikacja z API:</h3>
 * <p>Obiekty Student są automatycznie serializowane do JSON przy komunikacji
 * z serwerem backend przez {@link StudentService}. Struktura JSON jest dopasowana
 * do oczekiwań API serwera.</p>
 *
 * @author Mateusz Orski
 * @version 1.0
 * @since 2025
 *
 * @see Group Klasa grupy studenckiej
 * @see Attendance Klasa obecności studenta
 * @see StudentService Serwis zarządzania studentami
 * @see GroupDetailController Kontroler zarządzania studentami w grupie
 * @see ModernController Kontroler główny z globalnym dodawaniem studentów
 */
public class Student {

    /**
     * Imię studenta.
     *
     * <p>Pole przechowuje imię studenta w postaci tekstu. Może zawierać
     * znaki specjalne i wielką literę na początku według konwencji.</p>
     *
     * <p>Przykłady prawidłowych wartości:</p>
     * <ul>
     *   <li>"Jan"</li>
     *   <li>"Anna-Maria"</li>
     *   <li>"José"</li>
     * </ul>
     */
    private String firstName;

    /**
     * Nazwisko studenta.
     *
     * <p>Pole przechowuje nazwisko studenta w postaci tekstu. Może zawierać
     * znaki specjalne, myślniki i prefiksy według konwencji.</p>
     *
     * <p>Przykłady prawidłowych wartości:</p>
     * <ul>
     *   <li>"Kowalski"</li>
     *   <li>"Nowak-Kowalska"</li>
     *   <li>"von Neumann"</li>
     * </ul>
     */
    private String lastName;

    /**
     * Unikalny numer indeksu studenta.
     *
     * <p>6-cyfrowy identyfikator studenta, unikalny w całym systemie.
     * Numer indeksu jest używany jako klucz główny do identyfikacji studenta
     * w operacjach bazodanowych i API.</p>
     *
     * <p><strong>Wymagania:</strong></p>
     * <ul>
     *   <li>Dokładnie 6 cyfr (format: "123456")</li>
     *   <li>Unikalność w całym systemie</li>
     *   <li>Niezmienialność po utworzeniu studenta</li>
     *   <li>Tylko cyfry, bez prefiksów lub sufiksów</li>
     * </ul>
     *
     * <p><strong>Przykłady prawidłowych numerów:</strong></p>
     * <ul>
     *   <li>"123456"</li>
     *   <li>"000001"</li>
     *   <li>"999999"</li>
     * </ul>
     */
    private String indexNumber;

    /**
     * Nazwa grupy do której student jest przypisany.
     *
     * <p>Pole może być null jeśli student nie jest przypisany do żadnej grupy.
     * Student bez grupy jest dostępny do przypisania do dowolnej grupy
     * przez administratorów systemu.</p>
     *
     * <p><strong>Możliwe stany:</strong></p>
     * <ul>
     *   <li><strong>null</strong> - student bez grupy, dostępny do przypisania</li>
     *   <li><strong>nazwa grupy</strong> - student przypisany do konkretnej grupy</li>
     * </ul>
     *
     * <p><strong>Przykłady nazw grup:</strong></p>
     * <ul>
     *   <li>"INF-2024" - grupa informatyków rocznik 2024</li>
     *   <li>"MAT-A" - grupa matematyków grupa A</li>
     *   <li>"FIZ-2023" - grupa fizyków rocznik 2023</li>
     * </ul>
     *
     * @see Group Klasa reprezentująca grupę studencką
     */
    private String groupName; // Przypisana grupa

    /**
     * Data i czas dodania studenta do systemu.
     *
     * <p>Pole jest automatycznie ustawiane na bieżący moment podczas tworzenia
     * obiektu Student. Używane do celów audytu, sortowania i wyświetlania
     * informacji o tym kiedy student został dodany do systemu.</p>
     *
     * <p>Pole jest tylko do odczytu po utworzeniu obiektu i służy jako
     * znacznik czasowy dla operacji administracyjnych.</p>
     */
    private LocalDateTime addedDate;

    /**
     * Konstruktor tworzący nowego studenta z podstawowymi danymi.
     *
     * <p>Tworzy instancję studenta z podanymi danymi osobowymi, numerem indeksu
     * i opcjonalną grupą. Data dodania jest automatycznie ustawiana na bieżący
     * moment wykonania konstruktora.</p>
     *
     * <p>Konstruktor nie wykonuje walidacji danych wejściowych - walidacja
     * jest wykonywana na poziomie interfejsu użytkownika i serwisu API.</p>
     *
     * <h3>Scenariusze użycia:</h3>
     * <ul>
     *   <li><strong>Student bez grupy:</strong> groupName = null</li>
     *   <li><strong>Student z grupą:</strong> groupName = nazwa istniejącej grupy</li>
     *   <li><strong>Import danych:</strong> tworzenie studentów z zewnętrznych źródeł</li>
     *   <li><strong>Deserializacja:</strong> odtwarzanie obiektów z JSON/bazy danych</li>
     * </ul>
     *
     * <h3>Przykłady użycia:</h3>
     * <pre>
     * {@code
     * // Student bez grupy - typowy scenariusz przy globalnym dodawaniu
     * Student newStudent = new Student("Anna", "Kowalska", "123456", null);
     *
     * // Student z grupą - scenariusz przy dodawaniu bezpośrednio do grupy
     * Student groupStudent = new Student("Piotr", "Nowak", "654321", "INF-2024");
     *
     * // Student z nazwami zawierającymi znaki specjalne
     * Student specialStudent = new Student("José María", "García-López", "789012", "FIZ-2024");
     * }
     * </pre>
     *
     * @param firstName imię studenta (może zawierać znaki specjalne)
     * @param lastName nazwisko studenta (może zawierać znaki specjalne, myślniki)
     * @param indexNumber unikalny 6-cyfrowy numer indeksu (format: "123456")
     * @param groupName nazwa grupy do przypisania lub null jeśli brak grupy
     *
     * @see #getFullName() Metoda zwracająca pełne imię i nazwisko
     * @see #getFormattedDate() Metoda formatująca datę dodania
     */
    public Student(String firstName, String lastName, String indexNumber, String groupName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.indexNumber = indexNumber;
        this.groupName = groupName;
        this.addedDate = LocalDateTime.now();
    }

    // === GETTERY - Metody dostępowe do pól klasy ===

    /**
     * Zwraca imię studenta.
     *
     * @return imię studenta (nigdy null po prawidłowej inicjalizacji)
     * @see #setFirstName(String)
     */
    public String getFirstName() { return firstName; }

    /**
     * Zwraca nazwisko studenta.
     *
     * @return nazwisko studenta (nigdy null po prawidłowej inicjalizacji)
     * @see #setLastName(String)
     */
    public String getLastName() { return lastName; }

    /**
     * Zwraca unikalny numer indeksu studenta.
     *
     * <p>Numer indeksu jest niezmienialnym identyfikatorem studenta w systemie.
     * Jest używany jako klucz główny w operacjach bazodanowych i jako parametr
     * w wywołaniach API.</p>
     *
     * @return 6-cyfrowy numer indeksu (nigdy null po prawidłowej inicjalizacji)
     * @see #setIndexNumber(String)
     */
    public String getIndexNumber() { return indexNumber; }

    /**
     * Zwraca nazwę grupy do której student jest przypisany.
     *
     * <p>Jeśli student nie jest przypisany do żadnej grupy, metoda zwraca null.
     * Taki student jest dostępny do przypisania do dowolnej grupy.</p>
     *
     * @return nazwa grupy lub null jeśli student nie ma grupy
     * @see #setGroupName(String)
     * @see Group
     */
    public String getGroupName() { return groupName; }

    /**
     * Zwraca datę i czas dodania studenta do systemu.
     *
     * <p>Data jest automatycznie ustawiana podczas tworzenia obiektu Student
     * i reprezentuje moment dodania studenta do systemu dziennika.</p>
     *
     * @return data i czas dodania studenta (nigdy null)
     * @see #getFormattedDate()
     */
    public LocalDateTime getAddedDate() { return addedDate; }

    // === SETTERY - Metody modyfikujące pola klasy ===

    /**
     * Ustawia nowe imię studenta.
     *
     * <p>Metoda pozwala na zmianę imienia studenta. Może być używana
     * do korekty danych osobowych lub aktualizacji informacji.</p>
     *
     * @param firstName nowe imię studenta
     * @see #getFirstName()
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }

    /**
     * Ustawia nowe nazwisko studenta.
     *
     * <p>Metoda pozwala na zmianę nazwiska studenta. Może być używana
     * do korekty danych osobowych lub aktualizacji informacji
     * (np. po ślubie, adopcji, etc.).</p>
     *
     * @param lastName nowe nazwisko studenta
     * @see #getLastName()
     */
    public void setLastName(String lastName) { this.lastName = lastName; }

    /**
     * Ustawia nowy numer indeksu studenta.
     *
     * <p><strong>UWAGA:</strong> Ta metoda powinna być używana z największą ostrożnością!
     * Zmiana numeru indeksu może naruszyć integralność danych w systemie,
     * ponieważ numer indeksu jest używany jako klucz główny.</p>
     *
     * <p>Zaleca się używanie tej metody tylko w następujących sytuacjach:</p>
     * <ul>
     *   <li>Korekta błędnie wprowadzonego numeru przed zapisem w bazie</li>
     *   <li>Migracja danych z zewnętrznych systemów</li>
     *   <li>Operacje administracyjne pod nadzorem</li>
     * </ul>
     *
     * @param indexNumber nowy 6-cyfrowy numer indeksu
     * @see #getIndexNumber()
     */
    public void setIndexNumber(String indexNumber) { this.indexNumber = indexNumber; }

    /**
     * Ustawia nową grupę studenta lub usuwa przypisanie do grupy.
     *
     * <p>Ta metoda jest kluczowa dla zarządzania przypisaniami studentów do grup.
     * Może być używana w następujących scenariuszach:</p>
     *
     * <ul>
     *   <li><strong>Przypisanie do grupy:</strong> groupName = nazwa grupy</li>
     *   <li><strong>Usunięcie z grupy:</strong> groupName = null</li>
     *   <li><strong>Przeniesienie między grupami:</strong> zmiana z jednej grupy na inną</li>
     * </ul>
     *
     * <h3>Przykłady użycia:</h3>
     * <pre>
     * {@code
     * // Przypisanie studenta do grupy
     * student.setGroupName("INF-2024");
     *
     * // Usunięcie studenta z grupy (bez usuwania z systemu)
     * student.setGroupName(null);
     *
     * // Przeniesienie do innej grupy
     * student.setGroupName("MAT-2024");
     * }
     * </pre>
     *
     * @param groupName nazwa nowej grupy lub null aby usunąć przypisanie
     * @see #getGroupName()
     * @see Group
     */
    public void setGroupName(String groupName) { this.groupName = groupName; }

    /**
     * Zwraca pełne imię i nazwisko studenta.
     *
     * <p>Łączy imię i nazwisko studenta w jeden ciąg znaków oddzielony spacją.
     * Ta metoda jest szeroko używana w interfejsie użytkownika do wyświetlania
     * czytelnej nazwy studenta.</p>
     *
     * <p>Metoda obsługuje automatyczne formatowanie, łącząc pola firstName
     * i lastName ze spacją między nimi.</p>
     *
     * <h3>Przykłady zwracanych wartości:</h3>
     * <ul>
     *   <li>"Jan Kowalski"</li>
     *   <li>"Anna-Maria García-López"</li>
     *   <li>"José von Neumann"</li>
     * </ul>
     *
     * @return pełne imię i nazwisko w formacie "Imię Nazwisko"
     * @see #toString() Pełna reprezentacja studenta z indeksem i grupą
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Zwraca sformatowaną datę dodania studenta do systemu.
     *
     * <p>Formatuje {@link #addedDate} do czytelnego formatu "dd.MM.yyyy HH:mm"
     * używanego w interfejsie użytkownika i raportach.</p>
     *
     * <p>Format jest zgodny z konwencją używaną w całej aplikacji
     * dla wszystkich znaczników czasowych.</p>
     *
     * <h3>Przykład zwracanego formatu:</h3>
     * <ul>
     *   <li>"15.03.2024 14:30"</li>
     *   <li>"01.01.2025 00:01"</li>
     *   <li>"31.12.2024 23:59"</li>
     * </ul>
     *
     * @return sformatowana data dodania w formacie "dd.MM.yyyy HH:mm"
     * @see #getAddedDate() Oryginalna data jako LocalDateTime
     * @see DateTimeFormatter#ofPattern(String) Użyty formatter
     */
    public String getFormattedDate() {
        return addedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Zwraca pełną tekstową reprezentację studenta.
     *
     * <p>Generuje kompletsny opis studenta w formacie przydatnym do wyświetlania
     * w listach, logach i raportach. Format uwzględnia wszystkie kluczowe
     * informacje o studencie w czytelnej formie.</p>
     *
     * <p><strong>Format wyniku:</strong><br>
     * "[Pełne imię] ([numer indeksu]) - [grupa lub komunikat o braku grupy]"</p>
     *
     * <h3>Przykłady zwracanych wartości:</h3>
     * <ul>
     *   <li>"Jan Kowalski (123456) - INF-2024"</li>
     *   <li>"Anna Nowak (654321) - null" (gdy brak grupy)</li>
     *   <li>"José García (789012) - MAT-A"</li>
     * </ul>
     *
     * <p>Ta metoda jest automatycznie wykorzystywana przez komponenty JavaFX
     * takie jak ListView, ComboBox przy wyświetlaniu obiektów Student.</p>
     *
     * @return pełna tekstowa reprezentacja studenta
     * @see #getFullName() Tylko imię i nazwisko
     * @see #getIndexNumber() Tylko numer indeksu
     * @see #getGroupName() Tylko nazwa grupy
     */
    @Override
    public String toString() {
        return getFullName() + " (" + indexNumber + ") - " + groupName;
    }
}