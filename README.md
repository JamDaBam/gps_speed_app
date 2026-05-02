# Sauseschaf

Minimale native Android-App fur kurze GPS-basierte Geschwindigkeitsmessungen, gedacht fur spielerische Anwendungsfalle wie das Messen einer Rutschfahrt.

## Funktionsumfang

- Aktuelle Geschwindigkeit in `km/h`
- Distanz pro Messsitzung
- Durchschnittsgeschwindigkeit pro Messsitzung
- Hochstgeschwindigkeit pro Messsitzung
- GPS-Statusleiste mit Satelliten, Fix-Qualitat und Genauigkeit
- Sitzungssteuerung mit `Start`, `Stopp` als Pause und `Reset`

## Technik

- Kotlin
- Native Android-App
- Jetpack Compose
- Google Play Services Fused Location Provider fur schnelle Standortupdates
- GNSS-Status uber `LocationManagerCompat`

## Bauen und Starten

1. Projekt in Android Studio offnen.
2. Android Studio die erforderlichen Android-SDK- und Gradle-Komponenten installieren lassen.
3. Ein echtes Android-Gerat bevorzugen, weil kurze GPS-Messungen im Emulator nur eingeschrankt aussagekraftig sind.
4. Die Konfiguration `app` bauen und starten.

Falls du lieber uber die Kommandozeile baust:

```bash
./gradlew assembleDebug
```

## Berechtigungen und Voraussetzungen

- Genaue Standortberechtigung
- Aktivierte Standortdienste
- Moglichst freier Himmel fur brauchbare GPS-Werte

Die App behandelt fehlende Berechtigungen und deaktivierte Standortdienste direkt in der Oberflache.

## Datenschutz und Backup-Verhalten

- Die App sendet keine Messdaten an einen Server.
- Sitzungsdaten werden nur im Arbeitsspeicher gehalten und beim Beenden nicht dauerhaft gespeichert.
- Android-Backups sind absichtlich deaktiviert, damit keine standortnahen Zustandsdaten versehentlich uber Auto Backup oder Device Transfer exportiert werden.
- Die einzige lokale Preference speichert nur, ob die Standortberechtigung bereits angefragt wurde, damit der Berechtigungsfluss sinnvoll reagiert.

## Messlogik und Grenzen

- Live-Werte und Sitzungsmetriken verwenden nur aktuelle, zeitlich vorwarts laufende Standortproben.
- `Start` beginnt aus `Bereit` eine neue Messung und setzt nach `Stopp` dieselbe Sitzung fort.
- `Stopp` pausiert nur. Erst `Reset` verwirft Distanz, Durchschnitt und Maximum.
- Die Durchschnittsgeschwindigkeit basiert nur auf aktiver Messzeit; Pausen zahlen nicht mit.
- Live-Geschwindigkeit bevorzugt direkte GPS-Speed-Werte und verarbeitet alle frischen Fused-Location-Proben einer Lieferung.
- Veraltete, ruckwarts laufende, offensichtlich ungultige und als Mock markierte Positionen werden verworfen.
- Distanz, Durchschnitt und Maximum einer Sitzung ignorieren Proben mit schlechter Genauigkeit sowie unrealistische Positionssprunge.
- Dadurch reagiert die App defensiver auf GPS-Rauschen, ersetzt aber weiterhin keine spezialisierte Messtechnik.

## Testhinweise

1. Beim ersten Start prufen, dass die App nach der genauen Standortberechtigung fragt.
2. Berechtigung ablehnen und prufen, dass der Hinweiszustand mit erneutem Button sichtbar bleibt.
3. Berechtigung dauerhaft ablehnen und prufen, dass ein Sprung in die App-Einstellungen angeboten wird.
4. Berechtigung erteilen, Standortdienste deaktivieren und prufen, dass der GPS-Hinweiszustand erscheint.
5. Standortdienste aktivieren und prufen, dass die App in den Messbildschirm wechselt.
6. Berechtigung wahrend die App im Vordergrund entziehen und prufen, dass keine Absturze auftreten und der Berechtigungszustand nach `Resume` wieder erscheint.
7. Im Freien prufen, dass aktuelle Geschwindigkeit und Genauigkeit fortlaufend aktualisiert werden.
8. Eine Messung starten und prufen, dass Distanz, Durchschnitt und Maximum wahrend einer plausiblen Bewegung steigen.
9. Messung mit schlechter Genauigkeit, altem Mock-Standort oder deutlichem Positionssprung gegenprufen und prufen, dass Sitzungsmetriken nicht spurios steigen.
10. Die Messung stoppen und prufen, dass die Sitzung pausiert und die Werte stehen bleiben.
11. Nach `Stopp` wieder `Start` drucken und prufen, dass dieselbe Sitzung mit vorhandener Distanz, Durchschnitt und Maximum fortgesetzt wird.
12. Wahrend einer Pause kurz warten und dann fortsetzen; prufen, dass die Durchschnittsgeschwindigkeit nicht allein durch die Wartezeit sinkt.
13. `Reset` auslosen und prufen, dass die Sitzungswerte wieder auf den Ausgangszustand gehen.
14. Prufen, dass die GPS-Statusleiste Satelliten, Qualitat und Genauigkeit sichtbar halt.

## Genauigkeitsgrenzen

Die App ist bewusst einfach gehalten und nicht fur Hochprazisionsmessungen gedacht. Bei sehr kurzen Fahrten konnen GPS-Geschwindigkeit und Distanz deutlich schwanken. Besonders kleine Distanzen reagieren stark auf:

- verzogerte GPS-Fixes
- schwankende Genauigkeit
- schlechte Satellitensicht
- Positionssprunge zwischen zwei Updates

Die Statusleiste mit Satelliten, Fix-Qualitat und Genauigkeit hilft bei der Einordnung, ersetzt aber keine professionelle Messtechnik.
