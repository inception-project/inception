/*
 * ## INCEpTION ##
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
 *
 * ## brat ##
 * Copyright (C) 2010-2012 The brat contributors, all rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/** 
 * id, type, comment 
 */ 
export type sourceCommentType = [string | Array<unknown>, string, string];

export type OffsetsList = Array<Offsets>;

export type Offsets = [number, number];

export type sourceEntityAttributesType = {
  l: string;
  c: string;
  h: string;
  a: number;
  cl: string;
}

/** 
 * id, type, offsets, attributes 
 */ 
export type sourceEntityType = [string, string, [Offsets], sourceEntityAttributesType];

/** 
 * id, name, spanId, value 
 */ 
export type sourceAttributeType = [string, string, string, string];

/**
 * This class represents the JSON object that we receive from the server. Note that the class is
 * currently only used for documentation purposes. The JSON we get is not really cast into an
 * object of this class.
 */
export class SourceData {
  text: string = undefined;
  attributes: sourceAttributeType = undefined;
  comments: Array<sourceCommentType> = [];
  entities: Array<sourceEntityType> = undefined;
  equivs = [];
  events = [];
  normalizations = [];
  relations = [];
  triggers = [];
  sentence_offsets: number[][];
  token_offsets: number[][];
  sentence_number_offset: number;
  rtl_mode: boolean;
  font_zoom: number;
  args: Record<string, []>;

  /**
   * @deprecated INCEpTION does not use the collection name.
   */
  collection: string;

  /**
   * @deprecated INCEpTION does not use the document name.
   */
  document: string;

  constructor() {
    // Object.seal(this);
  }
}
