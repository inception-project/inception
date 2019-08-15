/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.doc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;

public class GenerateDocumentation
{
    private static Path asciiDocPath = Paths.get("src", "main", "resources", "META-INF", "asciidoc");


    private static List<Path> getAsciiDocs(Path dir) throws Exception
    {
        return Files.list(dir)
                .filter(Files::isDirectory)
                .filter(p -> Files.exists(p.resolve("pom.xml")))
                .map(p -> p.resolve(asciiDocPath))
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
    }

    private static void buildDoc(String type, Path outputDir)
    {
        Attributes attributes = AttributesBuilder.attributes()
                .attribute("source-dir", getInceptionDir() + "/")
                .attribute("include-dir", outputDir.resolve("asciidoc").resolve(type)
                        .toString() + "/")
                .attribute("imagesdir", outputDir.resolve("asciidoc").resolve(type)
                        .resolve("images").toString() + "/")
                .attribute("doctype", "book")
                .attribute("toclevels", "8")
                .attribute("sectanchors", "true")
                .attribute("docinfo1", "true")
                .attribute("project-version", "DEVELOPER BUILD")
                .attribute("revnumber", "DEVELOPER BUILD")
                .attribute("product-name", "INCEpTION")
                .attribute("product-website-url", "https://inception-project.github.io")
                .attribute("icons", "font")
                .attribute("toc", "preamble")
                .attribute("sourceHighlighter", "coderay")
                .get();
        OptionsBuilder options = OptionsBuilder.options()
                .toDir(outputDir.toFile())
                .safe(SafeMode.UNSAFE)
                .attributes(attributes);
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.requireLibrary("asciidoctor-diagram");
        File f = new File(outputDir.resolve("asciidoc").resolve(type).toString() + ".adoc");
        asciidoctor.convertFile(f , options);
    }

    public static void main(String... args) throws Exception
    {

        Path inceptionDir = getInceptionDir();
        Path webannoDir = inceptionDir.getParent().resolve("webanno");
        Path outputDir = Paths.get(System.getProperty("user.dir")).resolve("target")
                .resolve("doc-out");

        Files.createDirectories(outputDir);

        List<Path> modules = new ArrayList<>();
        modules.addAll(getAsciiDocs(webannoDir));
        modules.addAll(getAsciiDocs(inceptionDir));

        FileUtils.deleteQuietly(outputDir.toFile());
        Files.createDirectory(outputDir);

        for (Path module : modules) {
            System.out.printf("Including module: %s\n", module);
            
            for (File f : FileUtils.listFiles(module.toFile(), TrueFileFilter.INSTANCE,
                    TrueFileFilter.INSTANCE)) {
                Path p = f.toPath();
                Path targetPath = f.toPath().subpath(module.toAbsolutePath().getNameCount()
                        , p.toAbsolutePath().getNameCount());
                FileUtils.copyFile(f, outputDir.resolve("asciidoc").resolve(targetPath).toFile());
            }
        }

        buildDoc("user-guide", outputDir);
        buildDoc("developer-guide", outputDir);
        buildDoc("admin-guide", outputDir);
        
        System.out.printf("Documentation written to: %s\n", outputDir);
    }

    private static Path getInceptionDir()
    {
        Path userDir = Paths.get(System.getProperty("user.dir"));
        return runningFromIntelliJ() ? userDir : userDir.getParent();
    }

    private static boolean runningFromIntelliJ()
    {
        return System.getenv().containsKey("INTELLIJ");
    }
}
