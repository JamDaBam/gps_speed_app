# Sauseschaf

Sauseschaf ist eine kleine native Android-App für kurze GPS-basierte Geschwindigkeitsmessungen, gedacht für spielerische Anwendungsfälle wie das Messen einer Rutschfahrt.

## Funktionsumfang

- Aktuelle Geschwindigkeit groß in `m/s` mit zusätzlicher Anzeige in `km/h`
- Sitzungsmetriken für Distanz, Durchschnitt und Maximum
- GPS-Statusleiste mit Satelliten, Fix-Qualität und Genauigkeit
- Sitzungssteuerung mit `Start`, `Stopp` als Pause und `Reset`
- Fortsetzung einer pausierten Messung bis zum expliziten `Reset`
- Wiederherstellung des letzten Messstands nach App-Neustart

## Technik

- Kotlin
- Native Android-App mit Jetpack Compose
- Google Play Services Fused Location Provider für schnelle Standortupdates
- GNSS-Status über `LocationManagerCompat`
- Lokale Snapshot-Speicherung über `SharedPreferences`

## Entwicklung

Projekt in Android Studio öffnen und die Konfiguration `app` auf einem echten Android-Gerät starten. Für kurze GPS-Messungen ist ein Emulator nur eingeschränkt aussagekräftig.

Wichtige Gradle-Befehle:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew clean
```

## Berechtigungen und Voraussetzungen

- Genaue Standortberechtigung
- Aktivierte Standortdienste
- Möglichst freier Himmel für brauchbare GPS-Werte

Fehlende Berechtigungen und deaktivierte Standortdienste behandelt die App direkt in der Oberfläche inklusive Weiterleitung in die passenden Systemeinstellungen.

## Datenschutz und lokaler Zustand

- Die App sendet keine Messdaten an einen Server.
- Der aktuelle Messstand wird lokal auf dem Gerät gespeichert, damit eine laufende oder pausierte Sitzung nach einem App-Neustart wiederhergestellt werden kann.
- `Reset` verwirft den gespeicherten Sitzungsstand.
- Android-Backups sind absichtlich deaktiviert, damit keine standortnahen Zustandsdaten über System-Backups oder Device Transfer exportiert werden.
- Zusätzlich wird nur gespeichert, ob die Standortberechtigung bereits angefragt wurde, damit der Berechtigungsfluss sinnvoll reagiert.

## Messlogik

- Live-Werte verwenden nur aktuelle, zeitlich vorwärts laufende und nicht als Mock markierte Standortproben mit gültigen Koordinaten.
- Die Live-Anzeige bevorzugt frische Fused-Location-Proben und aktualisiert Geschwindigkeit sowie Genauigkeit fortlaufend.
- `Start` beginnt aus `Bereit` eine neue Sitzung und setzt nach `Stopp` dieselbe Sitzung fort.
- `Stopp` pausiert nur. Die Durchschnittsgeschwindigkeit basiert ausschließlich auf aktiver Messzeit.
- Distanz, Durchschnitt und Maximum ignorieren Proben mit schlechter Genauigkeit sowie unrealistische Positionssprunge.
- Dadurch reagiert die App defensiver auf GPS-Rauschen, ersetzt aber keine spezialisierte Messtechnik.

## Testhinweise

1. Beim ersten Start prüfen, dass die App nach der genauen Standortberechtigung fragt.
2. Berechtigung ablehnen und prüfen, dass der Hinweiszustand mit erneutem Button sichtbar bleibt.
3. Berechtigung dauerhaft ablehnen und prüfen, dass ein Sprung in die App-Einstellungen angeboten wird.
4. Berechtigung erteilen, Standortdienste deaktivieren und prüfen, dass der GPS-Hinweiszustand erscheint.
5. Standortdienste aktivieren und prüfen, dass die App in den Messbildschirm wechselt.
6. Im Freien prüfen, dass aktuelle Geschwindigkeit, Genauigkeit und GPS-Status fortlaufend aktualisiert werden.
7. Eine Messung starten und prüfen, dass Distanz, Durchschnitt und Maximum bei plausibler Bewegung steigen.
8. Die Messung stoppen und prüfen, dass die Sitzung pausiert und die Werte stehen bleiben.
9. Nach `Stopp` wieder `Start` drücken und prüfen, dass dieselbe Sitzung mit vorhandener Distanz, Durchschnitt und Maximum fortgesetzt wird.
10. Während einer Pause kurz warten und dann fortsetzen; prüfen, dass die Durchschnittsgeschwindigkeit nicht allein durch die Wartezeit sinkt.
11. App während einer laufenden oder pausierten Messung beenden und erneut öffnen; prüfen, dass der letzte Messstand wiederhergestellt wird.
12. `Reset` auslösen und prüfen, dass die Sitzungswerte wieder auf den Ausgangszustand gehen und die gespeicherte Sitzung verschwindet.

## Genauigkeitsgrenzen

Die App ist bewusst einfach gehalten und nicht für Hochpräzisionsmessungen gedacht. Bei sehr kurzen Fahrten können GPS-Geschwindigkeit und Distanz deutlich schwanken. Besonders kleine Distanzen reagieren stark auf verzögerte GPS-Fixes, schwankende Genauigkeit, schlechte Satellitensicht und Positionssprünge zwischen zwei Updates.
