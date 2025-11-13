package app.contollers;

import app.App;
import app.services.AuthService;
import app.db.Db;
import app.db.UserRepo;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final AuthService auth = new AuthService(new UserRepo(Db.ds()));

    @FXML
    public void onLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText().trim();
        if (u.isEmpty() || p.isEmpty()){
            statusLabel.setText("Введите логин и пароль");
            return;
        }
        if (auth.login(u, p)) {
            statusLabel.setText("");
            App.get().switchToOhm();
        } else {
            statusLabel.setText("Неверный логин или пароль");
        }
    }

    @FXML
    public void onGoRegister() {
        App.get().switchToRegister();
    }
}
