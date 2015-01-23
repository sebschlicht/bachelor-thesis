#!/usr/bin/python

import argparse
import atexit
import os
import readline
import rlcompleter
import sys
import zmq

from circusnode import CircusNode
from sshptclient import SshClient

"""
config section
"""
SSH_USER = 'ubuntu'
PORT_CIRCUS = 5555
TIMEOUT_POLL = 200

# SSH options
PATH_SSH_KEY = os.path.join(os.environ['HOME'], '.ssh/id_rsa')
PATH_LOCAL_SSH_NODES = '/tmp/sshpt-hosts'
PATH_LOCAL_SSH_RESULTS = 'ssh_results.txt'

# base directories
scriptDir = os.path.dirname(os.path.realpath(sys.argv[0]))
LOCAL_DIR_PROJECT = os.path.abspath(os.path.join(scriptDir, os.pardir, os.pardir, os.pardir))
REMOTE_DIR_WORKING = '/home/' + SSH_USER + '/circus/'
REMOTE_DIR_NEO4J = '/var/lib/neo4j/'
REMOTE_DIR_TITAN = '/var/lib/titan/'
REMOTE_DIR_CONFIG_TEMPLATES = '/usr/local/etc/templates/'

# local paths
LOCAL_DIR_RESOURCES = os.path.join(LOCAL_DIR_PROJECT, 'src/main/resources/')
LOCAL_FILE_NODES = os.path.join(LOCAL_DIR_RESOURCES, 'nodes')
LOCAL_DIR_CONFIG_TEMPLATES = os.path.join(LOCAL_DIR_RESOURCES, 'config-templates/')
LOCAL_FILE_COMMAND_CONFIGURE = os.path.join(LOCAL_DIR_PROJECT, 'src/main/python/configure.py')
LOCAL_FILE_NEO4J_PLUGIN = os.path.join(LOCAL_DIR_RESOURCES, 'neo4j-plugin.jar')
LOCAL_FILE_NEO4J_SCRIPT = os.path.join(LOCAL_DIR_RESOURCES, 'neo4j-circus')
LOCAL_FILE_TITAN_EXTENSION = os.path.join(LOCAL_DIR_RESOURCES, 'titan-extension.jar')
LOCAL_FILE_TITAN_SCRIPT = os.path.join(LOCAL_DIR_RESOURCES, 'titan-circus.sh')

FILENAMES_CONFIG_TEMPLATES = [
  'neo4j.properties.tmpl',
  'neo4j-server.properties.tmpl',
  'cassandra-cluster.yaml.tmpl',
  'cassandra-rackdc.properties.tmpl',
  'rexster-cassandra-cluster.xml.tmpl'
]

# remote paths
REMOTE_FILE_COMMAND_CONFIGURE = os.path.join(REMOTE_DIR_WORKING, 'configure.py')
REMOTE_FILE_NEO4J_PLUGIN = os.path.join(REMOTE_DIR_WORKING, 'graphity-plugin-neo4j-0.0.1-SNAPSHOT.jar')
REMOTE_FILE_NEO4J_SCRIPT = os.path.join(REMOTE_DIR_NEO4J, 'bin/neo4j-circus')
REMOTE_FILE_TITAN_EXTENSION = os.path.join(REMOTE_DIR_WORKING, 'graphity-extension-titan-0.0.1-SNAPSHOT.jar')
REMOTE_FILE_TITAN_SCRIPT = os.path.join(REMOTE_DIR_TITAN, 'bin/titan-circus.sh')

# enable command history
histfile = os.path.join(os.environ['HOME'], '.circusmanhistory')
try:
  readline.read_history_file(histfile)
except IOError:
  pass
atexit.register(readline.write_history_file, histfile)

# custom argument parser to stay in embedded console on error/help
class ConsoleExitsException(Exception):
  def __init__(self, status, message):
    self.status = status
    self.message = message

class EmbeddedConsoleParser(argparse.ArgumentParser):
  def exit(self, status=0, message=None):
    raise ConsoleExitsException(status, message)

class CircusController:
  def __init__(self):
    self.nodes = []
    self.nodeAddresses = []
    self.isBusy = False
    self.connected = False
    self.context = zmq.Context()
  
  def disconnect(self):
    if self.connected:
      for node in self.nodes:
        node.close()
      self.connected = False
  
  def close(self):
    self.disconnect()
    self.context.destroy()
  
  def setNodes(self, nodes):
    del self.nodes[:]
    del self.nodeAddresses[:]
    for node in nodes:
      self.addNode(node)
    self.connected = True
    with open(PATH_LOCAL_SSH_NODES, 'w') as fNodeAddresses:
      for nodeAddress in self.nodeAddresses:
        fNodeAddresses.write(nodeAddress + '\n')
  
  def addNode(self, node):
    self.nodes.append(node)
    self.nodeAddresses.append(node.address)
    node.connect(self.context)
  
  def getResources(self):
    return [
      # remote shell scripts
      (LOCAL_DIR_RESOURCES + 'start.sh', REMOTE_DIR_WORKING + 'start.sh'),
      (LOCAL_DIR_RESOURCES + 'restart.sh', REMOTE_DIR_WORKING + 'restart.sh'),
      (LOCAL_DIR_RESOURCES + 'reset.sh', REMOTE_DIR_WORKING + 'reset.sh'),
      # remote service scripts
      (LOCAL_FILE_NEO4J_SCRIPT, REMOTE_FILE_NEO4J_SCRIPT),
      (LOCAL_FILE_TITAN_SCRIPT, REMOTE_FILE_TITAN_SCRIPT),
      # Circus config + command
      (LOCAL_DIR_RESOURCES + 'circus.ini', REMOTE_DIR_WORKING + 'circus.ini'),
      (LOCAL_FILE_COMMAND_CONFIGURE, REMOTE_FILE_COMMAND_CONFIGURE),
      # Graphity plugin/extension
      (LOCAL_FILE_NEO4J_PLUGIN, REMOTE_FILE_NEO4J_PLUGIN),
      (LOCAL_FILE_TITAN_EXTENSION, REMOTE_FILE_TITAN_EXTENSION)
    ]
  
  def startCircus(self):
    # upload resources, start Circus
    client = SshClient()
    client.doScpMulti(self.getResources())
    client.doSsh([
      REMOTE_DIR_WORKING + 'start.sh'
    ])
  
  def restartCircus(self):
    # upload resources, stop and restart Circus
    client = SshClient()
    client.doScpMulti(self.getResources())
    client.doSsh([
      REMOTE_DIR_WORKING + 'restart.sh &'
    ])
    
  def upload(self):
    files = []
    # upload configuration file templates
    for filename in FILENAMES_CONFIG_TEMPLATES:
      files.append((LOCAL_DIR_CONFIG_TEMPLATES + filename, REMOTE_DIR_CONFIG_TEMPLATES + filename))
    client = SshClient()
    client.doScpMulti(files)
  
  def configure(self):
    self.isBusy = True
    cluster = []
    for node in self.nodes:
      cluster.append(node.address)
    # write configuration files
    for node in self.nodes:
      node.configure(cluster)
    self.isBusy = False
  
  def getStats(self):
    self.isBusy = True
    for node in self.nodes:
      node.getStats()
    self.isBusy = False
  
  def update(self):
    stats = self.getStats()
    node_list = []
    for node in self.nodes:
      node_list.append(node.getDict())
    # build content object and fill template
    #content = {'node_list': node_list}
    #htmlContent = self.template.render(Context(content))
    #with open(PATH_LOCAL_HTML, 'w') as htmlFile:
    #  htmlFile.write(htmlContent)
  
  def start(self, name):
    self.isBusy = True
    for node in self.nodes:
      node.start(name)
    self.isBusy = False
  
  def stop(self, name):
    self.isBusy = True
    for node in self.nodes:
      node.stop(name)
    self.isBusy = False
  
  def reset(self):
    # reset the data files of a watcher
    client = SshClient()
    client.doSsh([
      REMOTE_DIR_WORKING + 'reset.sh'
    ])

# command dict
commands = {}
def newCommand(commandName, description):
  command = EmbeddedConsoleParser(prog=commandName,description=description)
  commands[commandName] = command
  return command

def printUsage():
  print 'available commands:'
  for cmd in commands:
    print '\t' + cmd
  print '\nUse\n\t<command> -h\nto get help with its usage.'

# available commands

# clear
cmdClear = newCommand('clear', 'Clears all remote service data and/or log files.')
cmdClear.add_argument('-t', '--type', choices=['data', 'log'],
  help='Limits the deletion to a type. By default both remote service data and log files will be deleted.')
cmdClear.add_argument('-b', '--backup', metavar='backupLocation',
  help='When clearing remote log files you can pass a local directory where the log files will be backed up first. The log files will be suffixed with the address of their source node.')
# cluster
cmdCluster = newCommand('cluster', 'Redefines the cluster addresses and the Circus port.')
cmdCluster.add_argument('filePath', nargs='?', default=LOCAL_FILE_NODES,
  help='Path to the cluster file. Each line must contain one node address. (default: ' + LOCAL_FILE_NODES + ')')
cmdCluster.add_argument('port', type=int, nargs='?', default=PORT_CIRCUS,
  help='remote Circus port (default: ' + str(PORT_CIRCUS) + ')')
# (circus) configure
cmdConfigure = newCommand('configure', 'Configures Neo4j and Titan on all cluster nodes. In order to use this command Circus must be running on the nodes.')
# circus restart
cmdRestartCircus = newCommand('restartCircus', 'Restarts Circus on all cluster nodes where it is running. Start Circus on all cluster nodes where Circus is not running. Updates all scripts/service plugins/config templates at first.')
# (circus) start
cmdStart = newCommand('start', 'Starts the Circus arbiter/a Circus watcher on all cluster nodes.')
cmdStart.add_argument('watcher', nargs='?', metavar='name',
  help='Start a certain watcher only. By default the arbiter is started.')
# (circus) stop
cmdStop = newCommand('stop', 'Stops the Circus arbiter/a Circus watcher on all cluster nodes.')
cmdStop.add_argument('watcher', nargs='?', metavar='name',
  help='Stop a certain watcher only. By default the arbiter is stopped.')

def loadNodes(filepath, port):
  nodeLines = []
  with open(filepath, 'r') as fNodes:
    nodeLines = fNodes.readlines()
  
  nodes = []
  identifier = 1
  for nodeLine in nodeLines:
    nodes.append(CircusNode(identifier, nodeLine.strip(), port, TIMEOUT_POLL))
    identifier = identifier + 1
  return nodes

try:
  print 'CircusMan console'
  controller = CircusController()
  controller.setNodes(loadNodes(LOCAL_FILE_NODES, PORT_CIRCUS))
  while True:
    cmd = raw_input('>')
    cmd_args = cmd.split()
    if len(cmd_args) > 0:
      cmd = cmd_args[0]
      del cmd_args[0]
    else:
      del cmd
    if (cmd is None) or not (cmd in commands):
      printUsage()
      continue
    command = commands[cmd]
    try:
      args = command.parse_args(cmd_args)
    except ConsoleExitsException as e:
      if not (len(cmd_args) > 0 and (cmd_args[0] == '-h' or cmd_args[0] == '--help')):
        print e.message.strip()
      continue
    
    if cmd == 'clear':
      #TODO
      print 'not implemented'
      continue
    elif cmd == 'cluster':
      controller.disconnect()
      nodes = loadNodes(args.filePath, args.port)
      controller.setNodes(nodes)
      continue
    elif cmd == 'configure':
      controller.configure()
      continue
    elif cmd == 'restartCircus':
      controller.restartCircus()
      continue
    elif cmd == 'start':
      controller.start(args.watcher)
      continue
    elif cmd == 'stop':
      controller.stop(args.watcher)
      continue
    
    command.print_help()
        
except KeyboardInterrupt:
  print

