# Developing a Raycast extension

This guide requires nbb 0.3.9 or higher.

Raycast is a launcher similar to Alfred and Spotlight. It is extensible with
scripts and extensions. This document explains how to write an extension with nbb.

Go to [https://developers.raycast.com/](https://developers.raycast.com/) and
follow [Create your first
extension](https://developers.raycast.com/basics/create-your-first-extension).

You should see `"Hello world"` appear when running with `npm run dev`. Now we
will modify the `.tsx` file to call nbb instead. We will use `nbb` as an NPM
library, so first execute `npm install nbb`. Then replace your `.tsx` file with
the code that is in [src/nbb-extension.tsx](src/nbb-extension.tsx)

The extension is built using a bundler. To include the lazily loaded reagent
module, we need to make it visible to the bundler, thus we load it explicitly:

```
import 'nbb/lib/nbb_reagent.js';
```

Because `loadFile` is async we need to use an async React component. We are
using `react-async` for this. With this boilerplate out of the way, we can
continue building our extension in `script.cljs`. Save that file in `assets` in
the extension directory. See [assets/script.cljs](assets/script.cljs).

The `script.cljs` file returns the `Command` component and is rendered in the
`.tsx` file. Done! You can see the result [here](https://twitter.com/borkdude/status/1517442325588463617).
