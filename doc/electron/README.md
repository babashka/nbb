# Electron

This guide describes how you can use nbb within the context of an Electron application.

First off, install the npm deps:

```
$ npm install
```

This project is based on the [Electron Quick Start](https://www.electronjs.org/docs/latest/tutorial/quick-start).

What has been added:

- Webpack config to compile this project along with nbb (an ES6 project) into a
  single `index.js` file.
- IPC handlers to demonstrate communication between the renderer and the
  Electron process in which nbb runs

Because webpack turns nbb into a CommonJS file, some things don't work as
expected, most importantly the way `require` works. This is why NPM dependencies are made available by putting them on the global object:

In JS:

``` javascript
globalThis.path = path;
```

In CLJS:

``` clojure
(def path js/path)
```

To compile and view the Electron app:

```
$ npx webpack --config webpack.dev.js && npm start
```

