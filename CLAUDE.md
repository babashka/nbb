# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

nbb (Node.js Babashka) is an ad-hoc ClojureScript scripting tool for Node.js. It uses SCI (Small Clojure Interpreter) to interpret ClojureScript code at runtime without requiring compilation. Key characteristics: ~1.2MB artifact, ~170ms startup, first-class macro support.

## Prerequisites

- [Babashka](https://babashka.org) (bb) - task runner
- Node.js 14+
- Java 21+ and Clojure CLI (for shadow-cljs compilation)
- SCI must be available at `../babashka/sci` (local dependency in deps.edn)

## Common Commands

```bash
bb tasks              # List all available tasks
bb npm-install        # Install NPM dependencies
bb dev                # Watch mode with tests enabled (compiles to lib/)
bb release            # Production build (clean + npm-install + compile with :advanced)
bb run-tests          # Run unit tests: node lib/nbb_tests.js
bb run-integration-tests  # Run integration tests (babashka scripts)
bb nrepl-tests        # Run nREPL tests
bb ci:test            # Full CI suite: clean, release, run-tests, integration, nrepl
```

### Development workflow

1. `bb dev` — starts shadow-cljs in watch mode (tests enabled via `NBB_TESTS=true`)
2. Test the compiled nbb: `node lib/nbb_main.js -e "(+ 1 2 3)"`
3. Run unit tests: `bb run-tests`
4. You must run `bb dev` (or `bb release` with `NBB_TESTS=true`) before `bb run-tests`, otherwise `lib/nbb_tests.js` won't exist

## Architecture

### Build System

nbb is a ClojureScript project compiled by **shadow-cljs** to ESM modules targeting Node.js. The output goes to `lib/`. Babashka (bb) orchestrates all build tasks defined in `bb.edn`, with build logic in `build/src/nbb/build.clj`.

Shadow-cljs configuration is in `shadow-cljs.edn`. Test-specific config merges from `shadow-tests.edn` when `NBB_TESTS=true`.

### Module System

The project compiles to ~23 separate JS modules in `lib/` with a dependency graph rooted at `nbb_core`:

- **nbb_core** — root module; SCI interpreter setup, namespace management, dynamic `require` handling, promise/await integration
- **nbb_main** — CLI entry point (`cli.js` → `lib/nbb_main.js`)
- **nbb_api** — JavaScript API (`index.mjs` → `loadFile`, `loadString`, `addClassPath`, etc.)
- **Feature modules** — lazily loaded on first `require`: reagent, promesa, pprint, nrepl_server, repl, transit, bundler, spec, etc.

Each feature module has an `init` function that registers its SCI namespaces. Modules depend on `nbb_core` and are loaded dynamically when their namespaces are required.

### Core Source Structure

- `src/nbb/core.cljs` — The heart of nbb. SCI interpreter, namespace/module loading, `await`/`await?` promise handling, dynamic import resolution
- `src/nbb/impl/sci.cljs` — SCI namespace configuration (`copy-ns`, `copy-var` macros for mapping ClojureScript namespaces into SCI)
- `src/nbb/impl/main.cljs` — CLI argument parsing (-e, -m, -x, -cp, --config, REPL modes, bundle command)
- `src/nbb/api.cljs` — Public JS API for embedding nbb in Node.js projects
- `src/nbb/impl/*.cljs` — One file per feature module (reagent, promesa, nrepl-server, repl, bundler, etc.)
- `src/nbb/error.cljs` — Error formatting with file/line/column context

### Test Structure

- `test/nbb/main_test.cljs` — Unit tests (cljs.test), compiled to `lib/nbb_tests.js`
- `script/nbb_tests.clj` — Integration tests (babashka), runs nbb against test-scripts/
- `script/nbb_nrepl_tests.clj` — nREPL protocol tests
- `test-scripts/` — Real-world integration test scripts

### Features System

nbb supports bundling additional libraries as "features" via [nbb-features](https://github.com/babashka/nbb-features). A feature provides: `deps.edn` (dependencies), `src/nbb_features.edn` (namespace→JS mapping), and `src/nbb/impl/$LIBRARY.cljs` (SCI bindings). Features are added to the classpath in `bb.edn`.

### Custom Builds

- `NBB_CLI_NAME=nbb-logseq bb release` — custom CLI name
- `NBB_NPM_LIB_NAME=@logseq/nbb-logseq bb release` — custom npm package name

## Contribution Guidelines

- Create an issue before writing code; discuss approach first
- Keep changes small and focused (one problem per issue/PR)
- Bug fixes and new features must include tests
- Update CHANGELOG.md
- Do not force-push on PR branches
