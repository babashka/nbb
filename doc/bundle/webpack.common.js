//webpack.config.js
const path = require('path');
const webpack = require('webpack');

module.exports = {
  target: 'node',
  entry: {
    main: "./out.mjs",
  },
  output: {
    path: path.resolve(__dirname, '.'),
    filename: "dist/index.js" // <--- Will be compiled to this single file
  },
  resolve: {
    extensions: [".js"],
  },
  experiments: {
    topLevelAwait: true
  }
};
