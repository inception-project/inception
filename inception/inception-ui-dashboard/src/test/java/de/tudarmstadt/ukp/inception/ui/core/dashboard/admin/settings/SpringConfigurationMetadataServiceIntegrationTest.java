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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings.model.ConfigurationMetadata;

/**
 * Verifies what {@link SpringConfigurationMetadataService} reports for nested-property holders
 * declared with different field shapes on the outer {@code @ConfigurationProperties} class.
 *
 * <p>
 * Findings the test pins down (each case is a static nested holder; the differentiator is the outer
 * field's declaration):
 * <ul>
 * <li><b>SETTER</b>: non-{@code final} field with a setter on the outer class. The
 * {@code spring-boot-configuration-processor} extracts {@code defaultValue} from the holder's field
 * initialisers.</li>
 * <li><b>FINAL</b>: {@code final} field, no setter (matches the {@code AssistantPropertiesImpl}
 * convention). Processor extracts {@code defaultValue}.</li>
 * <li><b>BARE</b>: non-{@code final}, no setter. Processor still extracts {@code defaultValue} --
 * the static-ness of the holder is the actual prerequisite, not the outer field's shape.</li>
 * <li><b>METHOD_INIT</b>: static holder, but the holder's field is initialised via a method call.
 * The processor cannot evaluate the initialiser, so no {@code defaultValue} is emitted. The
 * service's live-lookup fallback (walking the {@code @ConfigurationProperties} bean tree via
 * getters) must then surface the runtime value so the property is never reported as "unset".</li>
 * </ul>
 *
 * Regression guard for the {@code RecommenderPropertiesImpl.Messages} situation we hit on the
 * {@code feature/5991-Ability-for-admin-to-see-global-settings} branch.
 */
@SpringBootTest( //
        classes = SpringConfigurationMetadataServiceIntegrationTest.TestContext.class, //
        properties = { //
                "spring.main.banner-mode=off", //
                // Load a real classpath file so the property source has TextResourceOrigin
                // tracking -- exercises the OriginLookup-based filename extraction in the
                // service. Inline "properties=" entries don't carry a file origin and would
                // not exercise that path.
                "spring.config.import=classpath:test-map-case.properties" })
public class SpringConfigurationMetadataServiceIntegrationTest
{
    enum FieldShape
    {
        SETTER("test-setter-case", true), FINAL("test-final-case", true),
        BARE("test-bare-case", true),
        // METHOD_INIT exercises the gap that the service's live-lookup fallback fills:
        // the processor can't extract a literal default from a method-call initialiser.
        METHOD_INIT("test-method-init-case", false);

        final String prefix;
        /**
         * Whether the {@code spring-boot-configuration-processor} is expected to write a
         * {@code defaultValue} into {@code spring-configuration-metadata.json} for this shape.
         */
        final boolean processorEmitsDefault;

        FieldShape(String aPrefix, boolean aProcessorEmitsDefault)
        {
            prefix = aPrefix;
            processorEmitsDefault = aProcessorEmitsDefault;
        }

        String propertyName()
        {
            return prefix + ".holder.flag";
        }
    }

    /**
     * Runtime value of {@code holder.flag} on every fixture below. Kept identical so the observable
     * result is the same string regardless of which path supplies it.
     */
    private static final String EXPECTED_VALUE = "true";

    private @Autowired Environment environment;
    private @Autowired ListableBeanFactory beanFactory;

    /**
     * Public-API guarantee for every shape: the property is listed and not unset (defaultValue or
     * effectiveValue is non-null). This is the regression guard against the
     * {@code Messages}-was-non-static problem and against any future regression where a
     * runtime-only initialiser would otherwise come out blank.
     */
    @ParameterizedTest
    @EnumSource(FieldShape.class)
    void propertyIsReportedNotUnset(FieldShape aShape)
    {
        var property = findProperty(newService().listProperties(), aShape.propertyName());

        assertThat(property.getDefaultValue() != null || property.getEffectiveValue() != null) //
                .as("property %s should not be unset (default=%s, effective=%s)",
                        aShape.propertyName(), property.getDefaultValue(),
                        property.getEffectiveValue()) //
                .isTrue();
    }

    /**
     * Whichever path supplies the value (metadata default or live-lookup fallback), it must equal
     * the holder's runtime field value.
     */
    @ParameterizedTest
    @EnumSource(FieldShape.class)
    void reportedValueMatchesRuntimeState(FieldShape aShape)
    {
        var property = findProperty(newService().listProperties(), aShape.propertyName());

        var observed = property.getEffectiveValue() != null //
                ? property.getEffectiveValue() //
                : property.getDefaultValue();

        assertThat(observed) //
                .as("reported value for %s", aShape.propertyName()) //
                .isEqualTo(EXPECTED_VALUE);
    }

    /**
     * Pins down the {@code spring-boot-configuration-processor}'s behaviour for each shape. If a
     * Spring Boot upgrade ever changes this (e.g. METHOD_INIT starts getting a default, making the
     * live-lookup fallback unnecessary for that case), this assertion will flag it.
     */
    @ParameterizedTest
    @EnumSource(FieldShape.class)
    void processorMetadataDefaultMatchesShapeExpectation(FieldShape aShape) throws IOException
    {
        var metadataDefault = readProcessorDefault(aShape.propertyName());

        if (aShape.processorEmitsDefault) {
            assertThat(metadataDefault) //
                    .as("processor should emit defaultValue for %s shape", aShape) //
                    .isNotNull() //
                    .isEqualTo(EXPECTED_VALUE);
        }
        else {
            assertThat(metadataDefault) //
                    .as("processor should NOT emit defaultValue for %s shape", aShape) //
                    .isNull();
        }
    }

    private SpringConfigurationMetadataServiceImpl newService()
    {
        return new SpringConfigurationMetadataServiceImpl(environment, emptyList(),
                () -> emptySet(), beanFactory);
    }

    private static ConfigurationProperty findProperty(List<ConfigurationProperty> aProperties,
            String aName)
    {
        return aProperties.stream() //
                .filter(p -> aName.equals(p.getName())) //
                .findFirst() //
                .orElseThrow(() -> new AssertionError("Property [" + aName + "] not listed"));
    }

    /**
     * Reads {@code defaultValue} for {@code aName} directly from
     * {@code META-INF/spring-configuration-metadata.json} on the classpath -- i.e. exactly what the
     * {@code spring-boot-configuration-processor} wrote at compile time, bypassing any live-lookup
     * fallback the service applies.
     */
    private static String readProcessorDefault(String aName) throws IOException
    {
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver
                .getResources("classpath*:META-INF/spring-configuration-metadata.json");
        for (var resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                var metadata = JSONUtil.fromJsonStream(ConfigurationMetadata.class, in);
                for (var property : metadata.getProperties()) {
                    if (aName.equals(property.getName()) && property.getDefaultValue() != null) {
                        return String.valueOf(property.getDefaultValue());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Reproduces the multi-bean-per-prefix collision we observed at runtime:
     * {@code RecommenderPropertiesImpl} and {@code SpanRecommenderPropertiesImpl} both declare
     * {@code @ConfigurationProperties("recommender")}, and the live-lookup must fall through to the
     * bean that actually owns the requested sub-path.
     */
    @Test
    void liveLookupFallsThroughWhenMultipleBeansSharePrefix()
    {
        var properties = newService().listProperties();

        var owned = findProperty(properties, "test-shared-prefix.deep.flag");
        var observed = owned.getEffectiveValue() != null //
                ? owned.getEffectiveValue() //
                : owned.getDefaultValue();

        assertThat(observed) //
                .as("property only owned by SharedPrefixOwnerProperties should be resolved "
                        + "even though SharedPrefixOtherProperties shares the prefix and does "
                        + "not declare it") //
                .isEqualTo(EXPECTED_VALUE);
    }

    /**
     * Hidden namespaces support multi-segment prefixes, not just top-level segments. So adding
     * {@code "telemetry.matomo"} to the hidden set hides exactly the matomo subtree while leaving
     * other {@code telemetry.*} properties visible. A bare top-level entry (e.g.
     * {@code "telemetry"}) still hides everything below it as before.
     */
    @Test
    void multiSegmentHiddenPrefixHidesOnlyMatchingSubtree()
    {
        var sut = new SpringConfigurationMetadataServiceImpl(environment, emptyList(),
                () -> Set.of("test-hidden.matomo"), beanFactory);

        var names = sut.listProperties().stream() //
                .map(ConfigurationProperty::getName) //
                .filter(n -> n.startsWith("test-hidden.")) //
                .toList();

        assertThat(names) //
                .as("multi-segment prefix hides exactly the matomo subtree") //
                .doesNotContain("test-hidden.matomo.flag") //
                .contains("test-hidden.kept.flag");
    }

    @Test
    void topLevelHiddenPrefixHidesEntireSubtree()
    {
        var sut = new SpringConfigurationMetadataServiceImpl(environment, emptyList(),
                () -> Set.of("test-hidden"), beanFactory);

        var names = sut.listProperties().stream() //
                .map(ConfigurationProperty::getName) //
                .filter(n -> n.startsWith("test-hidden.")) //
                .toList();

        assertThat(names) //
                .as("top-level prefix hides the entire subtree") //
                .isEmpty();
    }

    /**
     * Map-typed properties (e.g. {@code style.header.icon} as a {@code Map<String, HeaderLink>})
     * are flattened by Spring into leaf keys in the property source -- there is no literal
     * {@code style.header.icon} key. The service must:
     * <ul>
     * <li>still attribute the property to the source that supplied the leaves (not "default"),
     * and</li>
     * <li>render the value in a form that mirrors what was actually written, instead of
     * {@code Map.toString()} with object hashes.</li>
     * </ul>
     */
    @Test
    void mapTypedPropertyAttributesSourceAndRendersLeafKeys()
    {
        var property = findProperty(newService().listProperties(), "test-map-case.icon");

        assertThat(property.getEffectiveSource()) //
                .as("source must be attributed to the .properties filename via the leaf "
                        + "keys' Origin tracking, not to the verbose 'Config resource ... "
                        + "via location ...' form, and not to 'default'") //
                .isEqualTo("test-map-case.properties");

        assertThat(property.getEffectiveValue()) //
                .as("value should be rendered as flattened leaf key=value lines, not as "
                        + "Map.toString() with object hashes") //
                .isNotNull() //
                .doesNotContain("@") //
                .contains("inception.link-url=https://inception-project.github.io/") //
                .contains("inception.image-url=images/inception.png") //
                .contains("docs.link-url=https://example.org/docs") //
                .contains("docs.image-url=images/docs.png");

        // Lines should be sorted so the output is stable.
        var lines = property.getEffectiveValue().split("\n");
        var sorted = lines.clone();
        java.util.Arrays.sort(sorted);
        assertThat(lines) //
                .as("rendered lines should be sorted for stable output") //
                .containsExactly(sorted);
    }

    @Configuration
    @EnableConfigurationProperties({ //
            SetterCaseProperties.class, //
            FinalCaseProperties.class, //
            BareCaseProperties.class, //
            MethodInitCaseProperties.class, //
            SharedPrefixOwnerProperties.class, //
            SharedPrefixOtherProperties.class, //
            MapCaseProperties.class, //
            HiddenCaseProperties.class })
    public static class TestContext
    {
        // Beans enabled via @EnableConfigurationProperties above.
    }

    /** Non-final field, with setter. */
    @ConfigurationProperties("test-setter-case")
    public static class SetterCaseProperties
    {
        private Holder holder = new Holder();

        public Holder getHolder()
        {
            return holder;
        }

        public void setHolder(Holder aHolder)
        {
            holder = aHolder;
        }

        public static class Holder
        {
            private boolean flag = true;

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }
    }

    /** Final field, no setter. */
    @ConfigurationProperties("test-final-case")
    public static class FinalCaseProperties
    {
        private final Holder holder = new Holder();

        public Holder getHolder()
        {
            return holder;
        }

        public static class Holder
        {
            private boolean flag = true;

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }
    }

    /**
     * Non-final field, no setter -- the original {@code RecommenderPropertiesImpl.messages} shape
     * after {@code Messages} was made static.
     */
    @ConfigurationProperties("test-bare-case")
    public static class BareCaseProperties
    {
        private Holder holder = new Holder();

        public Holder getHolder()
        {
            return holder;
        }

        public static class Holder
        {
            private boolean flag = true;

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }
    }

    /**
     * Holder field initialised via a method call. The {@code spring-boot-configuration-processor}
     * only extracts literal default values; method-call initialisers are opaque to it, so no
     * {@code defaultValue} is emitted. The service must surface the runtime value via its
     * live-lookup fallback.
     */
    @ConfigurationProperties("test-method-init-case")
    public static class MethodInitCaseProperties
    {
        private final Holder holder = new Holder();

        public Holder getHolder()
        {
            return holder;
        }

        public static class Holder
        {
            private boolean flag = computeDefault();

            private static boolean computeDefault()
            {
                return true;
            }

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }
    }

    /**
     * Owns {@code test-shared-prefix.deep.flag}. Shares the {@code test-shared-prefix} prefix with
     * {@link SharedPrefixOtherProperties}, which does not declare {@code deep.flag}.
     */
    @ConfigurationProperties("test-shared-prefix")
    public static class SharedPrefixOwnerProperties
    {
        private final Deep deep = new Deep();

        public Deep getDeep()
        {
            return deep;
        }

        public static class Deep
        {
            private boolean flag = true;

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }
    }

    /**
     * Shares the {@code test-shared-prefix} prefix with {@link SharedPrefixOwnerProperties} but
     * declares a different sub-path. The live-lookup must not stop at this bean when resolving
     * {@code test-shared-prefix.deep.flag}.
     */
    @ConfigurationProperties("test-shared-prefix")
    public static class SharedPrefixOtherProperties
    {
        private boolean unrelated;

        public boolean isUnrelated()
        {
            return unrelated;
        }

        public void setUnrelated(boolean aUnrelated)
        {
            unrelated = aUnrelated;
        }
    }

    /**
     * Two static nested holders so we can verify multi-segment hidden-namespace matching:
     * {@code test-hidden.matomo.*} (intended to be hidden) and {@code test-hidden.kept.*} (intended
     * to remain visible).
     */
    @ConfigurationProperties("test-hidden")
    public static class HiddenCaseProperties
    {
        private final Matomo matomo = new Matomo();
        private final Kept kept = new Kept();

        public Matomo getMatomo()
        {
            return matomo;
        }

        public Kept getKept()
        {
            return kept;
        }

        public static class Matomo
        {
            private boolean flag = false;

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }

        public static class Kept
        {
            private boolean flag = false;

            public boolean isFlag()
            {
                return flag;
            }

            public void setFlag(boolean aFlag)
            {
                flag = aFlag;
            }
        }
    }

    /**
     * Mirrors {@code HeaderLinkPropertiesImpl}: a {@code Map<String, Entry>} bound from leaf keys
     * in YAML/properties. The metadata processor records a single entry for the {@code icon}
     * prefix; the actual values are spread across leaf keys in the property source.
     */
    @ConfigurationProperties("test-map-case")
    public static class MapCaseProperties
    {
        private Map<String, Entry> icon = new HashMap<>();

        public Map<String, Entry> getIcon()
        {
            return icon;
        }

        public void setIcon(Map<String, Entry> aIcon)
        {
            icon = aIcon;
        }

        public static class Entry
        {
            private String linkUrl;
            private String imageUrl;

            public String getLinkUrl()
            {
                return linkUrl;
            }

            public void setLinkUrl(String aLinkUrl)
            {
                linkUrl = aLinkUrl;
            }

            public String getImageUrl()
            {
                return imageUrl;
            }

            public void setImageUrl(String aImageUrl)
            {
                imageUrl = aImageUrl;
            }
        }
    }
}
