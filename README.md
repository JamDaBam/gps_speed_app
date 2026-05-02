# Rutschentacho

Minimale native Android-App fur kurze GPS-basierte Geschwindigkeitsmessungen, gedacht fur spielerische Anwendungsfalle wie das Messen einer Rutschfahrt.

## Aktueller Stand

Schritt 4 ist umgesetzt:

- Android-Projektgrundgerust
- App-Modul mit Kotlin und Jetpack Compose
- Laufzeitabfrage fur die genaue Standortberechtigung
- Behandlung der Zustande fur aktivierte GPS- und Standortdienste
- Hochfrequente Fused-Location-Updates, solange die App im Vordergrund ist
- Live-Anzeige der aktuellen Geschwindigkeit und der aktuellen Genauigkeit
- Sitzungssteuerung mit `Start`, `Stopp` und `Zurucksetzen`
- Sitzungsstatistiken fur Distanz, Durchschnitt und Maximum

GNSS-Satellitenstatus und eine echte GPS-Statusleiste sind noch nicht umgesetzt.

## Geplante App-Funktionen

- Aktuelle Geschwindigkeit in `km/h`
- Zuruckgelegte Distanz pro Sitzung
- Durchschnittsgeschwindigkeit
- Hochstgeschwindigkeit
- GPS-Statusleiste mit Fix-Qualitat, Satelliten und Genauigkeit
- Steuerung zum Starten, Stoppen und Zurucksetzen

## Bauen und Starten

Dieses Repository enthalt derzeit das App-Grundgerust, den Ablauf fur Berechtigungen und GPS-Bereitschaft sowie die erste Sitzungslogik.

So startest du die App:

1. Projekt in Android Studio offnen.
2. Android Studio die erforderlichen Android-SDK- und Gradle-Komponenten installieren lassen.
3. Die Konfiguration `app` auf einem Emulator oder Android-Gerat bauen und starten.

Falls du lieber uber die Kommandozeile baust, richte zuerst den Gradle-Wrapper ein und fuhre dann aus:

```bash
./gradlew assembleDebug
```

## So testest du Schritt 4

1. Starte die App auf einem Gerat oder Emulator mit Google-Play-Diensten oder normalen Android-Standorteinstellungen.
2. Prufe beim ersten Start, dass die App nach der genauen Standortberechtigung fragt.
3. Lehne die Berechtigung einmal ab und prufe, dass weiterhin der Berechtigungszustand mit erneutem Button angezeigt wird.
4. Lehne die Berechtigung dauerhaft ab und prufe, dass die App einen Button zu den App-Einstellungen anbietet.
5. Erteile die Berechtigung bei deaktivierten Standortdiensten und prufe, dass der GPS-deaktiviert-Zustand erscheint.
6. Aktiviere die Standortdienste, kehre zur App zuruck und prufe, dass ohne Neustart in den Bereitschaftszustand gewechselt wird.
7. Prufe im Bereitschaftszustand, dass die aktuelle Geschwindigkeit und Genauigkeit auch ohne laufende Messung aktualisiert werden.
8. Starte eine Messung und bewege dich im Freien oder mit Mock-Standorten. Prufe, dass Distanz, Durchschnitt und Maximum wahrend der laufenden Sitzung aktualisiert werden.
9. Stoppe die Messung und prufe, dass die Sitzungswerte eingefroren bleiben, wahrend die aktuelle Geschwindigkeit weiter aktualisiert wird.
10. Starte erneut und prufe, dass eine neue Sitzung bei `0` beginnt.
11. Setze zuruck und prufe, dass Distanz, Durchschnitt und Maximum wieder auf den Ausgangszustand gehen.
12. Prufe, dass die App im Hintergrund keine Updates mehr verarbeitet.

## Hinweis zur Genauigkeit

Die App ist fur sehr kurze Messungen gedacht. GPS-basierte Geschwindigkeit und Distanz konnen auf kurzen Strecken verrauscht sein, besonders bei schlechter Satellitensicht oder schwacher Genauigkeit. Die bisherige Distanzberechnung zahlt nur Punkte mit brauchbarer Genauigkeit, um grobe Ausreisser etwas zu begrenzen. In spateren Schritten werden Satelliten- und Genauigkeitsinformationen noch sichtbarer aufbereitet, damit sich Ergebnisse besser einordnen lassen.
