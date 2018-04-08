/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.openwebnet;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.Sets;

/**
 * The {@link OpenWebNetBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Massimo Valla - Initial contribution
 */

public class OpenWebNetBindingConstants {

    private static final String BINDING_ID = "openwebnet";

    public static final int DEVICE_STATE_REQ_TIMEOUT = 5; // seconds

    // List of all Thing Type UIDs
    // bridges
    public static final ThingTypeUID THING_TYPE_DONGLE = new ThingTypeUID(BINDING_ID, "dongle");
    public static final String THING_LABEL_DONGLE = "OpenWebNet ZigBee USB Dongle";
    public static final ThingTypeUID THING_TYPE_BUS_GATEWAY = new ThingTypeUID(BINDING_ID, "bus_gateway");
    public static final String THING_LABEL_BUS_GATEWAY = "OpenWebNet BUS/SCS Gateway";
    // generic (unknown) device
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final String THING_LABEL_DEVICE = "OpenWebNet GENERIC Device";
    // other thing types
    // BUS
    public static final ThingTypeUID THING_TYPE_BUS_ON_OFF_SWITCH = new ThingTypeUID(BINDING_ID, "bus_on_off_switch");
    public static final String THING_LABEL_BUS_ON_OFF_SWITCH = "OpenWebNet BUS/SCS On/Off Switch";
    public static final ThingTypeUID THING_TYPE_BUS_DIMMER = new ThingTypeUID(BINDING_ID, "bus_dimmer");
    public static final String THING_LABEL_BUS_DIMMER = "OpenWebNet BUS/SCS Dimmer";
    // ZIGBEE
    public static final ThingTypeUID THING_TYPE_ON_OFF_SWITCH = new ThingTypeUID(BINDING_ID, "on_off_switch");
    public static final String THING_LABEL_ON_OFF_SWITCH = "OpenWebNet ZigBee On/Off Switch";
    public static final ThingTypeUID THING_TYPE_ON_OFF_SWITCH_2UNITS = new ThingTypeUID(BINDING_ID, "on_off_switch2u");
    public static final String THING_LABEL_ON_OFF_SWITCH_2UNITS = "OpenWebNet ZigBee 2-units On/Off Switch";
    public static final ThingTypeUID THING_TYPE_DIMMER = new ThingTypeUID(BINDING_ID, "dimmer");
    public static final String THING_LABEL_DIMMER = "OpenWebNet ZigBee Dimmer";
    public static final ThingTypeUID THING_TYPE_AUTOMATION = new ThingTypeUID(BINDING_ID, "automation");
    public static final String THING_LABEL_AUTOMATION = "OpenWebNet ZigBee Automation";

    // supported things
    public final static Set<ThingTypeUID> DEVICE_SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_ON_OFF_SWITCH,
            THING_TYPE_ON_OFF_SWITCH_2UNITS, THING_TYPE_DIMMER, THING_TYPE_DEVICE, THING_TYPE_BUS_ON_OFF_SWITCH,
            THING_TYPE_BUS_DIMMER);
    public final static Set<ThingTypeUID> BRIDGE_SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_DONGLE,
            THING_TYPE_BUS_GATEWAY);

    public final static Set<ThingTypeUID> ALL_SUPPORTED_THING_TYPES = Sets.union(DEVICE_SUPPORTED_THING_TYPES,
            BRIDGE_SUPPORTED_THING_TYPES);

    // List of all Channel ids
    public static final String CHANNEL_SWITCH = "switch";
    public static final String CHANNEL_SWITCH_01 = "switch_01";
    public static final String CHANNEL_SWITCH_02 = "switch_02";
    public static final String CHANNEL_BRIGHTNESS = "brightness";
    public static final String CHANNEL_SHUTTER = "shutter";

    // config properties
    public static final String CONFIG_PROPERTY_SERIAL_PORT = "serialPort";

    public static final String CONFIG_PROPERTY_WHERE = "where";
    public static final String CONFIG_PROPERTY_HOST = "host";
    public static final String CONFIG_PROPERTY_PORT = "port";
    public static final String CONFIG_PROPERTY_PASSWD = "passwd";

    public static final String CONFIG_PROPERTY_FIRMWARE = "firmwareVersion";

}
