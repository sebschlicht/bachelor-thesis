# Benchmark data
I use the wiki dump of the year 2011. It is the [same dataset](http://www.rene-pickhardt.de/graphity-source-code/) that Rene Pickhardt used in his [Graphity Evaluation](http://www.rene-pickhardt.de/graphity-an-efficient-graph-model-for-retrieving-the-top-k-news-feeds-for-users-in-social-networks/). The dump can be seens as a social network between the Wikipedia articles.

## Short benchmark
The short benchmark makes use of the first 100.000 requests of the wiki dump.
It is important to note, that this social network starts from the very beginning.
Many of the first request target the same node, as there are only a few existing. From request to request more nodes get created lazily and the chances that two consecutive requests target different nodes increase.  
This is important when we fire requests concurrently.

| Request type | Number of requests | Percentage |
| ------------ | ------------------ | ---------- |
| RemoveFollowship | 34385 | ~34.4% |
| AddFollowship | 47263 | ~47.3% |
| AddStatusUpdate | 18352 | ~18.4% |
(distribiution of first 100.000 wiki dump request types)
