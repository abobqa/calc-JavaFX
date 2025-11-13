package app.contollers;

import app.App;
import app.db.Db;
import app.db.HistoryRepo;
import app.db.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.time.ZoneId;

public class HistoryController {
    @FXML private TableView<HistoryRepo.HistoryRow> table;
    @FXML private TableColumn<HistoryRepo.HistoryRow, Number> colId;
    @FXML private TableColumn<HistoryRepo.HistoryRow, String> colType;
    @FXML private TableColumn<HistoryRepo.HistoryRow, String> colInput;
    @FXML private TableColumn<HistoryRepo.HistoryRow, String> colResult;
    @FXML private TableColumn<HistoryRepo.HistoryRow, String> colCreated;
    @FXML private ComboBox<String> typeFilter;
    @FXML private DatePicker fromDate, toDate;

    private HistoryRepo repo;

    @FXML
    private void initialize(){
        repo = new HistoryRepo(Db.ds());

        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleLongProperty(d.getValue().id()));
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().calcType()));
        colInput.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().inputJson()));
        colResult.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().resultJson()));
        colCreated.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().createdAt().toString()));

        typeFilter.setItems(FXCollections.observableArrayList("ALL","OHM","DIVIDER"));
        typeFilter.getSelectionModel().select("ALL");

        // подгружаем после появления сцены и логина
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && Session.isLoggedIn()) {
                reload();
            }
        });

        // Двойной клик по строке — открыть решение
        table.setRowFactory(tv -> {
            TableRow<HistoryRepo.HistoryRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    openItem(row.getItem());
                }
            });
            return row;
        });
    }

    private void openItem(HistoryRepo.HistoryRow r) {
        if (r == null) return;
        switch (r.calcType()) {
            case "OHM" -> App.get().openOhmFromHistory(r.inputJson(), r.resultJson());
            case "DIVIDER" -> App.get().openDividerFromHistory(r.inputJson(), r.resultJson());
            default -> {}
        }
    }

    @FXML
    public void reload(){
        if (!Session.isLoggedIn()) return;
        var role = Session.role();
        long uid = Session.userIdOrThrow();

        String calcType = typeFilter.getValue();
        var from = fromDate.getValue() == null ? null
                : fromDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        var to   = toDate.getValue() == null ? null
                : toDate.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        var list = repo.listFiltered(role, uid, calcType, from, to);
        table.setItems(FXCollections.observableArrayList(list));
    }

    @FXML public void onBack(){ App.get().switchToOhm(); }
    @FXML public void onLogout(){ App.get().logout(); }
}
