// http://eslint.org/docs/user-guide/configuring

module.exports = {
  root: true,
  parser: 'babel-eslint',
  parserOptions: {
    sourceType  : 'module',
    ecmaVersion : 8 // or 2017
  },
  env: {
    browser: true,
    jquery: true,
    node: true
  },
  // https://github.com/feross/standard/blob/master/RULES.md#javascript-standard-style
  extends: 'standard',
  // add your custom rules here
  'rules': {
    // allow paren-less arrow functions
    'arrow-parens': 0,
    // allow async-await
    'generator-star-spacing': 0,
    // allow debugger during development
    'no-debugger': process.env.NODE_ENV === 'production' ? 2 : 0,
    "key-spacing": [1, {
      "align": "colon",
      "beforeColon": true,
      "afterColon": true
    }],
    "semi" : 1,
    "space-before-function-paren" : 1,
    "padded-blocks" : 0,
    "keyword-spacing" : 1,
    "no-multi-spaces" : 0,
    "space-infix-ops" : 1,
    "no-unused-vars" : 1,
    "no-multiple-empty-lines" : 1,
    "indent" : [1, 2],
    "quotes" : 1,
    "no-duplicate-imports" : 1,
    "eqeqeq" : 1,
    "no-undef" : 1,
    "object-property-newline" : 1,
    "camelcase" : 1,
    "curly" : 1,
    "no-irregular-whitespace" : 1,
    "eol-last" : 1,
    "no-redeclare" : 1,
    "operator-linebreak" : [1, "before"],
    "comma-spacing" : 1,
    "no-throw-literal" : 1,
    "standard/object-curly-even-spacing" : 1,
    "comma-dangle" : 1,
    "arrow-spacing" : 1,
    "no-unexpected-multiline" : 1,
    "no-sequences" : 1,
    "no-useless-escape" : 0
  }
}
