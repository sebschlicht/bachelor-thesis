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
