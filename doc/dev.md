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

Run the `bb dev` task to start compilation in development mode and to start a watcher to compile changes.
Then run `node out/nbb_main.js <args>` to test the compiled nbb.

## Test

To run tests, run `bb run-tests` for unit tests and `bb run-integration-tests` for running integration tests.

## Features

`nbb` can optionally bundle additional Clojure(Script) libraries as features. These can be specified with `$NBB_FEATURES` to compilation tasks e.g. `NBB_FEATURES=datascript,datascript-transit bb release`. The following features are provided:

* [datascript](https://github.com/tonsky/datascript)
* [datascript-transit](https://github.com/tonsky/datascript-transit)

To add a new feature, add the following under `features/$LIBRARY/`:
- `deps.edn` - Dependencies for library
- `shadow-cljs.edn` - Compiler options to build library in advanced/release mode
- `src/nbb/impl/$LIBRARY.cljs` - Sci mappings
- `src/nbb_features.edn` - Configuration to map namespaces to js assets
