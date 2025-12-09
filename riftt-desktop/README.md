# Riftt Desktop

The `riftt-desktop` module provides the Graphical User Interface (GUI) for the Riftt download manager. It is built using Java Swing to ensure a native look and feel with high performance on desktop environments.

## Features

- **Swing-based UI**: Utilizes Java Swing and modern flat design principals for a clean interface.
- **Card Layout**: Dynamic switching between Download List, Settings, and other views.
- **System Integration**: file system dialogs for choosing download locations.
- **Clipboard Monitoring**: (Future/Planned) Automatically detecting URLs copied to the clipboard.

## Challenges & Implementation Details

- **Responsive UI**: Keeping the Event Dispatch Thread (EDT) free from blocking operations (like database access or initial network checks) to prevent the UI from freezing.
- **Swing Worker**: heavily used to perform background tasks and update UI components safely.
- **Cross-Platform Loos**: Ensuring the application looks consistent and functions correctly across Windows, Linux, and macOS.
