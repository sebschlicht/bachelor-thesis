# Software
OS: `Debian Wheezy XFCE 7.7.0 amd64`

[converted](http://docs.openstack.org/image-guide/content/ch_converting.html) an image from VirtualBox to raw format via

    $ qemu-img convert -f vdi -O raw UnikloudNode.vdi UnikloudNode.img

* `sudo`
* SSH server
* [Neo4j + Titan](cluster-setup.md)
* (cluster control)
 * [circus](http://circus.readthedocs.org/en/0.11.1/) or
   * control your cluster by sending [commands](http://circus.readthedocs.org/en/0.11.1/for-ops/commands/) using ZMQ: `start`, `stop`, `stats`
   * allows multiple watchers (programs) and ordered startup
     * Neo4j
     * Titan
     * Cassandra, ElasticSearch, ...
   * allows to write own startup [hooks](http://circus.readthedocs.org/en/0.11.1/for-devs/writing-hooks/)
 * [pssh](http://www.theether.org/pssh/) or
 * [cssh](https://github.com/duncs/clusterssh)

### OS configuration

    $ su -
    $ nano /etc/apt/sources.list
    $ apt-get update

**/etc/apt/sources.list** (full):

    deb http://http.debian.net/debian wheezy main
    deb-src http://http.debian.net/debian wheezy main
    
    deb http://http.debian.net/debian wheezy-updates main
    deb-src http://http.debian.net/debian wheezy-updates main
    
    deb http://security.debian.org/ wheezy/updates main
    deb-src http://security.debian.org/ wheezy/updates main

## sudo

    $ apt-get install sudo
    $ adduser <user> sudo
    $ exit

and do a relog for <user>

## SSH server
A SSH server is installed in order to login to a cluster node remotely when problems occurr. If `circus` is not used multi-SSH tools like `pssh` could be an option to control the cluster, too.

    $ sudo apt-get install shh-server

### [Configuration](https://help.ubuntu.com/community/SSH/OpenSSH/Configuring)

    cd /etc/ssh
    sudo cp sshd_config sshd_config.defaults
    sudo chmod a-w sshd_config.defaults
    sudo nano sshd_config
    cd
    mkdir .ssh
    nano authorized_keys
    sudo service ssh restart
    
**/etc/ssh/sshd_config** (changes):

    PasswordAuthentification no

**`/home/<user>/authorized_keys`**: public SSH key(s)

# Networking

## Change MAC address permanently
Edit

    $ sudo nano /etc/network/interfaces

and append or edit the eth0 related section according to

    # ethernet interface for VM cloud
    allow-hotplug eth0
    iface eth0 inet dhcp
      hwaddress ether <MAC address>

After a reboot the MAC address should have changed.

    $ sudo reboot
