# Notes

As a first note: I were not able to profile, but to sample the Neo4j server.
This is [less accurate](http://stackoverflow.com/questions/12130107/difference-between-sampling-and-profiling-in-jvisualvm) but better than nothing.

The benchmark ran 3 minutes and could execute 17.580 requests which is an average of 98 req/s.
Note: This can not be compared to previous results, because the VM had 3 cores and 4GB of memory for the sampling.

# Results (CPU)

| Method | Self time (%) | Selftime (ms) |
| org.eclipse.jetty.io.SelectorManager.ManagedSelector.select | 43.9 | 165107 |
| org.neo4j.kernel.impl.nioneo.store.StoreFileChannel.force | 16.4 | 61806 |
| org.apache.lucene.store.NIOFSDirectory.NIOFSIndexInput.readInteral | 4.6 | 17315 |
| org.apache.lucene.index.FormatPostingsDocsWrite.finish | 4.1 | 15574 |
| org.apache.lucene.store.FSDirectory.FSIndexOutput.<init> | 1.7 | 6369 |
| org.apache.lucene.store.FSDirectory.FSIndexOutput.flushBuffer | 1.6 | 6017 |
| org.eclipse.jetty.io.ChannelEndPoint.flush | 1.6 | 5941 |

All other methods were taking less than 1.5% of the CPU time.
17 of the 20 most CPU-heavy processes are related to IO, according to their name.
Though `iotop` did not measure an IO uitlization higher than about 33%.
