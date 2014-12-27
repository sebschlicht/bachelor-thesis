#!/usr/bin/python
import os, time
import json
from threading import Event, Thread
# apt-get install python-zmq
import zmq
from zmq.utils import jsonapi
# apt-get install python-django
from django.template import Template, Context
from django.conf import settings
settings.configure()

class CircusController:
  def __init__(self):
    self.nodes = []
    self.isBusy = False
    self.connected = False
    self.context = zmq.Context()
    with open('../resources/tmpl_list.html', 'r') as templateFile:
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
    for node in nodes:
      self.addNode(node)
    self.connected = True
  
  def addNode(self, node):
    self.nodes.append(node)
    node.connect(self.context)
  
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
    with open('/var/www/circusMan/index.html', 'w') as htmlFile:
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

class CircusMan:
  def __init__(self, nodes):
    self.controller = CircusController()
    self.controller.setNodes(nodes)
    self.stopUpdate = Event()
  
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
PORT = 5555
TIMEOUT_POLL = 200
# initial cluster
nodes = genNodes('127.0.0', 1, PORT)
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
      if cmd == 'start':
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
        print 'update stopped'
        c.disconnect()
        print 'disconnected'
        nodes = genNodes(args[0], args[1], PORT)
        print 'cluster generated'
        c.setNodes(nodes)
        print 'connected to cluster'
        man.start()
        print 'success.'
except KeyboardInterrupt:
  print 'shutting down...'
  man.close()
  
