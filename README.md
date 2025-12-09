# Riftt

Riftt is a modern, resume-capable download manager built with Java. It is designed to be lightweight, efficient, and reliable, handling large file downloads with ease.

## Features

- **Multi-threaded Downloading**: Accelerates downloads by splitting files into chunks and downloading them in parallel.
- **Resume Capability**: Automatically resumes downloads from where they left off, even after connection failures or application restarts.
- **Persistent State**: Maintains download progress and list in a local lightweight database (SQLite).
- **Modern UI**: Clean, light-themed card-based interface for managing downloads.
- **Unknown Size Handling**: Gracefully handles servers that don't report file size.
- **Browser-like Headers**: Mimics standard browser requests to avoid server-side blocking (403 Forbidden).

## Architecture

The project is split into two modules:

- `riftt-core`: The platform-agnostic download engine containing the logic for connection management, chunk handling, and state tracking.
- `riftt-desktop`: The desktop application layer providing the Swing-based UI and desktop-specific integrations (like database storage location).

## Getting Started

### Prerequisites

- Java JDK 8 or higher
- Maven

### Building the Project

To build the application from source, run:

```bash
mvn clean install
```

This will compile the core and desktop modules. The executable JAR file will be located in:
`riftt-desktop/target/riftt-desktop-1.0-SNAPSHOT-jar-with-dependencies.jar`

### Running the Application

You can run the built application directly using Java:

```bash
java -jar riftt-desktop/target/riftt-desktop-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Usage

1. **Add Download**: Click the "Add URL" button and paste your link. The application will resolve the filename automatically.
2. **Manage**: Use the Pause, Resume, and Cancel buttons to control your active downloads.
3. **Remove**: Remove completed or unwanted downloads from the list.

## Technologies

- Java Swing (UI)
- SQLite (Data Persistence)
- Maven (Dependency Management)

## Future Scope

### Mobile (Android)

We are actively planning to bring the Riftt experience to Android. The goal is to leverage the `riftt-core` module, which is already platform-agnostic, and wrap it in a modern Android-native UI (using Jetpack Compose).

- **Core Reuse**: The existing download logic in `riftt-core` will be reused with minimal changes.
- **Service Integration**: Android Services will be used to ensure downloads continue in the background.
