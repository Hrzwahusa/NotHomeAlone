# Territory & Work Area Configuration

## Overview
Each station has two important areas:
1. **Territory** (Claim Area) - Where no other stations can be placed
2. **Work Area** - Where the NPC can perform their tasks (varies by worker type)

## Station Configurations

### Settlement Management (Must Reach All Stations)
| Station      | Territory Radius | Territory Size | Work Radius | Work Area Size | Special Behavior                                    |
|--------------|------------------|----------------|-------------|----------------|-----------------------------------------------------|
| Builder      | 6 blocks         | 12x12          | 128 blocks  | 256x256        | Must reach all stations to build/repair structures  |
| Storage      | 12 blocks        | 24x24          | 128 blocks  | 256x256        | Must reach all stations to collect/distribute items |

### Resource Gathering (Large Work Areas)
| Station      | Territory Radius | Territory Size | Work Radius | Work Area Size | Special Behavior                        |
|--------------|------------------|----------------|-------------|----------------|----------------------------------------|
| Hunter       | 6 blocks         | 12x12          | 96 blocks   | 192x192        | Very large range - animals are far away |
| Lumberjack   | 6 blocks         | 12x12          | 48 blocks   | 96x96          | Needs space to find trees               |
| Fisher       | 6 blocks         | 12x12          | 32 blocks   | 64x64          | Works around water sources              |

### Claim-Based Workers (Work Within Territory)
| Station      | Territory Radius | Territory Size | Work Radius | Work Area Size | Special Behavior                              |
|--------------|------------------|----------------|-------------|----------------|-----------------------------------------------|
| Farmer       | 12 blocks        | 24x24          | 12 blocks   | 24x24          | Works ONLY within farm territory              |
| Blacksmith   | 6 blocks         | 12x12          | 6 blocks    | 12x12          | Stays exclusively within claim at forge       |

### Special Case: Miner (Vertical Mining)
| Station | Territory Radius | Territory Size | Work Radius | Initial Area | Special Behavior                                         |
|---------|------------------|----------------|-------------|--------------|----------------------------------------------------------|
| Miner   | 6 blocks         | 12x12          | 6 blocks    | 12x12        | Starts mining UNDER claim, expands downward/outward      |

## Important Notes

### Territory (Claim Area)
- Prevents other stations from being placed in overlapping areas
- Defines where the station's base structure will be built
- Storage and Farmer stations have larger claim areas (24x24) for their operations

### Work Area Strategies

#### Settlement-Wide Workers (Builder & Storage)
- **MUST reach all other stations** in the settlement
- Work radius: 128 blocks (256x256 area)
- Can dynamically find and path to any station
- If stations are placed too far apart, these workers will warn players

#### Roaming Workers (Hunter, Lumberjack, Fisher)
- Large work areas to find resources that spawn naturally
- Hunter has largest range (192x192) - animals can be very far away
- These workers will wander within their radius to find targets

#### Territory-Bound Workers (Farmer, Blacksmith)
- Work ONLY within their claimed territory
- Farmer: 24x24 farm - plants and tends crops/animals here
- Blacksmith: 12x12 area - stays at forge to craft items

#### Special: Miner (Vertical Worker)
- Starts mining directly UNDER the station claim (6 block radius)
- Mines vertically downward up to 64 blocks deep
- Expands horizontally as depth increases (1 block per 8 levels)
- By max depth, has ~14 block radius underground

### Design Philosophy
- Small territories allow dense settlements without overlap conflicts
- Work areas are tailored to each NPC's actual needs
- Builder/Storage act as settlement coordinators - must reach everyone
- Resource gatherers roam widely, specialized workers stay focused
- Only territories block placement, work areas can freely overlap
