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

  grunt.initConfig({
    ts: {
      options: {
        compile: true,
        comments: false,
        target: 'es5',
        module: 'amd',
        sourceMap: false,
        declaration: false,
        noImplicitAny: false,
        verbose: true
      },
      dev: {
        src: "client/**/*.ts",
        out: "../js/ExperienceAPI.js",
        options: {
          moduleResolution: 'node',
          allowSyntheticDefaultImports: true
        }
      }
    },

    transform_amd: {
      bundle: {
        options: {
          root: "src/main",
          newLoader: "--es6"
        },
        src:  '../js/ExperienceAPI.js',
        dest: '../ExperienceAPICompiled.js',
      }
    },

    clean: {
      folder: {
        src: ['node_modules', 'grunt-maven.json', 'package-lock.json', '.tscache']
      }
    }
  });

  grunt.loadNpmTasks("grunt-ts");
  grunt.loadNpmTasks('grunt-transform-amd');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.registerTask("default", ["ts:dev","transform_amd:bundle","clean"]);
};