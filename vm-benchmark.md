# VM benchmark

## Results
Note: The maximum number of requests/s for the Neo4j server is [at about 1350](/vm-performance-differences.md).

| Key | Value |
|:----|:------|
| Duration | 602419ms |
| Requests/s | 166 |
| CPU utilization | 50-85%/30-75% |
| Memory utilization | slowly growing, <15% |
| Disc IO (host) | jumping extremely: 5-85%, ~7MB/s, 2-12 M/s |

The throughput differs by nearly a magnitude.

### RAM disc
Though the results of `iotop` do not blame the IO to cause this performance drop I used a RAM disc to verify this.
What `iotop` displays does not seem to be valid. 12 M/s is far too less to be 85% of the maximum write performance of my HDD. With the graph mounted in memory using

    sudo mount -t tmpfs -o size=1024M tmpfs /tmp/graph/

the benchmark was repeated.

| Key | Value |
|:----|:------|
| Duration | 241195ms |
| Requests/s | 415 |
| CPU utilization | 80-90%/45-80% |
| Memory utilization | growing faster, <15% |
| Disc IO (host) | - |
