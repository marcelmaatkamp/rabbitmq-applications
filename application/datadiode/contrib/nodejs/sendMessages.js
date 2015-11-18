var amqp = require('amqp');
var connection = amqp.createConnection({ host: "rabbitblack", port: 5673 });
var count = 1;

connection.on('ready', function () {
    connection.exchange("nodejsExchange", options = {type: 'headers', durable: true, autoDelete: false}, function (exchange) {

        var sendMessage = function(exchange, payload) {
            var encoded_payload = JSON.stringify(payload);
            exchange.publish('', encoded_payload, {})
        }

        setInterval( function() {
            var test_message = 'TEST '+count
            console.log('send ' + count + " ..")
            sendMessage(exchange, test_message)
            count += 1;
        }, 1000)
    })
})
