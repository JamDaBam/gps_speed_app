# Sauseschaf

Sauseschaf ist eine kleine native Android-App fur kurze GPS-basierte Geschwindigkeitsmessungen, gedacht fur spielerische Anwendungsfalle wie das Messen einer Rutschfahrt.

## Funktionsumfang

- Aktuelle Geschwindigkeit gross in `m/s` mit zusatzlicher Anzeige in `km/h`
- Sitzungsmetriken fur Distanz, Durchschnitt und Maximum
- GPS-Statusleiste mit Satelliten, Fix-Qualitat und Genauigkeit
- Sitzungssteuerung mit `Start`, `Stopp` als Pause und `Reset`
- Fortsetzung einer pausierten Messung bis zum expliziten `Reset`
- Wiederherstellung des letzten Messstands nach App-Neustart

## Technik

- Kotlin
- Native Android-App mit Jetpack Compose
- Google Play Services Fused Location Provider fur schnelle Standortupdates
- GNSS-Status uber `LocationManagerCompat`
- Lokale Snapshot-Speicherung uber `SharedPreferences`

## Entwicklung

Projekt in Android Studio offnen und die Konfiguration `app` auf einem echten Android-Gerat starten. Fur kurze GPS-Messungen ist ein Emulator nur eingeschrankt aussagekraftig.

Wichtige Gradle-Befehle:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew clean
```

## Berechtigungen und Voraussetzungen

- Genaue Standortberechtigung
- Aktivierte Standortdienste
- Moglichst freier Himmel fur brauchbare GPS-Werte

Fehlende Berechtigungen und deaktivierte Standortdienste behandelt die App direkt in der Oberflache inklusive Weiterleitung in die passenden Systemeinstellungen.

## Datenschutz und lokaler Zustand

- Die App sendet keine Messdaten an einen Server.
- Der aktuelle Messstand wird lokal auf dem Gerat gespeichert, damit eine laufende oder pausierte Sitzung nach einem App-Neustart wiederhergestellt werden kann.
- `Reset` verwirft den gespeicherten Sitzungsstand.
- Android-Backups sind absichtlich deaktiviert, damit keine standortnahen Zustandsdaten uber System-Backups oder Device Transfer exportiert werden.
- Zusatzlich wird nur gespeichert, ob die Standortberechtigung bereits angefragt wurde, damit der Berechtigungsfluss sinnvoll reagiert.

## Messlogik

- Live-Werte verwenden nur aktuelle, zeitlich vorwarts laufende und nicht als Mock markierte Standortproben mit gultigen Koordinaten.
- Die Live-Anzeige bevorzugt frische Fused-Location-Proben und aktualisiert Geschwindigkeit sowie Genauigkeit fortlaufend.
- `Start` beginnt aus `Bereit` eine neue Sitzung und setzt nach `Stopp` dieselbe Sitzung fort.
- `Stopp` pausiert nur. Die Durchschnittsgeschwindigkeit basiert ausschliesslich auf aktiver Messzeit.
- Distanz, Durchschnitt und Maximum ignorieren Proben mit schlechter Genauigkeit sowie unrealistische Positionssprunge.
- Dadurch reagiert die App defensiver auf GPS-Rauschen, ersetzt aber keine spezialisierte Messtechnik.

## Testhinweise

1. Beim ersten Start prufen, dass die App nach der genauen Standortberechtigung fragt.
2. Berechtigung ablehnen und prufen, dass der Hinweiszustand mit erneutem Button sichtbar bleibt.
3. Berechtigung dauerhaft ablehnen und prufen, dass ein Sprung in die App-Einstellungen angeboten wird.
4. Berechtigung erteilen, Standortdienste deaktivieren und prufen, dass der GPS-Hinweiszustand erscheint.
5. Standortdienste aktivieren und prufen, dass die App in den Messbildschirm wechselt.
6. Im Freien prufen, dass aktuelle Geschwindigkeit, Genauigkeit und GPS-Status fortlaufend aktualisiert werden.
7. Eine Messung starten und prufen, dass Distanz, Durchschnitt und Maximum bei plausibler Bewegung steigen.
8. Die Messung stoppen und prufen, dass die Sitzung pausiert und die Werte stehen bleiben.
9. Nach `Stopp` wieder `Start` drucken und prufen, dass dieselbe Sitzung mit vorhandener Distanz, Durchschnitt und Maximum fortgesetzt wird.
10. Wahrend einer Pause kurz warten und dann fortsetzen; prufen, dass die Durchschnittsgeschwindigkeit nicht allein durch die Wartezeit sinkt.
11. App wahrend einer laufenden oder pausierten Messung beenden und erneut offnen; prufen, dass der letzte Messstand wiederhergestellt wird.
12. `Reset` auslosen und prufen, dass die Sitzungswerte wieder auf den Ausgangszustand gehen und die gespeicherte Sitzung verschwindet.

## Genauigkeitsgrenzen

Die App ist bewusst einfach gehalten und nicht fur Hochprazisionsmessungen gedacht. Bei sehr kurzen Fahrten konnen GPS-Geschwindigkeit und Distanz deutlich schwanken. Besonders kleine Distanzen reagieren stark auf verzogerte GPS-Fixes, schwankende Genauigkeit, schlechte Satellitensicht und Positionssprunge zwischen zwei Updates.
