# Clean previous builds
mvn clean

# Compile the project
mvn compile

# Run tests (if you have any)
mvn test

# Create JAR file
mvn package

# Clean + Compile + Package
mvn clean install

# Run the application
mvn exec:java -Dexec.mainClass="com.sunny.Main"
```

### Or use IntelliJ's Maven Tool Window:
1. **View → Tool Windows → Maven**
2. Expand **Lifecycle**
3. Double-click **clean**, **compile**, or **package**

---

## 📂 Updated Package Structure with Maven
```
src/
├── com.sunny/
│   └── Main.java
├── com.sunny.model/
│   ├── Download.java
│   ├── DownloadStatus.java (enum)
│   └── DownloadConfig.java
├── com.sunny.database/
│   ├── DatabaseManager.java
│   └── DownloadDAO.java
├── com.sunny.downloader/
│   ├── DownloadThread.java
│   ├── DownloadManager.java
│   └── FileDownloader.java
├── com.sunny.gui/
│   ├── MainFrame.java
│   ├── DownloadPanel.java
│   ├── AddDownloadDialog.java
│   └── HistoryPanel.java
├── com.sunny.exceptions/
│   ├── InvalidURLException.java
│   ├── DownloadFailedException.java
│   └── DatabaseException.java
└── com.sunny.utils/
├── FileUtils.java
└── URLValidator.java