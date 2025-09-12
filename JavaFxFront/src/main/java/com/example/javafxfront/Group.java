package com.example.javafxfront;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Klasa reprezentująca grupę studencką w systemie dziennika elektronicznego.
 *
 * <p>Group stanowi podstawową jednostkę organizacyjną do zarządzania studentami
 * w systemie dziennika online. Każda grupa ma unikalną nazwę, specjalizację
 * i datę utworzenia w systemie. Grupa może zawierać wielu studentów i mieć
 * przypisane terminy zajęć.</p>
 *
 * <p>Główne funkcjonalności klasy:</p>
 * <ul>
 *   <li><strong>Identyfikacja grupy</strong> - unikalna nazwa grupy (np. "INF-2024")</li>
 *   <li><strong>Specjalizacja</strong> - kierunek studiów lub obszar tematyczny</li>
 *   <li><strong>Znacznik czasowy</strong> - automatyczna rejestracja daty utworzenia</li>
 *   <li><strong>Formatowanie danych</strong> - czytelna prezentacja informacji</li>
 * </ul>
 *
 * <h3>Przykład użycia:</h3>
 * <pre>
 * {@code
 * // Utworzenie nowej grupy
 * Group informatykaGroup = new Group("INF-2024", "Informatyka");
 *
 * // Wyświetlenie informacji o grupie
 * System.out.println("Nazwa: " + informatykaGroup.getName());
 * System.out.println("Specjalizacja: " + informatykaGroup.getSpecialization());
 * System.out.println("Utworzona: " + informatykaGroup.getFormattedDate());
 *
 * // Reprezentacja tekstowa
 * System.out.println(informatykaGroup.toString());
 * // Output: "INF-2024 (Informatyka)"
 * }
 * </pre>
 *
 * <h3>Konwencje nazewnictwa grup:</h3>
 * <p>Zaleca się stosowanie następujących konwencji dla nazw grup:</p>
 * <ul>
 *   <li><strong>Format:</strong> [KOD_KIERUNKU]-[ROK] (np. "INF-2024", "MAT-2023")</li>
 *   <li><strong>Unikalna nazwa</strong> - każda grupa powinna mieć unikalną nazwę w systemie</li>
 *   <li><strong>Czytelność</strong> - nazwa powinna być zrozumiała dla użytkowników</li>
 * </ul>
 *
 * <h3>Integracja z systemem:</h3>
 * <p>Klasa Group współpracuje z następującymi komponentami:</p>
 * <ul>
 *   <li>{@link Student} - studenci przypisani do grupy</li>
 *   <li>{@link ClassSchedule} - terminy zajęć dla grupy</li>
 *   <li>{@link GroupService} - synchronizacja z serwerem backend</li>
 *   <li>{@link GroupDetailController} - interfejs zarządzania grupą</li>
 * </ul>
 *
 * <p><strong>Uwagi dotyczące wydajności:</strong></p>
 * <ul>
 *   <li>Data utworzenia jest ustawiana automatycznie przy konstrukcji obiektu</li>
 *   <li>Formatowanie daty jest wykonywane na żądanie (bez cache'owania)</li>
 *   <li>toString() generuje tekst dynamicznie przy każdym wywołaniu</li>
 * </ul>
 *
 * @author Mateusz ORski
 * @version 1.0
 * @since 2025
 *
 * @see Student Klasa studenta przypisanego do grupy
 * @see ClassSchedule Klasa terminu zajęć dla grupy
 * @see GroupService Serwis zarządzania grupami
 * @see GroupDetailController Kontroler interfejsu grupy
 */
public class Group {
    /**
     * Nazwa grupy studenckiej.
     * <p>Powinna być unikalna w systemie i czytelna dla użytkowników.
     * Przykłady: "INF-2024", "MAT-A", "FIZ-2023".</p>
     */
    private String name;

    /**
     * Specjalizacja lub kierunek studiów grupy.
     * <p>Opisuje obszar tematyczny lub kierunek studiów dla grupy.
     * Przykłady: "Informatyka", "Matematyka Stosowana", "Fizyka Teoretyczna".</p>
     */
    private String specialization;

    /**
     * Data i czas utworzenia grupy w systemie.
     * <p>Automatycznie ustawiana przy tworzeniu obiektu grupy.
     * Używana do celów audytu i sortowania grup według daty utworzenia.</p>
     */
    private LocalDateTime createdDate;

    /**
     * Konstruktor tworzący nową grupę z podaną nazwą i specjalizacją.
     *
     * <p>Automatycznie ustawia datę utworzenia na bieżący moment.
     * Ten konstruktor jest używany przy tworzeniu nowych grup w aplikacji
     * lub podczas deserializacji danych z zewnętrznych źródeł.</p>
     *
     * <p>Przykład użycia:</p>
     * <pre>
     * {@code
     * Group group = new Group("INF-2024", "Informatyka");
     * System.out.println(group.getFormattedDate()); // np. "15.03.2024 14:30"
     * }
     * </pre>
     *
     * @param name nazwa grupy (zalecane: unikalna w systemie)
     * @param specialization specjalizacja lub kierunek studiów grupy
     */
    public Group(String name, String specialization) {
        this.name = name;
        this.specialization = specialization;
        this.createdDate = LocalDateTime.now();
    }

    /**
     * Zwraca nazwę grupy.
     *
     * @return nazwa grupy (może być null jeśli nie została ustawiona)
     * @see #setName(String)
     */
    public String getName() { return name; }

    /**
     * Zwraca specjalizację grupy.
     *
     * @return specjalizacja lub kierunek studiów grupy (może być null)
     * @see #setSpecialization(String)
     */
    public String getSpecialization() { return specialization; }

    /**
     * Zwraca datę utworzenia grupy w systemie.
     *
     * @return data i czas utworzenia grupy (nigdy nie jest null)
     * @see #getFormattedDate()
     */
    public LocalDateTime getCreatedDate() { return createdDate; }

    /**
     * Ustawia nazwę grupy.
     *
     * <p><strong>Uwaga:</strong> Zmiana nazwy grupy może wpłynąć na integralność
     * danych w systemie, jeśli grupa jest już używana. Upewnij się, że nowa
     * nazwa jest unikalna w systemie.</p>
     *
     * @param name nowa nazwa grupy
     * @see #getName()
     */
    public void setName(String name) { this.name = name; }

    /**
     * Ustawia specjalizację grupy.
     *
     * <p>Specjalizacja może być zmieniona w dowolnym momencie bez wpływu
     * na integralność danych systemu.</p>
     *
     * @param specialization nowa specjalizacja grupy
     * @see #getSpecialization()
     */
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    /**
     * Zwraca sformatowaną datę utworzenia grupy.
     *
     * <p>Formatuje datę utworzenia do czytelnego formatu "dd.MM.yyyy HH:mm"
     * używanego w interfejsie użytkownika.</p>
     *
     * <p>Przykład zwracanego formatu: "15.03.2024 14:30"</p>
     *
     * @return sformatowana data utworzenia w formacie "dd.MM.yyyy HH:mm"
     * @see #getCreatedDate()
     */
    public String getFormattedDate() {
        return createdDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Zwraca tekstową reprezentację grupy.
     *
     * <p>Format: "[nazwa] ([specjalizacja])"</p>
     * <p>Przykład: "INF-2024 (Informatyka)"</p>
     *
     * <p>Ta reprezentacja jest używana w listach, combobox-ach
     * i wszędzie gdzie potrzebny jest krótki opis grupy.</p>
     *
     * @return tekstowa reprezentacja w formacie "[nazwa] ([specjalizacja])"
     */
    @Override
    public String toString() {
        return name + " (" + specialization + ")";
    }
}