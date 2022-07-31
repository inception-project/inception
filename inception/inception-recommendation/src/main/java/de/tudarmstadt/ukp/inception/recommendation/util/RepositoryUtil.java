/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * Little helper class to manage the folders used to store the results, models and recommendation
 * settings.
 * 
 * @deprecated Class appears not to be used anymore and is to be removed without replacement.
 */
@Deprecated
public class RepositoryUtil
{
    private static final Logger logger = LoggerFactory.getLogger(RepositoryUtil.class);

    private static final String RECOMMENDATION_DIR_NAME = "/recommendation/";

    private static final String MODEL_DIR_NAME = "/model/";
    private static final String RESULT_DIR_NAME = "/results/";

    public static File getRecommendationDir(DocumentService dc, Project p)
    {
        if (dc == null || p == null) {
            logger.error("Cannot create file path. DocumentService or Project object is null");
            return null;
        }

        File result = new File(dc.getDir(), "/" + ProjectService.PROJECT_FOLDER + "/");
        result = new File(result, Long.toString(p.getId()));
        result = new File(result, RECOMMENDATION_DIR_NAME);
        return result;
    }

    public static List<File> getRecommendationDirs(DocumentService dc)
    {
        if (dc == null) {
            logger.error("Cannot create file paths. DocumentService object is null");
            return null;
        }

        File dir = new File(dc.getDir(), "/" + ProjectService.PROJECT_FOLDER + "/");

        if (!dir.exists()) {
            return null;
        }

        List<File> result = new LinkedList<>();

        for (File subDir : dir.listFiles()) {
            if (subDir.isDirectory()) {
                result.add(new File(subDir, RECOMMENDATION_DIR_NAME));
            }
        }

        return result;
    }

    public static File getModelDir(DocumentService dc, Project p)
    {
        File result = getRecommendationDir(dc, p);
        return new File(result, MODEL_DIR_NAME);
    }

    public static File getResultDir(DocumentService dc, Project p)
    {
        File result = getRecommendationDir(dc, p);
        return new File(result, RESULT_DIR_NAME);
    }
}
