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
from django.template import Template, Context
from django.conf import settings
settings.configure()

class SshClient:
  def __init__(self):
    self.username = SSH_USER
    self.sshArgs = [
      './sshpt.py',
      '-u', self.username,
      '-s',
      '-U', self.username,
      '-P', SSH_USER,
      '-f', PATH_LOCAL_SSH_NODES,
      '-o', PATH_LOCAL_SSH_RESULTS
    ]
  
  def doSsh(self, args):
    sshArgs = self.sshArgs + args
    return subprocess.check_output(sshArgs)
  
  def doScp(self, pathLocal, pathRemote):
    scpArgs = self.sshArgs + [
      '-c', pathLocal,
      '-D', pathRemote
    ]
    result = subprocess.check_output(scpArgs)
    f = StringIO.StringIO(result)
    reader = csv.reader(f, delimiter=',')
    for row in reader:
      if len(row) != 5 or row[1] != 'SUCCESS' or row[4].startswith('[Errno'):
       print 'failed to copy "' + pathLocal + '" @ ' + row[0]
       return False
    return True
  
  def doScpMulti(self, files):
    # create archive file
    zipArgs = [
      'zip',
      '/tmp/circusman-scp.zip'
    ]
    for f in files:
      zipArgs.append(f[0])
    if not subprocess.check_output(zipArgs):
      return False
    
    # transmit archive file
    self.doScp('/tmp/circusman-scp.zip', '/tmp/')
    
    # remove archive file
    rmArgs = [
      'rm',
      '/tmp/circusman-scp.zip'
    ]
    if not subprocess.check_output(rmArgs):
      return False
    
    # unzip remote archive file
    unzipArgs = [
      'unzip -j /tmp/circusman-scp.zip -d /tmp/circusman-scp',
      'rm /tmp/circusman-scp.zip'
    ]
    # move files to their destinations
    for f in files:
      parent, filename = ntpath.split(f[0])
      unzipArgs.append('cp /tmp/circusman-scp/' + filename + ' ' + f[1])
    unzipArgs.append('rm -rf /tmp/circusman-scp')
    if not self.doSsh(unzipArgs):
      return False
    return True

class CircusController:
  def __init__(self):
    self.nodes = []
    self.nodeAddresses = []
    self.isBusy = False
    self.connected = False
    self.context = zmq.Context()
    self.key = None
    with open(PATH_LOCAL_TMPL_HTML, 'r') as templateFile:
      self.template = Template(templateFile.read())
  
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
    content = {'node_list': node_list}
    htmlContent = self.template.render(Context(content))
    with open(PATH_LOCAL_HTML, 'w') as htmlFile:
      htmlFile.write(htmlContent)
  
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

class CircusNode:
  def __init__(self, identifier, address, port):
    self.identifier = identifier
    self.address = address
    self.port = int(port)
    self.sending = False
    self.isMaster = False
    if identifier == 1:
      self.isMaster = True
  
  def getDict(self):
    return {
      'address': self.address,
      'identifier': self.identifier,
      'status': self.status
    }
    
  def connect(self, context):
    self.socket = context.socket(zmq.PAIR)
    self.socket.setsockopt(zmq.LINGER, 0)
    self.socket.connect("tcp://{host}:{port}".format(host=self.address,port=self.port))
    self.poller = zmq.Poller()
    self.poller.register(self.socket, zmq.POLLIN)
    
  def close(self):
    self.socket.close(0)
    
  def sendJson(self, jsonValue):
    self.socket.send_json(jsonValue)
    self.sending = True
    socks = dict(self.poller.poll(TIMEOUT_POLL))
    if self.socket in socks and socks[self.socket] == zmq.POLLIN:
      reply = self.socket.recv_json()
    else:
      reply = False
    self.sending = False
    return reply
  
  def getStats(self):
    stats = self.sendJson({"command":"stats"})
    if stats:
      apps = []
      if 'infos' in stats.keys():
        for app in stats['infos']:
          running = False
          for process in stats['infos'][app]:
            running = True
            break
          if running:
            apps.append(app)
      self.status = apps
    else:
      self.status = 'offline'
  
  def start(self, name):
    cmdStart = {
      'command': 'start'
    }
    if not name is None:
      cmdStart['properties'] = {
        'name': name
      }
    self.sendJson(cmdStart)
  
  def stop(self, name):
    cmdStop = {
      'command': 'stop'
    }
    if not name is None:
      cmdStop['properties'] = {
        'name': name
      }
    self.sendJson(cmdStop)
  
  def configure(self, cluster):
    cmdConfigure = {
      'command': 'configure',
      'properties': {
        'address': self.address,
        'cluster': cluster,
        'identifier': self.identifier,
        'isMaster': self.isMaster
      }
    }
    reply = self.sendJson(cmdConfigure)
    if not reply:
      print self.address + ' is not responding'
    elif not 'success' in reply:
      print 'failed to configure ' + self.address + ':'
      print reply

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

def genNodes(network, numNodes, port):
  i = 1
  nodes = []
  while i <= int(numNodes):
    address = network + '.' + str(i)
    nodes.append(CircusNode(i, address, port))
    i = i+1
  return nodes

"""
config section
"""
INTERVAL_UPDATE = 1
PORT = 5555
TIMEOUT_POLL = 200
# HTML file paths (node status overview)
PATH_LOCAL_HTML = '/var/www/circusMan/index.html'
PATH_LOCAL_TMPL_HTML = '../resources/tmpl_list.html'
# SSH options
PATH_SSH_KEY = os.path.expanduser('~') + '/.ssh/id_rsa'
SSH_USER = 'node'
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
nodes = [
  CircusNode(1, '192.168.56.101', PORT)#,
  #CircusNode(2, '192.168.56.102', PORT)
]
# init with auto-update
man = CircusMan(nodes)
man.start()

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
  
