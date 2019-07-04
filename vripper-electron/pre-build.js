const cheerio = require('cheerio');
const fs = require('fs');
const rimraf = require("rimraf");
const copydir = require("copy-dir");


rimraf.sync('./build/vripper-ui');
copydir.sync('../vripper-ui/dist/vripper-ui', './build/vripper-ui', {});


const index = fs.readFileSync('./build/vripper-ui/index.html');


const $ = cheerio.load(index);
$('body').prepend(`<script>require('../renderer.js')</script>`);

fs.writeFileSync('./build/vripper-ui/index.html', $.html());

