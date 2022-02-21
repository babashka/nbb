# Creating a standalone executable with caxa

## Packaging an nbb project

This document provides an example of how to create a standalone executable from
an nbb project with [caxa](https://github.com/leafac/caxa).

In `print-cli-args` is a small project which prints CLI arguments. Normally you would invoke this with:

```
$ nbb -cp src -m print-cli-args.core 1 2 3
```

Let's turn that project into a standalone executable. Execute this from the
`print-cli-args` directory:

```
$ npx caxa@2.1.0 --input . --output print-cli-args -- \
  "{{caxa}}/node_modules/.bin/nbb" \
  "-cp" "{{caxa}}/src" \
  "-m" "print-cli-args.core"
```

This produces an executable named `print-cli-args` of around 30MB. Let's invoke it:

```
$ ./print-cli-args 1 2 3
Your command line arguments: (1 2 3)
```

That's it!

## Packaging nbb itself

You can also package nbb itself as a standalone executable, which is basically
the same as above but without invoking pre-defined command line arguments:

```
$ npx caxa --input . --output nbb-standalone \
  -- "{{caxa}}/node_modules/.bin/node" "{{caxa}}/node_modules/.bin/nbb"
$ ./nbb-standalone -e '(+ 1 2 3)'
6
$ ./nbb-standalone
Welcome to nbb v0.1.8!
user=>
```

That wasn't too bad, was it?
