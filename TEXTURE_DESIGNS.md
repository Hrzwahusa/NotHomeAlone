# Block Texture Design Guide

## Builder Station
**Theme**: Construction Tools + Crafting

### Level 1 (Basic Builder)
```
Top Face Layout (16x16):
┌─────────────────┐
│                 │
│   SHOVEL        │
│     ↑           │
│                 │
│ PICK  CRAFT  AXE│
│   ←      →      │
│                 │
│                 │
└─────────────────┘

Center: Crafting table texture
Middle Left: Wooden Pickaxe
Middle Top: Wooden Shovel  
Middle Right: Wooden Axe
Background: Brown planks

Colors:
- Background: Oak wood planks (#9C7F4E)
- Tools: Light brown wood (#C4A574)
- Tool heads: Gray (#8B8B8B)
```

### Level 2 (Stone Tools)
Same layout, but with stone tools (gray tool heads)

### Level 3 (Iron Tools)
Same layout, but with iron tools (white/light gray tool heads)

---

## Lumberjack Station
**Theme**: Axe + Trees

### Level 1
```
Top Face:
┌─────────────────┐
│   🌲   🌲   🌲   │  Tree symbols (green)
│                 │
│  AXE   LOG      │  Big axe + wood log
│                 │
│   🌲   🌲   🌲   │
└─────────────────┘

Center: Large wooden axe
Right: Oak log cross-section
Corners: Small tree symbols
Background: Dark oak planks
```

### Level 2: Stone Axe
### Level 3: Iron Axe

---

## Hunter Station  
**Theme**: Bow + Arrow + Leather

### Level 1
```
Top Face:
┌─────────────────┐
│                 │
│    BOW          │  Wooden bow
│     ||          │
│    ARROW        │  Arrow pointing
│                 │
│  LEATHER        │  Leather texture
└─────────────────┘

Center Top: Bow shape
Center: Arrow
Bottom: Leather pattern
Background: Spruce planks
```

### Level 2: Better bow
### Level 3: Enchanted bow glow

---

## Farmer Station
**Theme**: Hoe + Wheat + Animals

### Level 1
```
Top Face:
┌─────────────────┐
│  🌾  🌾  🌾    │  Wheat
│                 │
│   HOE   🐄      │  Hoe + Cow/Sheep
│                 │
│  🌾  🌾  🌾    │
└─────────────────┘

Center Left: Wooden hoe
Center Right: Animal (cow/sheep pixel art)
Top/Bottom: Wheat plants
Background: Farmland brown (#654321)
```

### Level 2: Stone hoe, more crops
### Level 3: Iron hoe, various animals

---

## Miner Station
**Theme**: Pickaxe + Ores

### Level 1
```
Top Face:
┌─────────────────┐
│  ⛏️  ⛏️       │  Pickaxe symbols
│                 │
│   PICK  ORE     │  Large pickaxe + coal ore
│                 │
│  💎  ⚡  🔥    │  Ore symbols
└─────────────────┘

Center: Large wooden pickaxe
Right: Stone/coal texture
Corners: Different ore dots (coal, iron, gold)
Background: Stone/cobblestone
```

### Level 2: Stone pickaxe, better ores
### Level 3: Iron pickaxe, diamond hints

---

## Fisher Station
**Theme**: Fishing Rod + Water + Fish

### Level 1
```
Top Face:
┌─────────────────┐
│ ≈≈≈ WATER ≈≈≈   │
│   ROD            │  Fishing rod
│     \\           │
│  🐟  🐟         │  Fish symbols
│ ≈≈≈≈≈≈≈≈≈≈≈     │
└─────────────────┘

Top: Water waves (blue wavy lines)
Center: Fishing rod
Bottom: Fish shapes
Background: Blue water (#4A90E2)
```

### Level 2: Better rod, more fish
### Level 3: Enchanted rod glow, treasure

---

## Storage Station
**Theme**: Chest + Items

### Level 1
```
Top Face:
┌─────────────────┐
│  📦   📦       │
│                 │
│   CHEST LOCK    │  Large chest with lock
│                 │
│  📦   📦       │
└─────────────────┘

Center: Large chest texture (like vanilla chest top)
Lock symbol in center
Corners: Small chest/barrel symbols
Background: Oak planks
```

### Level 2: Iron-reinforced chest
### Level 3: Gold-reinforced, glowing

---

## Blacksmith Station
**Theme**: Anvil + Hammer + Fire

### Level 1
```
Top Face:
┌─────────────────┐
│  🔨            │  Hammer
│                 │
│  ANVIL  FIRE    │  Anvil + flames
│                 │
│  ⚒️   ⚒️       │  Tools
└─────────────────┘

Center: Anvil top view
Left: Hammer
Right: Fire/furnace glow (orange)
Background: Dark gray stone
```

### Level 2: Iron anvil, brighter flames
### Level 3: Gold anvil, blue flames

---

## Color Palette Reference

### Wood Tones
- Light Oak: #C4A574
- Dark Oak: #6F4E37
- Spruce: #8B7355

### Stone/Metal
- Cobblestone: #7F7F7F
- Stone: #9D9D9D
- Iron: #D8D8D8
- Gold: #FCEE4B

### Nature
- Grass: #7CBD6B
- Water: #4A90E2
- Dirt: #8B6F47
- Leather: #A0522D

### Special
- Fire: #FFA500
- Coal: #2C2C2C
- Diamond: #5DADE2
- Wheat: #E8D68A

---

## Notes for Texture Creation

1. All textures should be 16x16 pixels
2. Use Minecraft's color palette for consistency
3. Keep designs simple and readable at small sizes
4. Level 1: Wood/basic materials
5. Level 2: Stone/upgraded materials  
6. Level 3: Iron/best materials + possible glow effects
7. Side textures should match the theme but be simpler
8. Bottom texture can be generic planks/stone
