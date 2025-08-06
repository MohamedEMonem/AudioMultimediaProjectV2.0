# AudioMultimediaProject V2.0

## Overview
This Java application differentiates between audio files in WAV and OGG formats. It reads the file headers, displays detailed information about the selected file, and checks for file corruption based on header data.

## Features
- Detects and distinguishes between WAV and OGG audio files.
- Displays header information in a user-friendly format.
- Checks for basic file corruption in both formats.
- Saves the header information to a `.FileHeader.txt` file alongside the selected audio file.
- Simple GUI using Swing for file selection and information display.

## How It Works
1. The user is prompted to select a WAV or OGG file via a file chooser dialog.
2. The application reads the file header and determines the file type.
3. Header details are displayed in a dialog box.
4. The header information is saved as a text file in the same directory as the selected audio file.

## Usage
1. Compile the project:
   ```sh
   javac -d bin src/Main.java
   ```
2. Run the application:
   ```sh
   java -cp bin Main
   ```
3. Follow the prompts to select a WAV or OGG file.

## File Structure
- `src/Main.java`: Main application logic and file header parsing.
- `DifferentiateProg.iml`: IntelliJ IDEA project file.

## Requirements
- Java 8 or higher
- No external dependencies

## Notes
- The application uses Swing for GUI dialogs.
- Only basic header validation is performed; it does not fully verify file integrity or decode audio data.

## License
This project is provided for educational purposes. No specific license is applied.
