module com.chatappfrontend.frontend {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires static lombok;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires tools.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires okhttp3;

    opens com.chatappfrontend.frontend to javafx.fxml;
    opens com.chatappfrontend.frontend.controller to javafx.fxml;

    exports com.chatappfrontend.frontend;
    exports com.chatappfrontend.frontend.controller;
    exports com.chatappfrontend.frontend.cell;
    opens com.chatappfrontend.frontend.cell to javafx.fxml;
    exports com.chatappfrontend.frontend.model;
    exports com.chatappfrontend.frontend.model.ui;
    opens com.chatappfrontend.frontend.model to com.fasterxml.jackson.databind, javafx.fxml;
}