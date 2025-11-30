# Project Documentation

## `com.sunny.model.Download`

A class representing a download task.

### Methods

- **`Download()`**: No-argument constructor.
- **`Download(String filename, String url, long fileSize, long downloadedSize, DownloadStatus status, String downloadPath, Timestamp startTime, Timestamp endTime, int threadCount)`**: Constructor with all parameters.
- **`getFilename()`**: Returns the filename.
- **`setFilename(String filename)`**: Sets the filename.
- **`getUrl()`**: Returns the download URL.
- **`setUrl(String url)`**: Sets the download URL.
- **`getFileSize()`**: Returns the total file size.
- **`setFileSize(long fileSize)`**: Sets the total file size.
- **`getDownloadedSize()`**: Returns the size of the downloaded portion.
- **`setDownloadedSize(long downloadedSize)`**: Sets the size of the downloaded portion.
- **`getStatus()`**: Returns the download status.
- **`setStatus(DownloadStatus status)`**: Sets the download status.
- **`getDownloadPath()`**: Returns the path where the file is saved.
- **`setDownloadPath(String downloadPath)`**: Sets the path where the file is saved.
- **`getStartTime()`**: Returns the start time of the download.
- **`setStartTime(Timestamp startTime)`**: Sets the start time of the download.
- **`getEndTime()`**: Returns the end time of the download.
- **`setEndTime(Timestamp endTime)`**: Sets the end time of the download.
- **`getThreadCount()`**: Returns the number of threads used for the download.
- **`setThreadCount(int threadCount)`**: Sets the number of threads used for the download.
- **`getId()`**: Returns the download ID.
- **`setId(int id)`**: Sets the download ID.

## `com.sunny.model.DownloadStatus`

An enum representing the status of a download.

### Values

- **`DOWNLOADING`**
- **`PAUSED`**
- **`COMPLETED`**
- **`FAILED`**
- **`PENDING`**
- **`CANCELED`**

## `com.sunny.utils.Logger`

A utility class for logging.

### Methods

- **`logError(String message)`**: Logs an error message. (Currently a placeholder).

## `com.sunny.utils.FileUtils`

A utility class for file operations.

### Methods

- **`isValidFilename(String file)`**: Checks if a filename is valid.
- **`hasWritePermission(String dir)`**: Checks if the application has write permission to a directory.
- **`getAvailableSpace(String dir)`**: Returns the available space in a directory in kilobytes.
- **`createDirectoryIfNotExists(String dir)`**: Creates a directory if it does not exist.
- **`getAppDataDirectory()`**: Returns the application data directory based on the operating system.

## `com.sunny.utils.URLValidator`

A utility class for validating URLs.

### Methods

- **`isValidURL(String url)`**: Checks if a URL is syntactically valid.
- **`isReachable(String url)`**: Checks if a URL is reachable.
- **`supportsResume(String url)`**: Checks if a server supports download resuming.

## `com.sunny.database.DownloadDAO`

A class for Data Access Object for downloads.

### Methods

- **`insertDownload(Download download)`**: Inserts a new download into the database.
- **`updateDownload(Download download)`**: Updates an existing download in the database.
- **`updateDownloadStatus(int downloadId, DownloadStatus status)`**: Updates the status of a download.
- **`updateEndTime(int downloadId, Timestamp endTime)`**: Updates the end time of a download.
- **`getDownloadById(int id)`**: Retrieves a download by its ID.
- **`getAllDownloads()`**: Retrieves all downloads from the database.
- **`getDownloadsByStatus(DownloadStatus status)`**: Retrieves all downloads with a specific status.
- **`deleteDownload(int id)`**: Deletes a download from the database.
- **`updateDownloadedSize(int downloadId, long bytesToAdd)`**: Updates the downloaded size of a download.

## `com.sunny.database.DatabaseManager`

A singleton class for managing the database connection.

### Methods

- **`getConnection()`**: Returns the database connection.
- **`closeConnection()`**: Closes the database connection.
- **`initializeDatabase()`**: Initializes the database and creates the `downloads` table if it doesn't exist.
- **`getInstance()`**: Returns the singleton instance of the `DatabaseManager`.

## `com.sunny.downloader.ChunkResult`

A class representing the result of a chunk download.

### Methods

- **`ChunkResult(int taskId, long bytesWritten, int attempts, Throwable error)`**: Constructor with all parameters.
- **`getAttempts()`**: Returns the number of attempts for the chunk download.
- **`getError()`**: Returns the error that occurred during the chunk download.
- **`setError(Throwable error)`**: Sets the error that occurred during the chunk download.
- **`getBytesWritten()`**: Returns the number of bytes written in the chunk download.
- **`getTaskId()`**: Returns the task ID of the chunk download.

## `com.sunny.downloader.DownloadTask`

A class representing a callable task for downloading a chunk of a file.

### Methods

- **`DownloadTask(DownloadManager downloadManager, int downloadId, String fileUrl, RandomAccessFile saveFile, long startByte, long endByte, int taskId)`**: Constructor with all parameters.
- **`call()`**: Executes the download task.
- **`pauseDownload()`**: Pauses the download task.
- **`resumeDownload()`**: Resumes the download task.
- **`stopDownload()`**: Stops the download task.

## `com.sunny.downloader.DownloadThread`

A class representing a thread for downloading a chunk of a file.

### Methods

- **`DownloadThread(String fileURL, String savePath, long startByte, long endByte, int threadId, DownloadManager downloadManager, int downloadId)`**: Constructor with all parameters.
- **`run()`**: Executes the download thread.
- **`pauseDownload()`**: Pauses the download thread.
- **`resumeDownload()`**: Resumes the download thread.
- **`stopDownload()`**: Stops the download thread.
- **`getThreadId()`**: Returns the thread ID.

## `com.sunny.downloader.FileDownloader`

A class for downloading files.

### Methods

- **`supportsRangeRequests(String fileUrl)`**: Checks if the server supports range requests.
- **`downloadFileWithProgress(String fileUrl, String savePath, DownloadManager manager, int downloadId)`**: Downloads a file with progress reporting.
- **`downloadFile(String fileUrl, String savePath)`**: Downloads a file without progress reporting.
- **`getFileSize(String fileUrl)`**: Gets the size of a file.
- **`resumeDownload(int id)`**: Resumes a download.

## `com.sunny.downloader.DownloadManager`

A class for managing downloads.

### Methods

- **`addDownload(Download download)`**: Adds a new download.
- **`startDownload(int id)`**: Starts a download.
- **`pauseDownload(int id)`**: Pauses a download.
- **`resumeDownload(int id)`**: Resumes a download.
- **`cancelDownload(int id)`**: Cancels a download.
- **`getDownloadProgress(int downloadId)`**: Gets the progress of a download.
- **`shutDown()`**: Shuts down the download manager.
- **`reportProgress(int downloadId, long bytesBuffer)`**: Reports the progress of a download.
- **`getDownloadStatus(int downloadId)`**: Gets the status of a download.

## `com.sunny.downloader.ProgressReporter`

An interface for reporting download progress.

### Methods

- **`reportProgress(int downloadId, long deltaBytes)`**: Reports the progress of a download.

## `com.sunny.exceptions.DatabaseException`

An exception for database errors.

### Methods

- **`DatabaseException(String message)`**: Constructor with a message.

## `com.sunny.exceptions.InvalidURLException`

An exception for invalid URLs.

### Methods

- **`InvalidURLException(String url)`**: Constructor with the invalid URL.

## `com.sunny.exceptions.DownloadFailedException`

An exception for failed downloads.

### Methods

- **`DownloadFailedException(String message)`**: Constructor with a message.

## `com.sunny.exceptions.InsufficientSpaceException`

An exception for insufficient disk space.

### Methods

- **`InsufficientSpaceException(String message)`**: Constructor with a message.

## `com.sunny.Main`

The main class of the application.

### Methods

- **`main(String[] args)`**: The entry point of the application.
