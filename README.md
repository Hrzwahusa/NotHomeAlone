# NotHomeAlone - Minecraft Settlement Mod

Eine Minecraft 1.20.1 Forge Mod, die es ermöglicht Siedlungen mit NPCs zu bauen, die verschiedene Arbeiten ausführen.

## Features

### NPC-Arbeiter
- **Holzfäller/Förster**: Fällt Bäume und pflanzt Setzlinge
- **Jäger**: Jagt Tiere für Nahrung und Ressourcen
- **Bauer/Viehzüchter**: Baut Pflanzen an und züchtet Tiere
- **Baumeister**: Konstruiert und repariert Gebäude
- **Minenarbeiter**: Baut Erze und Ressourcen unter Tage ab
- **Fischer**: Fängt Fische aus Wasserquellen

## Entwicklung

### Voraussetzungen
- Java JDK 17 oder höher
- Minecraft 1.20.1
- Forge 47.3.0

### Projekt aufsetzen

1. **Gradle Wrapper installieren** (falls noch nicht vorhanden):
```powershell
gradle wrapper
```

2. **Projekt kompilieren**:
```powershell
.\gradlew build
```

3. **Minecraft Client starten** (zum Testen):
```powershell
.\gradlew runClient
```

4. **Minecraft Server starten** (zum Testen):
```powershell
.\gradlew runServer
```

### Projektstruktur

```
src/main/java/com/nothomealone/
├── NotHomeAlone.java              # Haupt-Mod-Klasse
└── entity/
    ├── ModEntities.java           # Entity-Registrierung
    └── custom/
        ├── WorkerEntity.java      # Basis-Klasse für alle Arbeiter
        ├── LumberjackEntity.java  # Holzfäller
        ├── HunterEntity.java      # Jäger
        ├── FarmerEntity.java      # Bauer
        ├── BuilderEntity.java     # Baumeister
        ├── MinerEntity.java       # Minenarbeiter
        └── FisherEntity.java      # Fischer
```

## Nächste Schritte

1. Entity-Attribute und AI-Ziele implementieren
2. Spawn-Eier für alle NPCs erstellen
3. Siedlungsverwaltungssystem entwickeln
4. Arbeits-Aufgaben für jeden NPC-Typ implementieren
5. Inventarsystem für NPCs hinzufügen
6. GUI für NPC-Interaktion erstellen

## Lizenz

MIT License
