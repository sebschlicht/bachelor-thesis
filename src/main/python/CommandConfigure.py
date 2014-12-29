from string import Template
# see https://github.com/sebschlicht/bachelor-thesis/wiki/HowTo:-Create-a-custom-circus-command
from circus.commands.base import Command
from circus.exc import ArgumentError, MessageError

PORT_NEO4J = 5001

# paths to configuration file templates
PATH_TMPL = '/home/sebschlicht/git/sebschlicht/graphity-benchmark/src/main/resources/'
PATH_TMPL_NEO4J_PROP = PATH_TMPL + 'neo4j.properties.tmpl'
PATH_TMPL_NEO4J_SERVER = PATH_TMPL + 'neo4j-server.properties.tmpl'
PATH_TMPL_TITAN_CASSANDRA = PATH_TMPL + 'cassandra-cluster.yaml.tmpl'
PATH_TMPL_TITAN_REXSTER = PATH_TMPL + 'rexster-cassandra-cluster.xml.tmpl'
# paths to configuration files
#PATH_CONF_NEO4J = '/var/lib/neo4j/conf/'
PATH_CONF_NEO4J = '/tmp/neo4jc/'
PATH_CONF_NEO4J_PROP = PATH_CONF_NEO4J + 'neo4j.properties'
PATH_CONF_NEO4J_SERVER = PATH_CONF_NEO4J + 'neo4j-server.properties'
#PATH_CONF_TITAN = '/etc/titan/'
PATH_CONF_TITAN = '/tmp/titanc/'
PATH_CONF_TITAN_CASSANDRA = PATH_CONF_TITAN + 'cassandra-cluster.yaml'
PATH_CONF_TITAN_REXSTER = PATH_CONF_TITAN + 'rexster-cassandra-cluster.xml'

OPT_ADDRESS = 'address'
OPT_CLUSTER = 'cluster'

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
    ('', OPT_CLUSTER, None, 'IP addresses of all cluster nodes')
  ]
  
  def execute(self, arbiter, props):
    # execute command
    self.configureNeo4j(props)
    self.configureTitan(props)
    return { 'test': False }
  
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

  def message(self, *args, **opts):
    numArgs = 2
    if len(args) < numArgs:
      raise ArgumentError('Invalid number of arguments.')
    if len(args) == numArgs:
      opts['address'] = args[0]
      opts['cluster'] = args[1].split(',')
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
      'endpoints': endpoints
    })
    #self.writeConfig(PATH_TMPL_NEO4J_SERVER, PATH_CONF_NEO4J_SERVER, props)
  
  def configureTitan(self, props):
    #self.writeConfig(PATH_TMPL_TITAN_CASSANDRA, PATH_CONF_TITAN_CASSANDRA, props)
    #self.writeConfig(PATH_TMPL_TITAN_REXSTER, PATH_CONF_TITAN_REXSTER, props)
    foo = True

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
