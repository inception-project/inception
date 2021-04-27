const path = require("path");

module.exports = {
    entry: {
        main: [
            path.resolve(__dirname, "client", "Client.ts"),
            path.resolve(__dirname, "server", "Server.ts")
        ],
    },
    output: {
        path: path.resolve(__dirname, "dist", "build"),
        library: 'express',
        filename: "bundle.js"
    },
    target : 'node',
    resolve: {
        extensions: [".js", ".jsx"],

    },
    module: {
        rules: [
            {loader: "ts-loader"},
        ],
    },
    mode: "development"
}
