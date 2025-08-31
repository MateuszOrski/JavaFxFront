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

public class ModernController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField groupNameField;
    @FXML private TextField specializationField;
    @FXML private Button addGroupButton;
    @FXML private TextField studentFirstNameField;
    @FXML private TextField studentLastNameField;
    @FXML private TextField studentIndexField;
    @FXML private ComboBox<String> groupSelectionComboBox;
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

    private ObservableList<Group> groups;
    private GroupService groupService;

    @FXML
    protected void initialize() {
        // Inicjalizacja listy grup i serwisu
        groups = FXCollections.observableArrayList();
        groupsListView.setItems(groups);
        groupService = new GroupService();

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
        updateGroupComboBox();
    }

    @FXML
    protected void onAddGroupClick() {
        String groupName = groupNameField.getText().trim();
        String specialization = specializationField.getText().trim();

        if (groupName.isEmpty() || specialization.isEmpty()) {
            showAlert("Błąd", "Wszystkie pola muszą być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        // Sprawdzenie czy grupa o takiej nazwie już istnieje
        boolean groupExists = groups.stream().anyMatch(g -> g.getName().equalsIgnoreCase(groupName));
        if (groupExists) {
            showAlert("Błąd", "Grupa o takiej nazwie już istnieje!", Alert.AlertType.WARNING);
            return;
        }

        // Dodanie grupy
        Group newGroup = new Group(groupName, specialization);
        groups.add(newGroup);

        // Animacja dodania
        animateButton(addGroupButton);

        // Czyszczenie pól
        groupNameField.clear();
        specializationField.clear();

        updateGroupCount();

        showAlert("Sukces", "Grupa '" + groupName + "' została dodana pomyślnie!", Alert.AlertType.INFORMATION);

        // Aktualizuj ComboBox z grupami
        updateGroupComboBox();

        // Opcjonalnie: wyślij też na serwer
        // sendGroupToServer(newGroup);
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
                groups.remove(selectedGroup);
                animateButton(deleteGroupButton);
                updateGroupCount();

                showAlert("Usunięto", "Grupa '" + selectedGroup.getName() + "' została usunięta.",
                        Alert.AlertType.INFORMATION);

                // Aktualizuj ComboBox z grupami
                updateGroupComboBox();
            }
        }
    }

    @FXML
    protected void onRefreshClick() {
        loadGroupsFromServer();
    }

    @FXML
    protected void onAddStudentGlobalClick() {
        // TODO: Implementacja dodawania studenta do globalnej bazy
        String firstName = studentFirstNameField.getText().trim();
        String lastName = studentLastNameField.getText().trim();
        String indexNumber = studentIndexField.getText().trim();
        String selectedGroup = groupSelectionComboBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || indexNumber.isEmpty()) {
            showAlert("Błąd", "Imię, nazwisko i numer indeksu muszą być wypełnione!", Alert.AlertType.WARNING);
            return;
        }

        if (!indexNumber.matches("\\d{6}")) {
            showAlert("Błąd", "Numer indeksu musi składać się z dokładnie 6 cyfr!", Alert.AlertType.WARNING);
            return;
        }

        // Placeholder - na razie tylko pokaz co zostało wprowadzone
        String groupInfo = selectedGroup != null ? " do grupy " + selectedGroup : " (bez przypisania do grupy)";
        showAlert("Informacja",
                "Student zostanie dodany do bazy:" +
                        "\nImię: " + firstName +
                        "\nNazwisko: " + lastName +
                        "\nNr indeksu: " + indexNumber +
                        groupInfo +
                        "\n\n(Logika będzie zaimplementowana później)",
                Alert.AlertType.INFORMATION);

        // Wyczyść formularz
        clearStudentGlobalForm();
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

    /**
     * Wysyła nową grupę na serwer (opcjonalne)
     */
    private void sendGroupToServer(Group group) {
        groupService.addGroupAsync(group)
                .thenAccept(savedGroup -> {
                    javafx.application.Platform.runLater(() -> {
                        // Możesz zaktualizować grupę z ID z serwera
                        System.out.println("Grupa zapisana na serwerze: " + savedGroup);
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        showAlert("Ostrzeżenie",
                                "Grupa dodana lokalnie, ale nie udało się wysłać na serwer:\n" + throwable.getMessage(),
                                Alert.AlertType.WARNING);
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
                            serverStatusLabel.setText("🟢 Połączony z serwerem");
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
        groupSelectionComboBox.setValue(null);
    }

    private void setupStudentIndexValidation() {
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

    private void updateGroupComboBox() {
        // Aktualizuj listę grup w ComboBox
        ObservableList<String> groupNames = FXCollections.observableArrayList();
        for (Group group : groups) {
            groupNames.add(group.getName());
        }
        groupSelectionComboBox.setItems(groupNames);
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
}