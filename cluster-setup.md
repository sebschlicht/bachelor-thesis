# Neo4j

## Installation
The Neo4j server can be [installed using your package manager](http://debian.neo4j.org/?_ga=1.174493282.1166350782.1407319663).

## Node configuration

### Master
This configuration is a result of the [performance guide](http://docs.neo4j.org/chunked/stable/performance-guide.html).
Detailed information can be found in the [server configuration section](http://neo4j.com/docs/stable/server-configuration.html) of the documentation.

| Configuration | Location | Command | Description |
| ------------- | -------- | ------- | ----------- |
| OS | /etc/security/limits.conf | neo4j  soft  nofile  40000<br>neo4j  hard  nofile  40000 | Increase maximum number of open files to 40.000. |
| | /etc/pam.d/su | session required pam_limits.so | see above |
||| Does not seems to work. ||
| JVM | conf/neo4j-wrapper.conf | wrapper.java.additional=-server | Start JVM in server mode. |
| | | wrapper.java.additional=-XX:+UseConcMarkSweepGC | Enable concurrent garbage collector. |
