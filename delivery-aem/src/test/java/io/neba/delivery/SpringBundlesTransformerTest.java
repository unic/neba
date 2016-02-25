package io.neba.delivery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static java.nio.file.Files.createTempDirectory;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class SpringBundlesTransformerTest {
    private File source;
    private File target;
    private File dependenciesDir;

    @Before
    public void setUp() throws Exception {
        URL sources = getResource("sources");
        assertThat(sources).isNotNull();
        File sourceBlueprint = new File(sources.getFile());

        this.source = createTempDirectory("neba-").toFile();
        this.target = createTempDirectory("neba-").toFile();
        this.dependenciesDir = new File(sourceBlueprint.getParentFile(), "dependenciesDir");

        copyDirectory(sourceBlueprint, source);
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(this.source);
        deleteDirectory(this.target);
    }

    @Test
    public void testJavaxImportWideningToUnstableVersions() throws Exception {
        transformUnpackedArtifacts();

        assertImportDirectiveContains("jar.with-inject.jar", "javax.inject;version=\"[0,2)\";resolution:=optional");
        assertSymbolicNameIs("jar.with-inject.jar", "io.neba.spring-beans");
    }

    @Test
    public void testInliningOfJacksonImports() throws Exception {
        transformUnpackedArtifacts();

        assertImportDirectiveDoesNotContain("jar.with-jackson.jar", "com.fasterxml.jackson");
        assertBundleClasspathContains("jar.with-jackson.jar", "lib/some-jackson-library.jar", ".");
        assertBundleContains("jar.with-jackson.jar", "lib/some-jackson-library.jar");
        assertSymbolicNameIs("jar.with-jackson.jar", "io.neba.spring-webmvc");
    }

    private void assertSymbolicNameIs(String fileName, String symbolicName) throws IOException, URISyntaxException {
        JarFile jarFile = getJarFile(fileName);
        String value = jarFile.getManifest().getMainAttributes().getValue("Bundle-SymbolicName");
        jarFile.close();

        assertThat(value).isEqualTo(symbolicName);

    }

    private void assertBundleContains(String fileName, String file) throws IOException, URISyntaxException {
        JarFile jarFile = getJarFile(fileName);
        ZipEntry entry = jarFile.getEntry(file);
        jarFile.close();

        assertThat(entry).describedAs("The embedded jackson jar file").isNotNull();
    }

    private void assertBundleClasspathContains(String fileName, String... s) throws IOException, URISyntaxException {
        JarFile jarFile = getJarFile(fileName);
        String bundleClasspathHeader = jarFile.getManifest().getMainAttributes().getValue("Bundle-Classpath");
        jarFile.close();

        for (String expected : s) {
            assertThat(bundleClasspathHeader).describedAs("The bundle class path header").contains(expected);
        }
    }

    private void assertImportDirectiveDoesNotContain(String fileName, String expected) throws IOException, URISyntaxException {
        assertThat(getImportDirectiveOfBundle(fileName)).doesNotContain(expected);
    }

    private void assertImportDirectiveContains(String fileName, String expected) throws URISyntaxException, IOException {
        assertThat(getImportDirectiveOfBundle(fileName)).contains(expected);
    }

    private String getImportDirectiveOfBundle(String fileName) throws URISyntaxException, IOException {
        JarFile jarFile = getJarFile(fileName);
        String value = jarFile.getManifest().getMainAttributes().getValue("Import-Package");
        jarFile.close();

        return value;
    }

    private JarFile getJarFile(String fileName) throws URISyntaxException, IOException {
        File file = new File(target, fileName);
        assertThat(file).isFile();
        return new JarFile(file);
    }

    private void transformUnpackedArtifacts() throws IOException {
        new SpringBundlesTransformer(source, target, dependenciesDir).run();
    }

    private URL getResource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}