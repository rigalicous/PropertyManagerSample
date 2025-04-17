
# 🏠 Property Manager (JavaFX + H2)

A desktop-based property management system built in Java using JavaFX and H2 database. It allows users to manage tenants across multiple properties, track rent payments, and import/export tenant data through CSV.

---

## ✨ Features

- Manage up to 5 properties (`property_1` to `property_5`)
- Add, edit, and delete tenants
- View tenant leases, security deposit, rent, and balance
- Color-coded rent balances (green = paid, red = overdue)
- Search and filter tenants by name or apartment number
- Drag-and-drop CSV file import with import history log
- Export tenant data to CSV

---

## 🚀 How to Run

1. Make sure you have **Java 17+** and **Maven** installed.
2. Clone this repository:
   ```bash
   git clone https://github.com/rigalicous/PropertyManagerSample.git
   cd property-manager
   ```
3. Run the application using Maven:
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

> 💡 These two lines are also provided as comments at the end of the `Main.java` class for convenience.

---

## 🧱 Built With

- Java 17
- JavaFX
- Maven
- H2 Embedded Database

---

## 📂 Project Structure

```
src/
├── main/
│   ├── java/
│   │   ├── gui/            # JavaFX GUI classes
│   │   └── org.core/           # Tenant model class
│   └── resources/          # FXML or styling if added later
```

---

## 📜 License

This project is open source and available under the [MIT License](LICENSE).

---

_Developed by [Matthew Rivera](https://github.com/rigalicous)_
