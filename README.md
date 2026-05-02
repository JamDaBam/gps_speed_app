# Sauseschaf

Minimale native Android-App fur kurze GPS-basierte Geschwindigkeitsmessungen, gedacht fur spielerische Anwendungsfalle wie das Messen einer Rutschfahrt.

## Funktionsumfang

- Aktuelle Geschwindigkeit in `km/h`
- Distanz pro Messsitzung
- Durchschnittsgeschwindigkeit pro Messsitzung
- Hochstgeschwindigkeit pro Messsitzung
- GPS-Statusleiste mit Satelliten, Fix-Qualitat und Genauigkeit
- Sitzungssteuerung mit `Start`, `Stopp` und `Reset`

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

## Testhinweise

1. Beim ersten Start prufen, dass die App nach der genauen Standortberechtigung fragt.
2. Berechtigung ablehnen und prufen, dass der Hinweiszustand mit erneutem Button sichtbar bleibt.
3. Berechtigung dauerhaft ablehnen und prufen, dass ein Sprung in die App-Einstellungen angeboten wird.
4. Berechtigung erteilen, Standortdienste deaktivieren und prufen, dass der GPS-Hinweiszustand erscheint.
5. Standortdienste aktivieren und prufen, dass die App in den Messbildschirm wechselt.
6. Im Freien oder mit Mock-Standorten prufen, dass aktuelle Geschwindigkeit und Genauigkeit fortlaufend aktualisiert werden.
7. Eine Messung starten und prufen, dass Distanz, Durchschnitt und Maximum wahrend der Fahrt steigen.
8. Die Messung stoppen und prufen, dass die Sitzungswerte stehen bleiben.
9. `Reset` auslosen und prufen, dass die Sitzungswerte wieder auf den Ausgangszustand gehen.
10. Prufen, dass die GPS-Statusleiste Satelliten, Qualitat und Genauigkeit sichtbar halt.

## Genauigkeitsgrenzen

Die App ist bewusst einfach gehalten und nicht fur Hochprazisionsmessungen gedacht. Bei sehr kurzen Fahrten konnen GPS-Geschwindigkeit und Distanz deutlich schwanken. Besonders kleine Distanzen reagieren stark auf:

- verzogerte GPS-Fixes
- schwankende Genauigkeit
- schlechte Satellitensicht
- Positionssprunge zwischen zwei Updates

Die Statusleiste mit Satelliten, Fix-Qualitat und Genauigkeit hilft bei der Einordnung, ersetzt aber keine professionelle Messtechnik.
