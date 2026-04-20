package de.petanqueturniermanager.installer.service;

public record PruefErgebnis(
    boolean gefunden,
    String version,
    String warnung
) {

    public static PruefErgebnis ok(String version) {
        return new PruefErgebnis(true, version, null);
    }

    public static PruefErgebnis nichtGefunden(String warnung) {
        return new PruefErgebnis(false, null, warnung);
    }

    public static PruefErgebnis veraltet(String version, String warnung) {
        return new PruefErgebnis(true, version, warnung);
    }

    public boolean hatWarnung() {
        return warnung != null;
    }
}
