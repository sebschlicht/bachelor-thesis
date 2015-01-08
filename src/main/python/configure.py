from string import Template
# see https://github.com/sebschlicht/bachelor-thesis/wiki/HowTo:-Create-a-custom-circus-command
from circus.commands.base import Command
from circus.exc import ArgumentError, MessageError

PORT_NEO4J = 5001

# paths to configuration file templates
PATH_TMPL = '/usr/local/etc/templates/'
PATH_TMPL_NEO4J_PROP = PATH_TMPL + 'neo4j.properties.tmpl'
PATH_TMPL_NEO4J_SERVER = PATH_TMPL + 'neo4j-server.properties.tmpl'
PATH_TMPL_TITAN_CASSANDRA = PATH_TMPL + 'cassandra-cluster.yaml.tmpl'
PATH_TMPL_TITAN_CASSANDRA_RACKDC = PATH_TMPL + 'cassandra-rackdc.properties.tmpl'
PATH_TMPL_TITAN_REXSTER = PATH_TMPL + 'rexster-cassandra-cluster.xml.tmpl'
# paths to configuration files
PATH_CONF_NEO4J = '/etc/neo4j/'
PATH_CONF_NEO4J_PROP = PATH_CONF_NEO4J + 'neo4j.properties'
PATH_CONF_NEO4J_SERVER = PATH_CONF_NEO4J + 'neo4j-server.properties'
PATH_CONF_TITAN = '/etc/titan/'
PATH_CONF_TITAN_CASSANDRA = PATH_CONF_TITAN + 'cassandra-cluster.yaml'
PATH_CONF_TITAN_CASSANDRA_RACKDC = PATH_CONF_TITAN + 'cassandra-rackdc.properties'
PATH_CONF_TITAN_REXSTER = PATH_CONF_TITAN + 'rexster-cassandra-cluster.xml'

OPT_ADDRESS = 'address'
OPT_CLUSTER = 'cluster'
OPT_IDENTIFIER = 'identifier'
OPT_IS_MASTER = 'isMaster'

class Configure(Command):
  """
  Command to configure a cluster node.
  At the moment this includes Neo4j and Titan.
  The command contains the IP address of the target node and all
  other nodes that are part of the cluster.
  """
  name = "configure"
  # ensure that the configuration is applied when returning
  waiting = True
  
  options = [
    ('', OPT_ADDRESS, None, 'IP address of the cluster node'),
    ('', OPT_CLUSTER, None, 'IP addresses of all cluster nodes'),
    ('', OPT_IDENTIFIER, None, 'unique node identifier'),
    ('', OPT_IS_MASTER, True, 'determines if node can be elected to master by Neo4j')
  ]
  
  def execute(self, arbiter, props):
    # execute command
    self.configureNeo4j(props)
    self.configureTitan(props)
    return { 'success': True }
  
  def validate(self, props):
    # validate arguments, can changes props
    # throws ArgumentError if invalid in total
    # throws MessageError if content invalid
    if not OPT_ADDRESS in props:
      raise MessageError('address is missing')
    if not OPT_CLUSTER in props:
      raise MessageError('cluster is missing')
    elif not isinstance(props[OPT_CLUSTER], list):
      raise MessageError('cluster malformed: list expected')
    elif len(props[OPT_CLUSTER]) == 0:
      raise MessageError('cluster is empty')
    if not OPT_IDENTIFIER in props:
      raise MessageError('identifier is missing')

  def message(self, *args, **opts):
    numArgs = 3
    if len(args) < numArgs:
      raise ArgumentError('Invalid number of arguments. Usage: <identifier> <address> <cluster>')
    if len(args) == numArgs:
      opts['identifier'] = args[0]
      opts['address'] = args[1]
      opts['cluster'] = args[2].split(',')
    return self.make_message(**opts)
  
  # write a config file using a template
  def writeConfig(self, pathTemplate, pathDestination, args):
    with open(pathTemplate, 'r') as fTemplate:
      t = Template(fTemplate.read())
      with open(pathDestination, 'w') as fDestination:
        fDestination.write(t.substitute(args).strip())
  
  def configureNeo4j(self, props):
    endpoints = []
    for node in props[OPT_CLUSTER]:
      endpoints.append(node + ':' + str(PORT_NEO4J))
    endpoints = ','.join(endpoints)
      
    self.writeConfig(PATH_TMPL_NEO4J_PROP, PATH_CONF_NEO4J_PROP, {
      'address': props[OPT_ADDRESS],
      'identifier': props[OPT_IDENTIFIER],
      'initial_hosts': endpoints,
      'slave_only': not props[OPT_IS_MASTER]
    })
    self.writeConfig(PATH_TMPL_NEO4J_SERVER, PATH_CONF_NEO4J_SERVER, {
    })
  
  def configureTitan(self, props):
    cluster = props[OPT_CLUSTER]
    # seeds node: first node and each i%5==0 ([5], [10], [15] aso.)
    seeds = [ cluster[0] ]
    n = len(cluster)
    i = 5
    while i < n:
      seeds.append(cluster[i])
      i = i+5
    seeds = ','.join(seeds)
    
    self.writeConfig(PATH_TMPL_TITAN_CASSANDRA, PATH_CONF_TITAN_CASSANDRA, {
      'address': props[OPT_ADDRESS],
      'seeds': seeds
    })
    self.writeConfig(PATH_TMPL_TITAN_CASSANDRA_RACKDC, PATH_CONF_TITAN_CASSANDRA_RACKDC, {
    })
    self.writeConfig(PATH_TMPL_TITAN_REXSTER, PATH_CONF_TITAN_REXSTER, {
      'address': props[OPT_ADDRESS]
    })

# testing separate from circus usage
"""
print 'testing ConfigureCommand...'
args = {
  'address': '127.0.0.1',
  'cluster': ['127.0.0.1', '127.0.0.2']
}
c = Configure()
c.validate(args)
c.execute(None, args)
print 'Neo4j config @ ' + PATH_CONF_NEO4J_PROP + ':'
with open(PATH_CONF_NEO4J_PROP, 'r') as f:
  print f.read()
#"""
