var dgram = require('dgram');
var s = dgram.createSocket('udp4');
s.bind(9999);