#!/usr/bin/python
import os, time
from os import listdir
from os.path import isfile, join
import json
import subprocess
from threading import Event, Thread
import StringIO
import csv
import ntpath
# pip install circus
import zmq
# apt-get install python-django
#from django.template import Template, Context
#from django.conf import settings
#settings.configure()
import argparse

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

class CircusMan:
  def __init__(self, nodes):
    self.controller = CircusController()
    self.controller.setNodes(nodes)
  
  def getController(self):
    numTries = 0
    while self.controller.isBusy:
      numTries = numTries+1
      time.sleep(1)
      if numTries > 1000:
        print 'controller too busy'
        return None
    return self.controller
  
  def start(self):
    self.stopUpdate = Event()
    self.updateThread = UpdateThread(self.stopUpdate, self, INTERVAL_UPDATE)
    self.updateThread.start()
  
  def stop(self):
    self.stopUpdate.set()
    self.updateThread = None
  
  def close(self):
    self.stop()
    self.controller.close()
    
class UpdateThread(Thread):
  def __init__(self, eventStop, man, interval):
    Thread.__init__(self)
    self.stopped = eventStop
    self.daemon = True
    self.man = man
    self.interval = interval
  
  def run(self):
    while not self.stopped.wait(self.interval):
      c = self.man.getController()
      if not c is None:
        c.update()

def genNodes(startAddress, endAddress, port):
  aStartAddress = startAddress.split('.')
  aEndAddress = endAddress.split('.')
  i = 0
  while i < 4:
    aStartAddress[i] = int(aStartAddress[i])
    aEndAddress[i] = int(aEndAddress[i])
    i = i+1

  identifier = 1
  nodes = []
  currentAddress = [
    str(aStartAddress[0]),
    str(aStartAddress[1]),
    str(aStartAddress[2]),
    str(aStartAddress[3])
  ]
  numNodes = aEndAddress[3] - aStartAddress[3] + 1
  
  while identifier <= numNodes:
    address = '.'.join(currentAddress)
    nodes.append(CircusNode(identifier, address, port))
    currentAddress[3] = str(aStartAddress[3] + identifier)
    identifier = identifier+1
  return nodes

def loadNodes(filepath, port):
  nodeLines = []
  with open(filepath, 'r') as fNodes:
    nodeLines = fNodes.readlines()
  
  nodes = []
  identifier = 1
  for nodeLine in nodeLines:
    nodes.append(CircusNode(identifier, nodeLine.strip(), port))
    identifier = identifier + 1
  return nodes

"""
config section
"""
INTERVAL_UPDATE = 1
PORT = 5555
TIMEOUT_POLL = 200
# SSH options
PATH_SSH_KEY = os.path.expanduser('~') + '/.ssh/id_rsa'
SSH_USER = 'ubuntu'
PATH_LOCAL_SSH_NODES = '/tmp/sshpt-hosts'
PATH_LOCAL_SSH_RESULTS = 'ssh_results.txt'

# base directories
LOCAL_DIR_PROJECT = '/media/ubuntu-prog/git/sebschlicht/graphity-benchmark/'
REMOTE_DIR_WORKING = '/home/' + SSH_USER + '/circus/'
REMOTE_DIR_NEO4J = '/var/lib/neo4j/'
REMOTE_DIR_TITAN = '/var/lib/titan/'
REMOTE_DIR_CONFIG_TEMPLATES = '/usr/local/etc/templates/'

# local paths
LOCAL_DIR_RESOURCES = LOCAL_DIR_PROJECT + 'src/main/resources/'
LOCAL_FILE_NODES = LOCAL_DIR_RESOURCES + 'nodes'
LOCAL_DIR_CONFIG_TEMPLATES = LOCAL_DIR_RESOURCES + 'config-templates/'
LOCAL_FILE_COMMAND_CONFIGURE = LOCAL_DIR_PROJECT + 'src/main/python/configure.py'
LOCAL_FILE_NEO4J_PLUGIN = LOCAL_DIR_RESOURCES + 'neo4j-plugin.jar'
LOCAL_FILE_NEO4J_SCRIPT = LOCAL_DIR_RESOURCES + 'neo4j-circus'
LOCAL_FILE_TITAN_EXTENSION = LOCAL_DIR_RESOURCES + 'titan-extension.jar'
LOCAL_FILE_TITAN_SCRIPT = LOCAL_DIR_RESOURCES + 'titan-circus.sh'

FILENAMES_CONFIG_TEMPLATES = [
  'neo4j.properties.tmpl',
  'neo4j-server.properties.tmpl',
  'cassandra-cluster.yaml.tmpl',
  'cassandra-rackdc.properties.tmpl',
  'rexster-cassandra-cluster.xml.tmpl'
]

# remote paths
REMOTE_FILE_COMMAND_CONFIGURE = REMOTE_DIR_WORKING + 'configure.py'
REMOTE_FILE_NEO4J_PLUGIN = REMOTE_DIR_WORKING + 'graphity-plugin-neo4j-0.0.1-SNAPSHOT.jar'
REMOTE_FILE_NEO4J_SCRIPT = REMOTE_DIR_NEO4J + 'bin/neo4j-circus'
REMOTE_FILE_TITAN_EXTENSION = REMOTE_DIR_WORKING + 'graphity-extension-titan-0.0.1-SNAPSHOT.jar'
REMOTE_FILE_TITAN_SCRIPT = REMOTE_DIR_TITAN + 'bin/titan-circus.sh'

# initial cluster
#nodes = genNodes('127.0.0', 1, PORT)
nodes = loadNodes(LOCAL_FILE_NODES, PORT)
# init with auto-update
man = CircusMan(nodes)
man.start()

def printUsage(cmd):
  if cmd == 'clear':
    print 'clear [OPTION] [data|log]'
    print '\tClears all remote service data and/or log files.'
    print '\tBy default both data and log files are cleared. You will be prompted to confirm your selection.'
    print '\nOptions:'
    print '\n\t-b <backupLocation>'
    print '\t\tWhen clearing remote log files you can pass a local directory where the log files will be backed up first. The log files will be suffixed with the address of their source node.'
  elif cmd == 'cluster [OPTION] <port>':
    print '\tRedefines the cluster IP addresses and the Circus port.'
    print '\nOptions:'
    print '\n\t-f <filePath>'
    print '\t\tLoad the cluster addresses from a file. Each line must contain one node address.'
    print '\n\t-r <startAddress> <endAddress>'
    print '\t\tLoad the cluster addresses from an IP address range. Start and end address must be in the same C net.'

try:
  print 'CircusMan console'
  while True:
    cmd = raw_input('>')
    args = cmd.split()
    cmd = args[0]
    del args[0]
    
    # handle command
    c = man.getController()
    if not c is None:
      if cmd == 'upload':
        c.upload()
      elif cmd == 'startCircus':
        c.startCircus()
      elif cmd == 'restartCircus':
        man.stop()
        c.restartCircus()
        man.start()
      elif cmd == 'start':
        if len(args) == 0:
          args.append(None)
        c.start(args[0])
      elif cmd == 'stop':
        if len(args) == 0:
          args.append(None)
        c.stop(args[0])
      elif cmd == 'reset':
        choice = raw_input('Do you really want to delete all remote data? [y/n] ').lower()
        if choice == 'y':
          c.reset()
        else:
          print 'Aborted.'
      elif cmd == 'cluster':
        if len(args) != 3:
          printUsage('cluster')
          continue;
        
        startAddress = args[0].split('.')
        endAddress = args[1].split('.')
        if len(startAddress) != 4 or len(endAddress) != 4:
          printUsage('cluster')
          continue;
        
        i = 0
        while i < 4:
          startAddress[i] = int(startAddress[i])
          endAddress[i] = int(endAddress[i])
          i = i+1
        i = 0
        while i < 3:
          if startAddress[i] != endAddress[i]:
            print 'Error: Start and end address must be in the same C net.'
            break;
          i = i+1
        if i < 3:
          continue;
      
        man.stop()
        c.disconnect()
        nodes = genNodes(args[0], args[1], PORT)
        c.setNodes(nodes)
        man.start()
      elif cmd == 'configure':
        c.configure()
except KeyboardInterrupt:
  print 'shutting down...'
  man.close()
  
