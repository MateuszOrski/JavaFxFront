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

    // Termin form - uproszczony
    @FXML private TextField terminField;
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
                openEmptyWindow();
            }
        });

        // Początkowy stan przycisków
        removeStudentButton.setDisable(true);
        removeScheduleButton.setDisable(true);

        // Dodaj listener do pola numeru indeksu - tylko cyfry, max 6 znaków
        indexNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Usuń wszystko co nie jest cyfrą
            String digitsOnly = newValue.replaceAll("[^0-9]", "");
            // Ogranicz do 6 cyfr
            if (digitsOnly.length() > 6) {
                digitsOnly = digitsOnly.substring(0, 6);
            }
            // Ustaw nową wartość tylko jeśli się zmieniła
            if (!digitsOnly.equals(newValue)) {
                indexNumberField.setText(digitsOnly);
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
            updateCounts();
        }
    }

    private void updateCounts() {
        studentCountLabel.setText("Liczba studentów: " + students.size());
        scheduleCountLabel.setText("Liczba terminów: " + schedules.size());
    }

    private void loadSampleData() {
        // Aplikacja startuje bez przykładowych studentów i terminów
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

        // Walidacja numeru indeksu - musi być dokładnie 6 cyfr
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
        String termin = terminField.getText().trim();

        if (termin.isEmpty()) {
            showAlert("Błąd", "Pole terminu musi być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        // Utworzenie terminu z automatycznym przypisaniem grupy
        // Używamy aktualnej daty i czasu jako domyślnych wartości
        LocalDateTime now = LocalDateTime.now();
        ClassSchedule newTermin = new ClassSchedule(
            termin,           // subject jako termin
            "",              // pusta sala
            now,             // czas rozpoczęcia
            now.plusHours(1), // czas zakończenia (1 godzina później)
            "",              // pusty prowadzący
            "",              // puste uwagi
            currentGroup.getName() // automatycznie przypisana grupa
        );
        
        schedules.add(newTermin);

        // Animacja i czyszczenie
        animateButton(addTerminButton);
        clearTerminForm();
        updateCounts();

        showAlert("Sukces", "Termin '" + termin + "' został dodany do grupy " + currentGroup.getName() + "!",
                Alert.AlertType.INFORMATION);
    }

    private void openEmptyWindow() {
        try {
            // Tworzenie nowego okna
            Stage newStage = new Stage();
            newStage.setTitle("Nowe okno - " + currentGroup.getName());
            newStage.setWidth(600);
            newStage.setHeight(400);
            
            // Tworzenie pustego kontenera
            VBox root = new VBox(20);
            root.setStyle("-fx-background-color: white; -fx-padding: 20;");
            
            Label titleLabel = new Label("Puste okno dla terminu");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            
            Label infoLabel = new Label("To jest puste okno otwarte po kliknięciu na termin.");
            infoLabel.setStyle("-fx-font-size: 14px;");
            
            Button closeButton = new Button("Zamknij");
            closeButton.setOnAction(e -> newStage.close());
            closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20;");
            
            root.getChildren().addAll(titleLabel, infoLabel, closeButton);
            
            Scene scene = new Scene(root);
            newStage.setScene(scene);
            newStage.show();
            
        } catch (Exception e) {
            showAlert("Błąd", "Nie udało się otworzyć nowego okna: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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
        terminField.clear();
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

                Label groupLabel = new Label("Grupa: " + schedule.getGroupName());
                groupLabel.getStyleClass().add("schedule-group");

                Label timeLabel = new Label("Dodano: " + schedule.getFormattedCreatedDate());
                timeLabel.getStyleClass().add("schedule-time");

                cellContent.getChildren().addAll(subjectLabel, groupLabel, timeLabel);
                setGraphic(cellContent);
                setText(null);
            }
        }
    }
}