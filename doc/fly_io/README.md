# Deploying an nbb app to fly.io

[Fly.io](https://fly.io/) is a service that can run full stack apps with minimal
configuration. If you like the ease of Heroku, you might like fly.io and perhaps
even better! This document shows how to get a minimal nbb application up and
running on `fly.io`. This guide is based on the the fly.io documentation for
Node.js [here](https://fly.io/docs/getting-started/node/).

In `example.cljs` we start an Express webserver on port `8092`. To run the site
locally, run `npm run start`.

To get this site running on `fly.io`, you need to
[install](https://fly.io/docs/getting-started/installing-flyctl/) and [log
in](https://fly.io/docs/getting-started/log-in-to-fly/).

Then run `flyctl launch` to create a new application. After making changes, you
can re-deploy the site with `flyctl deploy`.

That's it! Check out
[this](https://twitter.com/borkdude/status/1526159911033393152) tweet for a
demo.
