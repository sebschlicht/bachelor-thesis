import csv
import ntpath
import os
import StringIO
import subprocess
import tempfile

class SshClient:
  def __init__(self, username, fAddresses, fResults):
    self.username = username
    self.sshArgs = [
      './sshpt.py',
      '-u', self.username,
      '-s',
      '-U', self.username,
      '-P', self.username,
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
  
  # assumes that temporary directory equals on local and remote machine(s)
  def doScpMulti(self, files):
    # create archive file
    tmpDir = tempfile.mktemp('', 'sshpt-multiscp_tmp')
    tmpFile = tmpDir + '.zip'
    tmp, tmpFilename = os.path.split(tmpFile)
    zipArgs = [
      'zip',
      tmpFile
    ]
    for f in files:
      zipArgs.append(f[0])
    if not subprocess.check_output(zipArgs):
      return False
    
    # transmit archive file
    self.doScp(tmpFile, '/tmp/')
    
    # remove archive file
    rmArgs = [
      'rm',
      tmpFile
    ]
    if subprocess.check_output(rmArgs):
      return False
    
    # unzip remote archive file
    unzipArgs = [
      'unzip -j ' + tmpFile + ' -d ' + tmpDir,
      'rm ' + tmpFile
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

