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

public class AttendanceReportController {

    @FXML private Label groupNameLabel;
    @FXML private Label reportTitleLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalSchedulesLabel;

    // Tabela główna - obecności
    @FXML private TableView<AttendanceReportRow> attendanceTable;
    @FXML private TableColumn<AttendanceReportRow, String> studentNameColumn;
    @FXML private TableColumn<AttendanceReportRow, String> indexColumn;

    // Statystyki
    @FXML private Label avgAttendanceLabel;
    @FXML private Label bestStudentLabel;
    @FXML private Label worstStudentLabel;

    // Przyciski
    @FXML private Button exportCSVButton;
    @FXML private Button closeButton;
    @FXML private Button refreshButton;

    // Filtry
    @FXML private ComboBox<String> filterScheduleComboBox;
    @FXML private ComboBox<AttendanceFilter> filterTypeComboBox;

    private Group currentGroup;
    private List<Student> students;
    private List<ClassSchedule> schedules;
    private AttendanceService attendanceService;
    private ObservableList<AttendanceReportRow> reportData;

    public enum AttendanceFilter {
        ALL("Wszystkie"),
        PRESENT_ONLY("Tylko obecni"),
        ABSENT_ONLY("Tylko nieobecni"),
        LATE_ONLY("Tylko spóźnieni");

        private final String displayName;
        AttendanceFilter(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

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

    public void setData(Group group, List<Student> students, List<ClassSchedule> schedules) {
        this.currentGroup = group;
        this.students = students;
        this.schedules = schedules;

        updateHeader();
        setupScheduleFilter();
        generateReport();
        calculateStatistics();
    }

    private void setupTableColumns() {
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        indexColumn.setCellValueFactory(new PropertyValueFactory<>("indexNumber"));

        // Dynamiczne kolumny dla każdego terminu będą dodane w generateReport()
    }

    private void setupFilters() {
        filterTypeComboBox.setItems(FXCollections.observableArrayList(AttendanceFilter.values()));
        filterTypeComboBox.setValue(AttendanceFilter.ALL);
    }

    private void setupScheduleFilter() {
        ObservableList<String> scheduleNames = FXCollections.observableArrayList();
        scheduleNames.add("Wszystkie terminy");

        for (ClassSchedule schedule : schedules) {
            scheduleNames.add(schedule.getSubject() + " (" + schedule.getFormattedStartTime() + ")");
        }

        filterScheduleComboBox.setItems(scheduleNames);
        filterScheduleComboBox.setValue("Wszystkie terminy");
    }

    private void updateHeader() {
        if (currentGroup != null) {
            groupNameLabel.setText("Grupa: " + currentGroup.getName());
            reportTitleLabel.setText("Dziennik obecności - " + currentGroup.getName());
            totalStudentsLabel.setText("Liczba studentów: " + students.size());
            totalSchedulesLabel.setText("Liczba terminów: " + schedules.size());
        }
    }

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

    private void applyFilters() {
        // TODO: Implementuj filtrowanie danych
        // Na podstawie wybranego terminu i typu filtru
    }

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

    @FXML
    private void refreshReport() {
        generateReport();
        calculateStatistics();
        showAlert("Info", "Raport został odświeżony", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AttendanceReportRow {
        private final Student student;
        private final List<String> attendanceStatuses = new java.util.ArrayList<>();
        private String statistics = "";

        public AttendanceReportRow(Student student) {
            this.student = student;
        }

        public void addAttendance(String status) {
            attendanceStatuses.add(status);
        }

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

        public double getAttendancePercentage() {
            if (attendanceStatuses.isEmpty()) return 0.0;

            long present = attendanceStatuses.stream().filter(s -> s.equals("Obecny")).count();
            long late = attendanceStatuses.stream().filter(s -> s.equals("Spóźniony")).count();
            long total = attendanceStatuses.stream().filter(s -> !s.equals("Nie zaznaczono")).count();

            if (total == 0) return 0.0;
            return (double) (present + late) / total * 100;
        }

        public String getStudentName() { return student.getFullName(); }
        public String getIndexNumber() { return student.getIndexNumber(); }
        public String getStatistics() { return statistics; }

        public String getAttendanceForSchedule(int index) {
            if (index >= 0 && index < attendanceStatuses.size()) {
                return attendanceStatuses.get(index);
            }
            return "Nie zaznaczono";
        }
    }
}