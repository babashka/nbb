# Fastify server example

### Usage

- Install dependencies
  `npm install`

- Run the dev server
  `npm start`

### Features
- fastify web server
- authentication using
  - [bcrypt](https://www.npmjs.com/package/bcrypt) password hashing
  - [fast-jwt](https://www.npmjs.com/package/fast-jwt) token signing/verification
  - [@fastify/cookie](https://www.npmjs.com/package/@fastify/cookie) storage for token
- template rendering using built-in `reagent`
- frontend interactivity using [htmx](https://htmx.org/)
- static asset serving with [@fastify/static](https://www.npmjs.com/package/@fastify/static)
