#!/bin/bash
touch /tmp/circus-testfile
circusd /home/node/circus/circus.ini &>/home/node/circus/console.log &

