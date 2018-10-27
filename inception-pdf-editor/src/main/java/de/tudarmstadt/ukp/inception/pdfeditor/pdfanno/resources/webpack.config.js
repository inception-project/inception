/* eslint-disable */
const path = require('path')
const webpack = require('webpack')

module.exports = (env, argv) => {
  return {
    mode: process.env.NODE_ENV || 'development',
    entry: {
      'pdfanno.page': './src/pdfanno.js',
      'pdfanno.core': './src/core/index.js',
      'embedded-sample': './src/embedded-sample.js',
      'viewer': './src/viewer.js',
      'debugger': './src/debugger.js',
      'compatibility': './src/compatibility.js',
      'l10n': './src/l10n.js'
    },
    output: {
      path: path.resolve(__dirname, 'build'),
      filename: './[name].bundle.js',
      library: 'PDFAnnoCore',
      libraryTarget: 'umd'
    },
    module: {
      rules: [{
        test: /\.js$/,
        loader: 'eslint-loader',
        enforce: 'pre',
        include: [path.join(__dirname, 'src')],
        exclude: /node_modules/
      }, {
        test: /\.css$/,
        use: [
          {
            loader: 'style-loader'
          },
          {
            loader: 'css-loader',
            options: {
              url: false
            }
          }
        ]
      }]
    },
    plugins: [
      new webpack.LoaderOptionsPlugin({ options: {} })
    ],
    devServer: {
      host: 'localhost',
      contentBase: './build',
      port: 8080,
      watchOptions: {
        aggregateTimeout: 300,
        poll: 1000
      }
    },
    devtool: 'source-map'
  }
}
