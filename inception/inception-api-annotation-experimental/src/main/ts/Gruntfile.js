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
module.exports = function (grunt) {
  "use strict";

  grunt.loadNpmTasks('grunt-run');
  grunt.loadNpmTasks('grunt-contrib-clean');

  grunt.initConfig({
    run: {
      esbuild: {
        cmd: './node_modules/esbuild/bin/esbuild',
        args: [
          "client/AnnotationExperienceAPI.ts",
          "--format=cjs",
          "--target=es6",
          "--bundle",
          "--outfile=dist/AnnotationExperienceAPI.js"
        ]
      }
    },

    clean: {
      folder: {
        src: ['node_modules', 'grunt-maven.json', 'package-lock.json', '.tscache']
      }
    }
  });

  grunt.registerTask("default", ["run:esbuild","clean"]);
};