/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.xmi.dkprobackport;

import static java.io.ObjectInputFilter.Config.createFilter;
import static java.lang.String.join;
import static org.dkpro.core.api.resources.CompressionUtils.getInputStream;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.TypeSystemUtil;
import org.dkpro.core.api.io.ResourceCollectionReaderBase;
import org.dkpro.core.api.parameter.MimeTypes;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import eu.openminted.share.annotations.api.DocumentationResource;
import eu.openminted.share.annotations.api.Parameters;

/**
 * UIMA Binary CAS formats reader.
 */
@ResourceMetaData(name = "UIMA Binary CAS Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@Parameters(exclude = { ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
        ResourceCollectionReaderBase.PARAM_INCLUDE_HIDDEN,
        ResourceCollectionReaderBase.PARAM_USE_DEFAULT_EXCLUDES,
        ResourceCollectionReaderBase.PARAM_LOG_FREQ, BinaryCasReader.PARAM_TYPE_SYSTEM_LOCATION })
@MimeTypeCapability({ MimeTypes.APPLICATION_X_UIMA_BINARY })
public class BinaryCasReader
    extends ResourceCollectionReaderBase
{
    private static final byte[] DKPRO_HEADER = new byte[] { 'D', 'K', 'P', 'r', 'o', '1' };

    private final static ObjectInputFilter SERIALIZED_CAS_INPUT_FILTER = createFilter(join(";", //
            CASCompleteSerializer.class.getName(), //
            CASSerializer.class.getName(), //
            CASMgrSerializer.class.getName(), //
            String.class.getName(), //
            "!*"));

    /**
     * The location from which to obtain the type system when the CAS is stored in form 0.
     */
    public static final String PARAM_TYPE_SYSTEM_LOCATION = "typeSystemLocation";
    @ConfigurationParameter(name = PARAM_TYPE_SYSTEM_LOCATION, mandatory = false)
    private String typeSystemLocation;

    /**
     * Determines whether the type system from a currently read file should be merged with the
     * current type system
     */
    public static final String PARAM_MERGE_TYPE_SYSTEM = "mergeTypeSystem";
    @ConfigurationParameter(name = PARAM_MERGE_TYPE_SYSTEM, mandatory = true, defaultValue = "false")
    private boolean mergeTypeSystem;

    /**
     * Add DKPro Core metadata if it is not already present in the document.
     */
    public static final String PARAM_ADD_DOCUMENT_METADATA = "addDocumentMetadata";
    @ConfigurationParameter(name = PARAM_ADD_DOCUMENT_METADATA, mandatory = true, defaultValue = "true")
    private boolean addDocumentMetadata;

    /**
     * Generate new DKPro Core document metadata (i.e. title, ID, URI) for the document instead of
     * retaining what is already present in the XMI file.
     */
    public static final String PARAM_OVERRIDE_DOCUMENT_METADATA = "overrideDocumentMetadata";
    @ConfigurationParameter(name = PARAM_OVERRIDE_DOCUMENT_METADATA, mandatory = true, defaultValue = "false")
    private boolean overrideDocumentMetadata;

    private CASMgrSerializer casMgrSerializer;

    private TypeSystemImpl typeSystem;

    @Override
    public void getNext(CAS aCAS) throws IOException, CollectionException
    {
        var res = nextFile();
        TypeSystemImpl xts = null;
        byte[] header = new byte[DKPRO_HEADER.length];

        if (this.mergeTypeSystem) {
            // type system from input file
            TypeSystemDescription tsd;

            try (var is = getInputStream(res.getLocation(), res.getInputStream())) {
                var bis = new BufferedInputStream(is);

                getLogger().debug("Reading CAS from [" + res.getLocation() + "]");

                // Prepare for format detection
                bis.mark(32);
                var dis = new DataInputStream(bis);
                dis.read(header);

                // If it is DKPro Core format, read the type system
                if (Arrays.equals(header, DKPRO_HEADER)) {
                    xts = readDKProHeader(bis, header, xts);
                }
                else {
                    // No embedded DKPro TS, reset
                    bis.reset();
                    // Try reading an externalized type system instead
                    if (typeSystemLocation != null) {
                        xts = readTypeSystem();
                        initCasFromEmbeddedTS(header, aCAS);
                    }
                }

                if (xts != null) {
                    // use external type system if specified
                    tsd = TypeSystemUtil.typeSystem2TypeSystemDescription(xts);
                }
                else {
                    // else load the CAS from the input file and use its type system
                    CasIOUtils.load(bis, null, aCAS, CasLoadMode.REINIT);
                    tsd = TypeSystemUtil.typeSystem2TypeSystemDescription(aCAS.getTypeSystem());
                }
            }

            try {
                // Merge the current type system with the one specified by the file being read
                var mergedTypeSystem = CasCreationUtils.mergeTypeSystems(Arrays
                        .asList(TypeSystemUtil.typeSystem2TypeSystemDescription(typeSystem), tsd));

                // Create a new CAS based on the merged type system
                var mergedTypeSystemCas = CasCreationUtils.createCas(mergedTypeSystem,
                        (TypePriorities) null, (FsIndexDescription[]) null).getJCas();

                // Create a holder for the CAS metadata
                var cms = Serialization.serializeCASMgr((mergedTypeSystemCas).getCasImpl());

                // Reinitialize CAS with merged type system
                ((CASImpl) aCAS).getBinaryCasSerDes().setupCasFromCasMgrSerializer(cms);
            }
            catch (CASException | ResourceInitializationException e) {
                throw new CollectionException(e);
            }
        }

        // Read file again, this time into a CAS which has been prepared with the merged TS
        try (var is = getInputStream(res.getLocation(), res.getInputStream())) {
            BufferedInputStream bis = new BufferedInputStream(is);
            bis.mark(32);
            DataInputStream dis = new DataInputStream(bis);
            dis.read(header);

            // If it is DKPro Core format, read the type system
            if (Arrays.equals(header, DKPRO_HEADER)) {
                xts = readDKProHeader(bis, header, xts);
            }
            else {
                // No embedded DKPro TS, reset
                bis.reset();
                // Try reading an externalized type system instead
                if (typeSystemLocation != null) {
                    xts = readTypeSystem();
                    initCasFromEmbeddedTS(header, aCAS);
                }

            }

            SerialFormat format;
            if (xts != null) {
                format = CasIOUtils.load(bis, aCAS, xts);
            }
            else {
                format = CasIOUtils.load(bis, aCAS);
            }
            getLogger().debug("Found format " + format);
        }
        catch (IOException e) {
            throw new CollectionException(e);
        }

        // Initialize the JCas sub-system which is the most often used API in DKPro Core components
        try {
            aCAS.getJCas();
        }
        catch (CASException e) {
            throw new CollectionException(e);
        }

        // Handle DKPro Core DocumentMetaData
        AnnotationFS docAnno = aCAS.getDocumentAnnotation();
        if (docAnno.getType().getName().equals(DocumentMetaData.class.getName())) {
            if (overrideDocumentMetadata) {
                // Unless the language is explicity set on the reader, try to retain the language
                // already present in the XMI file.
                String language = getLanguage();
                if (language == null) {
                    language = aCAS.getDocumentLanguage();
                }
                aCAS.removeFsFromIndexes(docAnno);

                initCas(aCAS, res);

                aCAS.setDocumentLanguage(language);
            }
        }
        else if (addDocumentMetadata) {
            initCas(aCAS, res);
        }
    }

    // Check whether this is original UIMA CAS format or DKPro Core Legacy format
    private TypeSystemImpl readDKProHeader(InputStream aIs, byte[] header, TypeSystemImpl ts)
        throws CollectionException
    {
        getLogger().debug("Found DKPro-Core-style embedded type system");
        try {
            var ois = new ObjectInputStream(aIs);
            ois.setObjectInputFilter(SERIALIZED_CAS_INPUT_FILTER);
            var casMgr = (CASMgrSerializer) ois.readObject();

            if (ts == null) {
                ts = casMgr.getTypeSystem();
                ts = ts.commit();
            }

            return ts;
        }
        catch (IOException | ClassNotFoundException e) {
            throw new CollectionException(e);
        }
    }

    @Override
    public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException
    {
        if (typeSystemLocation == null) {
            typeSystem = (TypeSystemImpl) aTypeSystem;
        }
    }

    /**
     * It is possible that the type system overlaps with the scan pattern for files, e.g. because
     * the type system ends in {@code .ser} and the resources also end in {@code .ser}. If this is
     * the case, we filter the type system file from the resource files during scanning.
     */
    @Override
    protected Collection<Resource> scan(String aBase, Collection<String> aIncludes,
            Collection<String> aExcludes)
        throws IOException
    {
        Collection<Resource> resources = super.scan(aBase, aIncludes, aExcludes);
        if (typeSystemLocation != null) {
            org.springframework.core.io.Resource r = getTypeSystemResource();
            resources.remove(new Resource(null, null, r.getURI(), null, null, r));
        }
        return resources;
    }

    protected org.springframework.core.io.Resource getTypeSystemResource()
        throws MalformedURLException
    {
        org.springframework.core.io.Resource r;
        // Is absolute?
        if (typeSystemLocation.indexOf(':') != -1 || typeSystemLocation.startsWith("/")
                || typeSystemLocation.startsWith(File.separator)) {
            // If the type system location is absolute, resolve it absolute
            r = getResolver().getResource(locationToUrl(typeSystemLocation));
        }
        else {
            // If the type system is not absolute, resolve it relative to the base location
            r = getResolver().getResource(getBase() + typeSystemLocation);
        }
        return r;
    }

    private TypeSystemImpl readTypeSystem() throws IOException
    {
        if (typeSystemLocation == null) {
            return null;
        }

        if (typeSystem == null) {
            CASMgrSerializer casMgr = readCasManager();
            typeSystem = casMgr.getTypeSystem();
            typeSystem = typeSystem.commit();
        }

        return typeSystem;
    }

    private void initCasFromEmbeddedTS(byte[] header, CAS aCAS) throws IOException
    {
        // If we encounter a Java-serialized file with an external
        // TSI, then we reinitalize the CAS with the external TSI
        // prior to loading the data
        if (header[0] == (byte) 0xAC && header[1] == (byte) 0xED) {
            CASMgrSerializer casMgr = readCasManager();
            ((CASImpl) aCAS).getBinaryCasSerDes().setupCasFromCasMgrSerializer(casMgr);
        }
    }

    private CASMgrSerializer readCasManager() throws IOException
    {
        if (typeSystemLocation == null) {
            return null;
        }

        // If we already read the type system, return it - do not read it again.
        if (casMgrSerializer != null) {
            return casMgrSerializer;
        }

        org.springframework.core.io.Resource r = getTypeSystemResource();
        getLogger().debug("Reading type system from [" + r.getURI() + "]");

        try (var is = new ObjectInputStream(
                getInputStream(typeSystemLocation, r.getInputStream()))) {
            is.setObjectInputFilter(SERIALIZED_CAS_INPUT_FILTER);
            casMgrSerializer = (CASMgrSerializer) is.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }

        return casMgrSerializer;
    }
}
