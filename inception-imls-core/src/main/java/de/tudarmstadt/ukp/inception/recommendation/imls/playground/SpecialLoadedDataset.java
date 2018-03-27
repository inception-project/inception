/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.playground;

import static java.util.Arrays.asList;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.FileRole;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Split;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.internal.LoadedDataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.internal.util.AntFileFilter;

public class SpecialLoadedDataset
    extends LoadedDataset
{

    private DatasetFactory factoryObj = null;
    private DatasetDescription descriptionObj = null;

    public SpecialLoadedDataset(DatasetFactory aFactory, DatasetDescription aDescription)
    {
        super(aFactory, aDescription);
        this.factoryObj = aFactory;
        this.descriptionObj = aDescription;
    }

    public SpecialLoadedDataset(DatasetFactory aFactory, DatasetDescription aDescription,
            Dataset ds)
    {
        super(aFactory, aDescription);
        this.factoryObj = aFactory;
        this.descriptionObj = aDescription;
    }

    @Override
    public File[] getDataFiles()
    {
        Set<File> all = new HashSet<>();

        // Collect all data files
        all.addAll(asList(getFiles(FileRole.DATA)));

        // If no files are marked as data files, try aggregating over test/dev/train sets
        if (all.isEmpty()) {
            Split split = getDefaultSplit();
            if (split != null) {
                all.addAll(asList(split.getTrainingFiles()));
                all.addAll(asList(split.getTestFiles()));
                all.addAll(asList(split.getDevelopmentFiles()));
            }
        }

        // Sort to ensure stable order
        File[] result = all.toArray(all.toArray(new File[all.size()]));
        Arrays.sort(result, (a, b) -> {
            return a.getPath().compareTo(b.getPath());
        });

        return result;
    }

    private File[] getFiles(String aRole)
    {
        List<File> files = new ArrayList<>();
        String pattern = "dep\\*.conll10";

        if (factoryObj != null) {
            Path baseDir = factoryObj.resolve(descriptionObj);

            Collection<File> matchedFiles = FileUtils.listFiles(baseDir.toFile(),
                    new AntFileFilter(baseDir, asList(pattern), null), TrueFileFilter.TRUE);

            files.addAll(matchedFiles);
        }

        File[] all = files.toArray(new File[files.size()]);
        Arrays.sort(all, (File a, File b) -> {
            return a.getName().compareTo(b.getName());
        });

        return all;
    }

    public DatasetFactory getFactory()
    {
        return factoryObj;
    }

    public void setFactory(DatasetFactory factory)
    {
        this.factoryObj = factory;
    }

    public DatasetDescription getDescription()
    {
        return descriptionObj;
    }

    public void setDescription(DatasetDescription description)
    {
        this.descriptionObj = description;
    }
}
