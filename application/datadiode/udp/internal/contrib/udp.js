var dgram = require('dgram');
var s = dgram.createSocket('udp4');
s.bind(9999);
socket.on('message', function(msg, rinfo) {
    console.log('Received %d bytes from %s:%d\n', msg.length, rinfo.address, rinfo.port);
});
