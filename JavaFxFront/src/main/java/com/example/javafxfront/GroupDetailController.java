package com.example.javafxfront;

import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class GroupDetailController {
    @FXML private Label groupNameLabel;
    @FXML private Label groupSpecializationLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label scheduleCountLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField indexNumberField;
    @FXML private Button addStudentButton;

    @FXML private TextField terminNameField;
    @FXML private DatePicker terminDatePicker;
    @FXML private TextField terminStartTimeField;
    @FXML private TextField terminEndTimeField;
    @FXML private Label terminGroupLabel;
    @FXML private Button addTerminButton;

    @FXML private ListView<Student> studentsListView;
    @FXML private ListView<ClassSchedule> scheduleListView;

    @FXML private Button removeStudentButton;
    @FXML private Button removeScheduleButton;
    @FXML private Button backButton;
    @FXML private Button showReportButton; // DODANE - przycisk dziennika

    @FXML private Button refreshStudentsButton;
    @FXML private Button refreshSchedulesButton;
    @FXML private Button loadStudentsButton;

    private GroupService groupService;
    private StudentService studentService;
    private ScheduleService scheduleService;
    private AttendanceService attendanceService;

    private Group currentGroup;
    private ObservableList<Student> students;
    private ObservableList<ClassSchedule> schedules;

    @FXML
    protected void initialize() {
        students = FXCollections.observableArrayList();
        schedules = FXCollections.observableArrayList();
        groupService = new GroupService();
        studentService = new StudentService();
        scheduleService = new ScheduleService();
        attendanceService = new AttendanceService();

        studentsListView.setItems(students);
        scheduleListView.setItems(schedules);

        studentsListView.setCellFactory(listView -> new StudentListCell());
        scheduleListView.setCellFactory(listView -> new ScheduleListCell());

        studentsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            removeStudentButton.setDisable(newSelection == null);
        });

        scheduleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            removeScheduleButton.setDisable(newSelection == null);
        });

        // Pojedyncze kliknięcie dla szczegółów terminu
        scheduleListView.setOnMouseClicked(event -> {
            ClassSchedule selectedSchedule = scheduleListView.getSelectionModel().getSelectedItem();
            if (selectedSchedule != null && event.getClickCount() == 1) {
                openScheduleDetailWindow(selectedSchedule);
            }
        });

        removeStudentButton.setDisable(true);
        removeScheduleButton.setDisable(true);

        setupValidation();
    }

    private void setupValidation() {
        if (indexNumberField != null) {
            indexNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                if (digitsOnly.length() > 6) {
                    digitsOnly = digitsOnly.substring(0, 6);
                }
                if (!digitsOnly.equals(newValue)) {
                    indexNumberField.setText(digitsOnly);
                }
            });
        }

        if (terminStartTimeField != null) {
            setupTimeFieldValidation(terminStartTimeField);
        }

        if (terminEndTimeField != null) {
            setupTimeFieldValidation(terminEndTimeField);
        }
    }

    private void setupTimeFieldValidation(TextField timeField) {
        timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            String filtered = newValue.replaceAll("[^0-9:]", "");
            if (filtered.length() > 5) {
                filtered = filtered.substring(0, 5);
            }
            if (filtered.length() == 2 && !filtered.contains(":")) {
                filtered = filtered + ":";
            }
            if (!filtered.equals(newValue)) {
                timeField.setText(filtered);
                timeField.positionCaret(filtered.length());
            }
        });
    }

    public void setGroup(Group group) {
        this.currentGroup = group;
        updateGroupInfo();
        loadDataFromServer();
    }

    private void updateGroupInfo() {
        if (currentGroup != null) {
            groupNameLabel.setText(currentGroup.getName());
            groupSpecializationLabel.setText(currentGroup.getSpecialization());

            if (terminGroupLabel != null) {
                terminGroupLabel.setText(currentGroup.getName());
            }

            updateCounts();
        }
    }

    private void updateCounts() {
        studentCountLabel.setText("Liczba studentów: " + students.size());
        scheduleCountLabel.setText("Liczba terminów: " + schedules.size());

        // DODANE - Sprawdź czy można wygenerować dziennik obecności
        if (showReportButton != null) {
            boolean canGenerateReport = !students.isEmpty() && !schedules.isEmpty();
            showReportButton.setDisable(!canGenerateReport);

            if (canGenerateReport) {
                // Sprawdź czy są jakieś dane o obecności
                long totalAttendanceEntries = schedules.stream()
                        .mapToLong(s -> s.getAttendances().size())
                        .sum();

                if (totalAttendanceEntries > 0) {
                    showReportButton.setText("📊 Dziennik obecności (" + totalAttendanceEntries + ")");
                    showReportButton.setStyle(showReportButton.getStyle().replaceAll("-fx-background-color:[^;]*;", "") +
                            "; -fx-background-color: linear-gradient(to bottom, #38A169, #2F855A);");
                } else {
                    showReportButton.setText("📊 Dziennik obecności (pusty)");
                    showReportButton.setStyle(showReportButton.getStyle().replaceAll("-fx-background-color:[^;]*;", "") +
                            "; -fx-background-color: linear-gradient(to bottom, #F56500, #DD6B20);");
                }
            } else {
                showReportButton.setText("📊 Dziennik obecności");
            }
        }
    }

    private void loadDataFromServer() {
        if (currentGroup != null) {
            loadStudentsFromServer();
            loadSchedulesFromServer();
        }
    }

    private void loadStudentsFromServer() {
        if (currentGroup == null) {
            System.err.println("❌ Brak currentGroup - nie można załadować studentów");
            return;
        }

        System.out.println("🔄 ŁADOWANIE STUDENTÓW dla grupy: '" + currentGroup.getName() + "'");

        studentService.getStudentsByGroupAsync(currentGroup.getName())
                .thenAccept(serverStudents -> {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("📥 Otrzymano " + serverStudents.size() + " studentów z serwera dla grupy: " + currentGroup.getName());

                        // Debug - wypisz szczegóły wszystkich studentów
                        System.out.println("=== LISTA STUDENTÓW Z SERWERA ===");
                        for (int i = 0; i < serverStudents.size(); i++) {
                            Student student = serverStudents.get(i);
                            System.out.println((i + 1) + ". " + student.getFullName() +
                                    " (index: " + student.getIndexNumber() +
                                    ", grupa: '" + student.getGroupName() + "')");
                        }
                        System.out.println("================================");

                        // Wyczyść starą listę
                        int oldSize = students.size();
                        students.clear();
                        System.out.println("🗑️ Wyczyszczono " + oldSize + " starych studentów z listy");

                        // Dodaj nowych studentów
                        students.addAll(serverStudents);
                        System.out.println("➕ Dodano " + serverStudents.size() + " nowych studentów do listy");

                        // Wymuś odświeżenie ListView
                        studentsListView.refresh();
                        System.out.println("🔄 Wymuszone odświeżenie ListView");

                        updateCounts();

                        if (serverStudents.isEmpty()) {
                            System.out.println("⚠️ UWAGA: Brak studentów w grupie '" + currentGroup.getName() + "'");
                            System.out.println("💡 Sprawdź czy studenci są rzeczywiście przypisani do tej grupy w bazie");
                        } else {
                            System.out.println("✅ Pomyślnie załadowano " + serverStudents.size() + " studentów dla grupy '" + currentGroup.getName() + "'");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("❌ Błąd ładowania studentów dla grupy '" + currentGroup.getName() + "': " + throwable.getMessage());
                        throwable.printStackTrace();

                        showAlert("Ostrzeżenie",
                                "Nie udało się załadować studentów z serwera:\n" + throwable.getMessage(),
                                Alert.AlertType.WARNING);
                    });
                    return null;
                });
    }

    private void loadSchedulesFromServer() {
        if (currentGroup == null) {
            System.err.println("❌ Brak currentGroup - nie można załadować terminów");
            return;
        }

        System.out.println("🔄 ŁADOWANIE TERMINÓW dla grupy: '" + currentGroup.getName() + "'");

        scheduleService.getSchedulesByGroupAsync(currentGroup.getName())
                .thenAccept(serverSchedules -> {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("📥 Otrzymano " + serverSchedules.size() + " terminów z serwera dla grupy: " + currentGroup.getName());

                        // Debug - wypisz szczegóły wszystkich terminów
                        System.out.println("=== LISTA TERMINÓW Z SERWERA ===");
                        for (int i = 0; i < serverSchedules.size(); i++) {
                            ClassSchedule schedule = serverSchedules.get(i);
                            System.out.println((i + 1) + ". " + schedule.getSubject() +
                                    " (ID: " + schedule.getId() +
                                    ", data: " + schedule.getFormattedStartTime() +
                                    ", grupa: '" + schedule.getGroupName() + "')");
                        }
                        System.out.println("================================");

                        // Wyczyść starą listę
                        int oldSize = schedules.size();
                        schedules.clear();
                        System.out.println("🗑️ Wyczyszczono " + oldSize + " starych terminów z listy");

                        // Dodaj nowe terminy
                        schedules.addAll(serverSchedules);
                        System.out.println("➕ Dodano " + serverSchedules.size() + " nowych terminów do listy");

                        // Załaduj obecności dla każdego terminu
                        for (ClassSchedule schedule : serverSchedules) {
                            if (schedule.getId() != null) {
                                loadAttendanceFromServerSilent(schedule);
                            } else {
                                System.out.println("⚠️ Termin " + schedule.getSubject() + " nie ma ID - pomijam ładowanie obecności");
                            }
                        }

                        // Wymuś odświeżenie ListView
                        scheduleListView.refresh();
                        System.out.println("🔄 Wymuszone odświeżenie ListView terminów");

                        updateCounts();

                        if (serverSchedules.isEmpty()) {
                            System.out.println("⚠️ UWAGA: Brak terminów w grupie '" + currentGroup.getName() + "'");
                            System.out.println("💡 Sprawdź czy terminy są rzeczywiście przypisane do tej grupy w bazie");
                        } else {
                            System.out.println("✅ Pomyślnie załadowano " + serverSchedules.size() + " terminów dla grupy '" + currentGroup.getName() + "'");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("❌ Błąd ładowania terminów dla grupy '" + currentGroup.getName() + "': " + throwable.getMessage());
                        throwable.printStackTrace();

                        showAlert("Ostrzeżenie",
                                "Nie udało się załadować terminów z serwera:\n" + throwable.getMessage(),
                                Alert.AlertType.WARNING);
                    });
                    return null;
                });
    }

    // Metoda do cichego ładowania obecności (bez alertów)
    private void loadAttendanceFromServerSilent(ClassSchedule schedule) {
        if (schedule.getId() != null) {
            attendanceService.getAttendancesByScheduleAsync(schedule.getId())
                    .thenAccept(serverAttendances -> {
                        javafx.application.Platform.runLater(() -> {
                            schedule.getAttendances().clear();
                            schedule.getAttendances().addAll(serverAttendances);
                            refreshSchedulesList();
                        });
                    })
                    .exceptionally(throwable -> {
                        // Cicha obsługa błędów - tylko log do konsoli
                        System.err.println("Nie udało się załadować obecności z serwera dla terminu " +
                                schedule.getSubject() + ": " + throwable.getMessage());
                        return null;
                    });
        }
    }

    // Metoda do ładowania obecności z alertami
    private void loadAttendanceFromServer(ClassSchedule schedule) {
        if (schedule.getId() != null) {
            attendanceService.getAttendancesByScheduleAsync(schedule.getId())
                    .thenAccept(serverAttendances -> {
                        javafx.application.Platform.runLater(() -> {
                            schedule.getAttendances().clear();
                            schedule.getAttendances().addAll(serverAttendances);
                            refreshSchedulesList();

                            showAlert("Sukces",
                                    "Załadowano " + serverAttendances.size() + " wpisów obecności z serwera",
                                    Alert.AlertType.INFORMATION);
                        });
                    })
                    .exceptionally(throwable -> {
                        javafx.application.Platform.runLater(() -> {
                            showAlert("Błąd",
                                    "Nie udało się załadować obecności z serwera:\n" + throwable.getMessage(),
                                    Alert.AlertType.ERROR);
                        });
                        return null;
                    });
        } else {
            showAlert("Info",
                    "Ten termin nie ma ID z serwera - obecności są tylko lokalne.",
                    Alert.AlertType.INFORMATION);
        }
    }
    /**
     * Przypisuje istniejącego studenta do grupy
     */
    private void assignExistingStudentToGroup(String indexNumber) {
        System.out.println("=== PRZYPISYWANIE ISTNIEJĄCEGO STUDENTA ===");
        System.out.println("Szukam studenta o indeksie: " + indexNumber);

        // Najpierw spróbuj znaleźć studenta na serwerze
        studentService.getAllStudentsAsync()
                .thenAccept(allStudents -> {
                    System.out.println("Otrzymano " + allStudents.size() + " studentów z serwera");

                    javafx.application.Platform.runLater(() -> {
                        // Znajdź studenta o podanym indeksie
                        Student existingStudent = allStudents.stream()
                                .filter(s -> s.getIndexNumber().equals(indexNumber))
                                .findFirst()
                                .orElse(null);

                        System.out.println("Znaleziony student: " + (existingStudent != null ?
                                existingStudent.getFullName() + " (grupa: " + existingStudent.getGroupName() + ")" : "BRAK"));

                        if (existingStudent == null) {
                            addStudentButton.setDisable(false);
                            addStudentButton.setText("Dodaj studenta");
                            showAlert("Student nie znaleziony",
                                    "Nie znaleziono studenta o numerze indeksu " + indexNumber +
                                            " w systemie.\n\nAby utworzyć nowego studenta, wypełnij także pola: Imię i Nazwisko.",
                                    Alert.AlertType.WARNING);
                            return;
                        }

                        System.out.println("Student istnieje. Aktualna grupa: '" + existingStudent.getGroupName() + "'");
                        System.out.println("Docelowa grupa: '" + currentGroup.getName() + "'");

                        // Student istnieje - sprawdź czy już ma grupę
                        if (existingStudent.getGroupName() != null &&
                                !existingStudent.getGroupName().isEmpty()) {

                            // Sprawdź czy to ta sama grupa
                            if (existingStudent.getGroupName().equals(currentGroup.getName())) {
                                addStudentButton.setDisable(false);
                                addStudentButton.setText("Dodaj studenta");
                                showAlert("Student już w grupie",
                                        "Student " + existingStudent.getFullName() +
                                                " jest już przypisany do tej grupy (" + currentGroup.getName() + ")!",
                                        Alert.AlertType.INFORMATION);
                                return;
                            }

                            addStudentButton.setDisable(false);
                            addStudentButton.setText("Dodaj studenta");

                            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmAlert.setTitle("Przenieś studenta");
                            confirmAlert.setHeaderText("Student już ma grupę");
                            confirmAlert.setContentText("Student " + existingStudent.getFullName() +
                                    " (indeks: " + indexNumber + ") jest już przypisany do grupy: " +
                                    existingStudent.getGroupName() +
                                    "\n\nCzy chcesz przenieść go do grupy " + currentGroup.getName() + "?");

                            java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                addStudentButton.setDisable(true);
                                addStudentButton.setText("Przenoszę...");

                                // Przenieś studenta
                                Student updatedStudent = new Student(existingStudent.getFirstName(),
                                        existingStudent.getLastName(),
                                        existingStudent.getIndexNumber(),
                                        currentGroup.getName());
                                updateStudentGroup(updatedStudent, existingStudent.getFullName());
                            }
                            return;
                        }

                        System.out.println("Student nie ma grupy - przypisuję do: " + currentGroup.getName());

                        // Przypisz studenta do bieżącej grupy
                        Student updatedStudent = new Student(existingStudent.getFirstName(),
                                existingStudent.getLastName(),
                                existingStudent.getIndexNumber(),
                                currentGroup.getName());

                        updateStudentGroup(updatedStudent, existingStudent.getFullName());
                    });
                })
                .exceptionally(throwable -> {
                    System.err.println("BŁĄD pobierania studentów z serwera: " + throwable.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        addStudentButton.setDisable(false);
                        addStudentButton.setText("Dodaj studenta");
                        showAlert("Błąd serwera",
                                "Nie udało się sprawdzić czy student istnieje:\n" + throwable.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Tworzy nowego studenta lub aktualizuje istniejącego
     */
    private void createOrUpdateStudent(String firstName, String lastName, String indexNumber) {
        Student newStudent = new Student(firstName, lastName, indexNumber, currentGroup.getName());

        studentService.addStudentAsync(newStudent)
                .thenAccept(savedStudent -> {
                    javafx.application.Platform.runLater(() -> {
                        addStudentButton.setDisable(false);
                        addStudentButton.setText("Dodaj studenta");

                        students.add(newStudent);
                        animateButton(addStudentButton);
                        clearStudentForm();
                        updateCounts();

                        showAlert("Sukces", "Student " + newStudent.getFullName() +
                                        " został utworzony i dodany do grupy " + currentGroup.getName() + "!",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        addStudentButton.setDisable(false);
                        addStudentButton.setText("Dodaj studenta");

                        if (throwable.getCause() instanceof StudentService.StudentAlreadyExistsException) {
                            // Student istnieje - spróbuj go zaktualizować (przypisać do grupy)
                            updateStudentGroup(newStudent, newStudent.getFullName());
                        } else {
                            // Inny błąd - dodaj lokalnie
                            students.add(newStudent);
                            animateButton(addStudentButton);
                            clearStudentForm();
                            updateCounts();

                            showAlert("Ostrzeżenie",
                                    "Student " + newStudent.getFullName() +
                                            " został dodany lokalnie do grupy " + currentGroup.getName() +
                                            ", ale wystąpił problem z serwerem:\n" + throwable.getMessage(),
                                    Alert.AlertType.WARNING);
                        }
                    });
                    return null;
                });
    }

    /**
     * Aktualizuje grupę studenta na serwerze
     */
    private void updateStudentGroup(Student student, String studentDisplayName) {
        System.out.println("🔄 ROZPOCZYNAM aktualizację grupy dla studenta: " + studentDisplayName);
        System.out.println("📋 Nowa grupa: '" + student.getGroupName() + "'");

        studentService.updateStudentAsync(student.getIndexNumber(), student)
                .thenAccept(updatedStudent -> {
                    javafx.application.Platform.runLater(() -> {
                        addStudentButton.setDisable(false);
                        addStudentButton.setText("Dodaj studenta");

                        System.out.println("✅ Student zaktualizowany na serwerze: " + studentDisplayName);

                        // Dodaj studenta do lokalnej listy
                        students.add(student);
                        System.out.println("➕ Dodano studenta do lokalnej listy");

                        animateButton(addStudentButton);
                        clearStudentForm();
                        updateCounts();

                        // 🔧 KLUCZOWE: Automatyczne odświeżenie z serwera po 1 sekundzie
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                        pause.setOnFinished(e -> {
                            System.out.println("🔄 Auto-odświeżanie listy studentów po przypisaniu...");
                            loadStudentsFromServer();
                        });
                        pause.play();

                        showAlert("Sukces", "Student " + studentDisplayName +
                                        " został przypisany do grupy " + currentGroup.getName() + "!" +
                                        "\n\nLista zostanie automatycznie odświeżona.",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(updateThrowable -> {
                    javafx.application.Platform.runLater(() -> {
                        addStudentButton.setDisable(false);
                        addStudentButton.setText("Dodaj studenta");

                        System.err.println("❌ Błąd aktualizacji studenta: " + updateThrowable.getMessage());

                        // Mimo błędu, dodaj lokalnie i spróbuj odświeżyć
                        students.add(student);
                        animateButton(addStudentButton);
                        clearStudentForm();
                        updateCounts();

                        // Spróbuj odświeżyć z serwera
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                        pause.setOnFinished(e -> {
                            System.out.println("🔄 Auto-odświeżanie po błędzie...");
                            loadStudentsFromServer();
                        });
                        pause.play();

                        showAlert("Ostrzeżenie",
                                "Student " + studentDisplayName +
                                        " został dodany lokalnie do grupy " + currentGroup.getName() +
                                        ", ale nie udało się zaktualizować na serwerze:\n" +
                                        updateThrowable.getMessage() +
                                        "\n\nSpróbuję odświeżyć listę z serwera...",
                                Alert.AlertType.WARNING);
                    });
                    return null;
                });
    }
    @FXML
    protected void onAddStudentClick() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String indexNumber = indexNumberField.getText().trim();

        // DEBUG - pokaż co użytkownik wpisał
        System.out.println("=== DEBUG DODAWANIE STUDENTA ===");
        System.out.println("Imię: '" + firstName + "'");
        System.out.println("Nazwisko: '" + lastName + "'");
        System.out.println("Nr indeksu: '" + indexNumber + "'");
        System.out.println("Grupa bieżąca: " + (currentGroup != null ? currentGroup.getName() : "NULL"));

        if (indexNumber.isEmpty()) {
            showAlert("Błąd", "Numer indeksu jest wymagany!", Alert.AlertType.WARNING);
            return;
        }

        if (!indexNumber.matches("\\d{6}")) {
            showAlert("Błąd", "Numer indeksu musi składać się z dokładnie 6 cyfr!", Alert.AlertType.WARNING);
            return;
        }

        // Sprawdź czy student już jest w tej grupie
        boolean studentInGroup = students.stream()
                .anyMatch(s -> s.getIndexNumber().equals(indexNumber));
        if (studentInGroup) {
            showAlert("Błąd", "Student o numerze indeksu " + indexNumber +
                    " już jest przypisany do tej grupy!", Alert.AlertType.WARNING);
            return;
        }

        addStudentButton.setDisable(true);
        addStudentButton.setText("Sprawdzam...");

        // SCENARIUSZ 1: Tylko numer indeksu (przypisz istniejącego)
        if (firstName.isEmpty() && lastName.isEmpty()) {
            System.out.println(">>> SCENARIUSZ 1: Przypisywanie istniejącego studenta");
            assignExistingStudentToGroup(indexNumber);
        }
        // SCENARIUSZ 2: Pełne dane (utwórz nowego lub zaktualizuj istniejącego)
        else if (!firstName.isEmpty() && !lastName.isEmpty()) {
            System.out.println(">>> SCENARIUSZ 2: Tworzenie nowego studenta");
            createOrUpdateStudent(firstName, lastName, indexNumber);
        }
        // SCENARIUSZ 3: Niepełne dane
        else {
            System.out.println(">>> SCENARIUSZ 3: Niepełne dane - błąd");
            addStudentButton.setDisable(false);
            addStudentButton.setText("Dodaj studenta");
            showAlert("Błąd", "Podaj tylko numer indeksu (aby przypisać istniejącego studenta) " +
                            "lub pełne dane: imię, nazwisko i numer indeksu (aby utworzyć nowego).",
                    Alert.AlertType.WARNING);
        }
    }
    @FXML
    protected void onAddTerminClick() {
        String terminName = terminNameField.getText().trim();
        LocalDate date = terminDatePicker.getValue();
        String startTimeText = terminStartTimeField.getText().trim();
        String endTimeText = terminEndTimeField.getText().trim();

        if (terminName.isEmpty()) {
            showAlert("Błąd", "Nazwa terminu musi być wypełniona!", Alert.AlertType.WARNING);
            return;
        }

        if (date == null) {
            showAlert("Błąd", "Data musi być wybrana!", Alert.AlertType.WARNING);
            return;
        }

        if (startTimeText.isEmpty()) {
            showAlert("Błąd", "Godzina rozpoczęcia musi być wypełniona!", Alert.AlertType.WARNING);
            return;
        }

        LocalTime startTime;
        try {
            startTime = parseTime(startTimeText);
        } catch (Exception e) {
            showAlert("Błąd", "Nieprawidłowa godzina rozpoczęcia! Użyj formatu HH:MM (np. 10:15)", Alert.AlertType.WARNING);
            return;
        }

        LocalTime endTime;
        if (endTimeText.isEmpty()) {
            endTime = startTime.plusHours(1);
        } else {
            try {
                endTime = parseTime(endTimeText);
                if (!endTime.isAfter(startTime)) {
                    showAlert("Błąd", "Czas zakończenia musi być później niż czas rozpoczęcia!", Alert.AlertType.WARNING);
                    return;
                }
            } catch (Exception e) {
                showAlert("Błąd", "Nieprawidłowa godzina zakończenia! Użyj formatu HH:MM (np. 12:00)", Alert.AlertType.WARNING);
                return;
            }
        }

        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

        ClassSchedule newSchedule = new ClassSchedule(
                terminName, "", startDateTime, endDateTime, "", "", currentGroup.getName()
        );

        addTerminButton.setDisable(true);
        addTerminButton.setText("Dodawanie...");

        scheduleService.addScheduleAsync(newSchedule)
                .thenAccept(savedSchedule -> {
                    javafx.application.Platform.runLater(() -> {
                        addTerminButton.setDisable(false);
                        addTerminButton.setText("Dodaj termin");

                        schedules.add(savedSchedule != null ? savedSchedule : newSchedule);
                        animateButton(addTerminButton);
                        clearTerminForm();
                        updateCounts();

                        showAlert("Sukces", "Termin '" + terminName + "' został dodany!", Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        addTerminButton.setDisable(false);
                        addTerminButton.setText("Dodaj termin");

                        schedules.add(newSchedule);
                        animateButton(addTerminButton);
                        clearTerminForm();
                        updateCounts();

                        showAlert("Ostrzeżenie", "Termin został dodany lokalnie, ale nie udało się wysłać na serwer.", Alert.AlertType.WARNING);
                    });
                    return null;
                });
    }

    // DODANE - Obsługa przycisku dziennika obecności
    @FXML
    protected void onShowReportClick() {
        if (currentGroup == null) {
            showAlert("Błąd", "Brak danych o grupie!", Alert.AlertType.ERROR);
            return;
        }

        if (students.isEmpty()) {
            showAlert("Info", "Brak studentów w grupie. Dodaj studentów aby wygenerować dziennik.",
                    Alert.AlertType.INFORMATION);
            return;
        }

        if (schedules.isEmpty()) {
            showAlert("Info", "Brak terminów w grupie. Dodaj terminy aby wygenerować dziennik.",
                    Alert.AlertType.INFORMATION);
            return;
        }

        try {
            // Animacja przycisku
            animateButton(showReportButton);

            // Załaduj FXML dla dziennika
            FXMLLoader loader = new FXMLLoader(getClass().getResource("attendance-report-view.fxml"));
            Parent root = loader.load();

            // Pobierz kontroler dziennika
            AttendanceReportController reportController = loader.getController();

            // Przekaż dane do kontrolera dziennika
            reportController.setData(currentGroup, new java.util.ArrayList<>(students),
                    new java.util.ArrayList<>(schedules));

            // Utwórz nowe okno
            Stage reportStage = new Stage();
            reportStage.setTitle("📊 Dziennik obecności - " + currentGroup.getName());
            reportStage.setScene(new Scene(root, 1200, 800));

            // Dodaj stylizację
            reportStage.getScene().getStylesheets().add(
                    getClass().getResource("styles.css").toExternalForm());

            // Ustaw minimalny rozmiar
            reportStage.setMinWidth(1000);
            reportStage.setMinHeight(600);

            // Ustaw modalność - okno blokuje interakcję z rodzicielskim oknem
            reportStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            reportStage.initOwner(showReportButton.getScene().getWindow());

            // Pokaż okno dziennika
            reportStage.show();

            System.out.println("✅ Otwarto dziennik obecności dla grupy: " + currentGroup.getName());
            System.out.println("📊 Studentów: " + students.size() + ", Terminów: " + schedules.size());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd", "Nie udało się otworzyć dziennika obecności:\n" + e.getMessage(),
                    Alert.AlertType.ERROR);
            System.err.println("❌ Błąd otwierania dziennika: " + e.getMessage());
        }
    }

    private LocalTime parseTime(String timeText) throws DateTimeParseException {
        if (!timeText.matches("\\d{2}:\\d{2}")) {
            throw new DateTimeParseException("Invalid format", timeText, 0);
        }
        String[] timeParts = timeText.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            throw new DateTimeParseException("Invalid time values", timeText, 0);
        }
        return LocalTime.of(hours, minutes);
    }

    // Otwórz okno szczegółów terminu z zarządzaniem obecnością
    private void openScheduleDetailWindow(ClassSchedule schedule) {
        try {
            Stage newStage = new Stage();
            newStage.setTitle("Zarządzanie terminem - " + schedule.getSubject());
            newStage.setWidth(900);
            newStage.setHeight(700);

            VBox root = new VBox(20);
            root.setStyle("-fx-background-color: white; -fx-padding: 20;");

            // Header z informacjami o terminie
            Label titleLabel = new Label("Zarządzanie terminem");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #DC143C;");

            VBox infoBox = new VBox(5);
            infoBox.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 15; -fx-background-radius: 10;");
            infoBox.getChildren().addAll(
                    createInfoLabel("Nazwa: " + schedule.getSubject()),
                    createInfoLabel("Data: " + schedule.getFormattedStartTime()),
                    createInfoLabel("Grupa: " + schedule.getGroupName()),
                    createInfoLabel("Źródło: " + (schedule.isFromServer() ? "Serwer (ID: " + schedule.getId() + ")" : "Lokalny")),
                    createInfoLabel("Frekwencja: " + schedule.getAttendanceSummary())
            );

            // Lista studentów z przyciskami obecności
            Label studentsLabel = new Label("Lista studentów grupy:");
            studentsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #212529;");

            VBox studentsBox = new VBox(10);
            for (Student student : students) {
                HBox studentRow = createStudentAttendanceRow(student, schedule);
                studentsBox.getChildren().add(studentRow);
            }

            ScrollPane scrollPane = new ScrollPane(studentsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(350);
            scrollPane.setStyle("-fx-background-color: transparent;");

            // Przyciski akcji
            HBox buttonsBox = new HBox(15);

            Button closeButton = new Button("Zamknij");
            closeButton.setOnAction(e -> newStage.close());
            closeButton.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 20;");

            Button clearAllButton = new Button("Wyczyść wszystko");
            clearAllButton.setOnAction(e -> clearAllAttendances(schedule, newStage));
            clearAllButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 20;");

            // Przycisk ładowania obecności z serwera
            Button loadServerAttendanceButton = new Button("Załaduj z serwera");
            loadServerAttendanceButton.setOnAction(e -> {
                loadAttendanceFromServer(schedule);
                // Odśwież okno po załadowaniu
                newStage.close();
                javafx.application.Platform.runLater(() -> openScheduleDetailWindow(schedule));
            });
            loadServerAttendanceButton.setStyle("-fx-background-color: #38A169; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 20;");

            if (schedule.isFromServer()) {
                buttonsBox.getChildren().addAll(loadServerAttendanceButton, clearAllButton, closeButton);
            } else {
                buttonsBox.getChildren().addAll(clearAllButton, closeButton);
            }

            root.getChildren().addAll(titleLabel, infoBox, studentsLabel, scrollPane, buttonsBox);

            Scene scene = new Scene(root);
            newStage.setScene(scene);
            newStage.show();

        } catch (Exception e) {
            showAlert("Błąd", "Nie udało się otworzyć zarządzania terminem.", Alert.AlertType.ERROR);
        }
    }

    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #212529;");
        return label;
    }

    private HBox createStudentAttendanceRow(Student student, ClassSchedule schedule) {
        HBox row = new HBox(15);
        row.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #E9ECEF; -fx-border-radius: 5;");

        // Informacje o studencie
        VBox studentInfo = new VBox(3);
        Label nameLabel = new Label(student.getFullName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label indexLabel = new Label("Nr indeksu: " + student.getIndexNumber());
        indexLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6C757D;");

        studentInfo.getChildren().addAll(nameLabel, indexLabel);

        // Status obecności
        Label statusLabel = new Label();
        statusLabel.setPrefWidth(120);

        Attendance attendance = schedule.getAttendanceForStudent(student);
        if (attendance != null) {
            statusLabel.setText(attendance.getStatus().getDisplayName());
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + attendance.getStatus().getColor() + ";");
        } else {
            statusLabel.setText("Nie zaznaczono");
            statusLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #6C757D;");
        }

        // Przyciski do oznaczania obecności
        HBox buttonsBox = new HBox(5);

        Button presentButton = new Button("Obecny");
        presentButton.setStyle("-fx-background-color: #38A169; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px;");
        presentButton.setOnAction(e -> {
            markAttendance(student, schedule, Attendance.Status.PRESENT, statusLabel);
            refreshSchedulesList();
        });

        Button lateButton = new Button("Spóźniony");
        lateButton.setStyle("-fx-background-color: #F56500; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px;");
        lateButton.setOnAction(e -> {
            markAttendance(student, schedule, Attendance.Status.LATE, statusLabel);
            refreshSchedulesList();
        });

        Button absentButton = new Button("Nieobecny");
        absentButton.setStyle("-fx-background-color: #E53E3E; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px;");
        absentButton.setOnAction(e -> {
            markAttendance(student, schedule, Attendance.Status.ABSENT, statusLabel);
            refreshSchedulesList();
        });

        Button clearButton = new Button("Wyczyść");
        clearButton.setStyle("-fx-background-color: #6C757D; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px;");
        clearButton.setOnAction(e -> {
            clearAttendance(student, schedule, statusLabel);
            refreshSchedulesList();
        });

        buttonsBox.getChildren().addAll(presentButton, lateButton, absentButton, clearButton);

        // Spacer aby przyciski były po prawej
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(studentInfo, spacer, statusLabel, buttonsBox);
        return row;
    }

    // Metoda markAttendance z wysyłaniem na serwer
    private void markAttendance(Student student, ClassSchedule schedule, Attendance.Status status, Label statusLabel) {
        Attendance attendance = new Attendance(student, schedule, status);

        // Dodaj lokalnie (natychmiastowa reakcja UI)
        schedule.addAttendance(attendance);

        // Zaktualizuj label statusu natychmiast
        statusLabel.setText(status.getDisplayName());
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + status.getColor() + ";");

        // Wyślij na serwer asynchronicznie
        if (schedule.getId() != null) { // Tylko jeśli termin ma ID z serwera
            attendanceService.markStudentAttendanceAsync(student, schedule.getId(), status, "")
                    .thenAccept(success -> {
                        javafx.application.Platform.runLater(() -> {
                            if (success) {
                                System.out.println("✅ Obecność wysłana na serwer: " + student.getFullName() + " - " + status.getDisplayName());
                            } else {
                                System.out.println("⚠️ Ostrzeżenie: Nie udało się wysłać obecności na serwer");
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        javafx.application.Platform.runLater(() -> {
                            System.err.println("❌ Błąd wysyłania obecności na serwer: " + throwable.getMessage());
                        });
                        return null;
                    });
        } else {
            System.out.println("ℹ️ Termin lokalny - obecność zapisana tylko lokalnie");
        }

        showAlert("Sukces", "Oznaczono " + student.getFullName() + " jako " + status.getDisplayName().toLowerCase(),
                Alert.AlertType.INFORMATION);
    }

    // Metoda clearAttendance z usuwaniem z serwera
    private void clearAttendance(Student student, ClassSchedule schedule, Label statusLabel) {
        // Usuń lokalnie (natychmiastowa reakcja UI)
        schedule.removeAttendance(student);

        // Zaktualizuj label statusu natychmiast
        statusLabel.setText("Nie zaznaczono");
        statusLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #6C757D;");

        // Usuń z serwera asynchronicznie
        if (schedule.getId() != null) { // Tylko jeśli termin ma ID z serwera
            attendanceService.removeAttendanceAsync(student.getIndexNumber(), schedule.getId())
                    .thenAccept(success -> {
                        javafx.application.Platform.runLater(() -> {
                            if (success) {
                                System.out.println("✅ Obecność usunięta z serwera: " + student.getFullName());
                            } else {
                                System.out.println("⚠️ Ostrzeżenie: Nie udało się usunąć obecności z serwera");
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        javafx.application.Platform.runLater(() -> {
                            System.err.println("❌ Błąd usuwania obecności z serwera: " + throwable.getMessage());
                        });
                        return null;
                    });
        } else {
            System.out.println("ℹ️ Termin lokalny - obecność usunięta tylko lokalnie");
        }

        showAlert("Info", "Usunięto oznaczenie dla " + student.getFullName(), Alert.AlertType.INFORMATION);
    }

    private void clearAllAttendances(ClassSchedule schedule, Stage stage) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Potwierdzenie");
        confirmAlert.setHeaderText("Czy na pewno chcesz wyczyścić wszystkie oznaczenia obecności?");
        confirmAlert.setContentText("Ta operacja usunie wszystkie wpisy frekwencji dla tego terminu" +
                (schedule.isFromServer() ? " (także z serwera)" : " (lokalnie)") + ".");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // Usuń wszystkie z serwera jeśli to termin serwerowy
            if (schedule.isFromServer() && schedule.getId() != null) {
                // Usuń każdą obecność z serwera
                for (Attendance attendance : schedule.getAttendances()) {
                    attendanceService.removeAttendanceAsync(
                            attendance.getStudent().getIndexNumber(),
                            schedule.getId()
                    ).exceptionally(throwable -> {
                        System.err.println("Błąd usuwania obecności z serwera: " + throwable.getMessage());
                        return null;
                    });
                }
            }

            // Usuń lokalnie
            schedule.getAttendances().clear();

            // Odśwież widok
            stage.close();
            openScheduleDetailWindow(schedule);
            refreshSchedulesList();

            showAlert("Sukces", "Wyczyszczono wszystkie oznaczenia obecności.", Alert.AlertType.INFORMATION);
        }
    }

    private void refreshSchedulesList() {
        // Wymusz odświeżenie ListView terminów
        scheduleListView.refresh();
        updateCounts();
    }

    // Dodaj tę metodę do klasy GroupDetailController.java
// Zastąpi istniejącą metodę onRemoveStudentClick()

    @FXML
    protected void onRemoveStudentClick() {
        Student selectedStudent = studentsListView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            // Tworzymy dialog dla USUWANIA Z GRUPY (nie z systemu!)
            Dialog<ButtonType> confirmDialog = new Dialog<>();
            confirmDialog.setTitle("Usuwanie studenta z grupy");
            confirmDialog.setHeaderText("Czy na pewno chcesz usunąć studenta " + selectedStudent.getFullName() + " z grupy " + currentGroup.getName() + "?");

            // Ikona ostrzeżenia
            confirmDialog.setGraphic(new javafx.scene.control.Label("⚠️"));

            // Buttons - ZMIENIONE nazwy przycisków
            ButtonType removeFromGroupButtonType = new ButtonType("Usuń z grupy", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getDialogPane().getButtonTypes().addAll(removeFromGroupButtonType, cancelButtonType);

            // Tworzenie content z dodatkowymi polami
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: #FFFFFF;");

            // Informacje o studencie
            VBox studentInfo = new VBox(8);
            studentInfo.setStyle("-fx-background-color: rgba(220, 20, 60, 0.05); " +
                    "-fx-padding: 15; " +
                    "-fx-background-radius: 10; " +
                    "-fx-border-color: rgba(220, 20, 60, 0.2); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 10;");

            Label studentNameLabel = new Label("Student: " + selectedStudent.getFullName());
            studentNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #DC143C;");

            Label studentIndexLabel = new Label("Nr indeksu: " + selectedStudent.getIndexNumber());
            studentIndexLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #212529;");

            Label currentGroupLabel = new Label("Obecna grupa: " + currentGroup.getName());
            currentGroupLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #212529;");

            Label actionLabel = new Label("Po usunięciu: Bez grupy (dostępny do przypisania)");
            actionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #38A169; -fx-font-weight: bold;");

            studentInfo.getChildren().addAll(studentNameLabel, studentIndexLabel, currentGroupLabel, actionLabel);

            // Dodatkowe pole - Powód usunięcia z grupy
            VBox reasonSection = new VBox(8);

            Label reasonLabel = new Label("Powód usunięcia z grupy (opcjonalne):");
            reasonLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #212529;");

            ComboBox<String> reasonComboBox = new ComboBox<>();
            reasonComboBox.getItems().addAll(
                    "Przeniesienie do innej grupy",
                    "Zmiana specjalizacji",
                    "Prośba studenta",
                    "Reorganizacja grup",
                    "Błędne przypisanie",
                    "Tymczasowe usunięcie",
                    "Inne"
            );
            reasonComboBox.setPromptText("Wybierz powód...");
            reasonComboBox.setMaxWidth(Double.MAX_VALUE);
            reasonComboBox.setStyle("-fx-background-color: #F8F9FA; " +
                    "-fx-border-color: rgba(220, 20, 60, 0.3); " +
                    "-fx-border-width: 0 0 2 0; " +
                    "-fx-background-radius: 5; " +
                    "-fx-padding: 8;");

            // Pole tekstowe dla dodatkowych uwag
            Label notesLabel = new Label("Dodatkowe uwagi:");
            notesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #212529;");

            TextArea notesTextArea = new TextArea();
            notesTextArea.setPromptText("Wpisz dodatkowe uwagi dotyczące usunięcia z grupy...");
            notesTextArea.setPrefRowCount(3);
            notesTextArea.setMaxHeight(80);
            notesTextArea.setStyle("-fx-background-color: #F8F9FA; " +
                    "-fx-border-color: rgba(220, 20, 60, 0.3); " +
                    "-fx-border-width: 0 0 2 0; " +
                    "-fx-background-radius: 5; " +
                    "-fx-padding: 8; " +
                    "-fx-font-size: 12px;");

            reasonSection.getChildren().addAll(reasonLabel, reasonComboBox, notesLabel, notesTextArea);

            // Checkbox dla potwierdzenia
            CheckBox confirmationCheckBox = new CheckBox("Potwierdzam, że chcę usunąć tego studenta z grupy " + currentGroup.getName());
            confirmationCheckBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #212529; -fx-font-weight: bold;");

            // Informacja - co się stanie
            VBox infoBox = new VBox(5);
            infoBox.setStyle("-fx-background-color: rgba(56, 161, 105, 0.1); " +
                    "-fx-padding: 12; " +
                    "-fx-background-radius: 8; " +
                    "-fx-border-color: rgba(56, 161, 105, 0.3); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 8;");

            Label infoTitle = new Label("ℹ️ INFORMACJA:");
            infoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38A169;");

            Label infoText1 = new Label("• Student zostanie usunięty tylko z tej grupy");
            infoText1.setStyle("-fx-font-size: 11px; -fx-text-fill: #38A169;");

            Label infoText2 = new Label("• Student pozostanie w systemie bez przypisanej grupy");
            infoText2.setStyle("-fx-font-size: 11px; -fx-text-fill: #38A169;");

            Label infoText3 = new Label("• Będzie można go przypisać do innej grupy");
            infoText3.setStyle("-fx-font-size: 11px; -fx-text-fill: #38A169;");

            Label infoText4 = new Label("• Wszystkie wpisy frekwencji dla tego studenta zostaną usunięte");
            infoText4.setStyle("-fx-font-size: 11px; -fx-text-fill: #F56500; -fx-font-weight: bold;");

            infoBox.getChildren().addAll(infoTitle, infoText1, infoText2, infoText3, infoText4);

            // Dodaj wszystko do content
            content.getChildren().addAll(studentInfo, reasonSection, confirmationCheckBox, infoBox);

            confirmDialog.getDialogPane().setContent(content);

            // Stylizacja dialogu
            confirmDialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("styles.css").toExternalForm());
            confirmDialog.getDialogPane().getStyleClass().add("alert-dialog");

            // Walidacja - przycisk usuń z grupy aktywny tylko gdy checkbox zaznaczony
            javafx.scene.Node removeButton = confirmDialog.getDialogPane().lookupButton(removeFromGroupButtonType);
            removeButton.setDisable(true);

            confirmationCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                removeButton.setDisable(!isSelected);
            });

            // Pokaż dialog i przetwórz wynik
            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == removeFromGroupButtonType) {
                // Zbierz dodatkowe informacje
                String reason = reasonComboBox.getValue();
                String notes = notesTextArea.getText().trim();

                // KLUCZOWE: Wywołaj metodę usuwania z grupy (NIE całkowite usuwanie!)
                performStudentRemovalFromGroup(selectedStudent, reason, notes);
            }
        }
    }

    private void performStudentRemovalFromGroup(Student student, String reason, String notes) {
        // Logowanie przed usunięciem z grupy
        logStudentRemovalFromGroup(student, reason, notes);

        System.out.println("🔄 ROZPOCZYNAM usuwanie studenta z grupy (nie z systemu)");
        System.out.println("📋 Student: " + student.getFullName() + " (indeks: " + student.getIndexNumber() + ")");
        System.out.println("📋 Grupa: " + currentGroup.getName());

        // KLUCZOWE: Używamy metody removeStudentFromGroupAsync zamiast deleteStudentAsync!
        studentService.removeStudentFromGroupAsync(student.getIndexNumber())
                .thenAccept(updatedStudent -> {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("✅ Student usunięty z grupy na serwerze");

                        // Usuń z lokalnej listy tej grupy
                        students.remove(student);

                        // Usuń studenta ze wszystkich terminów tej grupy (lokalnie i z serwera)
                        for (ClassSchedule schedule : schedules) {
                            if (schedule.hasAttendanceForStudent(student)) {
                                // Usuń z serwera jeśli termin ma ID
                                if (schedule.getId() != null) {
                                    attendanceService.removeAttendanceAsync(
                                            student.getIndexNumber(),
                                            schedule.getId()
                                    ).exceptionally(throwable -> {
                                        System.err.println("Błąd usuwania obecności studenta z serwera: " + throwable.getMessage());
                                        return null;
                                    });
                                }
                                // Usuń lokalnie
                                schedule.removeAttendance(student);
                            }
                        }

                        refreshSchedulesList();
                        updateCounts();

                        // Pokaż potwierdzenie
                        StringBuilder confirmMessage = new StringBuilder();
                        confirmMessage.append("✅ Student ").append(student.getFullName())
                                .append(" został usunięty z grupy ").append(currentGroup.getName()).append("!");
                        if (reason != null && !reason.isEmpty()) {
                            confirmMessage.append("\n📝 Powód: ").append(reason);
                        }
                        if (notes != null && !notes.isEmpty()) {
                            confirmMessage.append("\n💬 Uwagi: ").append(notes);
                        }
                        confirmMessage.append("\n\n🔄 Student pozostaje w systemie bez grupy.");
                        confirmMessage.append("\n🗑️ Usunięto wszystkie powiązane dane frekwencji z tej grupy.");

                        showAlert("Student usunięty z grupy", confirmMessage.toString(), Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("❌ Błąd usuwania studenta z grupy: " + throwable.getMessage());

                        // Usuń lokalnie mimo błędu serwera
                        students.remove(student);

                        // Usuń studenta ze wszystkich terminów (tylko lokalnie)
                        for (ClassSchedule schedule : schedules) {
                            schedule.removeAttendance(student);
                        }
                        refreshSchedulesList();
                        updateCounts();

                        StringBuilder warningMessage = new StringBuilder();
                        warningMessage.append("⚠️ Student ").append(student.getFullName())
                                .append(" został usunięty z grupy lokalnie,");
                        warningMessage.append("\nale wystąpił problem z serwerem: ").append(throwable.getMessage());
                        if (reason != null && !reason.isEmpty()) {
                            warningMessage.append("\n📝 Powód: ").append(reason);
                        }

                        showAlert("Ostrzeżenie", warningMessage.toString(), Alert.AlertType.WARNING);
                    });
                    return null;
                });
    }


    private void logStudentRemovalFromGroup(Student student, String reason, String notes) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("=== USUNIĘCIE STUDENTA Z GRUPY ===\n");
        logEntry.append("Data: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        logEntry.append("Student: ").append(student.getFullName()).append("\n");
        logEntry.append("Nr indeksu: ").append(student.getIndexNumber()).append("\n");
        logEntry.append("Grupa: ").append(currentGroup.getName()).append("\n");
        logEntry.append("Akcja: USUNIĘCIE Z GRUPY (nie z systemu)").append("\n");

        if (reason != null && !reason.isEmpty()) {
            logEntry.append("Powód: ").append(reason).append("\n");
        }
        if (notes != null && !notes.isEmpty()) {
            logEntry.append("Uwagi: ").append(notes).append("\n");
        }
        logEntry.append("================================\n");

        // Wyświetl w konsoli
        System.out.println(logEntry.toString());
    }

    /**
     * Wykonuje właściwe usunięcie studenta z dodatkowym logowaniem
     */
    private void performStudentRemoval(Student student, String reason, String notes) {
        // Logowanie przed usunięciem
        logStudentRemoval(student, reason, notes);

        // Wywołanie usunięcia z serwera
        studentService.deleteStudentAsync(student.getIndexNumber())
                .thenAccept(success -> {
                    javafx.application.Platform.runLater(() -> {
                        // Usuń z lokalnej listy
                        students.remove(student);

                        // Usuń studenta ze wszystkich terminów (lokalnie i z serwera)
                        for (ClassSchedule schedule : schedules) {
                            if (schedule.hasAttendanceForStudent(student)) {
                                // Usuń z serwera jeśli termin ma ID
                                if (schedule.getId() != null) {
                                    attendanceService.removeAttendanceAsync(
                                            student.getIndexNumber(),
                                            schedule.getId()
                                    ).exceptionally(throwable -> {
                                        System.err.println("Błąd usuwania obecności studenta z serwera: " + throwable.getMessage());
                                        return null;
                                    });
                                }
                                // Usuń lokalnie
                                schedule.removeAttendance(student);
                            }
                        }
                        refreshSchedulesList();
                        updateCounts();

                        // Pokaż potwierdzenie z dodatkowymi informacjami
                        StringBuilder confirmMessage = new StringBuilder();
                        confirmMessage.append("✅ Student ").append(student.getFullName()).append(" został usunięty z serwera!");
                        if (reason != null && !reason.isEmpty()) {
                            confirmMessage.append("\n📝 Powód: ").append(reason);
                        }
                        if (notes != null && !notes.isEmpty()) {
                            confirmMessage.append("\n💬 Uwagi: ").append(notes);
                        }
                        confirmMessage.append("\n🗑️ Usunięto wszystkie powiązane dane frekwencji.");

                        showAlert("Student usunięty", confirmMessage.toString(), Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        // Usuń lokalnie mimo błędu serwera
                        students.remove(student);

                        // Usuń studenta ze wszystkich terminów (tylko lokalnie)
                        for (ClassSchedule schedule : schedules) {
                            schedule.removeAttendance(student);
                        }
                        refreshSchedulesList();
                        updateCounts();

                        StringBuilder warningMessage = new StringBuilder();
                        warningMessage.append("⚠️ Student ").append(student.getFullName()).append(" został usunięty lokalnie,");
                        warningMessage.append("\nale wystąpił problem z serwerem: ").append(throwable.getMessage());
                        if (reason != null && !reason.isEmpty()) {
                            warningMessage.append("\n📝 Powód: ").append(reason);
                        }

                        showAlert("Ostrzeżenie", warningMessage.toString(), Alert.AlertType.WARNING);
                    });
                    return null;
                });
    }

    /**
     * Loguje usunięcie studenta do konsoli/pliku (rozszerz wedle potrzeb)
     */
    private void logStudentRemoval(Student student, String reason, String notes) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("=== USUNIĘCIE STUDENTA ===\n");
        logEntry.append("Data: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        logEntry.append("Student: ").append(student.getFullName()).append("\n");
        logEntry.append("Nr indeksu: ").append(student.getIndexNumber()).append("\n");
        logEntry.append("Grupa: ").append(student.getGroupName() != null ? student.getGroupName() : "Brak").append("\n");

        if (reason != null && !reason.isEmpty()) {
            logEntry.append("Powód: ").append(reason).append("\n");
        }
        if (notes != null && !notes.isEmpty()) {
            logEntry.append("Uwagi: ").append(notes).append("\n");
        }
        logEntry.append("========================\n");

        // Wyświetl w konsoli
        System.out.println(logEntry.toString());

        // TODO: Zapisz do pliku logów jeśli potrzebne
        // appendToLogFile(logEntry.toString());
    }

// Dodaj także import dla nowych klas JavaFX na górze pliku:
/*
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
*/

    @FXML
    protected void onRemoveScheduleClick() {
        ClassSchedule selectedSchedule = scheduleListView.getSelectionModel().getSelectedItem();
        if (selectedSchedule != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Potwierdzenie usunięcia");
            confirmAlert.setHeaderText("Czy na pewno chcesz usunąć termin?");
            confirmAlert.setContentText("Termin: " + selectedSchedule.getSubject() +
                    "\nData: " + selectedSchedule.getFormattedStartTime() +
                    "\nFrekwencja: " + selectedSchedule.getAttendanceSummary() +
                    "\n\nUWAGA: Wszystkie wpisy frekwencji zostaną utracone!" +
                    (selectedSchedule.isFromServer() ? "\n(Usunięcie także z serwera)" : "\n(Usunięcie lokalne)"));

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (selectedSchedule.isFromServer()) {
                    scheduleService.deleteScheduleAsync(selectedSchedule.getId())
                            .thenAccept(success -> {
                                javafx.application.Platform.runLater(() -> {
                                    schedules.remove(selectedSchedule);
                                    updateCounts();
                                    showAlert("Sukces", "Termin został usunięty z serwera.", Alert.AlertType.INFORMATION);
                                });
                            })
                            .exceptionally(throwable -> {
                                javafx.application.Platform.runLater(() -> {
                                    schedules.remove(selectedSchedule);
                                    updateCounts();
                                    showAlert("Ostrzeżenie", "Termin został usunięty lokalnie.", Alert.AlertType.WARNING);
                                });
                                return null;
                            });
                } else {
                    schedules.remove(selectedSchedule);
                    updateCounts();
                    showAlert("Sukces", "Lokalny termin został usunięty.", Alert.AlertType.INFORMATION);
                }
            }
        }
    }

    @FXML
    protected void onRefreshStudentsClick() {
        if (refreshStudentsButton != null) {
            loadStudentsFromServer();
        }
    }

    @FXML
    protected void onRefreshSchedulesClick() {
        if (refreshSchedulesButton != null) {
            loadSchedulesFromServer();
        }
    }

    @FXML
    protected void onLoadStudentsClick() {
        if (loadStudentsButton != null) {
            studentService.getStudentsWithoutGroupAsync()
                    .thenAccept(availableStudents -> {
                        javafx.application.Platform.runLater(() -> {
                            if (availableStudents.isEmpty()) {
                                showAlert("Info", "Brak studentów bez grupy.", Alert.AlertType.INFORMATION);
                            } else {
                                showAlert("Info", "Znaleziono " + availableStudents.size() + " studentów bez grupy.", Alert.AlertType.INFORMATION);
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        javafx.application.Platform.runLater(() -> {
                            showAlert("Błąd", "Nie udało się załadować studentów.", Alert.AlertType.ERROR);
                        });
                        return null;
                    });
        }
    }

    @FXML
    protected void onBackClick() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private void clearStudentForm() {
        firstNameField.clear();
        lastNameField.clear();
        indexNumberField.clear();
    }

    private void clearTerminForm() {
        terminNameField.clear();
        terminDatePicker.setValue(null);
        terminStartTimeField.clear();
        terminEndTimeField.clear();
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
        alert.showAndWait();
    }

    // Klasa StudentListCell
    private class StudentListCell extends ListCell<Student> {
        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);
            if (empty || student == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox cellContent = new VBox(3);
                cellContent.getStyleClass().add("student-cell");

                Label nameLabel = new Label(student.getFullName());
                nameLabel.getStyleClass().add("student-name");

                Label indexLabel = new Label("Nr indeksu: " + student.getIndexNumber());
                indexLabel.getStyleClass().add("student-index");

                Label groupLabel = new Label("Grupa: " + (student.getGroupName() != null ? student.getGroupName() : "Brak"));
                groupLabel.getStyleClass().add("student-group");

                cellContent.getChildren().addAll(nameLabel, indexLabel, groupLabel);
                setGraphic(cellContent);
                setText(null);
            }
        }
    }

    // Klasa ScheduleListCell z informacjami o frekwencji i statusie serwera
    private class ScheduleListCell extends ListCell<ClassSchedule> {
        @Override
        protected void updateItem(ClassSchedule schedule, boolean empty) {
            super.updateItem(schedule, empty);
            if (empty || schedule == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox cellContent = new VBox(3);
                cellContent.getStyleClass().add("schedule-cell");

                Label subjectLabel = new Label(schedule.getSubject());
                subjectLabel.getStyleClass().add("schedule-subject");

                Label dateTimeLabel = new Label("Data: " + schedule.getFormattedStartTime() + " - " + schedule.getFormattedEndTime());
                dateTimeLabel.getStyleClass().add("schedule-datetime");

                // Status źródła
                Label sourceLabel = new Label();
                if (schedule.isFromServer()) {
                    sourceLabel.setText("🔵 Serwer (ID: " + schedule.getId() + ")");
                    sourceLabel.setStyle("-fx-text-fill: #38A169; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    sourceLabel.setText("🔴 Lokalny");
                    sourceLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px; -fx-font-weight: bold;");
                }

                // Wyświetlanie statystyk frekwencji
                Label attendanceLabel = new Label(schedule.getAttendanceSummary());
                if (schedule.getTotalAttendanceCount() > 0) {
                    attendanceLabel.setStyle("-fx-text-fill: #212529; -fx-font-weight: bold; -fx-font-size: 12px;");
                } else {
                    attendanceLabel.setStyle("-fx-text-fill: #6C757D; -fx-font-style: italic; -fx-font-size: 12px;");
                }

                // Szczegółowe liczniki w kolorach
                if (schedule.getTotalAttendanceCount() > 0) {
                    HBox statsBox = new HBox(10);

                    Label presentLabel = new Label("✅ " + schedule.getPresentCount());
                    presentLabel.setStyle("-fx-text-fill: #38A169; -fx-font-size: 11px; -fx-font-weight: bold;");

                    Label lateLabel = new Label("⏰ " + schedule.getLateCount());
                    lateLabel.setStyle("-fx-text-fill: #F56500; -fx-font-size: 11px; -fx-font-weight: bold;");

                    Label absentLabel = new Label("❌ " + schedule.getAbsentCount());
                    absentLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px; -fx-font-weight: bold;");

                    statsBox.getChildren().addAll(presentLabel, lateLabel, absentLabel);
                    cellContent.getChildren().addAll(subjectLabel, dateTimeLabel, sourceLabel, attendanceLabel, statsBox);
                } else {
                    cellContent.getChildren().addAll(subjectLabel, dateTimeLabel, sourceLabel, attendanceLabel);
                }

                // Podpowiedź dla użytkownika
                Label clickHintLabel = new Label("💡 Kliknij aby zarządzać frekwencją");
                clickHintLabel.setStyle("-fx-text-fill: #6C757D; -fx-font-size: 10px; -fx-font-style: italic;");
                cellContent.getChildren().add(clickHintLabel);

                // Informacja o synchronizacji z serwerem
                if (schedule.isFromServer()) {
                    Label syncLabel = new Label("🔄 Synchronizacja z serwerem dostępna");
                    syncLabel.setStyle("-fx-text-fill: #38A169; -fx-font-size: 9px; -fx-font-style: italic;");
                    cellContent.getChildren().add(syncLabel);
                } else {
                    Label localLabel = new Label("⚠️ Tylko dane lokalne");
                    localLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 9px; -fx-font-style: italic;");
                    cellContent.getChildren().add(localLabel);
                }

                setGraphic(cellContent);
                setText(null);
            }
        }
    }

    @FXML
    protected void onForceRefreshStudentsClick() {
        System.out.println("🔄 WYMUSZONE ODŚWIEŻENIE przez użytkownika");

        if (refreshStudentsButton != null) {
            refreshStudentsButton.setText("Odświeżam...");
            refreshStudentsButton.setDisable(true);
        }

        loadStudentsFromServer();

        // Przywróć przycisk po 2 sekundach
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> {
            if (refreshStudentsButton != null) {
                refreshStudentsButton.setText("🔄 Odśwież z serwera");
                refreshStudentsButton.setDisable(false);
            }
        });
        pause.play();
    }

    // Dodaj też metodę sprawdzania stanu bazy danych
    @FXML
    protected void onCheckDatabaseClick() {
        System.out.println("🔍 SPRAWDZANIE STANU BAZY DANYCH");

        // Sprawdź wszystkich studentów
        studentService.getAllStudentsAsync()
                .thenAccept(allStudents -> {
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("=== WSZYSCY STUDENCI W BAZIE ===");
                        for (Student student : allStudents) {
                            System.out.println("- " + student.getFullName() +
                                    " (grupa: '" + student.getGroupName() + "')");
                        }

                        // Filtruj studentów dla bieżącej grupy
                        long studentsInCurrentGroup = allStudents.stream()
                                .filter(s -> s.getGroupName() != null && s.getGroupName().equals(currentGroup.getName()))
                                .count();

                        System.out.println("==============================");
                        System.out.println("📊 Studentów w grupie '" + currentGroup.getName() + "': " + studentsInCurrentGroup);
                        System.out.println("📊 Studentów w lokalnej liście: " + students.size());

                        showAlert("Info z bazy danych",
                                "Wszystkich studentów w bazie: " + allStudents.size() +
                                        "\nStudentów w grupie '" + currentGroup.getName() + "': " + studentsInCurrentGroup +
                                        "\nStudentów w lokalnej liście: " + students.size() +
                                        "\n\nSzczegóły w konsoli.",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("❌ Błąd sprawdzania bazy: " + throwable.getMessage());
                        showAlert("Błąd", "Nie można sprawdzić stanu bazy: " + throwable.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }
}