/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

/**
 * This PEG (parsing expression grammar) is used to validate the filter expressions
 * in the model metadata console's filter input field. The parser errors
 * are used to generate context-specific autocompletion suggestions.
 **/
expr
  = whitespace+ expr whitespace* {return expr}
      / l:term1 f:op_3 r:expr {return l + f + r}
      / t:term1 {return t}
term1
   = l:term2 f:op_4 r:term1 {return l + f + r}
      / t:term2 {return t}
term2
   = l:term3 f:op_6 r:term2 {return l + f + r}
      / t:term3 {return t}
term3
   = l:term4 f:op_7 r:term3 {return l + f + r}
      / t:term4 {return t}
term4
   = l:term5 f:op_11 r:term4 {return l + f + r}
      / t:term5 {return t}
term5
   = l:primary f:op_12 r:term5 {return l + f + r}
      / p:primary {return p}

primary
   = whitespace+ p:primary whitespace* {return p}
      / "-" p:primary {return "-" + p}
      / number
      / boolean
      / numVar
      / "entire(" v:numVar ")" {return "entire." + v + "Sum"}
      / "average(" v:numVar ")" {return "entire." + v + "Avg"}
      / '(' e:expr ')' {return "(" + e + ")"}

boolean "a boolean"
   = "true"
     / "false"

number "a number"
   = "." r:integer {return "0." + r}
   / l:integer "." r:integer {return l + "." + r}
   / integer

integer
  = val: [0-9]+ {return val.join("")}

numVar
   = v:"mappings" {return "modelData." + v}
   / v:"mappableFields" {return "modelData." + v}
   / v:"lazyFields" {return "modelData." + v}
   / v:"greedyFields" {return "modelData." + v}
   / v:"instantiations" {return "modelData." + v}
   / v:"averageMappingDuration" {return "modelData." + v}
   / v:"maximumMappingDuration" {return "modelData." + v}
   / v:"minimumMappingDuration" {return "modelData." + v}
   / v:"mappingDurationMedian" {return "modelData." + v}
   / v:"totalMappingDuration" {return "modelData." + v}
   / v:"cacheHits" {return "modelData." + v}

op_3
   = whitespace+ f:op_3 whitespace* {return f}
     / "*" 
     / "/"

op_4
   = whitespace+ f:op_4 whitespace* {return f}
     / "+" 
     / "-" 

op_6
   = whitespace+ f:op_6 whitespace* {return f}
     / "<=" 
     / ">=" 
     / "<" 
     / ">"

op_7
   = whitespace+ f:op_7 whitespace* {return f}
     / "==" 
     / "!="

op_7_1
   = whitespace+ f:op_7_1 whitespace* {return f}
     / "~" 

op_11
   = whitespace+ f:op_11 whitespace* {return f}
      / "&&"

op_12
   = whitespace+ f:op_12 whitespace* {return f}
     / "||" 

whitespace ""
   = [ \t]+
