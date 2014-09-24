# Neo4j

## Node setup

### Master
This configuration is a result of the [performance guide](http://docs.neo4j.org/chunked/stable/performance-guide.html).

| Configuration | Location | Command | Description |
| ------------- | -------- | ------- | ----------- |
| OS | /etc/security/limits.conf | neo4j  soft  nofile  40000  neo4j  hard  nofile  40000 | Increase maximum number of open files to 40.000. |
| JVM | conf/neo4j-wrapper.conf | wrapper.java.additional=-server | Start JVM in server mode. |
| | conf/neo4j-wrapper.conf | wrapper.java.additional=-XX:+UseConcMarkSweepGC | Enable concurrent garbage collector. |
