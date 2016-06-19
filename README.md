# college-vertx-cluster

Vertx Clustering Demo

## Installation Log

The plan:

```

 |- Host (Xubuntu 16.04) -------------------------------|
 |                                                      |
 |                 10.0.2.2 (virtualbox gateway)        |
 |                     o                                |
 |                     |                                |
 |                     |                                |
 |                     o nginx lb :80                   |
 |                10.0.2.15 (vmbr0)                     |
 |                     o                                |
 |  |- Virtual Host (Proxmox 4.2 inside VirtualBox) -|  |
 |  |                 o                              |  |
 |  |    ____________/                               |  |
 |  |   |                                            |  |
 |  |   |----o cluster.jar :9000                     |  |
 |  |   |      Container vertx-0 (Debian 8)          |  |
 |  |   |      10.0.2.100 (veth100i0)                |  |
 |  |   |                                            |  |
 |  |   |----o cluster.jar :9000                     |  |
 |  |   |      Container vertx-1 (Debian 8)          |  |
 |  |   |      10.0.2.101 (veth101i0)                |  |
 |  |   |                                            |  |
 |  |   |----o cluster.jar :9000                     |  |
 |  |   |      Container vertx-2 (Debian 8)          |  |
 |  |   |      10.0.2.102 (veth102i0)                |  |
 |  |   |                                            |  |
 |  |   |----o cluster.jar :9000                     |  |
 |  |          Container vertx-3 (Debian 8)          |  |
 |  |          10.0.2.103 (veth103i0)                |  |
 |  |                                                |  |
 |  |------------------------------------------------|  |
 |------------------------------------------------------|


```

```bash

#
#  install virtualbox
#

wget -q https://www.virtualbox.org/download/oracle_vbox_2016.asc -O- | sudo apt-key add -
sudo apt-get update
sudo apt-get install virtualbox

#
#  install proxmox
#
#     http://www.proxmox.com/en/downloads
#     install a new 'debian' machine in the vbox gui
#     I named it VertxCluster
#
#  find out what IP was assigned (mine was 10.0.2.15) and then
#  enable port forwarding (for the default NAT behaviour)
#

vboxmanage modifyvm VertxCluster --natpf1 "ssh,tcp,,2222,10.0.2.15,22"
vboxmanage modifyvm VertxCluster --natpf1 "proxmox,tcp,,8006,10.0.2.15,8006"
vboxmanage modifyvm VertxCluster --natpf1 "http,tcp,,8080,10.0.2.15,80"

# start the vm
vboxmanage startvm VertxCluster --type headless

ssh -p 2222 root@localhost

```

Inside the VM:

```bash

dpkg-reconfigure locales

```

Then simply create containers etc. with the proxmox gui or `pct`.


### Vertx

Add `compile group: 'io.vertx', name: 'vertx-hazelcast', version: vertxVersion` to `build.gradle`.

