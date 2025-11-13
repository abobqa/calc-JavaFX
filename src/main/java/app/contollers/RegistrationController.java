package app.contollers;

import app.*;
import app.db.Db;
import app.db.Role;
import app.db.UserRepo;
import app.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class RegistrationController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordRepeatField;
    @FXML private Label statusLabel;

    private final AuthService auth = new AuthService(new UserRepo(Db.ds()));

    @FXML
    public void onRegister() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText().trim();
        String pr = passwordRepeatField.getText().trim();

        if (u.isEmpty() || p.isEmpty() || pr.isEmpty()) {
            setStatus("Заполните все поля", Color.web("#e74c3c"));
            return;
        }

        if (!p.equals(pr)) {
            setStatus("Пароли не совпадают", Color.web("#e74c3c"));
            return;
        }

        boolean ok = auth.register(u, p, Role.USER);
        if (ok) {
            setStatus("Пользователь создан. Войдите.", Color.web("#27ae60"));
        } else {
            setStatus("Имя занято.", Color.web("#e74c3c"));
        }
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setTextFill(color);
    }

    @FXML
    public void onGoLogin() {
        App.get().switchToLogin();
    }
}
