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
                .attribute("source-dir", Paths.get(System.getProperty("user.dir")) + "/")
                .attribute("include-dir", outputDir.resolve("asciidoc").resolve(type).toString() + "/")
                .attribute("imagesdir", outputDir.resolve("asciidoc").resolve(type).resolve("images").toString() + "/")
                .attribute("doctype", "book")
                .attribute("toclevels", "8")
                .attribute("sectanchors", "true")
                .attribute("docinfo1", "true")
                .attribute("project-version", "0.8.0-SNAPSHOT")
                .attribute("revnumber", "0.8.0-SNAPSHOT")
                .attribute("product-name", "INCEpTION")
                .attribute("product-website-url", "https://inception-project.github.io")
                .attribute("icons", "font")
                .attribute("toc", "preamble")
                .get();
        OptionsBuilder options = OptionsBuilder.options()
                .toDir(outputDir.toFile())
                .safe(SafeMode.UNSAFE)
                .attributes(attributes);
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        File f = new File(outputDir.resolve("asciidoc").resolve(type).toString() + ".adoc");
        asciidoctor.convertFile(f , options);
    }

    public static void main(String... args) throws Exception
    {
        Path inceptionDir = Paths.get(System.getProperty("user.dir"));
        Path webannoDir = inceptionDir.getParent().resolve("webanno");
        Path outputDir = inceptionDir.resolve("doc-out");

        List<Path> modules = new ArrayList<>();
        modules.addAll(getAsciiDocs(webannoDir));
        modules.addAll(getAsciiDocs(inceptionDir));

        FileUtils.deleteQuietly(outputDir.toFile());
        Files.createDirectory(outputDir);

        for (Path module : modules) {
            List<File> files = (List<File>) FileUtils.listFiles(module.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            for (File f: files) {
                Path p = f.toPath();
                Path targetPath = f.toPath().subpath(module.toAbsolutePath().getNameCount(), p.toAbsolutePath().getNameCount());
                FileUtils.copyFile(f, outputDir.resolve("asciidoc").resolve(targetPath).toFile());
            }
        }

        buildDoc("user-guide", outputDir);
        buildDoc("developer-guide", outputDir);
        buildDoc("admin-guide", outputDir);
    }


}
