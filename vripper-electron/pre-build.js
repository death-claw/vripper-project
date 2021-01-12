const cheerio = require('cheerio');
const fs = require('fs');
const rimraf = require("rimraf");
const copydir = require("copy-dir");
const {execSync} = require("child_process");

rimraf.sync('./build/vripper-ui');
copydir.sync('../vripper-ui/dist/vripper-ui', './build/vripper-ui', {});


const index = fs.readFileSync('./build/vripper-ui/index.html');


const $ = cheerio.load(index);
$('body').prepend(`<script>require('../renderer.js')</script>`);

fs.writeFileSync('./build/vripper-ui/index.html', $.html());

console.log('Building runtime environment');
rimraf.sync('java-runtime');
execSync('jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules java.base,java.desktop,java.instrument,java.management,java.naming,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,jdk.httpserver,jdk.unsupported,jdk.crypto.ec --output java-runtime');
if (process.platform === 'linux') {
    console.log('Stripping libjvm.so');
    execSync('strip -p --strip-unneeded java-runtime/lib/server/libjvm.so');
}
