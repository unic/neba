package io.neba.delivery;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.*;
import java.util.logging.Logger;

import static aQute.bnd.osgi.Constants.BUNDLE_SYMBOLICNAME;
import static aQute.bnd.osgi.Constants.IMPORT_PACKAGE;
import static aQute.bnd.osgi.Processor.printClauses;

/**
 * <h1>This implementation addresses the following issues</h1>
 *
 * <h2>NEBA-69: Mitigate potential javax.inject import conflicts</h2>
 * <p>
 * Sling provides the javax.inject package as a default version (0.0.0). This may lead
 * to transitive dependency issues when this version collides with another javax.inject version, should
 * both reside on the dependency chains of the same bundle. To work around this, the javax.inject imports
 * of the Spring bundles deployed with NEBA are modified to import the default version ([0, 2)).
 * </p>
 *
 * <h2>NEBA-155: Make jackson library dependencies optional</h2>
 * <p>
 * AEM 6.x ships with an incomplete export of the jackson library. The jackson package imports of Spring are thus removed in favor of an explicit
 * relationship to jackson bundles using the "Require-Bundle" header.
 * </p>
 *
 * @author Olaf Otto
 */
public class SpringBundlesTransformer {
    public static final String MANIFEST_LOCATION = "META-INF/MANIFEST.MF";

    public static void main(String[] args) throws IOException {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("Expected exactly two arguments, got: " + Arrays.toString(args) + ".");
        }
        new SpringBundlesTransformer(
                asDirectory(args[0]),
                asDirectory(args[1])
        ).run();
    }

    private static File asDirectory(String arg) {
        File artifactsWorkDir = new File(arg);
        if (!artifactsWorkDir.exists()) {
            throw new IllegalArgumentException("The directory " + artifactsWorkDir.getPath() + " does not exist.");
        }
        if (!artifactsWorkDir.isDirectory()) {
            throw new IllegalArgumentException("The directory " + artifactsWorkDir.getPath() + " is not a directory.");
        }
        return artifactsWorkDir;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final File unpackedArtifactsDir;
    private final File repackToDirectory;

    public SpringBundlesTransformer(File unpackedArtifactsDir, File repackToDir) {
        if (unpackedArtifactsDir == null) {
            throw new IllegalArgumentException("Method argument unpackedArtifactsDir must not be null.");
        }
        if (repackToDir == null) {
            throw new IllegalArgumentException("Method argument repackToDir must not be null.");
        }

        this.unpackedArtifactsDir = unpackedArtifactsDir;
        this.repackToDirectory = repackToDir;
    }

    public void run() throws IOException {
        for (File dir : listFiles(this.unpackedArtifactsDir)) {
            logger.info("Transforming manifest in " + dir + " ...");

            Manifest manifest = getManifest(dir);
            Attributes mainAttributes = manifest.getMainAttributes();
            String importPackageDirectives = mainAttributes.getValue(IMPORT_PACKAGE);

            if (importPackageDirectives == null) {
                continue;
            }

            Parameters imports = new Analyzer().parseHeader(importPackageDirectives);

            if (allowUnstableJavaxImports(imports) || transformJacksonImportsToRequireBundle(mainAttributes, imports)) {
                updateImportPackageDirectives(mainAttributes, imports);
                alterSymbolicNameToReflectCustomization(mainAttributes);
            }

            write(dir, manifest);
            repackageArtifact(dir);
        }
    }

    private void alterSymbolicNameToReflectCustomization(Attributes mainAttributes) {
        String symbolicName = mainAttributes.getValue(BUNDLE_SYMBOLICNAME);
        mainAttributes.putValue(BUNDLE_SYMBOLICNAME, "io.neba." + symbolicName.substring("org.apache.servicemix.bundles.".length()));
    }

    private void updateImportPackageDirectives(Attributes mainAttributes, Parameters imports) throws IOException {
        mainAttributes.putValue(IMPORT_PACKAGE, printClauses(imports));
    }

    private boolean allowUnstableJavaxImports(Parameters imports) {
        Attrs attrs = imports.get("javax.inject");
        if (attrs == null) {
            return false;
        }
        attrs.put("version", "[0,2)");
        return true;
    }

    private boolean transformJacksonImportsToRequireBundle(Attributes mainAttributes, Parameters imports) throws IOException {
        Set<String> jacksonImports = new HashSet<>();
        imports.keySet().forEach(key -> {
            if (key.startsWith("com.fasterxml.jackson")) {
                jacksonImports.add(key);
            }
        });

        if (jacksonImports.isEmpty()) {
            return false;
        }

        jacksonImports.forEach(imports::remove);

        mainAttributes.putValue(
                "Require-Bundle",
                        "com.fasterxml.jackson.core.jackson-core; bundle-version=\"[2,3)\"; resolution:=optional," +
                        "com.fasterxml.jackson.core.jackson-databind; bundle-version=\"[2,3)\"; resolution:=optional," +
                        "com.fasterxml.jackson.core.jackson-annotations; bundle-version=\"[2,3)\"; resolution:=optional");
        return true;
    }

    private File[] listFiles(File dir) {
        File[] files = dir.listFiles();
        return files == null ? new File[]{} : files;
    }

    private void repackageArtifact(File dir) {
        File targetJarFile = getTargetJarFile(dir);
        try {
            Manifest manifest = getManifest(dir);
            try (JarOutputStream out = new JarOutputStream(new FileOutputStream(targetJarFile), manifest)) {
                for (File file : listFiles(dir)) {
                    pack(file, dir, out);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create jar file " + targetJarFile.getPath(), e);
        }
    }

    private void pack(File source, File sourceDirectory, JarOutputStream target) throws IOException {
        // Omit initial "/" in entry names - jar entries must be added in the form directory/file.extension
        String name = source.getPath().substring(sourceDirectory.getPath().length() + 1).replace("\\", "/");
        if (name.isEmpty()) {
            return;
        }
        if (source.isDirectory()) {
            if (!name.endsWith("/")) {
                name += "/";
            }
            JarEntry entry = new JarEntry(name);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            target.closeEntry();
            File[] files = listFiles(source);
            if (files == null) {
                return;
            }
            for (File nestedFile : files) {
                pack(nestedFile, sourceDirectory, target);
            }
        } else {
            if (JarFile.MANIFEST_NAME.equals(name)) {
                // Skip manifest: It is provided as the first entry via the jar ouput stream.
                return;
            }
            JarEntry entry = new JarEntry(name);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            try (InputStream in = new BufferedInputStream(new FileInputStream(source))) {
                IOUtils.copy(in, target);
            }
            target.closeEntry();
        }
    }

    private File getTargetJarFile(File dir) {
        return new File(repackToDirectory, dir.getName().replace("-jar", ".jar"));
    }

    private void write(File dir, Manifest manifest) {
        File manifestFile = getManifestFile(dir);
        try {
            try (FileOutputStream out = new FileOutputStream(manifestFile)) {
                manifest.write(out);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the manifest " + manifest + " to " + manifestFile.getPath() + ".", e);
        }
    }

    private Manifest getManifest(File dir) {
        File manifestFile = getManifestFile(dir);
        try {
            try (FileInputStream is = new FileInputStream(manifestFile)) {
                return new Manifest(is);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not locate manifest in " + manifestFile.getPath() + ".", e);
        }
    }

    private File getManifestFile(File dir) {
        return new File(dir, MANIFEST_LOCATION);
    }
}
