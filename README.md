# Rutschentacho

Minimale native Android-App fur kurze GPS-basierte Geschwindigkeitsmessungen, gedacht fur spielerische Anwendungsfalle wie das Messen einer Rutschfahrt.

## Aktueller Stand

Schritt 3 ist umgesetzt:

- Android-Projektgrundgerust
- App-Modul mit Kotlin und Jetpack Compose
- Laufzeitabfrage fur die genaue Standortberechtigung
- Behandlung der Zustande fur aktivierte GPS- und Standortdienste
- Hochfrequente Fused-Location-Updates, solange die App im Vordergrund ist
- Live-Anzeige der aktuellen Geschwindigkeit und der aktuellen Genauigkeit im Bereitschaftszustand

Distanz- und Sitzungsstatistiken, GNSS-Satellitenstatus und Sitzungssteuerung sind noch nicht umgesetzt.

## Geplante App-Funktionen

- Aktuelle Geschwindigkeit in `km/h`
- Zuruckgelegte Distanz pro Sitzung
- Durchschnittsgeschwindigkeit
- Hochstgeschwindigkeit
- GPS-Statusleiste mit Fix-Qualitat, Satelliten und Genauigkeit
- Steuerung zum Starten, Stoppen und Zurucksetzen

## Bauen und Starten

Dieses Repository enthalt derzeit das App-Grundgerust sowie den Ablauf fur Berechtigungen und GPS-Bereitschaft.

So startest du die App:

1. Projekt in Android Studio offnen.
2. Android Studio die erforderlichen Android-SDK- und Gradle-Komponenten installieren lassen.
3. Die Konfiguration `app` auf einem Emulator oder Android-Gerat bauen und starten.

Falls du lieber uber die Kommandozeile baust, richte zuerst den Gradle-Wrapper ein und fuhre dann aus:

```bash
./gradlew assembleDebug
```

## So testest du Schritt 3

1. Starte die App auf einem Gerat oder Emulator mit Google-Play-Diensten oder normalen Android-Standorteinstellungen.
2. Prufe beim ersten Start, dass die App nach der genauen Standortberechtigung fragt.
3. Lehne die Berechtigung einmal ab und prufe, dass weiterhin der Berechtigungszustand mit erneutem Button angezeigt wird.
4. Lehne die Berechtigung dauerhaft ab und prufe, dass die App einen Button zu den App-Einstellungen anbietet.
5. Erteile die Berechtigung bei deaktivierten Standortdiensten und prufe, dass der GPS-deaktiviert-Zustand erscheint.
6. Aktiviere die Standortdienste, kehre zur App zuruck und prufe, dass ohne Neustart in den Bereitschaftszustand gewechselt wird.
7. Bewege dich im Freien oder nutze Mock-Standorte und prufe, dass sich der Geschwindigkeitswert bei eingehenden Updates von `0 km/h` verandert.
8. Prufe, dass sich die Genauigkeitsanzeige in Metern aktualisiert und die App im Hintergrund keine Updates mehr verarbeitet.

## Hinweis zur Genauigkeit

Die App ist fur sehr kurze Messungen gedacht. GPS-basierte Geschwindigkeit und Distanz konnen auf kurzen Strecken verrauscht sein, besonders bei schlechter Satellitensicht oder schwacher Genauigkeit. In spateren Schritten werden Satelliten- und Genauigkeitsinformationen angezeigt, damit sich Ergebnisse besser einordnen lassen.
