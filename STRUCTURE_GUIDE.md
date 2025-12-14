# Building Structures for Not Home Alone Mod

## Creating Structure Files

### Requirements
- Minecraft 1.20.1 in Creative Mode
- Structure Blocks (use `/give @s structure_block`)

### Process

#### 1. Build Your Structure in Creative
Build the structure you want to save. For example, for the Builder Level 1 structure:
- Create a dirt foundation (9x9 recommended)
- Place a Crafting Table where the Builder will work
- Place an **Ender Chest** at the exact position where the station block should be

#### 2. Mark Station Position with Ender Chest
**Important:** The Ender Chest acts as a marker for where the station block will be placed. The Builder will NOT build an Ender Chest - it just marks the position and will be skipped during construction.

#### 3. Place Structure Block
1. Get a Structure Block: `/give @s structure_block`
2. Place it at one corner of your structure
3. Right-click to open the Structure Block GUI
4. **Set mode to SAVE** (very important!)

#### 4. Configure Structure Block
- **Structure Name:** Enter the path where to save
  - For Builder Level 1: `builder_level_1`
  - For Builder Level 2: `builder_level_2`
  - For Builder Level 3: `builder_level_3`
  - Other stations: `lumberjack_level_1`, etc.
  
- **Relative Position:** Set the offset from structure block to corner of your structure
  - Example: If structure block is at origin and structure starts 2 blocks up: `0 -2 0`
  
- **Structure Size:** Set width/height/depth of your structure
  - Example: For 9x9x5 structure: `9 5 9`
  
- **Show Bounding Box:** Enable this to visualize what will be saved (optional)
- **Include Entities:** Disable (we don't want to save entities)

#### 5. Save the Structure
1. Click **SAVE**
2. Structure is saved to: `.minecraft/saves/[WorldName]/generated/nothomealone/structures/`

#### 6. Copy to Mod Resources
Copy the `.nbt` file from the save folder to:
```
src/main/resources/data/nothomealone/structures/
```

## Structure Design Guidelines

### Builder Structures
- **Foundation:** Use Dirt blocks (will be placed first, priority 10)
- **Work Area:** Place Crafting Table (priority 100)
- **Station Marker:** Place Ender Chest at station location (will be skipped)
- **Support:** Dirt under Crafting Table gets priority 99
- **Walls/Fencing:** Normal blocks get priority 5

### Priority System
The Builder will construct blocks in this order:
1. **Priority 100:** Crafting Table (critical workspace)
2. **Priority 99:** Dirt directly under Crafting Table
3. **Priority 10:** Foundation dirt blocks
4. **Priority 5:** Walls, fencing, decoration
5. **Priority 1:** Everything else

### Special Blocks
- **Ender Chest:** Station marker, NOT built (skipped automatically)
- **Crafting Table:** Built first to allow crafting of materials
- **Dirt:** Foundation material, checked if already present before placing

### Size Recommendations
- **Level 1:** 9x9x5 (small hut)
- **Level 2:** 15x15x7 (medium house)
- **Level 3:** 21x21x10 (large building)

Keep structures within the territory radius:
- Most stations: 12x12 claim
- Builder: 12x12 claim
- Farmer: 24x24 claim

## Example Structure Layout

### Builder Level 1 (9x9x5)
```
Layer 0 (Foundation):
D D D D D D D D D
D D D D D D D D D
D D D D D D D D D
D D D D E D D D D  <- E = Ender Chest (station marker)
D D D D D D D D D
D D D D D D D D D
D D D D D D D D D
D D D D D D D D D
D D D D D D D D D

Layer 1:
. . . . . . . . .
. W W W W W W W .
. W . . . . . W .
. W . . C . . W .  <- C = Crafting Table
. W . . . . . W .
. W . . . . . W .
. W W D W W W W .  <- D = Door
. . . . . . . . .
. . . . . . . . .

(W = Wooden Planks, . = Air)
```

## Testing Your Structure

### In-Game Testing
1. Place a Builder Station block
2. Add dirt and wood planks to the station inventory (Shift + Right-Click)
3. Builder NPC will spawn and start building
4. Watch chat for missing material messages
5. Add any missing materials to the station inventory

### Debugging
- Check console for structure loading errors
- Verify the `.nbt` file is in the correct resources folder
- Ensure Ender Chest is placed at the correct position
- Check that all materials are available in station inventory

## Creating Structures for Other Stations

### Lumberjack
- Storage shed for logs
- Crafting area for tools
- Fence around work area

### Miner
- Mining shaft entrance
- Storage for ores
- Smelting area

### Farmer
- Farmland plots (9x9 each)
- Water sources
- Storage for crops
- Fence around farm

### Hunter
- Small shelter
- Archery target
- Storage for leather/meat

### Fisher
- Dock extending into water
- Boat storage
- Fishing rod rack

### Blacksmith
- Forge with furnace
- Anvil workspace
- Storage for metal

### Storage
- Large warehouse
- Multiple storage chests
- Organized shelving
