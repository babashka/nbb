//webpack.config.js
const path = require('path');

module.exports = {
  target: 'electron18.1-main',
  entry: {
    main: "./src/index.js",
  },
  output: {
    path: path.resolve(__dirname, '.'),
    filename: "index.js" // <--- Will be compiled to this single file
  },
  resolve: {
    extensions: [".js"],
  },
};
