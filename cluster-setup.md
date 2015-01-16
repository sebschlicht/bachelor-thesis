# Setup a cluster

## Overview

## Basic node setup
* `nano`
* `htop`

## Cluster nodes
### Software
* `neo4j-enterprise`
* Titan

## Router Node
### Software
* `apache2`

### Cluster access
At the moment we have a cluster with size 3. In my setting only the master is accessible from outside the cluster.
In production you would want to have a single endpoint to use for requests that uses load balancing behind the scenes.
Another aspect is that the REST endpoint should be accessible from outside the cluster but not the admin interface.
Thus the Neo4j team suggests to use an Apache server to proxy the cluster in the [documentation's security section](http://neo4j.com/docs/stable/security-server.html). This allows load balancing, fine control of accessible endpoints and e.g. the usage of HTACCESS.

We install an Apache2 server along with the Proxy module and its dependencies

    $ apt-get install apache2 libapache2-mod-proxy-html libxml2-dev
    
on the master node which will be accessible from anywhere and balance the load across the cluster.

#### Configuration
We create a copy of the default page configuration

    $ cd /etc/apache2/sites-available
    $ cp 000-default.conf neo4j.conf
    $ cp 000-default.conf titan.conf

and insert a load balancer for the REST endpoints of the cluster instances at port 82 for Neo4j

**/etc/apache2/sites-available/neo4j.conf**:

    <VirtualHost *:82>
      <Proxy balancer://neo4j>
      BalancerMember http://localhost:7474/db/data
      BalancerMember http://10.93.130.108:7474/db/data
      BalancerMember http://10.93.130.109:7474/db/data
      </Proxy>
      
      ProxyPass /neo4j balancer://neo4j
      ProxyPassReverse /neo4j balancer://neo4j
      
      # separate write load balancer
      ProxyPass /neo4j-write http://localhost:7474/db/data
      ProxyPassReverse /neo4j-write http://localhost:7474/db/data
      
      # logging configuration follows
    </VirtualHost>

and port 83 for Titan

**/etc/apache2/sites-available/titan.conf**:

    <VirtualHost *:83>
      <Proxy balancer://titan>
      BalancerMember http://localhost:8182/
      BalancerMember http://10.93.130.108:8182/
      BalancerMember http://10.93.130.109:8182/
      </Proxy>
      
      ProxyPass /titan balancer://titan
      ProxyPassReverse /titan balancer://titan
      
      # logging configuration follows
    </VirtualHost>

where the ports have to be enabled in the port config. Please note that the configuration below will disable the default HTTP port 80.

**/etc/apache2/ports.conf**

    NameVirtualHost *:82
    Listen 82
    NameVirtualHost *:83
    Listen 83

and enable both sites and the Proxy module

    $ a2enmod proxy_http proxy_html proxy_balancer xml2enc
    $ a2ensite neo4j titan
    $ service apache2 reload

While the first endpoint balances all requests across the cluster, the second endpoint (Neo4j only) does not include any nodes other than the master. The intention is to have a separate endpoint for write requests. The cluster may perform better when using only the master node for writes: Slaves are forced to synchronize with the master in order to execute a write request.
