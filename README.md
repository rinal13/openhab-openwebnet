
# OpenWebNet (BTicino/Legrand) Binding

This new binding integrates **BTicino / Legrand MyHOME(R) BUS & ZigBee Radio** devices using the **[OpenWebNet](https://en.wikipedia.org/wiki/OpenWebNet) protocol**.
It is the first known binding for openHAB 2 that **supports *both* wired BUS/SCS** as well as **wireless ZigBee setups**, all in the same biding. The two networks can be configured simultaneously.
It's also the first OpenWebNet binding with initial support for discovery of BUS/SCS devices.
Commands from openHAB and feedback (events) from BUS/SCS and ZigBee networks are supported.

## Prerequisites

In order for this biding to work, an OpenWebNet gateway is needed in your installation to talk to devices.
Currently these gateways are supported:
- **IP gateways**, such as the [MH200N](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?lang=EN&productId=016), [F453](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=027), [F454](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=006), etc.
  - As in the openHAB 1.x BTicino binding, the BUS/SCS gateway must be configured to accept connections from the openHAB computer IP
- **ZigBee USB gateways**, using USB/serial ports such as the [BTicino 3578](http://www.catalogo.bticino.it/BTI-3578-EN) and [Legrand 088328](https://www.legrand.fr/pro/catalogue/35115-interface-openradio/interface-open-webnet-et-radio-pour-pilotage-dune-installation-myhome-play)

***IMPORTANT NOTE***
As for the Serial binding, if you run OH2 on Linux to access the USB/serial port you might require that you add the `openhab` user to the `dialout` group to grant permission to read/write to the serial port:
```
sudo usermod -a -G dialout openhab
```
the user will need to logout from all login instances and log back in to see their new group added. If you add your user to this group and still cannot get permission, reboot Linux to ensure the new group permission is attached to the `openhab` user.

## Supported Things

The following Things and OpenWebNet `WHOs` are supported:
### BUS/SCS

Category | WHO | Thing Type IDs | Discovery? | Description | Status
---|:---:|:---:|:---:|---|---
Gateway | `13` | `bus_gateway`|*work in progress*|Any IP gateway supporting OpenWebNet protocol should work (e.g. MH200N/MH202/F453/F454)|Testers needed!!
Lighting | `1`| `bus_dimmer`, `bus_on_off_switch`|Yes|BUS dimmers and switches|Testers needed!!
Automation | `2`| *work in progress* |- |-|-

### ZigBee (Radio)

Category | WHO | Thing Type IDs | Discovery? | Description | Status
---|:---:|:---:|:---:|---|---
Gateway | `13` |`dongle`| Yes|ZigBee USB Dongle (BTicino/Legrand models: BTI-3578/088328)|Tested: BTI-3578
Lighting | `1`| `dimmer`, `on_off_switch`, `on_off_switch2u`|Yes|ZigBee dimmers, switches and 2-unit switches|Tested: BTI-4591, BTI-3584, BTI-4585
Automation | `2`| *work in progress* |- |-|-

***IMPORTANT NOTE***
BTicino/Legrand ZigBee Radio devices need to be attached to same network of the USB dongle before it is possible for the binding to discover them. Please refer to [this guide](http://www.bticino.com/products-catalogue/management-of-connected-lights-and-shutters/#installation) by BTicino to setup a ZigBee network which includes the USB dongle.

## Discovery

Discovery is supported using PaperUI by activating the discovery ("+") button form Inbox.

### BUS/SCS Discovery

- Gateway discovery using UPnP is *under development* and will be available only for those IP gateways supporting UPnP.
- For the moment the OpenWebNet IP gateway should be added manually (see BUS/SCS Gateway configuration below).
- Once the gateway is added manually as a Thing, a second discovery request from Inbox will discover its devices.
- BUS/SCS Dimmers must be ON and dimmed (20-100%) at time of discovery, otherwise they will be discovered as simple On/Off switches.

### ZigBee Discovery

- The USB dongle must be inserted in one of the USB ports of the openHAB computer before discovery is started and it must be part of the ZigBee network
- On Linux the `openhab` user must be member of the `dialout` group, to be able to use USB/serial port (see NOTE in the [Prerequisites](#prerequisites))
- Once the ZigBee gateway is discovered and added, devices will be discovered by starting a new discovery request from Inbox. Because of the ZigBee radio network, discovery of devices will take around 40-60 sec. Be patient!
- Only actuators connected to main power and registered on the ZigBee network and within radio coverage of the USB dongle will be discovered. Unreachable or not powered devices will be discovered as GENERIC devices and cannot be controlled. Control units cannot be discovered by the USB dongle and therefore are not supported


## Thing Configuration

### BUS/SCS Gateway

To configure the gateway: go to Inbox > "+" > OpenWebNet > click `ADD MANUALLY` and then select `OpenWebNet BUS Gateway` device, with this configuration:

- `host` : IP address / hostname of the BUS/SCS gateway (*mandatory*). Example: `192.168.1.35`
- `port` : port (optional, default: `20000`)
- `passwd` : gateway password (optional). Example: `1234`

#### Example

```
Bridge openwebnet:bus_gateway:myBridge1 [ host="192.168.1.35", passwd="5522" ]
```

***HELP NEEDED!!!***
Start a gateway discovery, and then send your (DEBUG-level) log file to the openHAB Community OpenWebNet thread to see if UPnP discovery is supported by your BTicino IP gateway.


### ZigBee USB Dongle

The ZigBee USB dongle is currently discovered automatically and put in Inbox. Manual configuration is not supported at the moment.

### Devices

For all OpenWebNet devices it must have configured:

- the associated gateway (`Bridge Selection`)
- the `where` config parameter (`OpenWebNet Device Address`):
  + example for BUS/SCS: Point to Point `A=2 PL=4` --> `where="24"`
  + example for BUS/SCS: Point to Point `A=6 PL=4` on local bus --> `where="64#4#01"`
  + example for ZigBee/Radio: use decimal format address without the UNIT part and network: ZigBee `WHERE=414122201#9` --> `where="4141222"`

#### Example

##### BUS/SCS:
`bus_dimmer        myDimmer   [ where="24" ]`
`bus_on_off_switch mySwitch   [ where="64#4#01" ]`

##### ZigBee:
`(TODO)`

## Channels

Devices support some of the following channels:

Channel Type ID   | Item Type       | Description
------------------|-----------------|----------------------------------------------------------
switch            | Switch          | This channel supports switching the device on and off
brightness        | Dimmer          | This channel supports adjusting the brightness value
shutter *(not yet supported)*| Rollershutter   | This channel supports activation of roller shutters (Up, Down, Stop)                                                                                      

## Full Example

### demo.things:

```
Bridge openwebnet:bus_gateway:myBridge1 [ host="192.168.1.35", passwd="5522" ] {
      bus_dimmer        mySimmer   [ where="24" ]
      bus_on_off_switch mySwitch   [ where="64#4#01" ]
}
``` 
```
<TODO----- ZigBee Dongle>
Bridge openwebnet:dongle:myDongle2  [serialPort="kkkkkkk"] {
      dimmer          myZigbeeDimmer [ where="xxxxx"]
      on_off_switch   myZigbeeSwitch [ where="yyyyy"]
}
```

### demo.items:

```
Switch BUS_Light1  { channel="openwebnet:bus_on_off_switch:myBridge1:mySwitch:switch" }
Dimmer BUS_Dimmer  { channel="openwebnet:bus_dimmer:myBridge1:myDimmer:brightness" }
```


### demo.sitemap

```
<not available yet>
```

## Disclaimer

- This binding is not associated by any means with BTicino or Legrand companies
- The OpenWebNet protocol is maintained and Copyright by BTicino/Legrand. The documentation of the protocol if freely accessible for developers on the [MyOpen Community website - https://www.myopen-legrandgroup.com/developers](https://www.myopen-legrandgroup.com/developers/)
- OpenWebNet and MyHOME are registered trademarks by BTicino/Legrand
- This binding uses `openwebnet-lib 0.9.x` an OpenWebNet Java lib partly based on [openwebnet/rx-openwebnet](https://github.com/openwebnet/rx-openwebnet) client library by niqdev, to support:
  - gateways and OWN frames for ZigBee
  - frame parsing
  - monitoring events from BUS
 The lib also uses few classes from the openHAB 1.x BTicino binding for socket handling and priority queues.
