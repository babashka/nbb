# Creating a standalone executable with caxa

This document provides an example of how to create a standalone executable from
an nbb project with [caxa](https://github.com/leafac/caxa).

This document uses the same example as the one in [# Publishing an nbb project
to npm ](../publish/README.md): a project that prints CLI arguments. If you
haven't read that yet, go read that first.

Given the `print-cli-args` example, let us turn that project into a standalone executable:

```
$ npx caxa --input . --output print-cli-args -- "{{caxa}}/node_modules/.bin/node" "{{caxa}}/index.mjs"
```

This produces an executable named `print-cli-args`. Let's invoke it:

```
$ ./print-cli-args 1 2 3
Your command line arguments: 1 2 3
```

That's it!
