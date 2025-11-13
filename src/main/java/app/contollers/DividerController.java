package app.contollers;

import app.*;
import app.db.Db;
import app.db.HistoryRepo;
import app.db.Session;
import app.CircuitView;
import com.google.gson.Gson;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;
import java.util.stream.Collectors;

public class DividerController {
    @FXML private TextField vinField, voutTargetField;
    @FXML private TextField tolField, rminField, rmaxField;
    @FXML private ChoiceBox<String> seriesChoice;
    @FXML private ChoiceBox<Integer> maxPartsChoice;
    @FXML private TableView<Solution> table;
    @FXML private TableColumn<Solution, Number> colVout, colErr, colParts, colPower, colR1, colR2;
    @FXML private TableColumn<Solution, String> colR1List, colR2List;
    @FXML private Label statusLabel;
    @FXML private CircuitView circuit;

    private HistoryRepo history;
    private Gson gson;

    /** Результат делителя: только последовательные стек(и) над и под Vout */
    public record Solution(double vout, double errorPct, int parts, double power,
                           double r1eq, double r2eq,
                           List<Double> r1Parts, List<Double> r2Parts) { }

    @FXML
    private void initialize() {
        history = new HistoryRepo(Db.ds());
        gson = new Gson();

        seriesChoice.setItems(FXCollections.observableArrayList("E6","E12","E24","E96"));
        seriesChoice.getSelectionModel().select("E24");

        maxPartsChoice.setItems(FXCollections.observableArrayList(2,3,4));
        maxPartsChoice.getSelectionModel().select(Integer.valueOf(4));

        colVout.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().vout()));
        colErr.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().errorPct()));
        colParts.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().parts()));
        colPower.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().power()));
        colR1.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().r1eq()));
        colR2.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().r2eq()));
        colR1List.setCellValueFactory(d -> new SimpleStringProperty(formatList(d.getValue().r1Parts())));
        colR2List.setCellValueFactory(d -> new SimpleStringProperty(formatList(d.getValue().r2Parts())));

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null){
                Double vin = parse(vinField.getText());
                if (vin == null) vin = 0.0;
                circuit.render(n.r1Parts(), n.r2Parts(), vin, n.vout());
            }
        });

        tolField.setText("1");
        rminField.setText("10");
        rmaxField.setText("1000000");
    }

    @FXML
    public void onSearch(){
        Double Vin = parse(vinField.getText());
        Double Vt  = parse(voutTargetField.getText());
        Double tolPct = parse(tolField.getText());
        Double rmin = parse(rminField.getText());
        Double rmax = parse(rmaxField.getText());
        Integer maxParts = maxPartsChoice.getValue();

        if (Vin == null || Vt == null || tolPct == null || rmin == null || rmax == null || maxParts == null ||
                Vin <= 0 || Vt <= 0 || Vt >= Vin || tolPct <= 0 || rmin <= 0 || rmax <= rmin ||
                maxParts < 2 || maxParts > 4) {
            statusLabel.setText("Проверь ввод: Vin>0, 0<Vout<Vin, допуск>0, Rmin>0<Rmax, детали 2..4");
            table.setItems(FXCollections.observableArrayList());
            return;
        }

        var libAll = switch (seriesChoice.getValue()) {
            case "E6"  -> e6();
            case "E12" -> e12();
            case "E24" -> e24();
            case "E96" -> e96();
            default    -> e24();
        };

        var lib = libAll.stream().filter(r -> r>=rmin && r<=rmax).toList();
        if (lib.isEmpty()){
            statusLabel.setText("Нет номиналов в диапазоне");
            table.setItems(FXCollections.observableArrayList());
            return;
        }

        // Готовим стек-комбинации: в каждом плече 1 или 2 резистора (чистая серия)
        var topStacks = stackCombosUpTo2(lib);
        var botStacks = stackCombosUpTo2(lib);

        double tol = tolPct / 100.0;
        ArrayList<Solution> out = new ArrayList<>(20000);

        for (StackCombo a : topStacks){
            for (StackCombo b : botStacks){
                int parts = a.parts().size() + b.parts().size();
                if (parts < 2 || parts > maxParts) continue;

                double R1 = a.req();
                double R2 = b.req();

                double Vout = Vin * R2 / (R1 + R2);
                double errAbs = Math.abs(Vout - Vt);
                double errPct = errAbs / Vt * 100.0;
                if (errAbs > Vt * tol) continue;

                double I = Vin / (R1 + R2);
                double Ptotal = I * Vin;

                out.add(new Solution(Vout, errPct, parts, Ptotal, R1, R2, a.parts(), b.parts()));
            }
        }

        out.sort(Comparator
                .comparingDouble(Solution::errorPct)
                .thenComparingInt(Solution::parts)
                .thenComparingDouble(Solution::power));

        var top = out.stream().limit(200).toList();
        table.setItems(FXCollections.observableArrayList(top));
        statusLabel.setText("Найдено: " + out.size());

        if (!top.isEmpty()){
            var s0 = top.get(0);
            circuit.render(s0.r1Parts(), s0.r2Parts(), Vin, s0.vout());
        }

        // ---- История: исправлено, чтобы не было null ----
        Map<String, Object> inputData = new LinkedHashMap<>();
        inputData.put("vin", Vin);
        inputData.put("vrequired", Vt);
        inputData.put("tolerancePct", tolPct);
        inputData.put("series", seriesChoice.getValue());
        inputData.put("rMin", rmin);
        inputData.put("rMax", rmax);
        inputData.put("maxPartsUsed", maxParts);

        var input = gson.toJson(inputData);
        var result = gson.toJson(top);
        history.save(Session.userIdOrThrow(), "DIVIDER", input, result);
    }

    // ===== только серия: стек из 1 или 2 резисторов =====
    private record StackCombo(double req, List<Double> parts) {}

    private List<StackCombo> stackCombosUpTo2(List<Double> lib){
        ArrayList<StackCombo> list = new ArrayList<>();
        // одиночный
        for (double r : lib) list.add(new StackCombo(r, List.of(r)));
        // два последовательно
        for (int i=0;i<lib.size();i++){
            for (int j=i;j<lib.size();j++){
                double r1 = lib.get(i), r2 = lib.get(j);
                list.add(new StackCombo(r1 + r2, List.of(r1, r2)));
            }
        }
        return list;
    }

    // ===== утилиты =====
    private String formatList(List<Double> parts){
        return parts.stream().map(this::fmt).collect(Collectors.joining(" + "));
    }
    private String fmt(double r){
        if (r >= 1_000_000) return String.format("%.2fMΩ", r/1_000_000.0);
        if (r >= 1_000) return String.format("%.2fkΩ", r/1_000.0);
        return String.format("%.2fΩ", r);
    }
    private Double parse(String s){
        if (s==null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }

    // Ряды E-серий
    private List<Double> e6(){
        double[] base = {10, 15, 22, 33, 47, 68};
        return buildSeries(base);
    }
    private List<Double> e12(){
        double[] base = {10,12,15,18,22,27,33,39,47,56,68,82};
        return buildSeries(base);
    }
    private List<Double> e24(){
        double[] base = {10,11,12,13,15,16,18,20,22,24,27,30,33,36,39,43,47,51,56,62,68,75,82,91};
        return buildSeries(base);
    }
    private List<Double> e96(){
        double[] base = {
                10.0,10.2,10.5,10.7,11.0,11.3,11.5,11.8,12.1,12.4,12.7,13.0,
                13.3,13.7,14.0,14.3,14.7,15.0,15.4,15.8,16.2,16.5,16.9,17.4,
                17.8,18.2,18.7,19.1,19.6,20.0,20.5,21.0
        };
        return buildSeries(base);
    }
    private List<Double> buildSeries(double[] base){
        ArrayList<Double> out = new ArrayList<>();
        for (int decade=-1; decade<=6; decade++){
            double mul = Math.pow(10, decade);
            for (double b : base) out.add(Math.round(b * mul * 1000.0)/1000.0);
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    // --- навигация из шапки ---
    @FXML public void onHistory() { App.get().switchToHistory(); }
    @FXML public void onBack()    { App.get().switchToOhm(); }
    @FXML public void onLogout()  { App.get().logout(); }

    // восстановление из истории (возврат к предыдущему поиску)
    public void restoreFromHistory(String inputJson, String resultJson) {
        try {
            var in = gson.fromJson(inputJson, Map.class);
            double vin = ((Number)in.get("vin")).doubleValue();
            double vreq = ((Number)in.get("vrequired")).doubleValue();
            double tol = ((Number)in.get("tolerancePct")).doubleValue();
            String series = (String) in.get("series");
            double rMin = ((Number)in.get("rMin")).doubleValue();
            double rMax = ((Number)in.get("rMax")).doubleValue();
            int maxParts = ((Number)in.get("maxPartsUsed")).intValue();

            vinField.setText(Double.toString(vin));
            voutTargetField.setText(Double.toString(vreq));
            tolField.setText(Double.toString(tol));
            rminField.setText(Double.toString(rMin));
            rmaxField.setText(Double.toString(rMax));
            seriesChoice.getSelectionModel().select(series);
            maxPartsChoice.getSelectionModel().select(Integer.valueOf(maxParts));

            // пересчитать варианты
            onSearch();

            // выделить первую запись из сохранённых результатов
            if (resultJson != null && !resultJson.isBlank() && !table.getItems().isEmpty()) {
                var s = table.getItems().get(0);
                table.getSelectionModel().select(s);
                table.scrollTo(s);
                circuit.render(s.r1Parts(), s.r2Parts(), vin, s.vout());
            }
        } catch (Exception e) {
            statusLabel.setText("Не удалось восстановить запись из истории");
        }
    }

}
