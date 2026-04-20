package de.petanqueturniermanager.installer;

import de.petanqueturniermanager.installer.service.PruefErgebnis;

import java.nio.file.Path;
import java.util.Optional;

public final class InstallerZustand {

    private Path unopkgPfad;
    private PruefErgebnis javaPruefErgebnis;
    private boolean loJavaCommonOk = true;
    private boolean installationErfolgreich;
    private String installationFehler;

    public Optional<Path> getUnopkgPfad() {
        return Optional.ofNullable(unopkgPfad);
    }

    public void setUnopkgPfad(Path unopkgPfad) {
        this.unopkgPfad = unopkgPfad;
    }

    public PruefErgebnis getJavaPruefErgebnis() {
        return javaPruefErgebnis;
    }

    public void setJavaPruefErgebnis(PruefErgebnis javaPruefErgebnis) {
        this.javaPruefErgebnis = javaPruefErgebnis;
    }

    public boolean isInstallationErfolgreich() {
        return installationErfolgreich;
    }

    public void setInstallationErfolgreich(boolean installationErfolgreich) {
        this.installationErfolgreich = installationErfolgreich;
    }

    public String getInstallationFehler() {
        return installationFehler;
    }

    public void setInstallationFehler(String installationFehler) {
        this.installationFehler = installationFehler;
    }

    public boolean isLoJavaCommonOk() {
        return loJavaCommonOk;
    }

    public void setLoJavaCommonOk(boolean loJavaCommonOk) {
        this.loJavaCommonOk = loJavaCommonOk;
    }

    public boolean kannInstallieren() {
        return unopkgPfad != null && loJavaCommonOk;
    }
}
