# VM
I use a VM aka. guest machine running in Oracle VirtualBox in order to work in a clean and save environment.

## Hardware
The VM uses 2 cores and has 4GB memory.
The host machine's hardware specification follows.
A host-only-adapter is used to establish networking from host to guest machine.

CPU: Intel(R) Core(TM) i5-4200U CPU @ 1.60GHz (4 cores)
Memory: 8GB
Disc: HDD 5400rpm (read: 96 MB/s, write: 101 MB/s)

Note:  
To [measure hard disk performance](http://askubuntu.com/questions/87035/how-to-check-hard-disk-performance) `Disk Utility` (read) and 

    `dd if=/dev/zero of=/tmp/output conv=fdatasync bs=384k count=1k; rm -f /tmp/output`
    
were used.

## Performance considerations
There are performance differences from host to guest machine, due to virtualization.
This is just one of many reasons why the results can not be compared to Rene's evaluation.

To get an idea of how many requests I could expect I configured my plugin to do nothing.
It still accepted requests but the requests were answered promptly.
This way I would be able to see if my plugin contains (unwanted) bottlenecks in my code.


| Duration | 74305 ms |
| Requests / s | 1345.8 |
| CPU utilization | steady at [~70%|~50%] |
| Memory utilization | steady at ~480 MB |
| Disc IO | - |

I will not be able to be faster than this, of course.
