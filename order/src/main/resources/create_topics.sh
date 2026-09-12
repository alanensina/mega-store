#!/bin/sh

set -eu

bootstrap_servers="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9094}"
topic="${KAFKA_TOPIC:-orders_topic}"

create_topic() {
	"$@" --create \
		--if-not-exists \
		--topic "$topic" \
		--bootstrap-server "$bootstrap_servers" \
		--partitions 1 \
		--replication-factor 1
}

if command -v kafka-topics.sh > /dev/null 2>&1; then
	create_topic kafka-topics.sh
elif [ -x /opt/kafka/bin/kafka-topics.sh ]; then
	create_topic /opt/kafka/bin/kafka-topics.sh
elif command -v docker > /dev/null 2>&1; then
	docker exec kafka /opt/kafka/bin/kafka-topics.sh \
		--create \
		--if-not-exists \
		--topic "$topic" \
		--bootstrap-server localhost:9092 \
		--partitions 1 \
		--replication-factor 1
else
	echo "Kafka topic CLI not found" >&2
	exit 1
fi
