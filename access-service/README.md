## About
Learning the Akka toolkit for parallel processing.
This takes advantage for not just multiple cores in a single machine,
but also for multiple cores in multiple machines.

This program will try to read csv and process it per row in parallel accross multiple cores and multiple nodes. It will be processed by getting data from other service, hence the name.

Akka provides the right-level of abstraction over concepts and problems related to programming in a distributed environment.
Those abstraction and problems helps me learn to build a reactive, or just better application.
Highly recommend this anyone who wish to be a better software engineer to learn this!

## How to build

Install SBT (scala build tools) and do

```
git clone https://github.com/Mandelag/scala.git
cd scala/access-service
sbt stage
```
It will create an executable script for the program at ```target/universal/stage/bin/useservice```

## How to run
This program has two modes for running: as a worker and as a driver.
Worker mode will create a pool of worker actors (```CsvWorker```) to process the input csv.
This mode also used to spawn a single instance of master actor (```CsvMaster```) to read and distribute the csv row.
Set the ```app.csv-master-address``` config properties to the ```CsvMaster``` actor path to run it as purely worker,
otherwise the master actor will be created.

The driver program will send a message to the ```CsvMaster``` to start reading the input file.

The order this should be run would be like this:
1. Run as worker node with a single instance of ```CsvMaster``` in a single node.
1. Run as purely worker node in any other nodes, referencing the ```CsvMaster``` actor path in (1).
1. Run as the driver.

The following example are used to setup a cluster in your local environment.

#### To run as a worker node with single instance of ```CsvMaster```
```
target/universal/stage/bin/useservice worker \
-Dakka.cluster.seed-nodes.0="akka://csv-processing@localhost:2661" \
-Dakka.remote.artery.canonical.hostname=localhost \
-Dakka.remote.artery.canonical.port=2661
```
#### To run as a purely worker node
```
target/universal/stage/bin/useservice worker \
-Dakka.cluster.seed-nodes.0="akka://csv-processing@localhost:2661" \
-Dakka.remote.artery.canonical.hostname=localhost \
-Dakka.remote.artery.canonical.port=2662 \
-Dapp.csv-master-address="akka://csv-processing@localhost:2661/user/csv-master"
```
#### To run the driver
```
target/universal/stage/bin/useservice driver \
-Dakka.cluster.seed-nodes.0="akka://csv-processing@localhost:2661" \
-Dakka.remote.artery.canonical.hostname=localhost \
-Dakka.remote.artery.canonical.port=2666 \
-Dapp.csv-master-address="akka://csv-processing@localhost:2661/user/csv-master"
```

##### Info
```akka.cluster.seed-nodes.1``` configuration is used by akka to find seed node(s) for our instance to join the cluster. The first step is to join the program to it self.

```akka.remote.artery.canonical.port``` are used to specify the port in which your actor system run. Because they are run in the same local machine, the port must differ.

```akka.remote.artery.canonical.host``` are used to specify the canonical host in which your actor system run.

```app.csv-master-address``` are used to locate the ```CsvMaster``` actor for registering worker and to start processing.

