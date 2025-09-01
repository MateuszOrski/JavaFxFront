package com.example.javafxfront;

import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class GroupDetailController {
    @FXML private Label groupNameLabel;
    @FXML private Label groupSpecializationLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label scheduleCountLabel;

    // Student form
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField indexNumberField;
    @FXML private Button addStudentButton;

    // Termin form - uproszczone pola
    @FXML private TextField terminNameField;
    @FXML private DatePicker terminDatePicker;
    @FXML private TextField terminStartTimeField;
    @FXML private TextField terminEndTimeField;
    @FXML private Label terminGroupLabel;
    @FXML private Button addTerminButton;

    // Lists
    @FXML private ListView<Student> studentsListView;
    @FXML private ListView<ClassSchedule> scheduleListView;

    // Action buttons
    @FXML private Button removeStudentButton;
    @FXML private Button removeScheduleButton;
    @FXML private Button backButton;

    private Group currentGroup;
    private ObservableList<Student> students;
    private ObservableList<ClassSchedule> schedules;

    @FXML
    protected void initialize() {
        // Inicjalizacja list
        students = FXCollections.observableArrayList();
        schedules = FXCollections.observableArrayList();

        studentsListView.setItems(students);
        scheduleListView.setItems(schedules);

        // Konfiguracja ListView
        studentsListView.setCellFactory(listView -> new StudentListCell());
        scheduleListView.setCellFactory(listView -> new ScheduleListCell());

        // Nasłuchiwanie zmian w selekcji
        studentsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            removeStudentButton.setDisable(newSelection == null);
        });

        scheduleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            removeScheduleButton.setDisable(newSelection == null);
        });

        // Dodaj listener do kliknięcia na termin
        scheduleListView.setOnMouseClicked(event -> {
            ClassSchedule selectedSchedule = scheduleListView.getSelectionModel().getSelectedItem();
            if (selectedSchedule != null) {
                openScheduleDetailWindow(selectedSchedule);
            }
        });

        // Początkowy stan przycisków
        removeStudentButton.setDisable(true);
        removeScheduleButton.setDisable(true);

        // Walidacja pól
        setupValidation();
    }

    private void setupValidation() {
        // Walidacja numeru indeksu - tylko cyfry, max 6 znaków
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

        // Walidacja czasu rozpoczęcia
        if (terminStartTimeField != null) {
            setupTimeFieldValidation(terminStartTimeField);
        }

        // Walidacja czasu zakończenia
        if (terminEndTimeField != null) {
            setupTimeFieldValidation(terminEndTimeField);
        }
    }

    private void setupTimeFieldValidation(TextField timeField) {
        timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Usuń wszystko co nie jest cyfrą ani dwukropkiem
            String filtered = newValue.replaceAll("[^0-9:]", "");

            // Ogranicz do formatu HH:MM (maksymalnie 5 znaków)
            if (filtered.length() > 5) {
                filtered = filtered.substring(0, 5);
            }

            // Automatycznie dodaj dwukropek po 2 cyfrach
            if (filtered.length() == 2 && !filtered.contains(":")) {
                filtered = filtered + ":";
            }

            // Ustaw nową wartość tylko jeśli się zmieniła
            if (!filtered.equals(newValue)) {
                timeField.setText(filtered);
                timeField.positionCaret(filtered.length());
            }
        });
    }

    public void setGroup(Group group) {
        this.currentGroup = group;
        updateGroupInfo();
        loadSampleData();
    }

    private void updateGroupInfo() {
        if (currentGroup != null) {
            groupNameLabel.setText(currentGroup.getName());
            groupSpecializationLabel.setText(currentGroup.getSpecialization());

            // Ustaw nazwę grupy w formularzu terminu
            if (terminGroupLabel != null) {
                terminGroupLabel.setText(currentGroup.getName());
            }

            updateCounts();
        }
    }

    private void updateCounts() {
        studentCountLabel.setText("Liczba studentów: " + students.size());
        scheduleCountLabel.setText("Liczba terminów: " + schedules.size());
    }

    private void loadSampleData() {
        // Aplikacja startuje bez przykładowych danych
        updateCounts();
    }

    @FXML
    protected void onAddStudentClick() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String indexNumber = indexNumberField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || indexNumber.isEmpty()) {
            showAlert("Błąd", "Wszystkie pola studenta muszą być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        // Walidacja numeru indeksu
        if (!indexNumber.matches("\\d{6}")) {
            showAlert("Błąd", "Numer indeksu musi składać się z dokładnie 6 cyfr!", Alert.AlertType.WARNING);
            return;
        }

        // Sprawdzenie czy student o takim numerze indeksu już istnieje
        boolean indexExists = students.stream().anyMatch(s -> s.getIndexNumber().equals(indexNumber));
        if (indexExists) {
            showAlert("Błąd", "Student o takim numerze indeksu już istnieje!", Alert.AlertType.WARNING);
            return;
        }

        // Dodanie studenta z przypisaną grupą
        Student newStudent = new Student(firstName, lastName, indexNumber, currentGroup.getName());
        students.add(newStudent);

        // Animacja i czyszczenie
        animateButton(addStudentButton);
        clearStudentForm();
        updateCounts();

        showAlert("Sukces", "Student " + newStudent.getFullName() +
                        " został dodany do grupy " + currentGroup.getName() + "!",
                Alert.AlertType.INFORMATION);
    }

    @FXML
    protected void onAddTerminClick() {
        String terminName = terminNameField.getText().trim();
        LocalDate date = terminDatePicker.getValue();
        String startTimeText = terminStartTimeField.getText().trim();
        String endTimeText = terminEndTimeField.getText().trim();

        // Walidacja wymaganych pól
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

        // Walidacja i parsowanie czasu rozpoczęcia
        LocalTime startTime;
        try {
            startTime = parseTime(startTimeText);
        } catch (Exception e) {
            showAlert("Błąd", "Nieprawidłowa godzina rozpoczęcia! Użyj formatu HH:MM (np. 10:15)", Alert.AlertType.WARNING);
            return;
        }

        // Walidacja i parsowanie czasu zakończenia (opcjonalne)
        LocalTime endTime;
        if (endTimeText.isEmpty()) {
            // Jeśli nie podano czasu zakończenia, ustaw na godzinę później
            endTime = startTime.plusHours(1);
        } else {
            try {
                endTime = parseTime(endTimeText);
                // Sprawdź czy czas zakończenia jest po czasie rozpoczęcia
                if (!endTime.isAfter(startTime)) {
                    showAlert("Błąd", "Czas zakończenia musi być później niż czas rozpoczęcia!", Alert.AlertType.WARNING);
                    return;
                }
            } catch (Exception e) {
                showAlert("Błąd", "Nieprawidłowa godzina zakończenia! Użyj formatu HH:MM (np. 12:00)", Alert.AlertType.WARNING);
                return;
            }
        }

        // Utworzenie terminu
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

        ClassSchedule newSchedule = new ClassSchedule(
                terminName,
                "",              // pusta sala
                startDateTime,
                endDateTime,
                "",              // pusty prowadzący
                "",              // puste uwagi
                currentGroup.getName()
        );

        schedules.add(newSchedule);

        // Animacja i czyszczenie
        animateButton(addTerminButton);
        clearTerminForm();
        updateCounts();

        showAlert("Sukces", "Termin '" + terminName + "' został dodany do grupy " + currentGroup.getName() + "!",
                Alert.AlertType.INFORMATION);
    }

    private LocalTime parseTime(String timeText) throws DateTimeParseException {
        // Sprawdź format HH:MM
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

    private void openScheduleDetailWindow(ClassSchedule schedule) {
        try {
            // Tworzenie nowego okna z detalami terminu
            Stage newStage = new Stage();
            newStage.setTitle("Szczegóły terminu - " + schedule.getSubject());
            newStage.setWidth(500);
            newStage.setHeight(350);

            // Tworzenie zawartości okna
            VBox root = new VBox(20);
            root.setStyle("-fx-background-color: white; -fx-padding: 30;");

            Label titleLabel = new Label("Szczegóły terminu");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #DC143C;");

            VBox detailsBox = new VBox(10);
            detailsBox.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 20; -fx-background-radius: 10;");

            detailsBox.getChildren().addAll(
                    createDetailLabel("Nazwa:", schedule.getSubject()),
                    createDetailLabel("Data:", schedule.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                    createDetailLabel("Czas:", schedule.getFormattedStartTime().substring(11) + " - " + schedule.getFormattedEndTime()),
                    createDetailLabel("Grupa:", schedule.getGroupName()),
                    createDetailLabel("Utworzono:", schedule.getFormattedCreatedDate())
            );

            Button closeButton = new Button("Zamknij");
            closeButton.setOnAction(e -> newStage.close());
            closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 20;");

            root.getChildren().addAll(titleLabel, detailsBox, closeButton);

            Scene scene = new Scene(root);
            newStage.setScene(scene);
            newStage.show();

        } catch (Exception e) {
            showAlert("Błąd", "Nie udało się otworzyć szczegółów terminu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox createDetailLabel(String label, String value) {
        VBox container = new VBox(3);

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6C757D;");

        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 14px; -fx-text-fill: #212529;");
        valueText.setWrapText(true);

        container.getChildren().addAll(labelText, valueText);
        return container;
    }

    @FXML
    protected void onRemoveStudentClick() {
        Student selectedStudent = studentsListView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Potwierdzenie usunięcia");
            confirmAlert.setHeaderText("Czy na pewno chcesz usunąć studenta?");
            confirmAlert.setContentText("Student: " + selectedStudent.getFullName() +
                    "\nNr indeksu: " + selectedStudent.getIndexNumber() +
                    "\nGrupa: " + selectedStudent.getGroupName() +
                    "\n\nTa operacja jest nieodwracalna!");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                students.remove(selectedStudent);
                animateButton(removeStudentButton);
                updateCounts();

                showAlert("Usunięto", "Student " + selectedStudent.getFullName() + " został usunięty.",
                        Alert.AlertType.INFORMATION);
            }
        }
    }

    @FXML
    protected void onRemoveScheduleClick() {
        ClassSchedule selectedSchedule = scheduleListView.getSelectionModel().getSelectedItem();
        if (selectedSchedule != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Potwierdzenie usunięcia");
            confirmAlert.setHeaderText("Czy na pewno chcesz usunąć termin?");
            confirmAlert.setContentText("Termin: " + selectedSchedule.getSubject() +
                    "\nData: " + selectedSchedule.getFormattedStartTime() +
                    "\nGrupa: " + selectedSchedule.getGroupName() +
                    "\n\nTa operacja jest nieodwracalna!");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                schedules.remove(selectedSchedule);
                animateButton(removeScheduleButton);
                updateCounts();

                showAlert("Usunięto", "Termin '" + selectedSchedule.getSubject() + "' został usunięty.",
                        Alert.AlertType.INFORMATION);
            }
        }
    }

    @FXML
    protected void onBackClick() {
        // Zamknięcie okna
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

    // Klasy dla customowych komórek ListView
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

                Label groupLabel = new Label("Grupa: " + student.getGroupName());
                groupLabel.getStyleClass().add("student-group");

                Label dateLabel = new Label("Dodano: " + student.getFormattedDate());
                dateLabel.getStyleClass().add("student-date");

                cellContent.getChildren().addAll(nameLabel, indexLabel, groupLabel, dateLabel);
                setGraphic(cellContent);
                setText(null);
            }
        }
    }

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

                Label groupLabel = new Label("Grupa: " + schedule.getGroupName());
                groupLabel.getStyleClass().add("schedule-group");

                Label createdLabel = new Label("Utworzono: " + schedule.getFormattedCreatedDate());
                createdLabel.getStyleClass().add("schedule-created");

                cellContent.getChildren().addAll(subjectLabel, dateTimeLabel, groupLabel, createdLabel);
                setGraphic(cellContent);
                setText(null);
            }
        }
    }
}