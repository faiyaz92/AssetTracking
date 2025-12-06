# Asset Tracking Application - BRD V2 (with RFID Support)

## Document Cross-References
- **Integration Guide**: `Chainway_C72_Integration_Guide.md` - Device specifications and SDK methods
- **Task Checklist V2**: `C72_Integration_Task_Checklist_V2.md` - Implementation tasks with AI notes
- **Cross-Reference Guide**: `BRD_TechDoc_V2_Cross_Reference.md` - Document relationships and traceability

## Table of Contents
1. [Business Requirements Document (BRD)](#business-requirements-document-brd)
   - [Introduction](#introduction)
   - [Business Objectives](#business-objectives)
   - [Functional Requirements](#functional-requirements)
   - [User Stories](#user-stories)
   - [Non-Functional Requirements](#non-functional-requirements)
2. [Technical Documentation V2](#technical-documentation-v2)
   - [Architecture Overview](#architecture-overview)
   - [Technology Stack](#technology-stack)
   - [Data Model](#data-model)
   - [Key Components](#key-components)
   - [File References](#file-references)
   - [Workflows](#workflows)

---

## Business Requirements Document (BRD)

### Introduction
The Asset Tracking Application is a mobile solution designed for Android devices to manage and track physical assets within rooms or locations. The app allows users to create rooms, add assets with unique barcodes and RFID tags, link assets to rooms, and print barcodes on thermal printers for physical labeling. Version 2 adds RFID scanning capabilities alongside existing barcode scanning for enhanced tracking options.

### Business Objectives
- Provide an easy-to-use interface for inventory management.
- Enable barcode generation and printing for asset identification.
- Support RFID tag scanning for contactless asset tracking.
- Maintain dual scanning mechanisms (barcode + RFID) for flexibility.
- Ensure data persistence and offline functionality.
- Facilitate quick asset search and assignment.

### Functional Requirements

#### FR-001: Room Management
- Users can create, edit, and delete rooms.
- Each room has a name and optional description.
- Rooms display the count of assigned assets.

#### FR-002: Asset Management
- Users can create, edit, and delete assets.
- Each asset has an auto-generated unique code (based on primary key), name, optional details, base location (room), and current location.
- Assets can be assigned to rooms or remain unassigned.
- Assets display generated barcodes.
- Base location is set during creation (optional); current location updates on scans.

#### FR-003: Asset-Room Linking
- Assets can be assigned to rooms via room detail screens.
- Users can scan barcodes or RFID tags to assign assets to rooms (updates current location).
- Assets can be detached from rooms.
- Base location remains unchanged; current location tracks latest assignment.
- Assignment includes optional condition tracking for asset state.

#### FR-007: Audit Trail
- Track all asset movements with timestamps.
- Audit trail screen shows history of room changes for each asset.
- Includes from room, to room, and timestamp.

#### FR-008: Quick Asset Movement
- Quick scan functionality from home screen for rapid asset relocation.
- Scan barcode or RFID, select target location, enter condition, and move asset instantly.
- Bypasses detailed location screens for efficiency.

#### FR-004: Barcode Generation and Printing
- Automatic CODE_128 barcode generation for each asset based on its code.
- Print functionality via Bluetooth-connected 58mm thermal printers.
- Print layout includes asset name, code, and barcode image.
- Printed barcodes are used for physical asset labeling and subsequent scanning for assignment.

#### FR-009: RFID Asset Tracking (NEW)
- Support for Chainway C72 UHF RFID scanning alongside barcode scanning.
- RFID tags contain the same asset ID as barcodes for consistent tracking.
- Contactless scanning with 1-8 meter range for improved efficiency.
- RFID tag writing capability for programming blank tags with asset IDs.
- Tag validation: check for existing data before writing, show appropriate warnings.
- Dual scanning modes: users can choose barcode, NFC, or UHF RFID scanning.
- RFID scanning maintains same assignment and movement workflows as barcode.
- **Cross-Reference**: Task 1-4 in `C72_Integration_Task_Checklist_V2.md`

#### FR-005: Search and Filtering
- Search assets by name or code.
- Filter assets in room detail views.

#### FR-006: Data Persistence
- All data stored locally using SQLite via Room.
- Offline functionality with no internet dependency.

### User Stories

#### US-001: As a user, I want to add a room so that I can organize my assets.
- Acceptance Criteria: Room name is required; description is optional.

#### US-002: As a user, I want to add an asset so that I can track my inventory.
- Acceptance Criteria: Asset name is required; details are optional; unique code is auto-generated; barcode is auto-generated.

#### US-003: As a user, I want to assign an asset to a room so that I know its location.
- Acceptance Criteria: From room detail, scan barcode/RFID or select asset to assign.

#### US-004: As a user, I want to print a barcode for an asset so that I can label it physically.
- Acceptance Criteria: Print button on asset row; connects to paired Bluetooth printer.

#### US-005: As a user, I want to search for assets so that I can find them quickly.
- Acceptance Criteria: Search field in assets screen; filters by name/code.

#### US-006: As a user, I want to set a base location for an asset so that I know its home room.
- Acceptance Criteria: Dropdown in add/edit asset dialog; optional selection from rooms.

#### US-007: As a user, I want to view the audit trail of asset movements so that I can track history.
- Acceptance Criteria: Third tile on home screen; list of movements with timestamps.

#### US-008: As a user, I want to quickly move assets between locations so that I can efficiently relocate inventory.
- Acceptance Criteria: Quick scan button on home screen; scan barcode/RFID, select location, enter condition, confirm move.

#### US-009: As a user, I want to scan RFID tags so that I can use contactless asset tracking.
- Acceptance Criteria: RFID scanning option in scan dialogs; same asset ID as barcode; maintains all existing workflows; supports Chainway C72 UHF RFID with 1-8 meter range.

#### US-010: As a user, I want to write RFID tags so that I can program blank tags with asset IDs.
- Acceptance Criteria: RFID write button on asset rows; validates existing tag data; shows confirmation dialogs for overwriting; writes pure asset ID to EPC field; handles tag locking and killing as needed.
- **Cross-Reference**: Task 2 in `C72_Integration_Task_Checklist_V2.md`

### Non-Functional Requirements
- **Performance**: App should load data quickly (<2s for lists).
- **Usability**: Intuitive UI with Material3 design, dual scanning options.
- **Security**: No sensitive data; local storage only.
- **Compatibility**: Android API 23+.
- **Reliability**: Error handling with user feedback via toasts/snackbars.
- **RFID Compatibility**: Support for standard RFID frequencies (13.56MHz for NFC).

---

## Technical Documentation V2

### Architecture Overview
The application follows Clean Architecture principles with separation into layers:
- **Presentation Layer**: Compose UI screens, ViewModels.
- **Domain Layer**: Use cases, models, repository interfaces.
- **Data Layer**: Room DAOs, repositories, entities.

Dependency injection via Hilt. Navigation handled by Compose Navigation. Version 2 adds RFID scanning capabilities while maintaining existing barcode functionality.

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Database**: Room (SQLite)
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Barcode**: ZXing for generation and scanning
- **RFID**: Android NFC API for RFID/NFC tag reading (NEW)
- **Printing**: ESCPOS library for thermal printing
- **Build Tool**: Gradle (AGP 8.1.4, Kotlin 1.9.22)

### Data Model

#### Entities
- **LocationEntity**: id (Long), name (String), description (String?)
- **AssetEntity**: id (Long), name (String), details (String?), condition (String?), baseRoomId (Long?), currentRoomId (Long?)
- **AssetMovementEntity**: id (Long), assetId (Long), fromRoomId (Long?), toRoomId (Long), timestamp (Long)

#### Models
- **LocationSummary**: id, name, description, assetCount
- **AssetSummary**: id, code, name, details, baseRoomId, baseRoomName, currentRoomId, currentRoomName
- **AssetMovement**: id, assetId, assetName, fromRoomName, toRoomName, timestamp

### Key Components

#### Presentation Layer
- **AssetTrackingApp**: Main composable with NavHost.
- **HomeScreen**: Landing page with tiles for Rooms, Assets, Scan, Analytics, Audit Trail, Settings.
- **LocationsScreen**: List of rooms with CRUD dialogs.
- **AssetsScreen**: List of assets with search, CRUD dialogs, print button.
- **RoomDetailScreen**: Room details with assigned/unassigned assets, dual scan (barcode/RFID) to assign.
- **AuditTrailScreen**: List of asset movements with timestamps.
- **ScanModeSelector**: New component for choosing between barcode and RFID scanning (TechDoc_V2 Ref: SCAN-001)
- **RfidScanner**: New composable for RFID scanning using NFC API (TechDoc_V2 Ref: RFID-001)

#### Domain Layer
- **Models**: Room, Asset, RoomSummary, AssetSummary, AssetMovement.
- **Use Cases**: CreateRoom, UpdateRoom, DeleteRoom, ObserveRooms, etc. (similar for assets and movements).
- **Repositories**: RoomRepository, AssetRepository, AssetMovementRepository interfaces.

#### Data Layer
- **AssetTrackingDatabase**: Room database with RoomDao, AssetDao, AssetMovementDao.
- **Repositories**: RoomRepositoryImpl, AssetRepositoryImpl, AssetMovementRepositoryImpl with Flow-based queries.

#### Utilities
- **BarcodeGenerator**: rememberBarcodeImage composable for display.
- **ThermalPrinter**: printBarcode function for Bluetooth printing.
- **RfidReader**: New utility for NFC-based RFID reading (TechDoc_V2 Ref: RFID-002)

### File References

#### Core Files
- `app/src/main/java/com/example/assettracking/AssetTrackingApplication.kt`: Hilt application class.
- `app/src/main/java/com/example/assettracking/MainActivity.kt`: Main activity with Compose setContent.
- `app/src/main/AndroidManifest.xml`: Permissions and app config (updated for NFC).

#### Data Layer
- `app/src/main/java/com/example/assettracking/data/local/AssetTrackingDatabase.kt`: Room database setup.
- `app/src/main/java/com/example/assettracking/data/local/entity/RoomEntity.kt`: Room entity.
- `app/src/main/java/com/example/assettracking/data/local/entity/AssetEntity.kt`: Asset entity.
- `app/src/main/java/com/example/assettracking/data/local/entity/AssetMovementEntity.kt`: Asset movement entity.
- `app/src/main/java/com/example/assettracking/data/local/dao/RoomDao.kt`: Room DAO.
- `app/src/main/java/com/example/assettracking/data/local/dao/AssetDao.kt`: Asset DAO.
- `app/src/main/java/com/example/assettracking/data/local/dao/AssetMovementDao.kt`: Asset movement DAO.
- `app/src/main/java/com/example/assettracking/data/repository/RoomRepositoryImpl.kt`: Room repo impl.
- `app/src/main/java/com/example/assettracking/data/repository/AssetRepositoryImpl.kt`: Asset repo impl.
- `app/src/main/java/com/example/assettracking/data/repository/AssetMovementRepositoryImpl.kt`: Asset movement repo impl.

#### Domain Layer
- `app/src/main/java/com/example/assettracking/domain/model/Room.kt`: Room model.
- `app/src/main/java/com/example/assettracking/domain/model/Asset.kt`: Asset model.
- `app/src/main/java/com/example/assettracking/domain/model/AssetMovement.kt`: Asset movement model.
- `app/src/main/java/com/example/assettracking/domain/repository/RoomRepository.kt`: Room repo interface.
- `app/src/main/java/com/example/assettracking/domain/repository/AssetRepository.kt`: Asset repo interface.
- `app/src/main/java/com/example/assettracking/domain/repository/AssetMovementRepository.kt`: Asset movement repo interface.
- `app/src/main/java/com/example/assettracking/domain/usecase/room/*.kt`: Room use cases.
- `app/src/main/java/com/example/assettracking/domain/usecase/asset/*.kt`: Asset use cases.
- `app/src/main/java/com/example/assettracking/domain/usecase/movement/*.kt`: Movement use cases.

#### Presentation Layer
- `app/src/main/java/com/example/assettracking/presentation/navigation/AssetTrackingNavHost.kt`: Navigation setup.
- `app/src/main/java/com/example/assettracking/presentation/tabs/HomeScreen.kt`: Home screen.
- `app/src/main/java/com/example/assettracking/presentation/rooms/RoomsScreen.kt`: Rooms management.
- `app/src/main/java/com/example/assettracking/presentation/assets/AssetsScreen.kt`: Assets management.
- `app/src/main/java/com/example/assettracking/presentation/roomdetail/RoomDetailScreen.kt`: Room details.
- `app/src/main/java/com/example/assettracking/presentation/audit/AuditTrailScreen.kt`: Audit trail screen.
- `app/src/main/java/com/example/assettracking/presentation/tabs/viewmodel/RoomListViewModel.kt`: Room list VM.
- `app/src/main/java/com/example/assettracking/presentation/tabs/viewmodel/AssetListViewModel.kt`: Asset list VM.
- `app/src/main/java/com/example/assettracking/presentation/roomdetail/RoomDetailViewModel.kt`: Room detail VM.
- `app/src/main/java/com/example/assettracking/presentation/audit/AuditTrailViewModel.kt`: Audit trail VM.
- `app/src/main/java/com/example/assettracking/presentation/scanning/ScanModeSelector.kt`: New - Choose barcode/RFID (TechDoc_V2 Ref: SCAN-001)
- `app/src/main/java/com/example/assettracking/presentation/scanning/RfidScanner.kt`: New - RFID scanning (TechDoc_V2 Ref: RFID-001)

#### DI
- `app/src/main/java/com/example/assettracking/di/DatabaseModule.kt`: Room DB module.
- `app/src/main/java/com/example/assettracking/di/RepositoryModule.kt`: Repo modules.

#### Utils
- `app/src/main/java/com/example/assettracking/util/BarcodeGenerator.kt`: Barcode generation.
- `app/src/main/java/com/example/assettracking/util/ThermalPrinter.kt`: Printing utility.
- `app/src/main/java/com/example/assettracking/util/RfidReader.kt`: New - RFID reading utility (TechDoc_V2 Ref: RFID-002)

#### Build Files
- `app/build.gradle`: App dependencies and config (updated for NFC).
- `build.gradle`: Project plugins.
- `settings.gradle`: Repos and includes.

### Workflows

#### Adding a Room (BRD Ref: FR-001, US-001)
1. User taps "Locations" tile on home screen.
2. Navigates to LocationsScreen.
3. Taps FAB (+) to open add dialog.
4. Enters name (required) and description (optional).
5. Saves: ViewModel calls CreateRoom use case -> Repo -> DAO insert.
6. UI updates via Flow.

#### Adding an Asset (BRD Ref: FR-002, US-002)
1. User taps "Assets" tile on home screen.
2. Navigates to AssetsScreen.
3. Taps FAB (+) to open add dialog.
4. Enters name (required), details (optional).
5. Saves: ViewModel calls CreateAsset use case -> Repo -> DAO insert (auto-generates id as code).
6. Barcode auto-generated and displayed based on id.

#### Linking Asset to Room (BRD Ref: FR-003, US-003)
1. From LocationsScreen, tap a location to open RoomDetailScreen.
2. View shows assigned and unassigned assets.
3. Tap "Scan" FAB to open ScanModeSelector (TechDoc_V2 Ref: SCAN-001).
4. Choose barcode or RFID scanning mode.
5. For barcode: ZXing scanner launches, scan CODE_128.
6. For RFID: RfidScanner activates NFC, scan RFID tag (TechDoc_V2 Ref: RFID-001).
7. On scan success, assign asset to room via AssignAssetToRoom use case (with optional condition).
8. Alternatively, select from unassigned list.

#### Quick Asset Movement (BRD Ref: FR-008, US-008, US-009)
1. User taps "Scan" tile on home screen.
2. Opens QuickScanDialog with scan mode selector (TechDoc_V2 Ref: SCAN-001).
3. User chooses barcode or RFID scanning.
4. For barcode: ZXing scan of CODE_128 barcode.
5. For RFID: NFC scan of RFID tag containing asset ID (TechDoc_V2 Ref: RFID-001).
6. Dialog shows scanned code, condition input field, and location selector.
7. User selects target location and enters condition.
8. Confirms move: Calls AssignAssetToRoomWithCondition use case.
9. Movement recorded in audit trail.

#### Printing Barcode (BRD Ref: FR-004, US-004)
1. In AssetsScreen, tap print icon on asset row.
2. Calls printBarcode(context, asset.id.toString().padStart(6, '0'), asset.name).
3. Generates bitmap, connects to Bluetooth printer, sends ESC/POS commands.
4. Prints centered name, code (padded to 6 digits), and barcode image.

#### RFID Tag Programming (TechDoc_V2 Ref: RFID-003)
1. RFID tags should be programmed with the same asset ID as the barcode.
2. Use external RFID programmer to write asset ID to tags.
3. Tags use standard NFC format for Android compatibility.

#### Setting Base Location (BRD Ref: FR-002, US-006)
1. In AssetsScreen, add/edit asset dialog.
2. Dropdown shows list of rooms.
3. User selects base room (optional).
4. Saves: Sets baseRoomId in AssetEntity.

#### Viewing Audit Trail (BRD Ref: FR-007, US-007)
1. User taps "Audit Trail" tile on home screen.
2. Navigates to AuditTrailScreen.
3. Displays list of AssetMovement with asset name, from/to rooms, timestamp.

This documentation provides a complete overview of the application's business and technical aspects with RFID support. For updates, refer to the codebase files listed.</content>
<parameter name="filePath">d:\Easy2SolutionsProjects\EasyAndroidProject\AssetTracking\BRD_and_TechDoc_V2.md