![build status](https://api.travis-ci.com/ozzi-/metacleaner.svg?branch=master)
![licence](https://img.shields.io/github/license/ozzi-/metacleaner.svg)
![open issues](https://img.shields.io/github/issues/ozzi-/metacleaner.svg)

# metacleaner
metacleaner will remove metadata of the following filetypes:
- jpg
- png
- pdf
- odt, ods, odp
- doc, xls, ppt
- docx, xlsx
- xml

## Usage
```
usage: java -jar metacleaner.jar
 -h,--harsh             remove ALL metadata, such as potentially harmless
                        data (i.E. title and subject)
 -i,--input <arg>       input file / directory path *
 -o,--overwrite         overwrite input file
 -r,--recursive         search for files recursively if input path is a
                        directory
 -s,--suffix <arg>      suffix of output file
```
