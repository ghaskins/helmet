# Test Resources

This directory contains test charts used to verify the behavior of Helmet.  The Charts largely consist of default output from 'helm create'.  The only modifications are w.r.t. dependencies in Chart.yaml.

The following topology is expressed as dependencies:

[foo] -> [bar bat] -> [baz]

In other words, `foo` depends directly on `bar` and `bat`, each of which in turn depends on `baz`.  `foo` is intended to be the entry point for a Helmet compilation. 