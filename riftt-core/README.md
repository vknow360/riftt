# Riftt Core

The `riftt-core` module is the heart of the Riftt download manager. It encapsulates all the business logic required for managing downloads, handling network connections, and persisting state, completely independent of any user interface.

## Features

- **Platform Agnostic**: Designed to be used in any environment (CLI, Desktop, Mobile, Server).
- **Multi-threaded Downloading**: Implements logic to split files into chunks and download them concurrently.
- **Resumability**: Logic to handle partial content requests (Range headers) to resume interrupted downloads.
- **Persistence**: Uses SQLite via JDBC to store download metadata (URL, path, status, progress).
- **Smart Headers**: Generates browser-compatible headers to mimic real user behavior and bypass basic anti-bot protections.

## Challenges & Implementation Details

- **Concurrency Control**: Managing multiple threads for a single download while updating shared state (progress, status) requires careful synchronization to avoid race conditions.
- **Network Reliability**: Handling timeouts, connection resets, and invalid server responses gracefully.
- **State Management**: Ensuring the internal state (Downloading, Paused, Error, Completed) transitions correctly and is persisted atomically to the database.
