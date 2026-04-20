package de.petanqueturniermanager.installer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public final class LinuxPaketPruefer {

    private static final Logger LOG = Logger.getLogger(LinuxPaketPruefer.class.getName());

    public record PaketManagerInfo(
        String name,
        String paketName,
        String[] pruefKommando,
        String erwarteteAusgabe,   // null → nur Exit-Code 0 prüfen
        String[] installKommando
    ) {}

    private static final List<PaketManagerInfo> PAKET_MANAGER = List.of(
        new PaketManagerInfo("apt", "libreoffice-java-common",
            new String[]{"dpkg-query", "-W", "-f=${Status}", "libreoffice-java-common"},
            "install ok installed",
            new String[]{"apt", "install", "-y", "libreoffice-java-common"}),
        new PaketManagerInfo("apt-get", "libreoffice-java-common",
            new String[]{"dpkg-query", "-W", "-f=${Status}", "libreoffice-java-common"},
            "install ok installed",
            new String[]{"apt-get", "install", "-y", "libreoffice-java-common"}),
        new PaketManagerInfo("dnf", "libreoffice-java",
            new String[]{"rpm", "-q", "libreoffice-java"},
            null,
            new String[]{"dnf", "install", "-y", "libreoffice-java"}),
        new PaketManagerInfo("yum", "libreoffice-java",
            new String[]{"rpm", "-q", "libreoffice-java"},
            null,
            new String[]{"yum", "install", "-y", "libreoffice-java"}),
        new PaketManagerInfo("zypper", "libreoffice-java",
            new String[]{"rpm", "-q", "libreoffice-java"},
            null,
            new String[]{"zypper", "install", "-y", "libreoffice-java"}),
        new PaketManagerInfo("pacman", "libreoffice-fresh",
            new String[]{"pacman", "-Q", "libreoffice-fresh"},
            null,
            new String[]{"pacman", "-S", "--noconfirm", "libreoffice-fresh"})
    );

    private static final Map<String, String[]> LO_INSTALL_KOMMANDOS = Map.ofEntries(
        Map.entry("apt",     new String[]{"apt",     "install", "-y", "libreoffice"}),
        Map.entry("apt-get", new String[]{"apt-get", "install", "-y", "libreoffice"}),
        Map.entry("dnf",     new String[]{"dnf",     "install", "-y", "libreoffice"}),
        Map.entry("yum",     new String[]{"yum",     "install", "-y", "libreoffice"}),
        Map.entry("zypper",  new String[]{"zypper",  "install", "-y", "libreoffice"}),
        Map.entry("pacman",  new String[]{"pacman",  "-S", "--noconfirm", "libreoffice-fresh"})
    );

    // Terminal-Emulator-Definitionen: Args die nach dem Skript-Pfad kommen
    private static final List<List<String>> TERMINAL_PREFIXE = List.of(
        List.of("gnome-terminal", "--wait", "--", "bash"),
        List.of("konsole",        "-e", "bash"),
        List.of("xfce4-terminal", "-x", "bash"),
        List.of("xterm",          "-e", "bash"),
        List.of("x-terminal-emulator", "-e", "bash"),
        List.of("tilix",          "-e", "bash"),
        List.of("terminator",     "-x", "bash")
    );

    private LinuxPaketPruefer() {}

    public static boolean istLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    public static boolean istWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    public static Optional<PaketManagerInfo> ermittlePaketManager() {
        return PAKET_MANAGER.stream()
            .filter(pm -> istKommandoVerfuegbar(pm.name()))
            .findFirst();
    }

    public static Optional<String[]> ermittleLoInstallKommando() {
        return ermittlePaketManager()
            .map(pm -> LO_INSTALL_KOMMANDOS.get(pm.name()));
    }

    public static boolean istKommandoVerfuegbar(String befehl) {
        try {
            return new ProcessBuilder("which", befehl)
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static PruefErgebnis pruefePaket(ResourceBundle texte) {
        if (!istLinux()) {
            return PruefErgebnis.ok("n/a");
        }
        var pmOpt = ermittlePaketManager();
        if (pmOpt.isEmpty()) {
            return PruefErgebnis.veraltet("unbekannt",
                texte.getString("voraussetzung.paket.dpkg.fehlt"));
        }
        return pruefeMitPaketManager(pmOpt.get(), texte);
    }

    private static PruefErgebnis pruefeMitPaketManager(PaketManagerInfo pm, ResourceBundle texte) {
        try {
            var prozess = new ProcessBuilder(pm.pruefKommando())
                .redirectErrorStream(true)
                .start();
            var ausgabe = new String(prozess.getInputStream().readAllBytes()).strip();
            int exitCode = prozess.waitFor();

            boolean installiert = pm.erwarteteAusgabe() != null
                ? ausgabe.contains(pm.erwarteteAusgabe())
                : exitCode == 0;

            if (installiert) {
                return PruefErgebnis.ok(pm.paketName());
            }
            return PruefErgebnis.nichtGefunden(
                texte.getString("voraussetzung.paket.fehlt"));

        } catch (IOException e) {
            LOG.warning("Paketprüfung fehlgeschlagen: " + e.getMessage());
            return PruefErgebnis.nichtGefunden(
                MessageFormat.format(texte.getString("voraussetzung.paket.pruef.fehler"),
                    e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PruefErgebnis.nichtGefunden(
                MessageFormat.format(texte.getString("voraussetzung.paket.unterbrochen"),
                    e.getMessage()));
        }
    }

    public static Process starteTerminalInstallation(PaketManagerInfo pm, ResourceBundle texte) throws IOException {
        return starteTerminalInstallation(pm.installKommando(), texte);
    }

    public static Process starteTerminalInstallation(String[] installKommando, ResourceBundle texte) throws IOException {
        var installBefehl = "sudo " + String.join(" ", installKommando);
        var skriptDatei = Files.createTempFile("ptm-install-", ".sh");
        Files.writeString(skriptDatei,
            "#!/bin/bash\n"
            + installBefehl + "\n"
            + "echo\nread -rp '" + texte.getString("terminal.enter.schliessen") + "'\n");
        skriptDatei.toFile().setExecutable(true);
        skriptDatei.toFile().deleteOnExit();
        return starteTerminalMitSkript(skriptDatei.toString());
    }

    public static Process starteTerminalMitSkript(String skriptPfad) throws IOException {
        for (var prefix : TERMINAL_PREFIXE) {
            if (!istKommandoVerfuegbar(prefix.get(0))) {
                continue;
            }
            var args = new java.util.ArrayList<>(prefix);
            args.add(skriptPfad);
            return new ProcessBuilder(args).start();
        }
        throw new IOException("Kein Terminal-Emulator gefunden");
    }
}
