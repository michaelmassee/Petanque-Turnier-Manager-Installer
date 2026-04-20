package de.petanqueturniermanager.installer.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class LibreOfficeJavaKonfigurierer {

    private static final Logger LOG = Logger.getLogger(LibreOfficeJavaKonfigurierer.class.getName());

    private LibreOfficeJavaKonfigurierer() {}

    public static boolean konfiguriereJava(Path javaHome, String version, Path konfigDatei) {
        try {
            Path realHome;
            try {
                realHome = javaHome.toRealPath();
            } catch (IOException e) {
                realHome = javaHome.toAbsolutePath();
            }

            Document doc;
            if (Files.exists(konfigDatei)) {
                var factory = DocumentBuilderFactory.newDefaultInstance();
                var builder = factory.newDocumentBuilder();
                try (var is = Files.newInputStream(konfigDatei)) {
                    doc = builder.parse(is);
                }
            } else {
                doc = erstelleMinimalDokument();
            }

            String locationUri = realHome.toUri().toString();
            String vendorData = erstelleVendorData(realHome);

            aktualisiereJavaInfo(doc, locationUri, version, vendorData);
            aktualisiereJreLocations(doc, locationUri);

            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            try (var out = Files.newOutputStream(konfigDatei)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
            LOG.info("LO-Java-Konfiguration gesetzt: " + locationUri);
            return true;
        } catch (Exception e) {
            LOG.warning("LO-Java-Konfiguration fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    private static void aktualisiereJavaInfo(Document doc, String locationUri,
                                              String version, String vendorData) {
        var list = doc.getElementsByTagName("javaInfo");
        Element javaInfo;
        if (list.getLength() > 0) {
            javaInfo = (Element) list.item(0);
        } else {
            javaInfo = doc.createElement("javaInfo");
            doc.getDocumentElement().appendChild(javaInfo);
        }

        javaInfo.setAttribute("xsi:nil", "false");
        javaInfo.setAttribute("vendorUpdate", "2019-07-26");
        javaInfo.setAttribute("autoSelect", "false");

        setzeKind(doc, javaInfo, "vendor", "Eclipse Adoptium");
        setzeKind(doc, javaInfo, "location", locationUri);
        setzeKind(doc, javaInfo, "version", version);
        setzeKind(doc, javaInfo, "features", "0");
        setzeKind(doc, javaInfo, "requirements", "1");
        setzeKind(doc, javaInfo, "vendorData", vendorData);
    }

    private static void aktualisiereJreLocations(Document doc, String locationUri) {
        var list = doc.getElementsByTagName("jreLocations");
        Element jreLocations;
        if (list.getLength() > 0) {
            jreLocations = (Element) list.item(0);
            jreLocations.setAttribute("xsi:nil", "false");
        } else {
            jreLocations = doc.createElement("jreLocations");
            jreLocations.setAttribute("xsi:nil", "false");
            doc.getDocumentElement().appendChild(jreLocations);
        }

        // Kein Doppeleintrag
        var locationNodes = jreLocations.getElementsByTagName("location");
        for (int i = 0; i < locationNodes.getLength(); i++) {
            if (locationUri.equals(locationNodes.item(i).getTextContent().strip())) {
                return;
            }
        }

        var locationEl = doc.createElement("location");
        locationEl.setTextContent(locationUri);
        jreLocations.appendChild(locationEl);
    }

    private static void setzeKind(Document doc, Element parent, String name, String value) {
        var children = parent.getElementsByTagName(name);
        Element el;
        if (children.getLength() > 0) {
            el = (Element) children.item(0);
        } else {
            el = doc.createElement(name);
            parent.appendChild(el);
        }
        el.setTextContent(value);
    }

    private static String erstelleVendorData(Path javaHome) {
        String arch = ermittleArch();
        String runtimeLibUri = findeRuntimeLibUri(javaHome, arch);
        String libPath = erstelleLibPath(javaHome, arch);

        String content = runtimeLibUri + "\n" + libPath + "\n";

        byte[] utf16le = content.getBytes(StandardCharsets.UTF_16LE);
        var hex = new StringBuilder(utf16le.length * 2);
        for (byte b : utf16le) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    // Wie sunjre.cxx getRuntimePaths() – erste existierende libjvm.so
    private static String findeRuntimeLibUri(Path javaHome, String arch) {
        Path[] kandidaten = {
            javaHome.resolve("lib").resolve(arch).resolve("client").resolve("libjvm.so"),
            javaHome.resolve("lib").resolve(arch).resolve("server").resolve("libjvm.so"),
            javaHome.resolve("lib").resolve("server").resolve("libjvm.so")
        };
        for (var k : kandidaten) {
            if (Files.exists(k)) return k.toUri().toString();
        }
        return kandidaten[kandidaten.length - 1].toUri().toString();
    }

    // Wie sunjre.cxx getLibraryPaths() – alle 4 Pfade, ohne Existenzprüfung
    private static String erstelleLibPath(Path javaHome, String arch) {
        String base = javaHome.toString();
        return base + "/lib/" + arch + "/client"
             + ":" + base + "/lib/" + arch + "/server"
             + ":" + base + "/lib/" + arch + "/native_threads"
             + ":" + base + "/lib/" + arch;
    }

    private static String ermittleArch() {
        return switch (System.getProperty("os.arch", "").toLowerCase()) {
            case "amd64", "x86_64" -> "amd64";
            case "aarch64"         -> "arm64";
            case "x86"             -> "i386";
            default                -> System.getProperty("os.arch", "amd64");
        };
    }

    private static Document erstelleMinimalDokument() throws Exception {
        var factory = DocumentBuilderFactory.newDefaultInstance();
        var builder = factory.newDocumentBuilder();
        var doc = builder.newDocument();
        doc.appendChild(doc.createComment("This is a generated file. Do not alter this file!"));

        var root = doc.createElement("java");
        root.setAttribute("xmlns", "http://openoffice.org/2004/java/framework/1.0");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        doc.appendChild(root);

        for (var name : new String[]{"enabled", "userClassPath", "vmParameters"}) {
            var el = doc.createElement(name);
            el.setAttribute("xsi:nil", "true");
            root.appendChild(el);
        }

        var jreLocations = doc.createElement("jreLocations");
        jreLocations.setAttribute("xsi:nil", "false");
        root.appendChild(jreLocations);

        var javaInfo = doc.createElement("javaInfo");
        javaInfo.setAttribute("xsi:nil", "true");
        root.appendChild(javaInfo);

        return doc;
    }
}
