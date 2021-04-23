module.exports = {
    mode: "development",
    devtool: "inline-source-map",
    entry: {
        main: "./client.ts",
    },
    output: {
        path: ('build'),
        filename: "[name]-bundle.js",
    },
    resolve: {
        extensions: [".ts", ".tsx"],
    },
    module: {
        rules: [
            {loader: "ts-loader"},
        ],
    }
}