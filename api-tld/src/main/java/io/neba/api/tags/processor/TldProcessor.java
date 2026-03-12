/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package io.neba.api.tags.processor;

import io.neba.api.tags.Tag;
import io.neba.api.tags.TagAttribute;
import io.neba.api.tags.TagLibrary;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor that generates TLD (Tag Library Descriptor) files from
 * {@link TagLibrary}, {@link Tag}, and {@link TagAttribute} annotations.
 */
@SupportedAnnotationTypes({
    "io.neba.api.tags.TagLibrary",
    "io.neba.api.tags.Tag",
    "io.neba.api.tags.TagAttribute"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TldProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        TagLibraryInfo libraryInfo = null;
        for (Element element : roundEnv.getElementsAnnotatedWith(TagLibrary.class)) {
            if (element.getKind() == ElementKind.PACKAGE) {
                PackageElement pkg = (PackageElement) element;
                TagLibrary ann = pkg.getAnnotation(TagLibrary.class);
                if (ann != null) {
                    libraryInfo = new TagLibraryInfo(
                            ann.value(),
                            ann.descriptorFile(),
                            ann.shortName(),
                            ann.description()
                    );
                    break;
                }
            }
        }

        if (libraryInfo == null) {
            return false;
        }

        List<TagInfo> tags = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Tag.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement type = (TypeElement) element;
                Tag ann = type.getAnnotation(Tag.class);
                if (ann != null) {
                    String tagName = deriveTagName(type.getSimpleName().toString());
                    List<AttributeInfo> attributes = new ArrayList<>();
                    for (Element member : type.getEnclosedElements()) {
                        if (member.getKind() == ElementKind.METHOD) {
                            TagAttribute attrAnn = member.getAnnotation(TagAttribute.class);
                            if (attrAnn != null) {
                                String attrName = deriveAttributeName(((ExecutableElement) member).getSimpleName().toString());
                                attributes.add(new AttributeInfo(attrName, attrAnn.description(), attrAnn.runtimeValueAllowed()));
                            }
                        }
                    }
                    tags.add(new TagInfo(
                            tagName,
                            type.getQualifiedName().toString(),
                            ann.description(),
                            attributes
                    ));
                }
            }
        }

        try {
            writeTld(libraryInfo, tags);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write TLD: " + e.getMessage());
        }

        return true;
    }

    private static String deriveTagName(String className) {
        if (className.endsWith("Tag")) {
            String base = className.substring(0, className.length() - 3);
            return base.substring(0, 1).toLowerCase() + base.substring(1);
        }
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    private static String deriveAttributeName(String setterName) {
        if (setterName.startsWith("set") && setterName.length() > 3) {
            String base = setterName.substring(3);
            return base.substring(0, 1).toLowerCase() + base.substring(1);
        }
        return setterName;
    }

    private void writeTld(TagLibraryInfo library, List<TagInfo> tags) throws IOException {
        FileObject file = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/" + library.descriptorFile
        );

        try (Writer writer = file.openWriter()) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<taglib xmlns=\"http://xmlns.jcp.org/xml/ns/javase\"\n");
            writer.write("        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.write("        xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javase http://xmlns.jcp.org/xml/ns/javase/web-jsptaglibrary_2_1.xsd\"\n");
            writer.write("        version=\"2.1\">\n");
            writer.write("    <description>");
            writer.write(escape(library.description));
            writer.write("</description>\n");
            writer.write("    <tlib-version>1.0</tlib-version>\n");
            writer.write("    <short-name>");
            writer.write(escape(library.shortName));
            writer.write("</short-name>\n");
            writer.write("    <uri>");
            writer.write(escape(library.uri));
            writer.write("</uri>\n");

            for (TagInfo tag : tags) {
                writer.write("    <tag>\n");
                writer.write("        <name>");
                writer.write(escape(tag.name));
                writer.write("</name>\n");
                writer.write("        <tag-class>");
                writer.write(escape(tag.tagClass));
                writer.write("</tag-class>\n");
                writer.write("        <description>");
                writer.write(escape(tag.description));
                writer.write("</description>\n");
                for (AttributeInfo attr : tag.attributes) {
                    writer.write("        <attribute>\n");
                    writer.write("            <name>");
                    writer.write(escape(attr.name));
                    writer.write("</name>\n");
                    writer.write("            <required>false</required>\n");
                    writer.write("            <rtexprvalue>");
                    writer.write(attr.runtimeValueAllowed ? "true" : "false");
                    writer.write("</rtexprvalue>\n");
                    writer.write("            <description>");
                    writer.write(escape(attr.description));
                    writer.write("</description>\n");
                    writer.write("        </attribute>\n");
                }
                writer.write("    </tag>\n");
            }

            writer.write("</taglib>\n");
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static final class TagLibraryInfo {
        final String uri;
        final String descriptorFile;
        final String shortName;
        final String description;

        TagLibraryInfo(String uri, String descriptorFile, String shortName, String description) {
            this.uri = uri;
            this.descriptorFile = descriptorFile;
            this.shortName = shortName;
            this.description = description;
        }
    }

    private static final class TagInfo {
        final String name;
        final String tagClass;
        final String description;
        final List<AttributeInfo> attributes;

        TagInfo(String name, String tagClass, String description, List<AttributeInfo> attributes) {
            this.name = name;
            this.tagClass = tagClass;
            this.description = description;
            this.attributes = attributes;
        }
    }

    private static final class AttributeInfo {
        final String name;
        final String description;
        final boolean runtimeValueAllowed;

        AttributeInfo(String name, String description, boolean runtimeValueAllowed) {
            this.name = name;
            this.description = description;
            this.runtimeValueAllowed = runtimeValueAllowed;
        }
    }
}
