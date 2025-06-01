# Batch Renamer IntelliJ Plugin

A powerful IntelliJ IDEA plugin that allows you to rename symbols across your codebase using a simple JSON configuration.

## Features

- **Two Renaming Modes**:
  - Rename all symbols in the current file with a user-friendly dialog
  - Batch rename across the project using JSON configuration
- **Symbol Type Detection**: Automatically identifies classes, methods, fields, etc.
- **Symbol Search**: Filter symbols by name to quickly find what you need
- **Bulk Operations**: Find/replace patterns across multiple symbols at once
- **Copy Symbols**: Easily copy symbol names to clipboard for use in other tools
- **JSON-based Configuration**: Define multiple rename operations in a single JSON file
- **Two Input Methods**: Either paste JSON directly or select a JSON configuration file
- **Relative Path Support**: Use project-relative paths for better portability
- **Real-time Validation**: Catches errors in your configuration before execution
- **Progress Tracking**: Background processing with progress indicators
- **Detailed Reporting**: Summary of successful and failed operations

## Installation

### From JetBrains Marketplace

1. Open IntelliJ IDEA
2. Go to `File → Settings → Plugins → Marketplace`
3. Search for "Batch Renamer"
4. Click "Install"

### Manual Installation

1. Download the latest release (`.jar` file) from the [Releases](https://github.com/yourusername/json-symbol-renamer/releases) page
2. In IntelliJ IDEA, go to `File → Settings → Plugins`
3. Click the gear icon and select "Install Plugin from Disk..."
4. Select the downloaded `.jar` file

## Usage

### Rename Symbols in Current File

There are multiple ways to access this feature:

1. Open a file in the editor, then either:
   - Select `Refactor → Rename Symbols in File` from the main menu
   - Right-click in the editor and select `Rename Symbols in File` from the context menu
   - Right-click on the file's editor tab and select `Rename Symbols in File`
   - Right-click on the file in the Project view and select `Rename Symbols in File`
   - Use the keyboard shortcut `Shift+Ctrl+Alt+R`
2. The plugin will display a dialog with all symbols in the file
3. Enter new names for the symbols you want to rename
4. Use the search box to filter symbols
5. Use the "Bulk Find/Replace" button to perform pattern-based renaming
6. Click "OK" to apply the changes

### Batch Rename Across Project (JSON-based)

1. Open your project in IntelliJ IDEA
2. Select `Tools → Batch Renamer` from the menu
3. Either paste your JSON configuration or select a configuration file
4. Review the validation results
5. Click "OK" to start the renaming process
6. Check the results summary when the operations are complete

## JSON Configuration Format

The plugin uses a simple JSON format to define rename operations:

```json
{
  "basePath": "optional/base/path",
  "operations": [
    {
      "filePath": "path/to/file.java",
      "line": 10,
      "column": 15,
      "newName": "newSymbolName"
    },
    {
      "filePath": "another/file.java",
      "line": 25,
      "column": 8,
      "newName": "anotherNewName"
    }
  ]
}
```

### Configuration Fields

| Field | Description |
|-------|-------------|
| `basePath` | (Optional) Base directory for all relative paths |
| `operations` | Array of rename operations to perform |
| `filePath` | Path to the file containing the symbol (absolute or relative to project/basePath) |
| `line` | Line number where the symbol is located (0-based) |
| `column` | Column number where the symbol is located (0-based) |
| `newName` | New name for the symbol |

### Path Resolution

Paths in the configuration can be:
- **Absolute**: `/full/path/to/file.java`
- **Project-relative**: `src/main/java/example/MyClass.java`
- **BasePath-relative**: When using the optional `basePath` field, all operations use paths relative to it

## Examples

### Basic Rename

```json
{
  "operations": [
    {
      "filePath": "src/main/java/com/example/MyClass.java",
      "line": 15,
      "column": 12,
      "newName": "updatedMethod"
    }
  ]
}
```

### Multiple Renames with Base Path

```json
{
  "basePath": "src/main/java",
  "operations": [
    {
      "filePath": "com/example/model/User.java",
      "line": 5,
      "column": 18,
      "newName": "Person"
    },
    {
      "filePath": "com/example/service/UserService.java",
      "line": 10,
      "column": 26,
      "newName": "getPersonById"
    }
  ]
}
```

## Tips & Tricks

- **Finding Coordinates**: Use the status bar in IntelliJ to find the exact line and column of a symbol
- **IDE Coordinates**: Remember that line and column numbers in the configuration are 0-based
- **Validation**: The plugin validates your JSON before executing any operations
- **Project-Relative Paths**: For portable configurations, use paths relative to your project root
- **Version Control**: Consider running rename operations after committing your changes to make reviewing easier

## Troubleshooting

### Common Issues

- **Symbol not found**: Ensure the line and column values are 0-based and point directly to the symbol
- **File not found**: Check that the file path is correct and accessible
- **JSON validation errors**: Review the error message and fix the JSON configuration
- **Rename failed**: Some symbols cannot be renamed due to conflicts or language limitations

### Logs

If you encounter issues, check the IntelliJ IDEA log files:
1. Go to `Help → Show Log in Explorer/Finder`
2. Look for entries related to "Batch Renamer"

## Building from Source

1. Clone the repository
   ```
   git clone https://github.com/anastasiya1155/intellij-batch-renamer-plugin.git
   ```

2. Build using Gradle
   ```
   ./gradlew buildPlugin
   ```

3. The plugin JAR will be created in `build/libs/`

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
