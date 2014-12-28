from string import Template
# see https://github.com/sebschlicht/bachelor-thesis/wiki/HowTo:-Create-a-custom-circus-command
from circus.commands.base import Command
from circus.exc import ArgumentError, MessageError

OPT_ADDRESS = 'address'
OPT_CLUSTER = 'cluster'

# paths to configuration file templates
PATH_NEO4J_TEMPLATE_PROP = '/home/sebschlicht/git/sebschlicht/graphity-benchmark/src/main/resources/neo4j_prop.tmpl'
# paths to configuration files
#PATH_NEO4J_CONF = '/var/lib/neo4j/conf'
PATH_NEO4J_CONF = '/tmp'
PATH_NEO4J_CONF_PROP = PATH_NEO4J_CONF + '/neo4j.properties'

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
    #TODO: configure Neo4j
    self.writeConfig(PATH_NEO4J_TEMPLATE_PROP, PATH_NEO4J_CONF_PROP, props)
  
  def configureTitan(self, props):
    #TODO: configure Titan
    PATH_TITAN_CONF = '/etc/titan'

# testing separate from circus usage
#print 'testing ConfigureCommand...'
#args = {
#  'address': '127.0.0.1',
#  'cluster': ['127.0.0.1', '127.0.0.2']
#}
#c = Configure()
#c.validate(args)
#c.execute(None, args)
#print 'Neo4j config @ ' + PATH_NEO4J_CONF_PROP + ':'
#with open(PATH_NEO4J_CONF_PROP, 'r') as f:
#  print f.read()
