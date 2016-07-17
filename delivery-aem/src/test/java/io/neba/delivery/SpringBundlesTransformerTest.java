package io.neba.delivery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import static java.nio.file.Files.createTempDirectory;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class SpringBundlesTransformerTest {
    private File source;
    private File target;

    @Before
    public void setUp() throws Exception {
        URL sources = getResource("sources");
        assertThat(sources).isNotNull();
        File sourceBlueprint = new File(sources.getFile());

        this.source = createTempDirectory("neba-").toFile();
        this.target = createTempDirectory("neba-").toFile();

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
    public void testTransformationOfJacksonImportsToRequireBundle() throws Exception {
        transformUnpackedArtifacts();

        assertImportDirectiveDoesNotContain("jar.with-jackson.jar", "com.fasterxml.jackson");
        assertBundleRequiresBundles("jar.with-jackson.jar",
                "com.fasterxml.jackson.core.jackson-core",
                "com.fasterxml.jackson.core.jackson-databind",
                "com.fasterxml.jackson.core.jackson-annotations");
        assertSymbolicNameIs("jar.with-jackson.jar", "io.neba.spring-webmvc");
    }

    private void assertSymbolicNameIs(String fileName, String symbolicName) throws IOException, URISyntaxException {
        JarFile jarFile = getJarFile(fileName);
        String value = jarFile.getManifest().getMainAttributes().getValue("Bundle-SymbolicName");
        jarFile.close();

        assertThat(value).isEqualTo(symbolicName);

    }

    private void assertBundleRequiresBundles(String fileName, String... symbolicNames) throws IOException, URISyntaxException {
        JarFile jarFile = getJarFile(fileName);
        String requireBundleHeader = jarFile.getManifest().getMainAttributes().getValue("Require-Bundle");
        jarFile.close();

        assertThat(requireBundleHeader.split(","))
                .describedAs("The Require-Bundle header must exactly contain")
                .containsExactlyInAnyOrder(symbolicNames);
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
        new SpringBundlesTransformer(source, target).run();
    }

    private URL getResource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}