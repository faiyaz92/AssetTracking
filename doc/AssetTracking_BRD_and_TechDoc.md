# Asset Tracking Application - BRD and Technical Documentation

## Table of Contents
1. [Business Requirements Document (BRD)](#business-requirements-document-brd)
   - [Introduction](#introduction)
   - [Business Objectives](#business-objectives)
   - [Functional Requirements](#functional-requirements)
   - [User Stories](#user-stories)
   - [Non-Functional Requirements](#non-functional-requirements)
2. [Technical Documentation](#technical-documentation)
   - [Architecture Overview](#architecture-overview)
   - [Technology Stack](#technology-stack)
   - [Data Model](#data-model)
   - [Key Components](#key-components)
   - [File References](#file-references)
   - [Workflows](#workflows)

---

## Business Requirements Document (BRD)

### Introduction
The Asset Tracking Application is a mobile solution designed for Android devices to manage and track physical assets within rooms or locations. The app allows users to create rooms, add assets with unique barcodes, link assets to rooms, and print barcodes on thermal printers for physical labeling.

### Business Objectives
- Provide an easy-to-use interface for inventory management.
- Enable barcode generation and printing for asset identification.
- Support room-based organization of assets.
- Ensure data persistence and offline functionality.
- Facilitate quick asset search and assignment.

### Functional Requirements

#### FR-001: Room Management
- Users can create, edit, and delete rooms.
- Each room has a name and optional description.
- Rooms display the count of assigned assets.

#### FR-002: Asset Management
- Users can create, edit, and delete assets.
- Each asset has a unique code, name, optional details, base location (room), and current location.
- Assets can be assigned to rooms or remain unassigned.
- Assets display generated barcodes.
- Base location is set during creation (optional); current location updates on scans.

#### FR-003: Asset-Room Linking
- Assets can be assigned to rooms via room detail screens.
- Users can scan barcodes to assign assets to rooms (updates current location).
- Assets can be detached from rooms.
- Base location remains unchanged; current location tracks latest assignment.

#### FR-007: Audit Trail
- Track all asset movements with timestamps.
- Audit trail screen shows history of room changes for each asset.
- Includes from room, to room, and timestamp.

#### FR-004: Barcode Generation and Printing
- Automatic CODE_128 barcode generation for each asset based on its code.
- Print functionality via Bluetooth-connected 58mm thermal printers.
- Print layout includes asset name, code, and barcode image.

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
- Acceptance Criteria: Asset code and name are required; details are optional; barcode is auto-generated.

#### US-003: As a user, I want to assign an asset to a room so that I know its location.
- Acceptance Criteria: From room detail, scan barcode or select asset to assign.

#### US-004: As a user, I want to print a barcode for an asset so that I can label it physically.
- Acceptance Criteria: Print button on asset row; connects to paired Bluetooth printer.

#### US-005: As a user, I want to search for assets so that I can find them quickly.
- Acceptance Criteria: Search field in assets screen; filters by name/code.

#### US-006: As a user, I want to set a base location for an asset so that I know its home room.
- Acceptance Criteria: Dropdown in add/edit asset dialog; optional selection from rooms.

#### US-007: As a user, I want to view the audit trail of asset movements so that I can track history.
- Acceptance Criteria: Third tile on home screen; list of movements with timestamps.

### Non-Functional Requirements
- **Performance**: App should load data quickly (<2s for lists).
- **Usability**: Intuitive UI with Material3 design.
- **Security**: No sensitive data; local storage only.
- **Compatibility**: Android API 23+.
- **Reliability**: Error handling with user feedback via toasts/snackbars.

---

## Technical Documentation

### Architecture Overview
The application follows Clean Architecture principles with separation into layers:
- **Presentation Layer**: Compose UI screens, ViewModels.
- **Domain Layer**: Use cases, models, repository interfaces.
- **Data Layer**: Room DAOs, repositories, entities.

Dependency injection via Hilt. Navigation handled by Compose Navigation.

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Database**: Room (SQLite)
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Barcode**: ZXing for generation, ESCPOS library for printing
- **Build Tool**: Gradle (AGP 7.3.1, Kotlin 1.8.20)

### Data Model

#### Entities
- **RoomEntity**: id (Long), name (String), description (String?)
- **AssetEntity**: id (Long), code (String), name (String), details (String?), baseRoomId (Long?), currentRoomId (Long?)
- **AssetMovementEntity**: id (Long), assetId (Long), fromRoomId (Long?), toRoomId (Long), timestamp (Long)

#### Models
- **RoomSummary**: id, name, description, assetCount
- **AssetSummary**: id, code, name, details, baseRoomId, baseRoomName, currentRoomId, currentRoomName
- **AssetMovement**: id, assetId, assetName, fromRoomName, toRoomName, timestamp

### Key Components

#### Presentation Layer
- **AssetTrackingApp**: Main composable with NavHost.
- **HomeScreen**: Landing page with tiles for Rooms, Assets, and Audit Trail.
- **RoomsScreen**: List of rooms with CRUD dialogs.
- **AssetsScreen**: List of assets with search, CRUD dialogs, print button.
- **RoomDetailScreen**: Room details with assigned/unassigned assets, scan to assign.
- **AuditTrailScreen**: List of asset movements with timestamps.

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

### File References

#### Core Files
- `app/src/main/java/com/example/assettracking/AssetTrackingApplication.kt`: Hilt application class.
- `app/src/main/java/com/example/assettracking/MainActivity.kt`: Main activity with Compose setContent.
- `app/src/main/AndroidManifest.xml`: Permissions and app config.

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

#### DI
- `app/src/main/java/com/example/assettracking/di/DatabaseModule.kt`: Room DB module.
- `app/src/main/java/com/example/assettracking/di/RepositoryModule.kt`: Repo modules.

#### Utils
- `app/src/main/java/com/example/assettracking/util/BarcodeGenerator.kt`: Barcode generation.
- `app/src/main/java/com/example/assettracking/util/ThermalPrinter.kt`: Printing utility.

#### Build Files
- `app/build.gradle`: App dependencies and config.
- `build.gradle`: Project plugins.
- `settings.gradle`: Repos and includes.

### Workflows

#### Adding a Room (BRD Ref: FR-001, US-001)
1. User taps "Rooms" tile on home screen.
2. Navigates to RoomsScreen.
3. Taps FAB (+) to open add dialog.
4. Enters name (required) and description (optional).
5. Saves: ViewModel calls CreateRoom use case -> Repo -> DAO insert.
6. UI updates via Flow.

#### Adding an Asset (BRD Ref: FR-002, US-002)
1. User taps "Assets" tile on home screen.
2. Navigates to AssetsScreen.
3. Taps FAB (+) to open add dialog.
4. Enters code (required), name (required), details (optional).
5. Saves: ViewModel calls CreateAsset use case -> Repo -> DAO insert.
6. Barcode auto-generated and displayed.

#### Linking Asset to Room (BRD Ref: FR-003, US-003)
1. From RoomsScreen, tap a room to open RoomDetailScreen.
2. View shows assigned and unassigned assets.
3. Tap "Scan" to use camera for barcode scan.
4. On scan success, assign asset to room via AssignAssetToRoom use case.
5. Alternatively, select from unassigned list.

#### Printing Barcode (BRD Ref: FR-004, US-004)
1. In AssetsScreen, tap print icon on asset row.
2. Calls printBarcode(context, asset.code, asset.name).
3. Generates bitmap, connects to Bluetooth printer, sends ESC/POS commands.
4. Prints centered name, code, and barcode image.

#### Setting Base Location (BRD Ref: FR-002, US-006)
1. In AssetsScreen, add/edit asset dialog.
2. Dropdown shows list of rooms.
3. User selects base room (optional).
4. Saves: Sets baseRoomId in AssetEntity.

#### Viewing Audit Trail (BRD Ref: FR-007, US-007)
1. User taps "Audit Trail" tile on home screen.
2. Navigates to AuditTrailScreen.
3. Displays list of AssetMovement with asset name, from/to rooms, timestamp.

This documentation provides a complete overview of the application's business and technical aspects. For updates, refer to the codebase files listed.