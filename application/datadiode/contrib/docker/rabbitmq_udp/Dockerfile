FROM rabbitmq:management

RUN rabbitmq-plugins enable --offline \
    rabbitmq_federation \
    rabbitmq_federation_management \
    rabbitmq_shovel \
    rabbitmq_shovel_management \
    rabbitmq_mqtt \
    rabbitmq_management

RUN apt-get update && apt-get dist-upgrade -fy && \
    apt-get install -fy build-essential zip wget git python xsltproc erlang-dev

RUN mkdir /rabbitmq
WORKDIR /rabbitmq

RUN git clone https://github.com/rabbitmq/rabbitmq-public-umbrella.git
WORKDIR /rabbitmq/rabbitmq-public-umbrella
RUN make co

WORKDIR /rabbitmq/rabbitmq-public-umbrella/deps
RUN git clone https://github.com/tonyg/udp-exchange.git

WORKDIR /rabbitmq/rabbitmq-public-umbrella/deps/udp-exchange
RUN cat ../rabbitmq_metronome/Makefile | sed 's/rabbitmq_metronome/rabbitmq_udp_exchange/' > Makefile && \
    cp ../rabbitmq_metronome/erlang.mk . && \
    cp ../rabbitmq_metronome/rabbitmq-components.mk . && \
    make && make dist

RUN cp plugins/*.ez /usr/lib/rabbitmq/lib/rabbitmq_server-*/plugins/ && \
    rm -rf /usr/lib/rabbitmq/lib/rabbitmq_server-*/plugins/amqp_client.ez

RUN rabbitmq-plugins enable --offline rabbitmq_udp_exchange

EXPOSE 1883 5672 15672 25672
