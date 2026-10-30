# Fight Preset (Fabric, Minecraft 1.21.11)

Inventar-Presets für den Kampf: Du speicherst dein Inventar als Preset, und im Inventar siehst du dann, was stimmt und was fehlt.

## Funktionen

- **Grün** (leicht transparent): richtiges Item im Slot
- **Rot** (leicht transparent): falsches Item im Slot
- **Ghost-Item**: In leeren Slots, in die laut Preset etwas gehört, wird das Item schwach durchscheinend angezeigt (zusätzlich mit orangem Schimmer)
- **Preset-Menü**: Inventar einlesen, Preset benennen, Symbol wählen, löschen, Vorschau
- **An/Aus im Spiel**: Button im Inventar oder Taste
- Funktioniert in jedem Fenster mit Spieler-Inventar (Inventar, Kisten, Werkbank, ...), außer im Kreativ-Inventar

## Bedienung

| Was | Wie |
| --- | --- |
| Overlay an/aus | Button `Overlay: AN/AUS` oben links im Inventar, oder Taste **H** (in der Welt) |
| Preset wechseln | Mittlerer Button oben links im Inventar (wechselt zum nächsten Preset) |
| Menü öffnen | Button `Preset-Menü` im Inventar, oder Taste **J** (in der Welt) |

Die Tasten lassen sich unter Steuerung > "Fight Preset" ändern.

**Preset erstellen:** Inventar so einrichten, wie du es im Kampf willst, Menü öffnen, `Neu aus Inventar` klicken. Mit `Inventar übernehmen` überschreibst du das ausgewählte Preset mit deinem aktuellen Inventar.

Verglichen wird nur der Item-Typ (nicht Anzahl, Verzauberungen oder Tränke-Effekte).

Die Presets liegen in `config/fightpreset.json`.

## Bauen

Voraussetzung: Java 21.

```
./gradlew build
```

Die fertige Mod liegt dann in `build/libs/fightpreset-1.0.0.jar`. Zusammen mit Fabric Loader 0.18.x und Fabric API für 1.21.11 in den `mods`-Ordner legen.

Zum Entwickeln/Testen: `./gradlew runClient`.

Falls Gradle die Loom-Version `1.14-SNAPSHOT` nicht findet, passe `loom_version`, `fabric_version` und `yarn_mappings` in `gradle.properties` an die Werte von https://fabricmc.net/develop an.
