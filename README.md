# JSON Combiner

Utility to scan and parse files by format and convert into json output that can be ingested by mongoimport

Currently supports parsing of `json` and `xml` formats

## Execution

```bash
java -jar json-combiner-1.0.jar -i extracted/ -o combined/ -vr --debug -f xml -e PubmedArticle.MedlineCitation
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