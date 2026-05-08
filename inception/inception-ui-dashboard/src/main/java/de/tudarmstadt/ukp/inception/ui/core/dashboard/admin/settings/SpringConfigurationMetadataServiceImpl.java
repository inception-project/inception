/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings.model.ConfigurationMetadata;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings.model.ConfigurationMetadataProperty;

public class SpringConfigurationMetadataServiceImpl
    implements SpringConfigurationMetadataService
{
    private static final Logger LOG = LoggerFactory
            .getLogger(SpringConfigurationMetadataServiceImpl.class);

    private static final String METADATA_PATTERN = //
            "classpath*:META-INF/spring-configuration-metadata.json";

    // Name of the ConfigurationPropertySourcesPropertySource that Spring Boot
    // attaches at index 0 to wrap all other sources for fast binder lookups.
    // Mirrors ConfigurationPropertySources.ATTACHED_PROPERTY_SOURCE_NAME, which
    // is package-private in Spring Boot 4 and not part of the public API.
    private static final String ATTACHED_PROPERTY_SOURCE_NAME = "configurationProperties";

    private final Environment environment;
    private final Sanitizer sanitizer;
    private final SettingsPageProperties settingsProperties;
    private final ListableBeanFactory beanFactory;

    private volatile List<ConfigurationMetadataProperty> cachedMetadata;
    // Multiple @ConfigurationProperties beans may share the same prefix (each binding the
    // subset of properties it has accessors for, e.g. recommender + spanRecommender both
    // declare prefix "recommender"). Keep all of them so the live-lookup can fall through
    // to whichever bean actually owns the requested sub-path.
    private volatile Map<String, List<Object>> cachedConfigPropertyBeans;

    public SpringConfigurationMetadataServiceImpl(Environment aEnvironment,
            Iterable<SanitizingFunction> aSanitizingFunctions,
            SettingsPageProperties aSettingsProperties, ListableBeanFactory aBeanFactory)
    {
        environment = aEnvironment;
        sanitizer = new Sanitizer(aSanitizingFunctions);
        settingsProperties = aSettingsProperties;
        beanFactory = aBeanFactory;
    }

    @Override
    public List<ConfigurationProperty> listProperties()
    {
        var metadata = getMetadata();
        var hidden = settingsProperties.getHiddenNamespaces();
        // Snapshot every PropertySource's property names once -- without this we'd rescan
        // every enumerable source for every metadata property, giving O(properties x sources
        // x keys) behavior on a page with thousands of keys across dozens of sources.
        var sources = snapshotSources();

        var views = new ArrayList<ConfigurationProperty>(metadata.size());
        for (var property : metadata) {
            if (isHidden(property.getName(), hidden)) {
                continue;
            }
            views.add(toView(property, sources));
        }
        views.sort(comparing(ConfigurationProperty::getName));
        return views;
    }

    private static boolean isHidden(String aName, Set<String> aHidden)
    {
        if (aName == null || aHidden == null || aHidden.isEmpty()) {
            return false;
        }
        for (var hidden : aHidden) {
            if (aName.equals(hidden) || aName.startsWith(hidden + ".")) {
                return true;
            }
        }
        return false;
    }

    private ConfigurationProperty toView(ConfigurationMetadataProperty aProperty,
            List<SourceSnapshot> aSources)
    {
        var name = aProperty.getName();

        var defaultValue = sanitizeAsString(null, name, aProperty.getDefaultValue());

        String effectiveValue = null;
        String effectiveSource = null;
        var match = findSource(name, aSources);
        if (match != null) {
            var source = match.source;
            var raw = source.getProperty(name);
            if (raw != null) {
                effectiveValue = sanitizeAsString(source, name, raw);
            }
            else {
                // The source contains sub-keys under "<name>." but not "<name>" itself --
                // e.g. a Map/Collection-typed property bound from YAML, where Spring flattens
                // the hierarchy into leaf keys. Render the value from the source's leaf keys
                // so the displayed form mirrors what's in settings.yml/properties, and
                // attribute it to that source.
                effectiveValue = renderFromLeafKeys(match, name);
                if (effectiveValue == null) {
                    effectiveValue = sanitizeAsString(source, name, readLiveValue(name));
                }
            }
            effectiveSource = resolveSourceName(match, name);
        }
        else if (environment.containsProperty(name)) {
            // Resolved through the environment but not directly attributable to one source
            // (e.g. a placeholder-resolved value). Sanitize without a source attribution.
            effectiveValue = sanitizeAsString(null, name, environment.getProperty(name));
        }
        else if (defaultValue == null) {
            // No PropertySource override and the metadata processor did not record a default
            // (e.g. method-call field initializers, constructor-set fields, or values computed
            // at runtime by getters). Walk the matching @ConfigurationProperties bean tree to
            // surface the live value as the effective default, since no source overrides it.
            var live = readLiveValue(name);
            if (live != null) {
                defaultValue = sanitizeAsString(null, name, live);
            }
            else {
                LOG.trace("Property [{}] comes out unset: no PropertySource override, no "
                        + "metadata default, and live-lookup returned null", name);
            }
        }

        var deprecationNote = describeDeprecation(aProperty);

        return new ConfigurationProperty( //
                name, //
                aProperty.getType(), //
                aProperty.getDescription(), //
                aProperty.getSourceType(), //
                defaultValue, //
                effectiveValue, //
                effectiveSource, //
                aProperty.isDeprecated(), //
                deprecationNote);
    }

    private String sanitizeAsString(PropertySource<?> aSource, String aName, Object aValue)
    {
        if (aValue == null) {
            return null;
        }
        var sanitized = sanitizer.sanitize(new SanitizableData(aSource, aName, aValue), true);
        return sanitized == null ? null : String.valueOf(sanitized);
    }

    /**
     * Read the live value of a property by walking the matching {@code @ConfigurationProperties}
     * bean tree via getter reflection. Returns {@code null} if no bean owns this property, the walk
     * hits a null intermediate value, or any reflection step fails.
     */
    private Object readLiveValue(String aName)
    {
        var beans = getConfigPropertyBeans();

        // Find the longest prefix that matches this property name. Multiple beans may share
        // it (e.g. RecommenderPropertiesImpl and SpanRecommenderPropertiesImpl both declare
        // prefix "recommender") -- each binds the subset of properties for which it has
        // accessors, so we have to try them all and return the first non-null result.
        List<Object> candidates = null;
        String matchedPrefix = null;
        for (var entry : beans.entrySet()) {
            var prefix = entry.getKey();
            if (aName.equals(prefix) || aName.startsWith(prefix + ".")) {
                if (matchedPrefix == null || prefix.length() > matchedPrefix.length()) {
                    matchedPrefix = prefix;
                    candidates = entry.getValue();
                }
            }
        }
        if (candidates == null || matchedPrefix == null) {
            LOG.trace("Live-lookup for [{}] failed: no @ConfigurationProperties bean owns a "
                    + "matching prefix", aName);
            return null;
        }

        var remaining = aName.equals(matchedPrefix) //
                ? "" //
                : aName.substring(matchedPrefix.length() + 1);

        for (var bean : candidates) {
            var value = walkBean(aName, bean, remaining);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object walkBean(String aName, Object aBean, String aRemaining)
    {
        if (aRemaining.isEmpty()) {
            return aBean;
        }

        Object current = aBean;
        var segments = aRemaining.split("\\.");
        for (int i = 0; i < segments.length; i++) {
            if (current == null) {
                LOG.trace(
                        "Live-lookup for [{}] hit a null intermediate value at segment [{}] "
                                + "(after walking [{}]) on bean [{}]",
                        aName, segments[i], String.join(".", Arrays.copyOfRange(segments, 0, i)),
                        aBean.getClass().getName());
                return null;
            }
            var next = invokeGetter(current, segments[i]);
            if (next == null) {
                LOG.trace("Live-lookup for [{}] returned null from getter for segment [{}] on "
                        + "[{}] (no JavaBean accessor matched, or the getter returned null)", aName,
                        segments[i], current.getClass().getName());
            }
            current = next;
        }
        return current;
    }

    private static Object invokeGetter(Object aTarget, String aKebabSegment)
    {
        var camel = kebabToCamel(aKebabSegment);
        var capitalised = Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
        for (var prefix : new String[] { "get", "is" }) {
            try {
                var method = aTarget.getClass().getMethod(prefix + capitalised);
                return method.invoke(aTarget);
            }
            catch (NoSuchMethodException e) {
                // try next prefix
            }
            catch (ReflectiveOperationException e) {
                LOG.debug("Live-value getter [{}{}] on {} threw", prefix, capitalised,
                        aTarget.getClass().getName(), e);
                return null;
            }
        }
        return null;
    }

    private static String kebabToCamel(String aKebab)
    {
        var sb = new StringBuilder(aKebab.length());
        var capitalize = false;
        for (int i = 0; i < aKebab.length(); i++) {
            var c = aKebab.charAt(i);
            if (c == '-') {
                capitalize = true;
            }
            else if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Map<String, List<Object>> getConfigPropertyBeans()
    {
        var cached = cachedConfigPropertyBeans;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedConfigPropertyBeans != null) {
                return cachedConfigPropertyBeans;
            }
            var byPrefix = new HashMap<String, List<Object>>();
            for (var bean : beanFactory.getBeansWithAnnotation(ConfigurationProperties.class)
                    .values()) {
                var ann = AnnotationUtils.findAnnotation(bean.getClass(),
                        ConfigurationProperties.class);
                if (ann == null) {
                    continue;
                }
                var prefix = !ann.prefix().isEmpty() ? ann.prefix() : ann.value();
                if (!prefix.isEmpty()) {
                    byPrefix.computeIfAbsent(prefix, k -> new ArrayList<>()).add(bean);
                }
            }
            cachedConfigPropertyBeans = byPrefix;
            return cachedConfigPropertyBeans;
        }
    }

    private List<ConfigurationMetadataProperty> getMetadata()
    {
        var cached = cachedMetadata;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedMetadata != null) {
                return cachedMetadata;
            }
            cachedMetadata = loadMetadata();
            return cachedMetadata;
        }
    }

    private List<ConfigurationMetadataProperty> loadMetadata()
    {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources(METADATA_PATTERN);
        }
        catch (IOException e) {
            LOG.warn("Unable to scan classpath for [{}]", METADATA_PATTERN, e);
            return List.of();
        }

        // Properties may be declared in multiple jars (e.g. an interface in -api and the
        // impl in the impl module both contribute the same property names). Deduplicate
        // by name, keeping the first occurrence with a non-null description if available.
        var byName = new LinkedHashMap<String, ConfigurationMetadataProperty>();
        var seenResources = new HashSet<String>();
        for (var resource : resources) {
            var key = describeResource(resource);
            if (!seenResources.add(key)) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                var metadata = JSONUtil.fromJsonStream(ConfigurationMetadata.class, in);
                for (var property : metadata.getProperties()) {
                    if (property.getName() == null) {
                        continue;
                    }
                    byName.merge(property.getName(), property,
                            SpringConfigurationMetadataServiceImpl::pickBetter);
                }
            }
            catch (IOException e) {
                LOG.warn("Unable to read configuration metadata from [{}]", resource, e);
            }
        }

        LOG.debug("Loaded {} configuration metadata properties from {} resource(s)", byName.size(),
                resources.length);

        return List.copyOf(byName.values());
    }

    private static ConfigurationMetadataProperty pickBetter(ConfigurationMetadataProperty aFirst,
            ConfigurationMetadataProperty aSecond)
    {
        if (aFirst.getDescription() == null && aSecond.getDescription() != null) {
            return aSecond;
        }
        if (aFirst.getDefaultValue() == null && aSecond.getDefaultValue() != null) {
            return aSecond;
        }
        return aFirst;
    }

    /**
     * For property sources backed by a config file, return the file's basename (e.g.
     * {@code application.properties}, {@code settings.yml}) using Spring Boot's per-property origin
     * tracking. For other sources (system properties, environment variables, command-line args,
     * ...) fall back to the source's own name.
     */
    private static String resolveSourceName(SourceSnapshot aSnapshot, String aName)
    {
        var source = aSnapshot.source;
        if (source instanceof OriginLookup<?> rawLookup) {
            @SuppressWarnings("unchecked")
            var lookup = (OriginLookup<String>) rawLookup;
            var fileName = filenameFromOrigin(lookup, aName);
            if (fileName != null) {
                return fileName;
            }
            // Map/Collection-typed properties have no Origin under the literal prefix --
            // only the leaf keys do. Look up the origin of any leaf to discover the file.
            for (var key : aSnapshot.leafKeys(aName + ".")) {
                fileName = filenameFromOrigin(lookup, key);
                if (fileName != null) {
                    return fileName;
                }
            }
        }
        return source.getName();
    }

    private static String filenameFromOrigin(OriginLookup<String> aLookup, String aKey)
    {
        for (var origin = aLookup.getOrigin(aKey); origin != null; origin = origin.getParent()) {
            if (origin instanceof TextResourceOrigin textOrigin) {
                var resource = textOrigin.getResource();
                if (resource != null && resource.getFilename() != null) {
                    return resource.getFilename();
                }
            }
        }
        return null;
    }

    private static String describeResource(Resource aResource)
    {
        try {
            return aResource.getURI().toString();
        }
        catch (IOException e) {
            return aResource.getDescription();
        }
    }

    private List<SourceSnapshot> snapshotSources()
    {
        if (!(environment instanceof ConfigurableEnvironment configurable)) {
            return emptyList();
        }
        var snapshots = new ArrayList<SourceSnapshot>();
        for (PropertySource<?> source : configurable.getPropertySources()) {
            // Skip the ConfigurationPropertySourcesPropertySource that Spring Boot
            // installs at index 0 to wrap all other sources for fast binder lookups.
            // It contains essentially every property and would mask the real origin.
            if (ATTACHED_PROPERTY_SOURCE_NAME.equals(source.getName())) {
                continue;
            }
            snapshots.add(new SourceSnapshot(source));
        }
        return snapshots;
    }

    private static SourceSnapshot findSource(String aName, List<SourceSnapshot> aSnapshots)
    {
        SourceSnapshot prefixMatch = null;
        var prefix = aName + ".";
        for (var snapshot : aSnapshots) {
            if (snapshot.sortedNames != null) {
                if (snapshot.names.contains(aName)) {
                    return snapshot;
                }
                if (prefixMatch == null && snapshot.hasLeafKey(prefix)) {
                    // Map/Collection-typed properties never appear under their literal name --
                    // Spring flattens YAML/properties hierarchies into leaf keys (e.g.
                    // "style.header.icon.inception.link-url"). Remember this source as a
                    // candidate but keep scanning in case a higher-priority source has the
                    // exact name.
                    prefixMatch = snapshot;
                }
            }
            else if (snapshot.source.getProperty(aName) != null) {
                return snapshot;
            }
        }
        return prefixMatch;
    }

    /**
     * One-shot snapshot of a {@link PropertySource}'s property names. For enumerable sources we
     * cache a {@link Set} for O(1) exact lookups and a sorted array for binary-search prefix scans,
     * so per-property work stays bounded across thousands of metadata entries.
     */
    private static final class SourceSnapshot
    {
        final PropertySource<?> source;
        final Set<String> names;
        final String[] sortedNames;

        SourceSnapshot(PropertySource<?> aSource)
        {
            source = aSource;
            if (aSource instanceof EnumerablePropertySource<?> enumerable) {
                var raw = enumerable.getPropertyNames();
                names = new HashSet<>(asList(raw));
                sortedNames = raw.clone();
                Arrays.sort(sortedNames);
            }
            else {
                names = null;
                sortedNames = null;
            }
        }

        boolean hasLeafKey(String aPrefix)
        {
            var idx = firstIndexAtOrAfter(aPrefix);
            return idx < sortedNames.length && sortedNames[idx].startsWith(aPrefix);
        }

        List<String> leafKeys(String aPrefix)
        {
            if (sortedNames == null) {
                return emptyList();
            }
            var result = new ArrayList<String>();
            for (var i = firstIndexAtOrAfter(aPrefix); i < sortedNames.length; i++) {
                if (!sortedNames[i].startsWith(aPrefix)) {
                    break;
                }
                result.add(sortedNames[i]);
            }
            return result;
        }

        private int firstIndexAtOrAfter(String aKey)
        {
            var idx = Arrays.binarySearch(sortedNames, aKey);
            return idx >= 0 ? idx : -idx - 1;
        }
    }

    /**
     * Render the value of a Map/Collection-typed property by collecting its leaf keys from
     * {@code aSource} (e.g. for {@code style.header.icon} bound from YAML, the leaves are
     * {@code style.header.icon.inception.link-url}, {@code style.header.icon.inception.image-url}
     * and so on). The output is a multi-line {@code <suffix>=<value>} block that closely mirrors
     * what was actually written in {@code settings.yml}/{@code .properties}, instead of the
     * {@code Map.toString()} form with object hashes.
     *
     * Returns {@code null} if the source has no leaf keys for {@code aName}, in which case callers
     * should fall back to the live value.
     */
    private String renderFromLeafKeys(SourceSnapshot aSnapshot, String aName)
    {
        var prefix = aName + ".";
        var leafKeys = aSnapshot.leafKeys(prefix);
        if (leafKeys.isEmpty()) {
            return null;
        }
        var source = aSnapshot.source;
        var sb = new StringBuilder();
        for (var key : leafKeys) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            var raw = source.getProperty(key);
            var sanitized = sanitizeAsString(source, key, raw);
            sb.append(key.substring(prefix.length())).append('=')
                    .append(sanitized != null ? sanitized : "");
        }
        return sb.toString();
    }

    private static String describeDeprecation(ConfigurationMetadataProperty aProperty)
    {
        var deprecation = aProperty.getDeprecation();
        if (deprecation == null) {
            return aProperty.isDeprecated() ? "Deprecated." : null;
        }
        var sb = new StringBuilder();
        if (deprecation.getLevel() != null) {
            sb.append('(').append(deprecation.getLevel()).append(") ");
        }
        if (deprecation.getReason() != null) {
            sb.append(deprecation.getReason()).append(' ');
        }
        if (deprecation.getReplacement() != null) {
            sb.append("Use ").append(deprecation.getReplacement()).append(" instead. ");
        }
        if (deprecation.getSince() != null) {
            sb.append("Since ").append(deprecation.getSince()).append('.');
        }
        var note = sb.toString().trim();
        return note.isEmpty() ? "Deprecated." : note;
    }
}
