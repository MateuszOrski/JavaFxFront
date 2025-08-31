module com.example.javafxfront {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.base;


    opens com.example.javafxfront to javafx.fxml;
    exports com.example.javafxfront;
}