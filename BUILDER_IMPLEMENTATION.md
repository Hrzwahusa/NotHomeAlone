# Builder System Implementation

## What Was Implemented

### 1. Builder Entity Extensions
- **Inventory System:** 27-slot inventory for storing building materials
- **Build Task Management:** Tracks current construction task with prioritized steps
- **Material Requests:** Automatically detects missing materials and notifies nearby players
- **Block Placement:** Places blocks according to BuildTask with obstacle clearing
- **Cooldown System:** 10-tick delay between block placements for realistic building

### 2. AI Goals
**CollectMaterialsGoal (Priority 1):**
- Returns to station when inventory has less than 3 filled slots
- Transfers up to 16 items per slot from station to builder inventory
- Navigates back to station automatically

**BuildStructureGoal (Priority 2):**
- Navigates to build site
- Places blocks when within 3 blocks of target position
- Handles pathfinding and stuck detection
- Integrates with performWork() for actual placement

### 3. Station Container System
**StationBlockEntity:**
- Implements `Container` interface for inventory management
- Implements `MenuProvider` for GUI
- 9-slot crafting materials storage (27-slot display)
- Full NBT serialization for persistence

**BaseStationBlock:**
- **Shift + Right-Click:** Opens material storage GUI
- **Right-Click:** Upgrades station (existing functionality)

### 4. Build System
**BuildTask.java:**
- Stores prioritized build steps
- Calculates required materials
- Tracks completion progress
- Sorts steps by priority

**StructureAnalyzer.java:**
- Loads NBT structure files
- Identifies special blocks:
  - Ender Chest = Station marker (skipped)
  - Crafting Table = Priority 100
  - Dirt under crafting table = Priority 99
  - Foundation dirt = Priority 10
  - Walls/fencing = Priority 5
- Creates BuildTask from structure template

**BuilderManager.java:**
- Initializes build tasks when station is placed
- Loads structure files from resources
- Links builder entity to build task
- Marks structures as completed

**StructureLoader.java:**
- Loads NBT files using StructureTemplate API
- Places structures in world
- Handles structure rotations

### 5. Event Handler Integration
**StationEventHandler:**
- Spawns worker NPCs when station is placed
- Initializes builder task for Builder stations
- Links worker entity to station block
- Stores worker ID in station entity

### 6. Documentation
**STRUCTURE_GUIDE.md:**
- Complete guide for creating structure files
- Structure Block usage instructions
- Priority system explanation
- Design guidelines for all station types
- Example layouts

## How It Works

### Placing a Builder Station
1. Player crafts Builder Station
2. Player places block in world
3. Territory is claimed (12x12)
4. Builder NPC spawns at station
5. BuilderManager loads `builder_level_1.nbt`
6. StructureAnalyzer creates prioritized BuildTask
7. Builder receives BuildTask

### Building Process
1. **CollectMaterialsGoal:** Builder checks inventory
2. If inventory low (<3 slots filled), returns to station
3. Transfers materials from station inventory
4. **BuildStructureGoal:** Navigates to next build position
5. **performWork():** 
   - Checks for required material in inventory
   - Requests material if missing (chat message)
   - Clears obstacles (if needed)
   - Places block
   - Waits 10 ticks (cooldown)
   - Marks step as complete
6. Repeats until structure is complete

### Material Management
**Adding Materials:**
- Shift + Right-Click on station block
- Opens 3x9 container GUI
- Place blocks/items in inventory
- Builder will collect them automatically

**Missing Materials:**
- Builder sends chat message to nearby players (32 block radius)
- Message format: "[Builder] Missing material: [Block Name]"
- Each material only requested once per task

## Creating Structure Files

### Quick Start
1. Enter Creative mode
2. Build structure (9x9x5 recommended for Level 1)
3. Place **Ender Chest** where station block goes
4. Place Crafting Table for workspace
5. Use Structure Block in SAVE mode
6. Name: `builder_level_1`
7. Set size and offset
8. Click SAVE
9. Copy `.nbt` file to `src/main/resources/data/nothomealone/structures/`

### Priority System
Blocks are built in this order:
1. Crafting Table (100) - Built first for crafting capability
2. Dirt under crafting table (99) - Support structure
3. Foundation dirt (10) - Base layer
4. Walls/fencing (5) - Enclosure
5. Other blocks (1) - Decoration

### Special Blocks
- **Ender Chest:** Marker only, NOT built (builder skips it)
- **Crafting Table:** Critical workspace, built first
- **Dirt:** Checked if already present, skipped if found

## Testing

### In-Game Test
1. Build and compile mod
2. Launch Minecraft
3. Get Builder Station from creative tab
4. Place station in world
5. **Shift + Right-Click** to open materials GUI
6. Add dirt blocks and wooden planks
7. Builder spawns and starts building
8. Watch chat for material requests

### Debug Checklist
- [ ] Builder NPC spawns when station placed
- [ ] Structure file loads without errors
- [ ] Builder navigates to build positions
- [ ] Builder places blocks with cooldown
- [ ] Chat messages appear for missing materials
- [ ] Material GUI opens with Shift + Right-Click
- [ ] Builder collects materials from station
- [ ] Structure completes successfully
- [ ] Territory prevents overlapping stations

## File Structure
```
src/main/java/com/nothomealone/
├── entity/
│   ├── custom/
│   │   └── BuilderEntity.java          (Inventory, performWork, material requests)
│   └── ai/
│       ├── BuildStructureGoal.java      (Navigate and build)
│       └── CollectMaterialsGoal.java    (Return to station for materials)
├── structure/
│   ├── BuildTask.java                   (Task and step management)
│   ├── StructureAnalyzer.java           (NBT parsing, priority assignment)
│   ├── BuilderManager.java              (Task initialization)
│   └── StructureLoader.java             (NBT loading)
├── block/
│   ├── custom/BaseStationBlock.java     (Container GUI opening)
│   └── entity/StationBlockEntity.java   (Container implementation)
└── event/
    └── StationEventHandler.java         (NPC spawning, task init)

src/main/resources/data/nothomealone/structures/
└── builder_level_1.nbt                  (Structure file - TO BE CREATED)
```

## Next Steps

### Required Before Testing
1. **Create Structure Files:**
   - `builder_level_1.nbt`
   - `builder_level_2.nbt`
   - `builder_level_3.nbt`
   - See STRUCTURE_GUIDE.md for instructions

### Future Enhancements
2. **Builder Enhancements:**
   - Crafting system (builder crafts missing materials)
   - Repair system (fixes damaged structures)
   - Multiple structures per station level
   - Structure selection GUI

3. **Other Workers:**
   - Lumberjack: Tree chopping AI
   - Miner: Mining AI (vertical shafts)
   - Farmer: Crop planting/harvesting
   - Hunter: Animal hunting/breeding
   - Fisher: Fishing AI
   - Storage: Item collection/organization
   - Blacksmith: Item repair/crafting

4. **Polish:**
   - Worker animations
   - Tool usage animations
   - Sound effects
   - Particle effects
   - Station textures (24 needed)
   - Worker models/textures

## Known Limitations
- Builder can only build one structure at a time
- Materials must be manually added to station
- No automatic resource gathering yet
- Structure files must be created manually
- No structure rotation support yet
- Builder doesn't craft missing materials yet
