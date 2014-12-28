#/bin/bash
DIR_CRR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
virtualenv /tmp/circus
/tmp/circus/bin/pip install circus
ln -s ${DIR_CRR}/src/main/python/CommandConfigure.py /tmp/circus/lib/python2.7/site-packages/circus/commands/configure.py
