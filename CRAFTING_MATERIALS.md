# Station Crafting Materials & NPC Equipment

## How it works:
1. Player crafts a station using specific materials
2. When placed, materials are stored in the station's BlockEntity
3. Builder NPC can extract these materials to build the structure
4. Worker NPC spawns with their tool(s) from the recipe

---

## Builder Station
**Recipe:**
```
   [Wooden Shovel]
[Wooden Pickaxe] [Crafting Table] [Wooden Axe]
```

**Builder gets:**
- Wooden Pickaxe (for breaking blocks)
- Wooden Shovel (for digging)
- Wooden Axe (for wood)

**Builder uses for construction:**
- Crafting Table (for crafting structure materials)

---

## Lumberjack Station
**Recipe:**
```
[Log] [Log] [Log]
[Log] [Wooden Axe] [Log]
[Log] [Log] [Log]
```

**Lumberjack gets:**
- Wooden Axe (for chopping trees)

**Builder uses for construction:**
- 8x Logs (fence, structure)

---

## Miner Station
**Recipe:**
```
[Cobblestone] [Cobblestone] [Cobblestone]
[Cobblestone] [Wooden Pickaxe] [Cobblestone]
[Cobblestone] [Cobblestone] [Cobblestone]
```

**Miner gets:**
- Wooden Pickaxe (for mining)

**Builder uses for construction:**
- 8x Cobblestone (structure, walls)

---

## Hunter Station
**Recipe:**
```
[Leather] [Leather] [Leather]
[Leather] [Bow] [Leather]
[Leather] [Arrow] [Leather]
```

**Hunter gets:**
- Bow (for hunting)
- Arrow (ammunition)

**Builder uses for construction:**
- 7x Leather (decorative)

---

## Farmer Station
**Recipe:**
```
   [Water Bucket]
   [Wooden Hoe]
   [Wheat Seeds]
```

**Farmer gets:**
- Wooden Hoe (for tilling)
- Wheat Seeds (for planting)
- Water Bucket (for irrigation)

**Builder uses for construction:**
- (Materials used for NPC equipment)

---

## Storage Station
**Recipe:**
```
[Chest] [Chest] [Chest]
[Chest] [Chest] [Chest]
[Chest] [Chest] [Chest]
```

**Storage Keeper gets:**
- (No tools needed)

**Builder uses for construction:**
- 9x Chests (storage area)

---

## Blacksmith Station
**Recipe:**
```
[Cobble] [Furnace] [Cobble]
[Lava Bucket] [Anvil] [Flint & Steel]
[Cobble] [Cobble] [Cobble]
```

**Blacksmith gets:**
- Flint and Steel (for forge)
- (Anvil used at station)

**Builder uses for construction:**
- Furnace (smelting)
- Lava Bucket (forge fuel)
- Anvil (workstation)
- 5x Cobblestone (structure)

---

## Fisher Station
**Recipe:**
```
   
   [Fishing Rod]
   [Oak Boat]
```

**Fisher gets:**
- Fishing Rod (for fishing)
- Oak Boat (for water access)

**Builder uses for construction:**
- (Materials used for NPC equipment)

---

## Implementation Notes

### StationBlockEntity
- Stores `craftingMaterials` (9 slots)
- `getCraftingMaterials()` - Builder accesses materials
- `hasMaterials()` - Check if materials available
- `clearMaterials()` - After builder uses them

### NPC Spawning
When NPC spawns, they receive tools from the recipe:
```java
// Example for Lumberjack
ItemStack axe = new ItemStack(Items.WOODEN_AXE);
npc.setItemInHand(InteractionHand.MAIN_HAND, axe);
```

### Builder Logic
Builder can query the station for materials:
```java
if (stationEntity.hasMaterials()) {
    NonNullList<ItemStack> materials = stationEntity.getCraftingMaterials();
    // Use materials to build structure
    stationEntity.clearMaterials();
}
```
