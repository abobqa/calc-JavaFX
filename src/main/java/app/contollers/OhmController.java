package app.contollers;

import app.App;
import app.db.Db;
import app.db.HistoryRepo;
import app.db.Session;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class OhmController {
    @FXML private TextField vField, iField, rField;
    @FXML private Label resultLabel;

    private HistoryRepo history;
    private Gson gson;

    @FXML
    private void initialize() {
        this.history = new HistoryRepo(Db.ds());
        this.gson = new Gson();
    }

    @FXML
    public void onCompute() {
        computeAndMaybeSave(true);
    }

    private void computeAndMaybeSave(boolean saveToHistory) {
        Double V = parse(vField.getText());
        Double I = parse(iField.getText());
        Double R = parse(rField.getText());

        int known = (V!=null?1:0) + (I!=null?1:0) + (R!=null?1:0);
        if (known != 2) {
            resultLabel.setText("Введите ровно два значения!");
            return;
        }

        if (V == null)      V = I * R;
        else if (I == null) I = V / R;
        else if (R == null) R = V / I;

        final double vOut = V, iOut = I, rOut = R;
        resultLabel.setText(String.format("U=%.2f V, I=%.2f A, R=%.2f Ω", vOut, iOut, rOut));

        if (saveToHistory) {
            var input = gson.toJson(new Object() {
                final Double v = vField.getText().isBlank()?null:parse(vField.getText());
                final Double i = iField.getText().isBlank()?null:parse(iField.getText());
                final Double r = rField.getText().isBlank()?null:parse(rField.getText());
            });
            var result = gson.toJson(new Object() { final double Vout=vOut, Iout=iOut, Rout=rOut; });
            history.saveOhm(Session.userIdOrThrow(),
                    vField.getText().isBlank()?null:parse(vField.getText()),
                    iField.getText().isBlank()?null:parse(iField.getText()),
                    rField.getText().isBlank()?null:parse(rField.getText()),
                    vOut, iOut, rOut);
        }
    }

    // Восстановление из истории (не сохраняем повторно)
    public void restoreFromHistory(String inputJson, String resultJson) {
        try {
            JsonObject in = gson.fromJson(inputJson, JsonObject.class);
            Double V = in.has("v") && !in.get("v").isJsonNull() ? in.get("v").getAsDouble() : null;
            Double I = in.has("i") && !in.get("i").isJsonNull() ? in.get("i").getAsDouble() : null;
            Double R = in.has("r") && !in.get("r").isJsonNull() ? in.get("r").getAsDouble() : null;

            vField.setText(V == null ? "" : strip(V));
            iField.setText(I == null ? "" : strip(I));
            rField.setText(R == null ? "" : strip(R));

            // просто посчитаем и покажем (без записи в историю)
            computeAndMaybeSave(false);
        } catch (Exception e) {
            // на всякий случай: если формат другой — не упасть
            resultLabel.setText("Не удалось восстановить запись из истории");
        }
    }

    private String strip(Double d) {
        if (d == null) return "";
        if (Math.abs(d - d.intValue()) < 1e-9) return Integer.toString(d.intValue());
        return Double.toString(d);
    }

    private Double parse(String s) {
        if (s==null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    @FXML public void onGoDivider() { App.get().switchToDivider(); }
    @FXML public void onGoHistory() { App.get().switchToHistory(); }
    @FXML public void onLogout() { App.get().logout(); }
}
