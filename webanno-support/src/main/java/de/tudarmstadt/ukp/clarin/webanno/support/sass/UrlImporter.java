package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.webjars.WicketWebjars;
import de.agilecoders.wicket.webjars.util.WebJarAssetLocator;
import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;

/**
 * SASS importer that knows how to load dependencies (imports) from:
 * <ul>
 * <li>WebJars</li>
 * <li>classpath</li>
 * <li>same package, different jar</li>
 * </ul>
 */
class UrlImporter
    implements Importer
{

    private static final Logger LOG = LoggerFactory.getLogger(SassSource.class);

    private static final String SCSS_EXT = ".scss";
    private static final String SASS_EXT = ".sass";

    /**
     * A mediator class that loads a class from wicket-webjars only when a dependency (an import)
     * with scheme "webjars!" needs to be resolved
     */
    private static final class Holder
    {
        private static final WebJarAssetLocator locator = new WebJarAssetLocator(
                WicketWebjars.settings());
    }

    public static final String CLASSPATH_SCHEME = "classpath!";
    public static final String PACKAGE_SCHEME = "package!";
    public static final String WEBJARS_SCHEME = "webjars!";
    public static final String WEB_CONTEXT_SCHEME = "webcontext!";
    public static final String JAR_SCHEME = "jar!";

    /**
     * The scope class used with SassResourceReference. Used to resolve dependencies with scheme
     * "package!"
     */
    private final String scopeClass;

    /**
     * @param scopeClass
     *            The scope class used with SassResourceReference.
     */
    public UrlImporter(String scopeClass)
    {
        this.scopeClass = scopeClass;
    }

    @Override
    public Collection<Import> apply(String url, Import previous)
    {
        return Stream.concat(withExtension(url), cssUri(url))
                .map(candidate -> tryResolveUrl(previous.getAbsoluteUri(), candidate))
                .filter(Optional::isPresent).map(Optional::get).findFirst()
                .map(Collections::singletonList).orElse(null);
    }

    private Optional<Import> tryResolveUrl(URI base, String url)
    {
        final Optional<Import> newImport;
        if (StringUtils.startsWith(url, WEBJARS_SCHEME)) {
            newImport = resolveWebJarsDependency(url);
        }
        else if (StringUtils.startsWith(url, CLASSPATH_SCHEME)) {
            newImport = resolveClasspathDependency(url);
        }
        else if (StringUtils.startsWith(url, WEB_CONTEXT_SCHEME)) {
            newImport = resolveWebContextDependency(url);
        }
        else if (scopeClass != null && StringUtils.startsWith(url, PACKAGE_SCHEME)) {
            newImport = resolvePackageDependency(url);
        }
        else {
            newImport = resolveLocalDependency(base, url);
        }

        return newImport;
    }

    private Stream<String> withExtension(String url)
    {
        String withScssExt = url + SCSS_EXT;
        String withSassExt = url + SASS_EXT;
        return url.endsWith(SCSS_EXT) || url.endsWith(SASS_EXT) ? Stream.of(url, addUnderscore(url))
                : Stream.of(withScssExt, addUnderscore(withScssExt), withSassExt,
                        addUnderscore(withSassExt));
    }

    private Stream<String> cssUri(String url)
    {
        return Stream.of(url).filter(u -> u.endsWith(".css"));
    }

    private String addUnderscore(String url)
    {
        int lastSlash = url.lastIndexOf("/");
        return new StringBuilder(url).insert(lastSlash + 1, "_").toString();
    }

    private Optional<Import> resolveWebJarsDependency(String url)
    {
        LOG.debug("Going to resolve an import from WebJars: {}", url);

        try {
            String file = UrlImporter.Holder.locator
                    .getFullPath(url.replaceFirst(WEBJARS_SCHEME, "/webjars/"));
            URL importUrl = Thread.currentThread().getContextClassLoader().getResource(file);
            return Optional.ofNullable(importUrl).map(this::buildImport);
        }
        catch (WebJarAssetLocator.ResourceException e) {
            LOG.debug("Webjar resource [" + url + "] wasn't found");
        }
        catch (RuntimeException e) {
            throw new WicketRuntimeException(e);
        }

        return Optional.empty();
    }

    private Optional<Import> resolveClasspathDependency(String url)
    {
        LOG.debug("Going to resolve an import from the classpath: {}", url);
        String resourceName = url.substring(CLASSPATH_SCHEME.length() + 1);
        if (resourceName.indexOf(0) != '/') {
            resourceName = '/' + resourceName;
        }

        URL importUrl = SassCacheManager.class.getResource(resourceName);
        return Optional.ofNullable(importUrl).map(this::buildImport);
    }

    private Optional<Import> resolveWebContextDependency(String url)
    {
        LOG.debug("Going to resolve an import from the web context: {}", url);
        String resourceName = url.substring(WEB_CONTEXT_SCHEME.length());
        if (resourceName.indexOf(0) == '/') {
            resourceName = resourceName.substring(1);
        }

        final ServletContext context = ((WebApplication) Application.get()).getServletContext();
        try {
            return Optional.of(buildImport(context.getResource(resourceName)));
        }
        catch (MalformedURLException ex) {
            throw new IllegalArgumentException(
                    "Cannot create a URL to a resource in the web context", ex);
        }
    }

    private Optional<Import> resolvePackageDependency(String url)
    {
        if (Strings.isEmpty(scopeClass)) {
            throw new IllegalStateException(
                    "Cannot resolve dependency '" + url + "' without a scope class!");
        }

        LOG.debug("Going to resolve an import from the package: {}", url);
        String resourceName = url.startsWith(PACKAGE_SCHEME)
                ? url.substring(PACKAGE_SCHEME.length())
                : url;
        if (resourceName.indexOf(0) == '/') {
            resourceName = resourceName.substring(1);
        }

        Class<?> scope = WicketObjects.resolveClass(scopeClass);
        URL importUrl = scope.getResource(resourceName);

        return Optional.ofNullable(importUrl).map(this::buildImport);

    }

    private Optional<Import> resolveLocalDependency(URI base, String url)
    {
        LOG.debug("Going to resolve an import from local dependency: {}", url);
        String importUrl = getAbsolutePath(base, url);
        Optional<Import> localImport = resolveLocalFileDependency(importUrl);

        // local resource maybe inside jar, webjar
        return localImport.isPresent() ? localImport : resolveJarDependency(importUrl);
    }

    private Optional<Import> resolveJarDependency(String url)
    {
        LOG.debug("Going to resolve an import from jar file: {}", url);
        // Using the last index here because in FAT JARs (e.g Spring Boot), there can be multiple
        // nested JARs
        int jarSchemeIndex = url.lastIndexOf(JAR_SCHEME);
        if (jarSchemeIndex == -1) {
            return Optional.empty();
        }

        int resourceNameIndex = jarSchemeIndex + JAR_SCHEME.length();
        String resourceName = url.substring(resourceNameIndex);
        if (!resourceName.startsWith("/")) {
            resourceName = "/" + resourceName;
        }

        URL importUrl = SassCacheManager.class.getResource(resourceName);
        return Optional.ofNullable(importUrl).map(this::buildImport);
    }

    private Optional<Import> resolveLocalFileDependency(String url)
    {
        LOG.debug("Going to resolve an import from local file: {}", url);
        File file = new File(url);

        if (file.exists()) {
            try {
                return Optional.of(buildImport(file.toURI().toURL()));
            }
            catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot resolve local dependency at path '%s'", url));
            }
        }

        return Optional.empty();
    }

    private String getAbsolutePath(URI base, String url)
    {
        String basePath = base.toString();
        Path parentBasePath = Paths.get(basePath).getParent();
        return parentBasePath.resolve(url).toString();
    }

    private Import buildImport(URL importUri)
    {
        try {
            final String contents = read(importUri);
            return new Import(importUri.toURI(), importUri.toURI(), contents);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String read(URL url)
    {
        try {
            return IOUtils.toString(url.openStream(), StandardCharsets.UTF_8.name());
        }
        catch (IOException ex) {
            throw new WicketRuntimeException(ex);
        }
    }
}
