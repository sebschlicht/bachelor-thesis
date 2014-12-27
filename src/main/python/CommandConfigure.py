from string import Template
# make circus library accessible:
# ln -s /tmp/circus/lib/python2.7/site-packages/circus circus
from circus.exc import MessageError

OPT_ADDRESS = 'address'
OPT_CLUSTER = 'cluster'

PATH_NEO4J_TEMPLATE_PROP = '../resources/neo4j_prop.tmpl'
#PATH_NEO4J_CONF = '/var/lib/neo4j/conf'
PATH_NEO4J_CONF = '/tmp'
PATH_NEO4J_CONF_PROP = PATH_NEO4J_CONF + '/neo4j.properties'

#circus.commands.base.Command
#class Configure(Command):
class Configure:
  """
  Command to configure a cluster node.
  At the moment this includes Neo4j and Titan.
  The command contains the IP address of the target node and all
  other nodes that are part of the cluster.
  """
  name = "configure"
  
  options = [
    ('', OPT_ADDRESS, None, 'IP address of the cluster node'),
    ('', OPT_CLUSTER, None, 'IP addresses of all cluster nodes')
  ]
  
  def execute(self, arbiter, props):
    # execute command
    self.configureNeo4j(props)
    self.configureTitan(props)
    return { self.name: True }
  
  def console_msg(self, msg):
    # format console output
    return msg
  
  def validate(self, props):
    # validate arguments, can changes props
    # throws ArgumentError if invalid in total
    # throws MessageError if content invalid
    if OPT_CLUSTER in props.keys():
      cluster = props[OPT_CLUSTER]
      if len(cluster) == 0:
        raise MessageError('cluster is empty')
      if len(cluster) > 1:
        props[OPT_CLUSTER] = ','.join(cluster)
    else:
      raise MessageError('cluster undefined')
  
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

#TODO: delete debug section
print 'testing ConfigureCommand...'
args = {
  'address': '127.0.0.1',
  'cluster': ['127.0.0.1', '127.0.0.2']
}
c = Configure()
c.validate(args)
c.execute(None, args)
print 'Neo4j config @ ' + PATH_NEO4J_CONF_PROP + ':'
with open(PATH_NEO4J_CONF_PROP, 'r') as f:
  print f.read()
