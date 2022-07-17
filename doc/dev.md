# Developing nbb

## Workflow

### Start with an issue before writing code

Before writing any code, please create an issue first that describes the problem
you are trying to solve with alternatives that you have considered. A little bit
of prior communication can save a lot of time on coding. Keep the problem as
small as possible. If there are two problems, make two issues. We discuss the
issue and if we reach an agreement on the approach, it's time to move on to a
PR.

### Follow up with a pull request

Post a corresponding PR with the smallest change possible to address the
issue. Then we discuss the PR, make changes as needed and if we reach an
agreement, the PR will be merged.

### Tests

Each bug fix, change or new feature should be tested well to prevent future
regressions.

### Force-push

Please do not use `git push --force` on your PR branch for the following
reasons:

- It makes it more difficult for others to contribute to your branch if needed.
- It makes it harder to review incremental commits.
- Links (in e.g. e-mails and notifications) go stale and you're confronted with:
  this code isn't here anymore, when clicking on them.
- CircleCI doesn't play well with it: it might try to fetch a commit which
  doesn't exist anymore.
- Your PR will be squashed anyway.

## Requirements

You need [babashka](https://babashka.org) for running tasks. Run `bb tasks` in
the project to see which tasks are relevant.

## Develop

Run the `bb dev` task to start compilation in development mode and to start a shadow-cljs watcher to compile changes. nbb is ultimately compiled entirely to js code so that it can be bootsrapped by `node`. `lib/nbb_main.js` will be produced as an entry point as well as many other js dependencies.

Then run `node lib/nbb_main.js <args>` to test the compiled nbb.

## Test

To run tests, run `bb run-tests` for unit tests and `bb run-integration-tests` for running integration tests.

You need to run `bb dev` first or you'll get the errors with `Error: Cannot find module './nbb/lib/nbb_tests.js'`.


## Build

The two main build tasks are `bb dev` and `bb release` for development and
production builds respectively. Some build tasks are available as a bb library
in `build/`. This is useful for custom builds with features enabled.

To build a version of nbb with a custom cli name, use `$NBB_CLI_NAME` e.g.
`NBB_CLI_NAME=nbb-logseq bb release`.

To build a version of nbb with a custom npm lib for bundle, use `$NBB_NPM_LIB_NAME` e.g.
`NBB_NPM_LIB_NAME=@logseq/nbb-logseq bb release`.

## Features

`nbb` supports bundling additional Clojure(Script) libraries as features. This
is particularly valuable for libraries that are not yet SCI compatible. nbb has
some pre-built features at
[nbb-features](https://github.com/babashka/nbb-features). Features are built
using the [build library](../build) and bb tasks. Features are enabled by adding
them on the classpath, usually as a dependency in `bb.edn`. See
https://github.com/babashka/nbb-features/blob/main/bb.edn for a working example.

To create a new feature, add the following files in a directory `$LIBRARY` and
put that directory on your classpath:
- `deps.edn` - Dependencies for library
- `src/nbb_features.edn` - Configuration to map namespaces to js assets
- `src/nbb/impl/$LIBRARY.cljs` - Sci mappings

`nbb_features.edn` is a vector of maps where each map consists of keys:

* `:name`: unique name to identify feature
* `:namespaces`: namespaces provided by feature
* `:shadow-cljs`: shadow-cljs config map for shadow-cljs module(s)
* `:js`: The javascript file produced by the shadow-cljs config. This will be
  loaded when one of the feature's namespaces are required.
