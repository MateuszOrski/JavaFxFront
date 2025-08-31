package com.example.javafxfront;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import java.util.Optional;

public class ModernController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField groupNameField;
    @FXML private TextField specializationField;
    @FXML private Button addGroupButton;
    @FXML private ListView<Group> groupsListView;
    @FXML private Button enterGroupButton;
    @FXML private Button deleteGroupButton;
    @FXML private VBox addGroupCard;
    @FXML private VBox groupsCard;
    @FXML private Label groupCountLabel;

    private ObservableList<Group> groups;

    @FXML
    protected void initialize() {
        // Inicjalizacja listy grup
        groups = FXCollections.observableArrayList();
        groupsListView.setItems(groups);

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

        // Aplikacja startuje bez grup - użytkownik dodaje je ręcznie
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
    }

    @FXML
    protected void onEnterGroupClick() {
        Group selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            animateButton(enterGroupButton);

            // Tu można dodać logikę przejścia do widoku grupy
            showAlert("Wejście do grupy",
                    "Wchodzisz do grupy: " + selectedGroup.getName() +
                            "\nSpecjalizacja: " + selectedGroup.getSpecialization(),
                    Alert.AlertType.INFORMATION);
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
            }
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
}