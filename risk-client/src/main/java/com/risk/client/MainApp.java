package com.risk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class MainApp extends Application {

    // HTTP
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String BASE = "http://localhost:8080/api";

    // Корневая навигация и статус
    private TabPane tabs;
    private Label status;

    // Финансовые отчёты
    private TableView<ApiClient.FsDto> tableFs;
    private Button btnFsRefresh;
    private Button btnFsImport;

    // Позиции
    private TableView<ApiClient.PositionDto> tablePos;
    private Button btnPosRefresh;
    private Button btnPosAdd;
    private Button btnPosVar;

    // Валидация
    private TableView<ApiClient.ValidationErrorDto> tableErr;
    private Button btnValidate;

    //var настройки
    private Button btnLoadConfig;
    private Button btnSaveConfig;

    // Загрузка данных
    private TableView<ApiClient.HistoricalDataDto> tableData;
    private ComboBox<ApiClient.DatasetDto> cbDatasets;
    private Label              lblCurrentDataset;
    private Button             btnRefreshDatasets, btnUploadCsv;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        // 1) Инициализируем TabPane и статус
        tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        status = new Label("Роль: GUEST");

        // 2) Настраиваем каждую вкладку
        Tab dataTab = createDataTab(stage);
        Tab fsTab   = createFsTab();
        Tab posTab  = createPosTab();
        Tab valTab  = createValidationTab();
        Tab cfgTab = createConfigTab();


        tabs.getTabs().addAll(
                createDataTab(stage),
                createFsTab(),
                createPosTab(),
                createValidationTab(),
                createConfigTab()
        );

        // 3) Меню Сессии
        MenuItem login    = new MenuItem("Войти");
        MenuItem register = new MenuItem("Регистрация");
        MenuItem logout   = new MenuItem("Выйти");
        login.setOnAction(e -> { if (LoginDialog.showLogin()) updateUi(); });
        register.setOnAction(e -> {
            LoginDialog.showRegister();
            updateUi();
        });

        logout.setOnAction(e -> {
            ApiClient.Session.user = null;
            ApiClient.Session.pass = null;
            ApiClient.Session.role = "GUEST";
            updateUi();
        });
        logout.setOnAction(e -> {
            ApiClient.Session.user = null;
            ApiClient.Session.pass = null;
            ApiClient.Session.role = "GUEST";
            updateUi();
        });

        // 4) Сценарий: собираем сцену
        BorderPane root = new BorderPane();
        root.setTop(new MenuBar(new Menu("Сессия", null, login, register, logout)));
        root.setCenter(tabs);
        root.setBottom(status);

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
        updateUi();
    }



    private Tab createConfigTab() {
        // ComboBox для метода
        ComboBox<String> cbMethod = new ComboBox<>();
        cbMethod.getItems().addAll("HISTORICAL","PARAMETRIC","MONTE_CARLO");

        // Spinner для уровня доверия
        Spinner<Double> spConfidence = new Spinner<>(0.80, 0.99, 0.95, 0.01);
        spConfidence.setEditable(true);

        // Spinner для горизонта
        Spinner<Integer> spHorizon = new Spinner<>(1, 30, 1, 1);
        spHorizon.setEditable(true);

        btnLoadConfig = new Button("Загрузить");
        btnSaveConfig = new Button("Сохранить");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Метод:"), cbMethod);
        grid.addRow(1, new Label("Уровень доверия:"), spConfidence);
        grid.addRow(2, new Label("Горизонт (дн.):"), spHorizon);
        grid.addRow(3, btnLoadConfig, btnSaveConfig);

        // Загрузка
        btnLoadConfig.setOnAction(e -> {
            try {
                var cfg = ApiClient.getConfig();
                cbMethod.setValue(cfg.method());
                spConfidence.getValueFactory().setValue(cfg.confidenceLevel());
                spHorizon.getValueFactory().setValue(cfg.horizonDays());
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        // Сохранение
        btnSaveConfig.setOnAction(e -> {
            try {
                var cfg = new ApiClient.CalculationConfigDto(
                        cbMethod.getValue(),
                        spConfidence.getValue(),
                        spHorizon.getValue()
                );
                boolean ok = ApiClient.updateConfig(cfg);
                new Alert(
                        ok ? AlertType.INFORMATION : AlertType.ERROR,
                        ok ? "Сохранено" : "Ошибка при сохранении"
                ).showAndWait();
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        // Обёртка
        VBox pane = new VBox(10, grid);
        pane.setPadding(new Insets(10));
        return new Tab("Настройки", pane);
    }


    // Создаёт вкладку «Данные»
    private Tab createDataTab(Stage stage) {
        // 1. Таблица исторических данных
        tableData = new TableView<>();
        // колонки для tableData
        TableColumn<ApiClient.HistoricalDataDto,String> colSym = new TableColumn<>("Symbol");
        colSym.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().symbol()));
        TableColumn<ApiClient.HistoricalDataDto,String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().date()));
        TableColumn<ApiClient.HistoricalDataDto,BigDecimal> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().price()));
        TableColumn<ApiClient.HistoricalDataDto,BigDecimal> colRet = new TableColumn<>("Ret");
        colRet.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().ret()));
        tableData.getColumns().addAll(colSym, colDate, colPrice, colRet);
        tableData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 2. Кнопки и ComboBox
        Button btnVarDataset = new Button("Посчитать VaR");
        btnRefreshDatasets = new Button("Обновить спис-к");
        btnUploadCsv       = new Button("Загрузить CSV");
        cbDatasets         = new ComboBox<>();
        lblCurrentDataset  = new Label("Работаем с: —");

        // — Устанавливаем конвертер, чтобы в списке видеть только name()
        cbDatasets.setConverter(new StringConverter<>() {
            @Override
            public String toString(ApiClient.DatasetDto ds) {
                return ds == null ? "—" : ds.name();
            }
            @Override
            public ApiClient.DatasetDto fromString(String s) {
                return null; // не нужно
            }
        });

        // 3. Раскладка в VBox
        HBox controls = new HBox(10, btnRefreshDatasets, btnUploadCsv, cbDatasets, btnVarDataset);
        controls.setPadding(new Insets(10));
        VBox pane = new VBox(10, controls, lblCurrentDataset, tableData);
        pane.setPadding(new Insets(10));

        // 4. Действия кнопок
        btnVarDataset.setOnAction(e -> {
            var ds = cbDatasets.getValue();
            if (ds == null) {
                new Alert(Alert.AlertType.WARNING,
                        "Сначала выберите датасет").showAndWait();
                return;
            }
            try {
                // получаем сохранённые настройки
                var cfg = ApiClient.getConfig();
                BigDecimal varVal = ApiClient.getVarForDataset(
                        ds.id(), cfg.confidenceLevel(), cfg.horizonDays()
                );
                String title = String.format(
                        "VaR (%.0f%%, %dd):",
                        cfg.confidenceLevel() * 100, cfg.horizonDays()
                );
                new Alert(Alert.AlertType.INFORMATION,
                        title + " " + varVal).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Ошибка при расчёте VaR: " + ex.getMessage()
                ).showAndWait();
            }
        });
        // 4.1 Обновить список доступных датасетов
        btnRefreshDatasets.setOnAction(e -> {
            try {
                List<ApiClient.DatasetDto> list = ApiClient.listDatasets();
                // Сбросим текущий выбор, чтобы listener не дернулся
                cbDatasets.getSelectionModel().clearSelection();
                cbDatasets.getItems().setAll(list);
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });


        // 4.2 Загрузить новый CSV
        btnUploadCsv.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV", "*.csv")
            );
            File f = chooser.showOpenDialog(stage);
            if (f != null) {
                try {
                    ApiClient.DatasetDto ds = ApiClient.uploadDataset(f);
                    cbDatasets.getItems().add(0, ds);
                    cbDatasets.setValue(ds);  // выберем его сразу
                } catch (Exception ex) {
                    new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        });

        // 5. Listener на выбор датасета: подтягиваем его данные в tableData
        ChangeListener<ApiClient.DatasetDto> dsListener = (obs, oldDs, newDs) -> {
            if (newDs == null) {
                lblCurrentDataset.setText("Работаем с: —");
                tableData.getItems().clear();
            } else {
                lblCurrentDataset.setText("Работаем с: " + newDs.name());
                try {
                    List<ApiClient.HistoricalDataDto> rows = ApiClient.listData(newDs.id());
                    tableData.getItems().setAll(rows);
                } catch (Exception ex) {
                    new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        };
        cbDatasets.getSelectionModel()
                .selectedItemProperty()
                .addListener(dsListener);

        return new Tab("Данные", pane);
    }

    // Создаёт вкладку «Финансовые отчёты»
    private Tab createFsTab() {
        btnFsRefresh = new Button("Обновить FS");
        btnFsImport  = new Button("Импорт FS");
        tableFs      = new TableView<>();

        TableColumn<ApiClient.FsDto,Long> colId   = new TableColumn<>("ID");
        TableColumn<ApiClient.FsDto,String> colPer = new TableColumn<>("Период");
        TableColumn<ApiClient.FsDto,String> colCur = new TableColumn<>("Валюта");
        TableColumn<ApiClient.FsDto,String> colSrc = new TableColumn<>("Источник");
        colId.setCellValueFactory(c -> new ReadOnlyLongWrapper(c.getValue().id()).asObject());
        colPer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().period()));
        colCur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().currency()));
        colSrc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().source()));
        tableFs.getColumns().addAll(colId, colPer, colCur, colSrc);

        btnFsRefresh.setOnAction(e -> {
            try {
                ApiClient.FsDto[] arr = new ObjectMapper()
                        .readValue(ApiClient.listFSJson(), ApiClient.FsDto[].class);
                tableFs.getItems().setAll(arr);
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
        btnFsImport.setOnAction(e -> {
            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Импорт финансового отчёта");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            DatePicker period = new DatePicker();
            TextField cur = new TextField(), src = new TextField();
            GridPane g = new GridPane();
            g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
            g.addRow(0,new Label("Период"), period);
            g.addRow(1,new Label("Валюта"), cur);
            g.addRow(2,new Label("Источник"), src);
            dlg.getDialogPane().setContent(g);
            dlg.setResultConverter(b -> {
                if (b==ButtonType.OK) {
                    ApiClient.createFS(
                            period.getValue().toString(),
                            cur.getText(), src.getText());
                    btnFsRefresh.fire();
                }
                return null;
            });
            dlg.showAndWait();
        });

        HBox box = new HBox(10, btnFsRefresh, btnFsImport);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        BorderPane pane = new BorderPane(tableFs, null, null, box, null);
        return new Tab("Финансовые отчёты", pane);
    }

    // Создаёт вкладку «Позиции»
    private Tab createPosTab() {
        btnPosRefresh = new Button("Обновить Позиции");
        btnPosAdd     = new Button("Добавить Позицию");
        btnPosVar     = new Button("VaR 95%");
        tablePos      = new TableView<>();

        TableColumn<ApiClient.PositionDto,Long> colId   = new TableColumn<>("ID");
        TableColumn<ApiClient.PositionDto,String> colSym = new TableColumn<>("Symbol");
        TableColumn<ApiClient.PositionDto,Integer> colQty= new TableColumn<>("Qty");
        TableColumn<ApiClient.PositionDto,BigDecimal> colPr = new TableColumn<>("Price");
        TableColumn<ApiClient.PositionDto,String> colDt  = new TableColumn<>("Date");
        colId.setCellValueFactory(c -> new ReadOnlyLongWrapper(c.getValue().id()).asObject());
        colSym.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol()));
        colQty.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantity()));
        colPr.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().lastPrice()));
        colDt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().valDate()));
        tablePos.getColumns().addAll(colId, colSym, colQty, colPr, colDt);

        btnPosRefresh.setOnAction(e -> {
            try {
                tablePos.getItems().setAll(ApiClient.listPositions());
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
        btnPosAdd.setOnAction(e -> addPosition());
        btnPosVar.setOnAction(e -> {
            var sel = tablePos.getSelectionModel().getSelectedItem();
            ApiClient.DatasetDto ds = cbDatasets.getValue();
            if (sel==null || ds==null) return;

            try {
                var cfg = ApiClient.getConfig();
                BigDecimal v = ApiClient.getVarByDataset(ds.id());
                String title = String.format(
                        "VaR (%.0f%%, %dd)",
                        cfg.confidenceLevel()*100, cfg.horizonDays()
                );
                new Alert(AlertType.INFORMATION,
                        title + ": " + v + " USD").showAndWait();
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage())
                        .showAndWait();
            }
        });


        HBox box = new HBox(10, btnPosRefresh, btnPosAdd, btnPosVar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        BorderPane pane = new BorderPane(tablePos, null, null, box, null);
        return new Tab("Позиции", pane);
    }

    // Создаёт вкладку «Валидация»
    private Tab createValidationTab() {
        btnValidate = new Button("Проверить данные");
        tableErr    = new TableView<>();

        // --- колонки ---
        TableColumn<ApiClient.ValidationErrorDto,String> colS =
                new TableColumn<>("Symbol");
        colS.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().symbol()));

// Теперь храним дату как String
        TableColumn<ApiClient.ValidationErrorDto,String> colD =
                new TableColumn<>("Date");
        colD.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().date())
        );


        // остальные колонки
        TableColumn<ApiClient.ValidationErrorDto,String> colF =
                new TableColumn<>("Field");
        colF.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().field()));

        TableColumn<ApiClient.ValidationErrorDto,String> colM =
                new TableColumn<>("Message");
        colM.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().message()));

        tableErr.getColumns().addAll(colS, colD, colF, colM);

        // --- загрузка данных ---
        btnValidate.setOnAction(e -> {
            ApiClient.DatasetDto ds = cbDatasets.getValue();
            if (ds == null) {
                new Alert(AlertType.WARNING,
                        "Сначала выберите датасет").showAndWait();
                return;
            }
            try {
                var errs = ApiClient.validateData(ds.id());
                tableErr.getItems().setAll(errs);
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage())
                        .showAndWait();
            }
        });



        VBox pane = new VBox(10, btnValidate, tableErr);
        pane.setPadding(new Insets(10));
        return new Tab("Валидация", pane);
    }


    /** Метод диалога добавления позиции */
    private void addPosition() {
        Dialog<ApiClient.PositionDto> dlg = new Dialog<>();
        dlg.setTitle("Добавить позицию");
        dlg.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        TextField symField   = new TextField();
        TextField qtyField   = new TextField();
        TextField priceField = new TextField();
        DatePicker datePicker= new DatePicker();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Symbol:"), symField);
        grid.addRow(1, new Label("Quantity:"), qtyField);
        grid.addRow(2, new Label("Price:"), priceField);
        grid.addRow(3, new Label("Date:"), datePicker);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(b -> {
            if (b == ButtonType.OK) {
                try {
                    return ApiClient.createPosition(
                            symField.getText().trim(),
                            Integer.parseInt(qtyField.getText().trim()),
                            new BigDecimal(priceField.getText().trim()),
                            datePicker.getValue().toString()
                    );
                } catch (Exception ex) {
                    new Alert(AlertType.ERROR, ex.getMessage())
                            .showAndWait();
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(p -> {
            tablePos.getItems().add(p);
            new Alert(AlertType.INFORMATION,
                    "Позиция создана, ID=" + p.id(), ButtonType.OK)
                    .showAndWait();
        });
    }

    /** Обновляем состояние UI по роли */
    private void updateUi() {
        boolean logged  = ApiClient.Session.user != null;
        boolean canEdit = "ADMIN".equals(ApiClient.Session.role)
                || "ANALYST".equals(ApiClient.Session.role);

        // FS
        btnFsRefresh.setDisable(!logged);
        btnFsImport.setDisable(!canEdit);
        // Позиции
        btnPosRefresh.setDisable(!logged);
        btnPosAdd.setDisable(!canEdit);
        btnPosVar.setDisable(!canEdit);
        // Данные и валидация
        btnUploadCsv.setDisable(!logged);
        btnValidate.setDisable(!logged);
        // Статус
        status.setText("Роль: " + ApiClient.Session.role);
    }

    /** Упаковываем CSV в multipart-запрос */
    private HttpRequest buildMultipartRequest(File file) throws IOException {
        String boundary = "Boundary-" + UUID.randomUUID();
        String sep      = "\r\n";
        byte[] bytes    = Files.readAllBytes(file.toPath());

        var parts = List.of(
                ("--"+boundary+sep+
                        "Content-Disposition: form-data; name=\"file\"; filename=\""+
                        file.getName()+"\""+sep+
                        "Content-Type: text/csv"+sep+sep)
                        .getBytes(StandardCharsets.UTF_8),
                bytes,
                (sep+"--"+boundary+"--"+sep)
                        .getBytes(StandardCharsets.UTF_8)
        );

        return HttpRequest.newBuilder(URI.create(BASE+"/data/upload"))
                .header("Content-Type","multipart/form-data; boundary="+boundary)
                .header("Authorization", ApiClient.basicAuth())
                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts))
                .build();
    }
}
