# JSON Combiner

Utility to scan and parse files by format and convert them into json output that can be ingested by mongoimport

Currently supports parsing of `json` and `xml` formats

## Execution

```bash
java -jar json-combiner.jar -i extracted/ -o combined/ -vr --debug -f xml -e RootNode.ChildNode
```

## Usage
```bash
json-combiner 1.x
Usage: json-combiner [options]

  -i, --in <value>       the input directory to scan for files files (required)
  -o, --out <value>      the output directory save files to (required)
  -s, --size <value>     the size of file to generate in bytes (default 16Mb-16kb = 16760832) 
  -f, --format <value>   format of file that should be parsed (default json)
  -p, --prefix <value>   prefix to give to the generated files (default none)
  -e, --element <value>  path to element to use as the root of the document using dot notation (default none)
  -r, --recursive        folder will be scanned recursively
  -v, --verbose          show information while processing
  --debug                show debug
``` 

## License

Copyright (c) 2019, Medicines Discovery Catapult
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Medicines Discovery Catapult nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL MEDICINES DISCOVERY CATAPULT BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.