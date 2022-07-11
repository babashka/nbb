import { nodeResolve } from '@rollup/plugin-node-resolve';

export default {
  input: 'out.mjs',
  output: {
    file: 'dist/index.mjs',
    format: 'esm'
  },
  plugins: [nodeResolve({exportConditions: ['node']})]
};
