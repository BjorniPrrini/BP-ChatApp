module com.chatapplauncher.launcher {
    requires javafx.controls;
    requires javafx.fxml;

    requires static lombok;

    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens com.chatapplauncher.launcher to javafx.fxml;
    opens com.chatapplauncher.launcher.controllers to javafx.fxml;

    exports com.chatapplauncher.launcher;
}