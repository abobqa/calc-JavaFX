package app;

import app.contollers.*;
import app.db.Db;
import app.db.Session;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {
    private static App instance;
    private Stage stage;

    private Scene loginScene, registerScene, ohmScene, dividerScene, historyScene;
    private LoginController loginController;
    private RegistrationController registrationController;
    private OhmController ohmController;
    private DividerController dividerController;
    private HistoryController historyController;

    public static App get() { return instance; }

    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        this.stage = stage;

        Db.init(
                System.getenv().getOrDefault("PGHOST","localhost"),
                Integer.parseInt(System.getenv().getOrDefault("PGPORT","5432")),
                System.getenv().getOrDefault("PGDATABASE","calculator"),
                System.getenv().getOrDefault("PGUSER","macbook"),
                System.getenv().getOrDefault("PGPASSWORD","default")
        );

        {
            FXMLLoader l1 = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/app/login.fxml")));
            loginScene = new Scene(l1.load(), 400, 350);
            loginController = l1.getController();
        }
        {
            FXMLLoader l2 = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/app/register.fxml")));
            registerScene = new Scene(l2.load(), 420, 370);
            registrationController = l2.getController();
        }
        {
            FXMLLoader l3 = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/app/ohm.fxml")));
            ohmScene = new Scene(l3.load(), 600, 400);
            ohmController = l3.getController();
        }
        {
            FXMLLoader l4 = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/app/divider.fxml")));
            dividerScene = new Scene(l4.load(), 1100, 650);
            dividerController = l4.getController();
        }
        {
            FXMLLoader l5 = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/app/history.fxml")));
            historyScene = new Scene(l5.load(), 1100, 650);
            historyController = l5.getController();
        }

        stage.setTitle("Calculators");
        stage.setScene(loginScene);
        stage.setResizable(true);
        stage.show();
    }

    public void switchToLogin() {
        stage.setScene(loginScene);
        stage.centerOnScreen();
    }

    public void switchToRegister() {
        stage.setScene(registerScene);
        stage.centerOnScreen();
    }

    public void switchToOhm() {
        stage.setScene(ohmScene);
        stage.centerOnScreen();
    }

    public void switchToDivider() {
        stage.setScene(dividerScene);
        stage.centerOnScreen();
    }

    public void switchToHistory() {
        stage.setScene(historyScene);
        stage.centerOnScreen();
    }


    public void openOhmFromHistory(String inputJson, String resultJson) {
        switchToOhm();
        if (ohmController != null) {
            ohmController.restoreFromHistory(inputJson, resultJson);
        }
    }
    public void openDividerFromHistory(String inputJson, String resultJson) {
        switchToDivider();
        if (dividerController != null) {
            dividerController.restoreFromHistory(inputJson, resultJson);
        }
    }

    public void logout() {
        Session.clear();
        switchToLogin();
    }
}
