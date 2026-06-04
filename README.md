# HappyCamper

HappyCamper is a Java desktop application designed to help camp leaders analyze and manage activity assignments for campers. The application provides a user-friendly interface for importing, analyzing, and exporting roster data, providing insights into scheduling oversights, preference satisfaction, and more.

The application is primarily intended for use with data exported from Campminder.com, but is designed to be source-agnostic and easily extensible to other data sources.

HappyCamper's feature-based, service-oriented architecture allows for easy extension and maintenance, and lets users add, analyze, and export just the information most relevant to them.

## Features

### Activity Tracking
- Import and process camper and activity data from CSV files
- View and filter campers by activity assignments
- Track assignment completion and distribution
- Identify scheduling conflicts and missing assignments

### Program Management
- Extract program information from enrollment data
- Filter and view campers by program
- Compare assignment patterns across programs, isolating oversights

### Preference Analysis
- Track camper activity preferences, indicating shortcomings and areas for amendment
- Calculate preference satisfaction, as objective measure and as percentile
- Identify campers with unrequested activities

### Swim Level Validation
- Validate swim levels against assigned activities
- Identify campers with activities beyond their swim level

### Medical Tracking
- Track campers with medical needs, helping staff manage and anticipate needs
- Identify programs with medical requirements

### Data Export
- Export filtered data to CSV files
- Customize export settings (columns, rows, empty value representation)

## Architecture

HappyCamper follows a clean architecture pattern with clear separation of concerns:

### Domain Layer (`com.echo.domain`)
- Core data models: `Roster`, `Camper`, `EnhancedRoster`
- Specialized roster types: `CamperRoster`, `ActivityRoster`
- Data constants and header definitions

### Feature Layer (`com.echo.feature`)
- Feature interface: `RosterFeature`
- Feature implementations: `ActivityFeature`, `ProgramFeature`, `PreferenceFeature`, `SwimLevelFeature`, `MedicalFeature`
- Pluggable feature system for extensibility

### Filter Layer (`com.echo.filter`)
- Filter interface: `RosterFilter`
- Filter manager: `FilterManager`
- Filter implementations: `AssignmentFilter`, `SortedProgramFilter`, `PreferenceFilter`, `SwimLevelFilter`, `MedicalFilter`
- Composable filters for flexible data views

### Service Layer (`com.echo.service`)
- Services for file operations: `ImportService`, `ExportService`
- Roster management: `RosterService`
- Settings management for import/export

### UI Layer (`com.echo.ui`)
- Main application: `HappyCamper`
- Main window: `MainWindow`
- Components: `RosterTable`
- Dialogs: `ImportDialog`, `ExportDialog`
- Filter UI: `FilterSidebar`

### Validation Layer (`com.echo.validation`)
- Data format validation: `RosterRegexBuilder`
- Import validation: `ImportFileValidator`
- Export validation: `ExportFileValidator`

### Logging Layer (`com.echo.logging`)
- Warning handling, general management: `WarningManager`, `RosterWarning`
- Exception handling: `RosterException`, `DetailedRosterException`

## Getting Started

### Prerequisites
- Java 22 or higher
- Maven 3.6 or higher

### Building the Project
```bash
cd redo
mvn clean package
```

### Running the Application
```bash
java -jar target/roster-manager-2.2-SNAPSHOT.jar
```

Or use the provided script:
```bash
./run_app.sh
```

## Usage

1. **Import Data**
   - Click the "Import" button
   - Select a camper roster CSV file
   - Select an activity roster CSV file
   - Choose which features to enable
   - Click "Import" to process the data

2. **View and Filter Data**
   - Use the sidebar to filter by program, activity preferences, or medical needs
   - Click column headers to sort the data
   - Use the "View Settings" button to customize visible columns

3. **Export Data**
   - Click the "Export" button
   - Choose export options (all/visible columns, all/visible rows)
   - Select a destination file
   - Click "Export" to save the data

## Design Patterns

The application uses several design patterns:

### Feature-based Design
Features are implemented as independent components that can be enabled or disabled.


### Service-oriented Architecture
Services handle specific responsibilities like import, export, and roster management.

### Composable Filters
Filters can be combined in any way to create complex filtering rules.

### Factory Pattern
Used for creating UI components and filter panels.

### Builder Pattern
Used for constructing complex objects like regex patterns.

## Development

### Project Structure
```
redo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── echo/
│   │   │           ├── automation/    # Test automation utilities
│   │   │           ├── domain/        # Core data models
│   │   │           ├── feature/       # Feature implementations
│   │   │           ├── filter/        # Filter implementations
│   │   │           ├── logging/       # Error and warning handling
│   │   │           ├── service/       # Services for file operations
│   │   │           ├── ui/            # User interface components
│   │   │           ├── validation/    # Data validation utilities
│   │   │           └── HappyCamper.java  # Main entry point
│   │   └── resources/                 # Application resources
│   └── test/
│       └── java/                      # Test classes
└── pom.xml                            # Maven configuration
```

### Adding a New Feature
1. Create a new class implementing `RosterFeature`
2. Implement the required methods
3. Add the feature to `RosterService.availableFeatures`
4. Create a filter if needed (see below)

### Adding a New Filter
1. Create a new class implementing `RosterFilter`
2. Implement the required methods
3. Add the filter to `FilterManager` when appropriate

### Testing
The project includes JUnit 5 tests for core functionality. Run tests with:
```bash
mvn test
```

### Packaging
To create a distributable package:
```bash
mvn package
```
