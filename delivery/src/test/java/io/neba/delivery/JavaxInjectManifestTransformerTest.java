package io.neba.delivery;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class JavaxInjectManifestTransformerTest {
    private File source;
    private File target;

    @Before
    public void setUp() throws Exception {
        URL resource = getResource("sources");
        assertThat(resource).isNotNull();
        this.source = new File(resource.toURI());
        this.target = new File(source.getParentFile(), "targetDir");
    }

    @Test
    public void testJavaxInjectRewrite() throws Exception {
        transformUnpackedArtifacts();
        assertImportDirectiveContains("javax.inject;resolution:=optional;version=\"[0,2)\"");
    }

    private void assertImportDirectiveContains(String expected) throws URISyntaxException, IOException {
        URL resource = getResource("targetDir/jar.file.name.jar");
        assertThat(resource).isNotNull();
        File file = new File(resource.toURI());
        assertThat(file).isFile();
        JarFile archive = new JarFile(file);
        String importPackageDirective = archive.getManifest().getMainAttributes().getValue("Import-Package");
        assertThat(importPackageDirective).contains(expected);
    }

    private void transformUnpackedArtifacts() {
        new JavaxInjectManifestTransformer(source, target).run();
    }

    private URL getResource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}