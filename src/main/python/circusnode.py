import zmq

class CircusNode:
  def __init__(self, identifier, address, port, timeoutPoll):
    self.identifier = identifier
    self.address = address
    self.port = int(port)
    self.timeoutPoll = timeoutPoll
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
    socks = dict(self.poller.poll(self.timeoutPoll))
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

