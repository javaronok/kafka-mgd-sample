## Пример приложения для обмена сообщениями через Apache Kafka в режиме единственного брокера 

Простой пример обмегна сообщениями по схеме поставщик-потребитель. 
Используется Kafka API начиная с версии 0.9.0.

Для запуска необходимо выполнить несколько шагов: скачать, установить Kafka, 
запустить и содать необходимые очереди (топики)     

### Шаг 1: Скачать Apache Kafka

На текущий момент крайняя релизная версия kafka_2.12-0.11.0.1. 
Далее следует распаковать tar архив.

```
$ tar -xzf kafka_2.12-0.11.0.1.tgz
$ cd kafka_2.12-0.11.0.1
```
### Шаг 2: Запуск сервера
Запустим сервер ZooKeeper ранее подготовленным скриптом run_zookeeper.sh. Скрипт запускает ZK с настройками поумолчанию. 
Kafka использует единственный узел для ZK. 

```
#!/bin/sh
kafka_2.12-0.10.2.1/bin/zookeeper-server-start.sh kafka_2.12-0.10.2.1/config/zookeeper.properties &
```

Потом запускаем сам брокер Kafka подготовленным скриптом run_kafka.sh.

```
#!/bin/sh
kafka_2.12-0.10.2.1/bin/kafka-server-start.sh kafka_2.12-0.10.2.1/config/server.properties &
```

Остановка происходит в обратной последовательности. 
Сначала комбинацией control-C или `kill` процесса останавливаем Kafka брокера, после ZK. 

### Шаг 3: Создаём необходимые топики для приложения

Нужен один топик fast-messages, создать которого можно скриптом create_topic.sh, 
где в качестве первого аргумента использует имя топика:

```
$ create_topic.sh fast-messages
```
Скрипт создаёт простейший топик с одним разделом и репликой.

```
#!/bin/sh
kafka_2.12-0.10.2.1/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic $1 &
```
Впоследствии можем сменить параметры используя срипты из дистрибутива Kafka или change_topic_partitions.sh для увеличения числа разделов.  
Топики могут автоматически создаваться и из приложения, но это считается опасным подходом, так как имена топиков имеют огранияения. 

Список топиков можно посмотреть скриптом list_topics.sh.

```
$ list-topics.sh
fast-messages
summary-markers
```

На этом этапе мы можем быть уверены что на локальной машине Kafka брокер успешно запустился.  

### Шаг 4: Собираем тестовй пример
Можно сделать командой из Maven

```
$ mvn package
...
```
В результате должны получить единственный исполняемый jar файл `target/kafka-mgd-full.jar`
включающий в себя все зависимости.

Следует отметить что настройки для работы поставщика и потребителя хранятся в каталоге `resources`.

### Шаг 5: Запускаем пример в режиме поставщика данных
Поставщик будет отправлять сообщения в топик `fast-messages`.

```
$ target/kafka-example producer
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Sent msg number 0
Sent msg number 1
Sent msg number 2
...
Sent msg number 997
Sent msg number 998
Sent msg number 999
```

### Шаг 6: Запускаем пример в режиме поребителя данных 
При запуске потребителя на позицию с которой забираются записи из раздела влияют несколько параметров.
Параметр group.id определяет группу потребителей в разрезе котоой хранятся смещений записей в разделе топика. 
Смещение с которого начинается чтение при старте потребителя определяется параметром `auto.offset.reset`.
Оно может принимать несколько значений:

- earliest. Смещение с начала очереди или с последнего смещения после предыдущей остановки.
- latest. Получение данных начинается только новых с момента запуска.
- none. Выбрасывается исключение если группа потребителей не установила смещение, например, не производила чтение из топика.
- anything else. Выбрасывает исключение в любом случае. 

Так же на поведение потребителя влияют настройки буферов при фетче из раздела топика а так же само количество топиков.
Если топиков меньше чем потоков в потребителя, то лишние потоки будут простаивать.

```
$ target/kafka-example consumer
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Got 500 records after 0 timeouts
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 0, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 1, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 2, time: 14.11.2017 19:20:50 
...
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 496, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 497, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 498, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 499, time: 14.11.2017 19:20:50 
Got 500 records after 0 timeouts
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 500, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 501, time: 14.11.2017 19:20:50 
...
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 996, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 997, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 998, time: 14.11.2017 19:20:50 
Thread: pool-1-thread-1, Topic:fast-messages, partition:0, Value: 999, time: 14.11.2017 19:20:50 

```
Далее потребитель будет ожидать получение новых сообщений из раздела топика. 
Не отключая потребителя мы можем снова запустить поставщика с новыми сообщения и они дойдут до потребителя.

## Fun Step: Mess with the consumer

While the producer is producing messages (you may want to put it in a
loop so it keeps going) and the consumer is eating them, try killing
and restarting the consumer. The new consumer will wait for about 10
seconds before Kafka assigns it to the partitions that the killed
consumer was handling. Once the consumer gets cooking, however, the
latency on the records it is processing should drop quickly to the
steady state rate of a few milliseconds. You can have similar effect
by using control-Z to pause the consumer for a few seconds but if you
do that, the consumer should restart processing immediately as soon as
you let it continue. The way that this works is if you pause for a
short time, the consumer still has the topic partition assigned to it
by the Kafka broker, so it can start right back up. On the other hand,
if you pause for more than about 10 seconds, the broker will decide
the consumer has died and that there is nobody available to handle the
partitions in the topic for that consumer group. As soon as the
consumer program comes back, however, it will reclaim ownership and
continue reading data.

## Remaining Mysteries
If you change the consumer properties, particular the buffer sizes
near the end of properties file, you may notice that the
consumer can easily get into a state where it has about 5 seconds of
timeouts during which no data comes from Kafka and then a full
bufferful arrives. Once in this mode, the consumer tends to not
recover to normal processing. It isn't clear what is going on, but
setting the buffer sizes large enough can avoid the problem.

## Cleaning Up
When you are done playing, stop Kafka and Zookeeper and delete the
data directories they were using from /tmp

```
$ fg
bin/kafka-server-start.sh config/server.properties
^C[2016-02-06 18:06:56,683] INFO [Kafka Server 0], shutting down (kafka.server.KafkaServer)
...
[2016-02-06 18:06:58,977] INFO EventThread shut down (org.apache.zookeeper.ClientCnxn)
[2016-02-06 18:06:58,978] INFO Closed socket connection for client /fe80:0:0:0:0:0:0:1%1:65170 which had sessionid 0x152b958c3300000 (org.apache.zookeeper.server.NIOServerCnxn)
[2016-02-06 18:06:58,979] INFO [Kafka Server 0], shut down completed (kafka.server.KafkaServer)
$ fg
bin/zookeeper-server-start.sh config/zookeeper.properties
^C
$ rm -rf /tmp/zookeeper/version-2/log.1  ; rm -rf /tmp/kafka-logs/
$
```

## Credits
Note that this example was derived in part from the documentation provided by the Apache Kafka project. We have 
added short, realistic sample programs that illustrate how real programs are written using Kafka.  
