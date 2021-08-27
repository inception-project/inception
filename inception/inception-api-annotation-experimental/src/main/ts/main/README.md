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
<p align="center">
  <br/>
  <br/><br/>
</p>

A Typescript API for text annotation. 

<br/>
<br/>

## Using the API
In order to use the API for your custom annotation editor, create a typescript file
that imports the file located in:

"inception-api-annotation-experimental/src/main/ts"

and assign a variable to create the object. 

var API = new Experimental()



### API endpoints


### Additional information
When the API is invoked, a websocket connection between the client (browser) and server will
also be created automatically. You can configure the URL that is used
in the "config.json" file. 
    
## License

