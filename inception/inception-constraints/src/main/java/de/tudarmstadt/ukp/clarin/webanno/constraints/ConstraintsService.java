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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface ConstraintsService
{
    String CONSTRAINTS = "constraints";

    /**
     * Creates Constraint Set
     * 
     * @param aSet
     *            the set to create/update
     */
    void createOrUpdateConstraintSet(ConstraintSet aSet);

    /**
     * Returns list of ConstraintSets in a project
     * 
     * @param aProject
     *            The project
     * @return List of Constraints in a project
     */
    List<ConstraintSet> listConstraintSets(Project aProject);

    /**
     * Remove a constraint
     * 
     * @param aSet
     *            the set to remove the constraint from
     */
    void removeConstraintSet(ConstraintSet aSet);

    String readConstrainSet(ConstraintSet aSet) throws IOException;

    void writeConstraintSet(ConstraintSet aSet, InputStream aContent) throws IOException;

    /**
     * Returns Constraint as a file
     * 
     * @param aSet
     *            The Constraint Set
     * @return File pointing to Constraint
     * @throws IOException
     *             if there was an I/O-level problem
     */
    File exportConstraintAsFile(ConstraintSet aSet) throws IOException;

    /**
     * Checks if there's a constraint set already with the name
     * 
     * @param constraintSetName
     *            The name of constraint set
     * @param aProject
     *            a project
     * @return true if exists
     */
    boolean existConstraintSet(String constraintSetName, Project aProject);

    ParsedConstraints getMergedConstraints(Project aProject);
}
