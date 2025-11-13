module app {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // JDBC и пул
    requires com.zaxxer.hikari;
    requires java.sql;
    requires org.postgresql.jdbc;

    // Прочее
    requires com.google.gson;
    requires jbcrypt;

    // FXML доступ
    opens app to javafx.fxml;

    // Экспорт публичного API
    exports app;
    exports app.contollers;
    opens app.contollers to javafx.fxml;
    exports app.db;
    opens app.db to javafx.fxml;
    exports app.services;
    opens app.services to javafx.fxml;
}
