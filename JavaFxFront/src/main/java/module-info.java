module com.example.javafxfront {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.base;

    // Jackson dependencies
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.annotation;

    opens com.example.javafxfront to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.example.javafxfront;
}