#!/bin/bash
touch /tmp/circus-testfile
circusd ~/circus/circus.ini &>~/circus/console.log &

