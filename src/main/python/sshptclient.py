import ntpath
import os
import tempfile

class SshClient:
  def __init__(self, username, fAddresses, fResults):
    self.username = username
    self.sshArgs = [
      './sshpt.py',
      '-u', self.username,
      '-s',
      '-U', self.username,
      '-P', SSH_USER,
      '-f', fAddresses,
      '-o', fResults
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
    tmpFile = tempfile.mkstemp('.zip', 'sshpt-multiscp_tmp')
    tmp, tmpFilename = os.path.split(tmpFile)    
    zipArgs = [
      'zip',
      tmpFile[1]
    ]
    for f in files:
      zipArgs.append(f[0])
    if not subprocess.check_output(zipArgs):
      return False
    
    # transmit archive file
    self.doScp(tmpFile[1], '/tmp/')
    
    # remove archive file
    # WARNING: assumes that temporary directory equals on local and remote machine(s)
    rmArgs = [
      'rm',
      tmpFile[1]
    ]
    if subprocess.check_output(rmArgs):
      return False
    
    # unzip remote archive file
    tmpDir = tempfile.mkdtemp()
    unzipArgs = [
      'unzip -j ' + tmpFile[1] + ' -d ' + tmpDir,
      'rm ' + tmpFile[1]
    ]
    # move files to their destinations
    for f in files:
      parent, filename = ntpath.split(f[0])
      sourceFile = os.path.join(tmpDir, filename)
      unzipArgs.append('cp ' + sourceFile + ' ' + f[1])
    unzipArgs.append('rm -rf ' + tmpDir)
    if not self.doSsh(unzipArgs):
      return False
    return True

