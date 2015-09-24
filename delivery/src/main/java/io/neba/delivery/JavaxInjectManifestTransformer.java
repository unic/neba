package io.neba.delivery;

import java.io.*;
import java.util.Arrays;
import java.util.jar.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * NEBA-69:  Mitigate potential javax.inject import conflicts
 *
 * Sling provides the javax.inject package as a default version (0.0.0). This may lead
 * to transitive dependency issues when this version collides with another javax.inject version, should
 * both reside on the dependency chains of the same bundle. To work around this, the javax.inject imports
 * of the thirdparty bundles deployed with NEBA are modified to import the default version ([0, 2)). This is what
 * this class is doing.
 *
 * @author Olaf Otto
 */
public class JavaxInjectManifestTransformer {
    public static final String MANIFEST_LOCATION = "META-INF/MANIFEST.MF";
    public static final String IMPORT_PACKAGE_HEADER = "Import-Package";
    public static final Pattern JAVAX_INJECT_IMPORT_DIRECTIVE = compile("(javax\\.inject[^,\\n]*;version=\")(\\[|\\()[^\\]\\)]+(\\]|\\))(\")");

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException("Expected exactly two arguments, got: " + Arrays.toString(args) + ".");
        }
        new JavaxInjectManifestTransformer(
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

    public JavaxInjectManifestTransformer(File unpackedArtifactsDir, File repackToDirectory) {
        if (unpackedArtifactsDir == null) {
            throw new IllegalArgumentException("Method argument unpackedArtifactsDir must not be null.");
        }
        if (repackToDirectory == null) {
            throw new IllegalArgumentException("Method argument repackToDirectory must not be null.");
        }

        this.unpackedArtifactsDir = unpackedArtifactsDir;
        this.repackToDirectory = repackToDirectory;
    }

    public void run() {
        for (File dir : unpackedArtifactsDir.listFiles()) {
            logger.info("Transforming manifest in " + dir + " ...");
            Manifest manifest = getManifest(dir);
            Attributes mainAttributes = manifest.getMainAttributes();
            String importPackageDirectives = mainAttributes.getValue(IMPORT_PACKAGE_HEADER);
            if (importPackageDirectives == null) {
                continue;
            }

            Matcher matcher = JAVAX_INJECT_IMPORT_DIRECTIVE.matcher(importPackageDirectives);
            StringBuffer buffer = new StringBuffer(importPackageDirectives.length());
            while (matcher.find()) {
                String replacement = matcher.group(1) + "[0,2)" + matcher.group(4);
                matcher.appendReplacement(buffer, replacement);
            }

            matcher.appendTail(buffer);
            mainAttributes.putValue(IMPORT_PACKAGE_HEADER, buffer.toString());
            write(dir, manifest);
            repackageArtifact(dir);
        }
    }

    private void repackageArtifact(File dir) {
        File targetJarFile = getTargetJarFile(dir);
        try {
            Manifest manifest = getManifest(dir);
            JarOutputStream out = new JarOutputStream(new FileOutputStream(targetJarFile), manifest);
            try {
                for (File file : dir.listFiles()) {
                    pack(file, dir, out);
                }
            } finally {
                out.close();
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
            File[] files = source.listFiles();
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
            InputStream in = new BufferedInputStream(new FileInputStream(source));
            try {
                byte[] buffer = new byte[4096];

                int count;
                while ((count = in.read(buffer)) != -1) {
                    target.write(buffer, 0, count);
                }
            } catch (IOException e) {
                in.close();
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
            FileOutputStream out = new FileOutputStream(manifestFile);
            try {
                manifest.write(out);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the manifest " + manifest + " to " + manifestFile.getPath() + ".", e);
        }
    }

    private Manifest getManifest(File dir) {
        File manifestFile = getManifestFile(dir);
        try {
            FileInputStream is = new FileInputStream(manifestFile);
            try {
                return new Manifest(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not locate manifest in " + manifestFile.getPath() + ".", e);
        }
    }

    private File getManifestFile(File dir) {
        return new File(dir, MANIFEST_LOCATION);
    }
}
