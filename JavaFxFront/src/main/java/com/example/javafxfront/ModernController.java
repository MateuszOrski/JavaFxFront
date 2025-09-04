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

public class ModernController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField groupNameField;
    @FXML private TextField specializationField;
    @FXML private Button addGroupButton;
    @FXML private TextField studentFirstNameField;
    @FXML private TextField studentLastNameField;
    @FXML private TextField studentIndexField;
    @FXML private Button addStudentGlobalButton;
    @FXML private VBox addStudentCard;
    @FXML private ListView<Group> groupsListView;
    @FXML private Button enterGroupButton;
    @FXML private Button deleteGroupButton;
    @FXML private VBox addGroupCard;
    @FXML private VBox groupsCard;
    @FXML private Label groupCountLabel;
    @FXML private Button refreshButton;
    @FXML private Label serverStatusLabel;

    // DODANE - Nowe elementy dla studentów
    @FXML private Button refreshStudentsGlobalButton;
    @FXML private Label studentCountLabel;

    // ========== NOWE POLA ZARZĄDZANIA STUDENTAMI ==========
    @FXML private VBox studentManagementCard;
    @FXML private TextField searchStudentField;
    @FXML private Button searchStudentButton;
    @FXML private Button refreshAllStudentsButton;
    @FXML private Label allStudentsCountLabel;

    @FXML private VBox foundStudentInfo;
    @FXML private Label foundStudentNameLabel;
    @FXML private Label foundStudentGroupLabel;
    @FXML private Label foundStudentDateLabel;
    @FXML private Button editFoundStudentButton;
    @FXML private Button removeFoundStudentButton;

    @FXML private ListView<Student> recentStudentsListView;
    @FXML private Button manageAllStudentsButton;

    // Obecnie znaleziony student
    private Student currentFoundStudent;

    // Lista ostatnio dodanych studentów
    private ObservableList<Student> recentStudents;
    // =====================================================

    private ObservableList<Group> groups;
    private GroupService groupService;
    private StudentService studentService;

    @FXML
    protected void initialize() {
        // Inicjalizacja listy grup i serwisów
        groups = FXCollections.observableArrayList();
        groupsListView.setItems(groups);
        groupService = new GroupService();
        studentService = new StudentService();

        // Konfiguracja ListView
        groupsListView.setCellFactory(listView -> new GroupListCell());

        // Nasłuchiwanie zmian w selekcji
        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            enterGroupButton.setDisable(!hasSelection);
            deleteGroupButton.setDisable(!hasSelection);
        });

        // Początkowy stan przycisków
        enterGroupButton.setDisable(true);
        deleteGroupButton.setDisable(true);

        updateGroupCount();
        checkServerConnection();
        setupStudentIndexValidation();
        loadStudentCountFromServer(); // DODANE

        // ========== DODANE - INICJALIZACJA ZARZĄDZANIA STUDENTAMI ==========
        initializeStudentManagement();
        // ==================================================================
    }

    // ========== NOWE METODY ZARZĄDZANIA STUDENTAMI ==========

    private void initializeStudentManagement() {
        recentStudents = FXCollections.observableArrayList();
        if (recentStudentsListView != null) {
            recentStudentsListView.setItems(recentStudents);
            recentStudentsListView.setCellFactory(listView -> new RecentStudentListCell());
        }

        // Ukryj info o znalezionym studencie na początku
        if (foundStudentInfo != null) {
            foundStudentInfo.setVisible(false);
            foundStudentInfo.setManaged(false);
        }

        // Walidacja pola wyszukiwania
        setupSearchValidation();

        // Załaduj ostatnio dodanych studentów
        loadRecentStudents();
        loadAllStudentsCount();
    }

    private void setupSearchValidation() {
        if (searchStudentField != null) {
            searchStudentField.textProperty().addListener((observable, oldValue, newValue) -> {
                // Tylko cyfry, max 6 znaków
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                if (digitsOnly.length() > 6) {
                    digitsOnly = digitsOnly.substring(0, 6);
                }
                if (!digitsOnly.equals(newValue)) {
                    searchStudentField.setText(digitsOnly);
                }

                // Ukryj info o znalezionym studencie gdy użytkownik zmienia wyszukiwanie
                hideFoundStudentInfo();
            });
        }
    }

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

        // Szukaj studenta na serwerze
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

    private void hideFoundStudentInfo() {
        currentFoundStudent = null;
        if (foundStudentInfo != null) {
            foundStudentInfo.setVisible(false);
            foundStudentInfo.setManaged(false);
        }
    }

    @FXML
    protected void onEditFoundStudentClick() {
        if (currentFoundStudent != null) {
            // Otwórz dialog edycji studenta
            openEditStudentDialog(currentFoundStudent);
        }
    }

    @FXML
    protected void onRemoveFoundStudentClick() {
        if (currentFoundStudent != null) {
            // *** TU UŻYWAMY NOWEJ FUNKCJI USUWANIA Z DODATKOWYMI POLAMI ***
            performAdvancedStudentRemoval(currentFoundStudent);
        }
    }

    @FXML
    protected void onRefreshAllStudentsClick() {
        loadAllStudentsCount();
        loadRecentStudents();
    }

    @FXML
    protected void onManageAllStudentsClick() {
        // Otwórz okno pełnego zarządzania studentami
        openFullStudentManagementWindow();
    }

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
     * GŁÓWNA FUNKCJA - Zaawansowane usuwanie studenta z dodatkowymi polami
     */
    private void performAdvancedStudentRemoval(Student student) {
        // Użyj tej samej logiki co w GroupDetailController, ale dostosowanej do głównego ekranu
        Dialog<ButtonType> confirmDialog = new Dialog<>();
        confirmDialog.setTitle("Usuwanie studenta z systemu");
        confirmDialog.setHeaderText("Czy na pewno chcesz usunąć studenta " + student.getFullName() + " z całego systemu?");

        ButtonType removeButtonType = new ButtonType("Usuń całkowicie", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getDialogPane().getButtonTypes().addAll(removeButtonType, cancelButtonType);

        // Content dialogu
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Info o studencie
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

        // Pole powodu
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

        // Checkbox potwierdzenia
        CheckBox confirmBox = new CheckBox("Potwierdzam całkowite usunięcie studenta z systemu");
        confirmBox.setStyle("-fx-font-weight: bold;");

        // Ostrzeżenie
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

        // Stylizacja dialogu
        confirmDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm());
        confirmDialog.getDialogPane().getStyleClass().add("alert-dialog");

        // Walidacja
        javafx.scene.Node removeButton = confirmDialog.getDialogPane().lookupButton(removeButtonType);
        removeButton.setDisable(true);
        confirmBox.selectedProperty().addListener((obs, was, is) -> removeButton.setDisable(!is));

        // Pokaż dialog
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == removeButtonType) {
            String reason = reasonCombo.getValue();
            String notes = notesArea.getText().trim();

            // Wykonaj usunięcie
            executeStudentRemovalFromSystem(student, reason, notes);
        }
    }

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

    // Placeholder metody - do implementacji
    private void openEditStudentDialog(Student student) {
        showAlert("Info", "Funkcja edycji studenta będzie dostępna w przyszłej wersji.", Alert.AlertType.INFORMATION);
    }

    private void openFullStudentManagementWindow() {
        showAlert("Info", "Pełny panel zarządzania studentami będzie dostępny w przyszłej wersji.", Alert.AlertType.INFORMATION);
    }

    // === KOMÓRKA LISTY OSTATNICH STUDENTÓW ===
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

    // ========== KONIEC NOWYCH METOD ==========

    @FXML
    protected void onAddGroupClick() {
        String groupName = groupNameField.getText().trim();
        String specialization = specializationField.getText().trim();

        if (groupName.isEmpty() || specialization.isEmpty()) {
            showAlert("Błąd", "Wszystkie pola muszą być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        // Sprawdzenie czy grupa o takiej nazwie już istnieje lokalnie
        boolean groupExists = groups.stream().anyMatch(g -> g.getName().equalsIgnoreCase(groupName));
        if (groupExists) {
            showAlert("Błąd", "Grupa o nazwie '" + groupName + "' już istnieje w lokalnej liście!", Alert.AlertType.WARNING);
            return;
        }

        // Utworzenie grupy
        Group newGroup = new Group(groupName, specialization);

        // Wyłączenie przycisku podczas wysyłania
        addGroupButton.setDisable(true);
        addGroupButton.setText("Dodawanie...");

        // WYSŁANIE NA SERWER
        groupService.addGroupAsync(newGroup)
                .thenAccept(savedGroup -> {
                    // Sukces - dodaj do lokalnej listy
                    javafx.application.Platform.runLater(() -> {
                        addGroupButton.setDisable(false);
                        addGroupButton.setText("Dodaj grupę");

                        groups.add(newGroup);
                        animateButton(addGroupButton);

                        // Czyszczenie pól
                        groupNameField.clear();
                        specializationField.clear();

                        updateGroupCount();

                        showAlert("Sukces", "Grupa '" + groupName + "' została dodana na serwer!", Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    // Błąd
                    javafx.application.Platform.runLater(() -> {
                        addGroupButton.setDisable(false);
                        addGroupButton.setText("Dodaj grupę");

                        // Sprawdź czy to błąd duplikatu
                        if (throwable.getCause() instanceof GroupService.GroupAlreadyExistsException) {
                            showAlert("Grupa już istnieje",
                                    throwable.getCause().getMessage() +
                                            "\nSprawdź nazwę grupy i spróbuj ponownie z inną nazwą.",
                                    Alert.AlertType.WARNING);
                        } else {
                            // Dodaj lokalnie mimo błędu serwera
                            groups.add(newGroup);
                            animateButton(addGroupButton);

                            // Czyszczenie pól
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

    @FXML
    protected void onEnterGroupClick() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            animateButton(enterGroupButton);

            // Otwórz nowe okno z detalami grupy
            openGroupDetailWindow(selectedGroup);
        }
    }

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

            // Stylizacja alertu
            confirmAlert.getDialogPane().getStylesheets().add(
                    getClass().getResource("styles.css").toExternalForm());
            confirmAlert.getDialogPane().getStyleClass().add("alert-dialog");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                // Wyłącz przycisk podczas usuwania
                deleteGroupButton.setDisable(true);
                deleteGroupButton.setText("Usuwanie...");

                // ========== ZMIENIONA LOGIKA USUWANIA ==========
                // Najpierw usuń lokalnie, potem spróbuj usunąć z serwera
                groups.remove(selectedGroup);
                updateGroupCount();
                animateButton(deleteGroupButton);

                // WYSŁANIE ŻĄDANIA USUNIĘCIA NA SERWER (asynchronicznie w tle)
                groupService.deleteGroupAsync(selectedGroup.getName())
                        .thenAccept(success -> {
                            javafx.application.Platform.runLater(() -> {
                                deleteGroupButton.setDisable(false);
                                deleteGroupButton.setText("Usuń grupę");

                                if (success) {
                                    // Sukces - grupa już usunięta lokalnie
                                    showAlert("Sukces",
                                            "Grupa '" + selectedGroup.getName() + "' została usunięta z serwera.",
                                            Alert.AlertType.INFORMATION);
                                } else {
                                    // Błąd serwera - ale grupa już usunięta lokalnie
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

                                // Grupa już usunięta lokalnie - tylko ostrzeżenie
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

// ========================================================================
// POPRAWKA 2: Dodaj metodę diagnostyczną do sprawdzania API grup
// Dodaj tę metodę do ModernController.java:

    @FXML
    protected void onDiagnoseGroupAPI() {
        System.out.println("=== DIAGNOSTYKA API GRUP ===");

        // Test 1: Sprawdź połączenie
        groupService.checkServerConnection()
                .thenAccept(isConnected -> {
                    System.out.println("Połączenie z serwerem: " + (isConnected ? "OK" : "BŁĄD"));

                    if (isConnected) {
                        // Test 2: Sprawdź listę grup
                        groupService.getAllGroupsAsync()
                                .thenAccept(serverGroups -> {
                                    System.out.println("Liczba grup na serwerze: " + serverGroups.size());

                                    for (Group group : serverGroups) {
                                        System.out.println("- Grupa: " + group.getName() + " (" + group.getSpecialization() + ")");

                                        // Test 3: Sprawdź czy możemy usunąć konkretną grupę
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

    @FXML
    protected void onRefreshClick() {
        loadGroupsFromServer();
    }

    // DODANE - Odświeżanie liczby studentów z serwera
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

        // Utworzenie studenta BEZ GRUPY (null)
        Student newStudent = new Student(firstName, lastName, indexNumber, null);

        // Wyłączenie przycisku podczas wysyłania
        addStudentGlobalButton.setDisable(true);
        addStudentGlobalButton.setText("Dodawanie...");

        // WYSŁANIE NA SERWER
        studentService.addStudentAsync(newStudent)
                .thenAccept(savedStudent -> {
                    // Sukces
                    javafx.application.Platform.runLater(() -> {
                        addStudentGlobalButton.setDisable(false);
                        addStudentGlobalButton.setText("Dodaj studenta");

                        animateButton(addStudentGlobalButton);
                        clearStudentGlobalForm();

                        // DODANE - Odśwież liczbę studentów
                        loadStudentCountFromServer();
                        // ========== DODANE - ODŚWIEŻ RÓWNIEŻ NOWE LISTY ==========
                        loadAllStudentsCount();
                        loadRecentStudents();
                        // =========================================================

                        showAlert("Sukces",
                                "Student " + newStudent.getFullName() + " został dodany na serwer!" +
                                        "\n(Przypisanie do grupy możliwe w oknie szczegółów grupy)",
                                Alert.AlertType.INFORMATION);
                    });
                })
                .exceptionally(throwable -> {
                    // Błąd
                    javafx.application.Platform.runLater(() -> {
                        addStudentGlobalButton.setDisable(false);
                        addStudentGlobalButton.setText("Dodaj studenta");

                        // Sprawdź czy to błąd duplikatu indeksu
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
     * Otwiera okno szczegółów grupy
     */
    private void openGroupDetailWindow(Group group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("group-detail-view.fxml"));
            Parent root = loader.load();

            // Przekaż grupę do kontrolera
            GroupDetailController controller = loader.getController();
            controller.setGroup(group);

            // Utwórz nowe okno
            Stage stage = new Stage();
            stage.setTitle("Grupa: " + group.getName());
            stage.setScene(new Scene(root, 1200, 800));

            // Dodaj stylizację
            stage.getScene().getStylesheets().add(
                    getClass().getResource("styles.css").toExternalForm());

            // Ustaw minimalny rozmiar
            stage.setMinWidth(1000);
            stage.setMinHeight(700);

            // Pokaż okno
            stage.show();

        } catch (Exception e) {
            e.printStackTrace(); // Pokaż błąd w konsoli
            showAlert("Błąd", "Nie udało się otworzyć widoku grupy: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    /**
     * Ładuje grupy z serwera
     */
    private void loadGroupsFromServer() {
        // Pokazuj loading
        refreshButton.setText("Ładowanie...");
        refreshButton.setDisable(true);

        groupService.getAllGroupsAsync()
                .thenAccept(serverGroups -> {
                    // Aktualizuj UI w JavaFX Application Thread
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
                    // Obsługa błędów w JavaFX Application Thread
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

    // DODANE - Ładowanie liczby studentów z serwera
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
     * Sprawdza połączenie z serwerem
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

    private void clearStudentGlobalForm() {
        studentFirstNameField.clear();
        studentLastNameField.clear();
        studentIndexField.clear();
    }

    private void setupStudentIndexValidation() {
        // Sprawdź czy pole istnieje (może być null jeśli FXML się nie załadował prawidłowo)
        if (studentIndexField != null) {
            // Dodaj listener do pola numeru indeksu - tylko cyfry, max 6 znaków
            studentIndexField.textProperty().addListener((observable, oldValue, newValue) -> {
                // Usuń wszystko co nie jest cyfrą
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                // Ogranicz do 6 cyfr
                if (digitsOnly.length() > 6) {
                    digitsOnly = digitsOnly.substring(0, 6);
                }
                // Ustaw nową wartość tylko jeśli się zmieniła
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

        // Stylizacja alertu
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");

        alert.showAndWait();
    }

    // Klasa dla customowej komórki ListView
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