// TenantManager.java
package gui;

import org.core.Tenant;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class TenantManager {
    private final String dbName;
    private final TableView<Tenant> tenantTable = new TableView<>();
    private final ObservableList<Tenant> tenantData = FXCollections.observableArrayList();
    private final FilteredList<Tenant> filteredData = new FilteredList<>(tenantData, p -> true);

    public TenantManager(String dbName) {
        this.dbName = dbName;
    }

    public Parent getView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = new Label("Managing: Property " + dbName.replaceAll("[^0-9]", ""));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or apartment...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(tenant -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return tenant.getName().toLowerCase().contains(lower) ||
                        tenant.getAptNumber().toLowerCase().contains(lower);
            });
        });

        TableColumn<Tenant, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Tenant, String> aptCol = new TableColumn<>("Apt#");
        aptCol.setCellValueFactory(new PropertyValueFactory<>("aptNumber"));

        TableColumn<Tenant, LocalDate> leaseStartCol = new TableColumn<>("Lease Start");
        leaseStartCol.setCellValueFactory(new PropertyValueFactory<>("leaseStart"));

        TableColumn<Tenant, LocalDate> leaseEndCol = new TableColumn<>("Lease Expired");
        leaseEndCol.setCellValueFactory(new PropertyValueFactory<>("leaseExpired"));

        TableColumn<Tenant, Double> rentCol = new TableColumn<>("Rent");
        rentCol.setCellValueFactory(new PropertyValueFactory<>("rent"));

        TableColumn<Tenant, Double> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
        balanceCol.setCellFactory(column -> new TableCell<Tenant, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%.2f", value));
                    setStyle(value < 0 ?
                            "-fx-background-color: lightcoral; -fx-text-fill: white;" :
                            "-fx-background-color: lightgreen; -fx-text-fill: black;");
                }
            }
        });

        tenantTable.getColumns().addAll(nameCol, aptCol, leaseStartCol, leaseEndCol, rentCol, balanceCol);
        tenantTable.setItems(filteredData);
        tenantTable.getSortOrder().add(balanceCol);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshTenants());

        Button addBtn = new Button("Add Tenant");
        addBtn.setOnAction(e -> showTenantFormDialog(null));

        Button editBtn = new Button("Edit Tenant");
        editBtn.disableProperty().bind(Bindings.isEmpty(tenantTable.getSelectionModel().getSelectedItems()));
        editBtn.setOnAction(e -> showTenantFormDialog(tenantTable.getSelectionModel().getSelectedItem()));

        Button deleteBtn = new Button("Delete Tenant");
        deleteBtn.disableProperty().bind(Bindings.isEmpty(tenantTable.getSelectionModel().getSelectedItems()));
        deleteBtn.setOnAction(e -> deleteSelectedTenant());

        Button exportBtn = new Button("Export to CSV");
        exportBtn.setOnAction(e -> exportToCSV());

        Label importLabel = new Label("Drag & Drop CSV File Here to Import Tenants");
        importLabel.setStyle("-fx-border-color: gray; -fx-border-style: dashed; -fx-padding: 10; -fx-alignment: center;");
        importLabel.setMaxWidth(Double.MAX_VALUE);
        importLabel.setOnDragOver(event -> {
            if (event.getGestureSource() != importLabel && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        importLabel.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                importFromCSV(file);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> {
            BuildingSelector selector = new BuildingSelector();
            SceneSwitcher.switchTo(selector.getView(), "Property Manager");
        });

        HBox controls = new HBox(10, refreshBtn, addBtn, editBtn, deleteBtn, exportBtn, backBtn);
        root.getChildren().addAll(title, searchField, tenantTable, controls, importLabel);

        refreshBtn.fire();
        return root;
    }

    private void importFromCSV(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file));
             Connection conn = DriverManager.getConnection("jdbc:h2:./" + dbName, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO tenants (id, name, apt_number, lease_start, lease_expired, security, rent, balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
             PrintWriter log = new PrintWriter(new FileWriter("import_log.txt", true))) {

            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 8) {
                    pstmt.setInt(1, Integer.parseInt(tokens[0]));
                    pstmt.setString(2, tokens[1]);
                    pstmt.setString(3, tokens[2]);
                    pstmt.setDate(4, Date.valueOf(tokens[3]));
                    pstmt.setDate(5, Date.valueOf(tokens[4]));
                    pstmt.setDouble(6, Double.parseDouble(tokens[5]));
                    pstmt.setDouble(7, Double.parseDouble(tokens[6]));
                    pstmt.setDouble(8, Double.parseDouble(tokens[7]));
                    pstmt.executeUpdate();
                    log.println("Imported tenant: " + String.join(", ", tokens));
                }
            }
            log.println("--- Import finished: " + java.time.LocalDateTime.now() + " ---\n");
            refreshTenants();
            new Alert(Alert.AlertType.INFORMATION, "CSV import completed successfully.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "CSV import failed: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshTenants() {
        tenantData.clear();
        try (Connection conn = DriverManager.getConnection("jdbc:h2:./" + dbName, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tenants")) {
            while (rs.next()) {
                Tenant tenant = new Tenant(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("apt_number"),
                        rs.getDate("lease_start").toLocalDate(),
                        rs.getDate("lease_expired").toLocalDate(),
                        rs.getDouble("security"),
                        rs.getDouble("rent"),
                        rs.getDouble("balance")
                );
                tenantData.add(tenant);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteSelectedTenant() {
        Tenant selected = tenantTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Tenant");
        alert.setHeaderText("Are you sure you want to delete this tenant?");
        alert.setContentText(selected.getName() + " (Apt: " + selected.getAptNumber() + ")");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection("jdbc:h2:./" + dbName, "sa", "");
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM tenants WHERE id = ?")) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                refreshTenants();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tenants to CSV");
        fileChooser.setInitialFileName(dbName + "_tenants.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("ID,Name,Apt#,LeaseStart,LeaseExpired,Security,Rent,Balance");
                for (Tenant t : tenantData) {
                    writer.printf("%d,%s,%s,%s,%s,%.2f,%.2f,%.2f\n",
                            t.getId(), t.getName(), t.getAptNumber(),
                            t.getLeaseStart(), t.getLeaseExpired(),
                            t.getSecurity(), t.getRent(), t.getBalance());
                }
                new Alert(Alert.AlertType.INFORMATION, "Export successful: " + file.getAbsolutePath()).showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void showTenantFormDialog(Tenant tenant) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(tenant == null ? "Add Tenant" : "Edit Tenant");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        TextField aptField = new TextField();
        TextField leaseStart = new TextField();
        TextField leaseEnd = new TextField();
        TextField securityField = new TextField();
        TextField rentField = new TextField();

        if (tenant != null) {
            nameField.setText(tenant.getName());
            aptField.setText(tenant.getAptNumber());
            leaseStart.setText(formatDateForInput(tenant.getLeaseStart()));
            leaseEnd.setText(formatDateForInput(tenant.getLeaseExpired()));
            securityField.setText(String.valueOf(tenant.getSecurity()));
            rentField.setText(String.valueOf(tenant.getRent()));
        }

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Apt#:"), aptField);
        grid.addRow(2, new Label("Lease Start (MM-DD-YYYY):"), leaseStart);
        grid.addRow(3, new Label("Lease End (MM-DD-YYYY):"), leaseEnd);
        grid.addRow(4, new Label("Security:"), securityField);
        grid.addRow(5, new Label("Rent:"), rentField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try (Connection conn = DriverManager.getConnection("jdbc:h2:./" + dbName, "sa", "");
                     PreparedStatement pstmt = tenant == null ?
                             conn.prepareStatement("INSERT INTO tenants (name, apt_number, lease_start, lease_expired, security, rent, balance) VALUES (?, ?, ?, ?, ?, ?, ?)") :
                             conn.prepareStatement("UPDATE tenants SET name=?, apt_number=?, lease_start=?, lease_expired=?, security=?, rent=? WHERE id=?")
                ) {
                    String formattedStart = formatDate(leaseStart.getText());
                    String formattedEnd = formatDate(leaseEnd.getText());
                    double rent = Double.parseDouble(rentField.getText());

                    pstmt.setString(1, nameField.getText());
                    pstmt.setString(2, aptField.getText());
                    pstmt.setDate(3, Date.valueOf(formattedStart));
                    pstmt.setDate(4, Date.valueOf(formattedEnd));
                    pstmt.setDouble(5, Double.parseDouble(securityField.getText()));
                    pstmt.setDouble(6, rent);

                    if (tenant == null) {
                        int months = getUnpaidMonths(formattedStart);
                        double balance = rent * months;
                        pstmt.setDouble(7, balance);
                    } else {
                        pstmt.setInt(7, tenant.getId());
                    }

                    pstmt.executeUpdate();
                    refreshTenants();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private String formatDate(String mmddyyyy) {
        String[] parts = mmddyyyy.split("-");
        return parts[2] + "-" + parts[0] + "-" + parts[1];
    }

    private String formatDateForInput(LocalDate date) {
        return String.format("%02d-%02d-%d", date.getMonthValue(), date.getDayOfMonth(), date.getYear());
    }

    private int getUnpaidMonths(String startDate) {
        String[] parts = startDate.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        LocalDate today = LocalDate.now();
        return Math.max(0, (today.getYear() - year) * 12 + (today.getMonthValue() - month));
    }
}


