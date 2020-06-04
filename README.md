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
 -i,--input <arg>       input file / directory path *
 -s,--suffix <arg>      suffix of output file
 -f,--filetype <arg>    override file ending in file path with this parameter
 -h,--harsh             remove ALL metadata, such as potentially harmless
                        data (i.E. title and subject)
 -o,--overwrite         overwrite input file
 -r,--recursive         search for files recursively if input path is a
                        directory
```


## Example
```
$ java -jar metacleaner.jar -i C:\Users\ozzi\Desktop\example\

metacleaner 1.0 - github.com/ozzi-/metacleaner
----------------------------------------------
\_ input path is a directory - won't scan recursively
  |_ 'custom_properties.xlsx'
    |_ cleaned document (stripped 366 B) successfully written to 'C:\Users\ozzi\Desktop\example\custom_properties_cleaned.xlsx'
  |_ 'custom_properties_cleaned.xlsx'
    |_ skipping 'C:\Users\ozzi\Desktop\example\custom_properties_cleaned.xlsx' as assumed it was already cleaned (ends with suffix '_cleaned')
  |_ 'd'
    |_ skipping 'C:\Users\ozzi\Desktop\example\d' - requiring a file ending (no dot found)
  |_ 'd.txt'
    |_ metacleaner does not (yet - feel free to open a GitHub issue) support the file type 'txt'. If the file ending differs the actual file type, use the -f option.
  |_ 'dd.xml'
    |_ cleaned document (stripped 815 B) successfully written to 'C:\Users\ozzi\Desktop\example\dd_cleaned.xml'
\_ cleaned 2 of 4 files
```
