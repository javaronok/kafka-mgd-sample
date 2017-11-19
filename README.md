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

Отключив потребителя брокер Kafka не сразу сможет понять что потребителя раздела больше нет, поэтому при повторном запуске можно наблюдать 
10-ти секундную задержку перед возобновлением получения данных из очереди. 

## Очистка
После выполнения работы и остановки брокера Kafka и Zookeeper очистка логов выполняется простым удалением настроенных каталогов.

```
$ rm -rf /tmp/zookeeper/version-2/log.1  ; rm -rf /tmp/kafka-logs/
```

##Запуск с помощью Docker в режиме единственного брокера.
Запускаем связку zk, kafka брокера, поставщика и потребителя с помощью docker-compose:

```
$ docker-compose -f docker/docker-singlebroker-compose.yml up -d
Starting docker_zookeeper_1 ... 
Starting docker_zookeeper_1 ... done
Creating docker_kafka_1 ... 
Creating docker_kafka_1 ... done
Creating kafka-producer ... 
Creating kafka-producer ... done
Creating kafka-consumer ... 
Creating kafka-consumer ... done
$
```
Состояние запуска контейнеров можно проверить и выглядить должно примерно так:

```
$ docker ps -a
CONTAINER ID        IMAGE                         COMMAND                  CREATED             STATUS                          PORTS                                        NAMES
b943e71c3ed4        kafka-consumer                "java -Xmx200m -ja..."   3 minutes ago       Up 3 minutes                                                                 kafka-consumer
f160745ce05e        kafka-producer                "java -Xmx200m -ja..."   3 minutes ago       Restarting (0) 49 seconds ago                                                kafka-producer
42dc8572b989        wurstmeister/kafka:0.10.2.0   "start-kafka.sh"         3 minutes ago       Up 3 minutes                    0.0.0.0:9092->9092/tcp                       docker_kafka_1
213f56df1de6        zookeeper:3.4.10              "/docker-entrypoin..."   4 minutes ago       Up 3 minutes                    2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp   docker_zookeeper_1
```

Командами `docker logs kafka-producer` и `docker logs kafka-consumer` можно убедиться что сообщения были переданы от поставщика и получены потребителем.

##Запуск с помощью Docker в режиме отказоустойчивого кластера с множеством брокеров.
Запускаем связку zk, kafka брокеры, поставщика и потребителя с помощью docker-compose:

```
$ docker-compose -f docker/docker-multibroker-compose.yml up -d
Starting docker_zookeeper_1 ... 
Starting docker_zookeeper_1 ... done
Starting docker_kafka1_1 ... 
Starting docker_kafka2_1 ... 
Starting docker_kafka3_1 ... 
Starting docker_kafka1_1
Starting docker_kafka2_1
Starting docker_kafka3_1 ... done
Starting kafka-producer ... 
Starting kafka-producer ... done
Starting kafka-consumer ... 
Starting kafka-consumer ... done
$
```

Убедимся что все узлы кластера стартовали успешно: 

```
$ docker-compose -f docker/docker-multibroker-compose.yml ps
       Name                     Command               State                     Ports                    
--------------------------------------------------------------------------------------------------------
docker_kafka1_1      start-kafka.sh                   Up      0.0.0.0:9191->9092/tcp                     
docker_kafka2_1      start-kafka.sh                   Up      0.0.0.0:9192->9092/tcp                     
docker_kafka3_1      start-kafka.sh                   Up      0.0.0.0:9193->9092/tcp                     
docker_zookeeper_1   /docker-entrypoint.sh zkSe ...   Up      0.0.0.0:2181->2181/tcp, 2888/tcp, 3888/tcp 
kafka-consumer       /start.sh                        Up                                                 
kafka-producer       /run.sh                          Up                                                 
$
```

Выполним команды `docker attach kafka-producer`

```
$ docker attach kafka-producer
...
Sent msg number 5587
Sent msg number 5588
Sent msg number 5589

```
 и `docker attach kafka-consumer` и убедимся что сообщения отправляются и доставляются.

```
$ docker attach kafka-consumer
Got 1 records after 0 timeouts
Thread: pool-1-thread-3, Topic:fast-messages, partition:1, Value: 7862, time: 19.11.2017 12:23:42 
Got 1 records after 0 timeouts
Thread: pool-1-thread-3, Topic:fast-messages, partition:1, Value: 7863, time: 19.11.2017 12:23:42 
Got 1 records after 0 timeouts
Thread: pool-1-thread-2, Topic:fast-messages, partition:0, Value: 7864, time: 19.11.2017 12:23:42 

```

