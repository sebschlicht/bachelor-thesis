# Benchmarking Framework
A scaling system has to be able to handle a large number of clients concurrently.
A benchmarking framework used to evaluate scaling thus has to take this into account.

A very basic framework was developed, that uses a single master where clients register, can be controlled and where you can configure the maximum throughput. Each client chooses its next request according to the statistics in the dump of the german Wikipedia, as used in the Graphity paper.

We consider 4 types of requests:
* feed: retrieve the news feed (aggregation of status updates of the users subscribed to)
* follow: subscribe to a different user
* unfollow: unsubscribe from a user
* post: post a status update that would appear on the user wall/start pages of the followers

The framework nodes were programmed in Java using an embedded Jetty server for cluster management.

## Master
The framework master is the first instance started.
It does not fire requests against the target cluster but will control the clients that do and collect the benchmark results.
Every client will be configured to contact this master first, thus its IP address has to be known and should be static.

### Cluster management actions
#### Register
A client visiting this URL will be added to the client list.
* URL: `/register`
* Method: `GET`

#### Deregister
A client visting this URL will be removed from the client list.
* URL: `/deregister`
* Method: `GET`

## Client
Framework clients fire requests against the target cluster and provide information about the benchmark progress.
Any number of clients can participate in the benchmark cluster.
Each client has to be configured in order to know the IP address of the benchmark master.
They all hold a copy of the Wikidump statistics. Additional benchmark parameters will be passed by the master.

### Cluster management actions
#### Start
The master visits this URL of every client in its client list in order to start a new benchmark. Additional parameters for this run will be passed in the `POST`-parameter.
* URL: `/start`
* Method: `POST`
 * `value`: This field contains a JSON object with the benchmark parameters.

##### Benchmark parameters (JSON)
| Key | Type | Description |
| --- | ---- | ----------- |
| requests          | Object | Request composition
| requests.feed     | Float  | Percentage of feed retrieval requests.
| requests.follow   | Float  | Percentage of follow requests.
| requests.unfollow | Float  | Percentage of unfollow requests.
| requests.post     | Float  | Percentage of post requests.
| maxThroughput | Integer | Maximum number of requests per second per client.
| numThreads | Integer | Number of threads per client.

#### Status
The master visits this URL of every client in its client list in a fixed interval (1 second) in order to retrieve a snapshot of the client's local benchmark result.
* URL: `/status`
* Method: `GET`
* Response: `application/json` - A JSON object with the snapshot of the local benchmark result.

#### Stop
The master visits this URL of every client in its client list in order to stop the current benchmark.
The client will respond with the local benchmark result.
* URL: `/stop`
* Method: `GET`
* Response: `application/json` - A JSON object with the local benchmark result.
