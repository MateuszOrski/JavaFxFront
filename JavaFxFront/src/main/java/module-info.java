module com.example.javafxfront {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.javafxfront to javafx.fxml;
    exports com.example.javafxfront;
}