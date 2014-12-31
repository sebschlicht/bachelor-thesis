#!/usr/bin/python
import os, time
from os import listdir
from os.path import isfile, join
import json
import subprocess
from threading import Event, Thread
# pip install circus
import zmq
# apt-get install python-django
from django.template import Template, Context
from django.conf import settings
settings.configure()
# pip install parallel-ssh
from pssh import ParallelSSHClient
import paramiko
import getpass

class SshClient:
  def __init__(self):
    self.username = SSH_USER
    self.sshArgs = [
      './sshpt.py',
      '-u', self.username,
      '-s',
      '-U', self.username,
      '-P', SSH_USER,
      '-f', PATH_LOCAL_NODES,
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
    return subprocess.check_output(scpArgs)

class CircusController:
  def __init__(self):
    self.nodes = []
    self.nodeAddresses = []
    self.isBusy = False
    self.connected = False
    self.context = zmq.Context()
    self.key = None
    with open(PATH_HTML_TMPL, 'r') as templateFile:
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
    with open(PATH_LOCAL_NODES, 'w') as fNodeAddresses:
      for nodeAddress in self.nodeAddresses:
        fNodeAddresses.write(nodeAddress + '\n')
  
  def addNode(self, node):
    self.nodes.append(node)
    self.nodeAddresses.append(node.address)
    node.connect(self.context)
  
  def getSshKey(self):
    if self.key is None:
      ssh_pw = getpass.getpass('(optional) unlock your SSH key:')
      self.key = paramiko.RSAKey.from_private_key_file(PATH_SSH_KEY,password=ssh_pw)
    return self.key
  
  def getSshClient(self):
    cluster = []
    for node in self.nodes:
      cluster.append(node.address)
    return ParallelSSHClient(hosts=cluster, user=SSH_USER, pkey=self.getSshKey())
  
  def startCircus(self):
    client = SshClient()
    # upload configure command
    client.doScp([
      
    ])
    client.doSsh([
      '/home/node/circus/start.sh'
    ])
    
  def restartCircus(self):
    #TODO stop and restart Circus via parallel SSH
    return
  
  def upload(self):
    # update configuration file templates via parallel SSH
    client = SshClient()
    for f in PATH_TMPL_CONF_FILES:
      client.doScp(PATH_TMPL_CONF_LOCAL + f, PATH_TMPL_CONF_REMOTE)
  
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
    with open(PATH_HTML, 'w') as htmlFile:
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
    
  def sendJson(self, json):
    self.socket.send_json(json)
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
      cmdStart['properties'] = {
        'name': name
      }
    self.sendJson(cmdStop)
  
  def configure(self, cluster):
    cmdConfigure = {
      'command': 'configure',
      'properties': {
        'address': self.address,
        'cluster': cluster,
        'identifier': self.identifier
      }
    }
    print self.sendJson(cmdConfigure)

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

# config
INTERVAL_UPDATE = 1
PATH_HTML = '/var/www/circusMan/index.html'
PATH_HTML_TMPL = '../resources/tmpl_list.html'
PATH_SSH_KEY = os.path.expanduser('~') + '/.ssh/id_rsa'

PATH_LOCAL_NODES = 'hosts'
PATH_LOCAL_SSH_RESULTS = 'ssh_results.txt'
PATH_REMOTE_SCRIPT_CIRCUS = '/home/node/circus/start.sh'
# path to config template directory
PATH_TMPL_CONF_LOCAL = '/media/ubuntu-prog/git/sebschlicht/graphity-benchmark/src/main/resources/config-templates/'
# files in config template directory
PATH_TMPL_CONF_FILES = [
  'neo4j.properties.tmpl',
  'neo4j-server.properties.tmpl',
  'cassandra-cluster.yaml.tmpl',
  'rexster-cassandra-cluster.xml.tmpl'
]
PATH_TMPL_CONF_REMOTE = '/usr/local/etc/templates/'
PORT = 5555
SSH_USER = 'node'
TIMEOUT_POLL = 200
# initial cluster
#nodes = genNodes('127.0.0', 1, PORT)
nodes = [ CircusNode(1, '192.168.56.101', PORT),
CircusNode(2, '192.168.56.102', PORT),
CircusNode(3, '192.168.56.103', PORT),
CircusNode(4, '192.168.56.104', PORT) ]
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
      elif cmd == 'init':
        c.startCircus()
      elif cmd == 'start':
        if len(args) == 0:
          args.append(None)
        c.start(args[0])
        c.update()
      elif cmd == 'stop':
        if len(args) == 0:
          args.append(None)
        c.stop(args[0])
        c.update()
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
  
