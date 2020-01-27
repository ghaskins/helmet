# helmet

A tool to assist with compiling Helm Charts

## Background

A core feature of Helm Charts is dependency management.  One interesting aspect of Dependency Management is the use of `file://` URLs.  These allow multiple charts to exist together and to be composed in various ways.

However, there is a limitation in Helm that `file://` URLs are not well supported:  Dependency commands such as `helm dep build` will not resolve transitive dependencies. 

`Helmet` enhances the Helm dep management story by providing a tool that can understand `file://` based dependencies and act as a replacement for the `helm dep build` command with full support for transitives.

It also offers an ability to override the `appVersion` of various charts in the process, enabling a straight forward way to create a fully packaged assembly.

## Usage

```
$ helmet -h
helmet version: v0.1.0-SNAPSHOT

Usage: helmet [options]

Options:
  -h, --help
  -v, --version                         Print the version and exit
  -p, --path PATH               .       The path to the Helm chart to package
  -o, --output PATH             target  The path for output files
      --version-overrides PATH          The path to a YAML table with appVersion overrides
```
