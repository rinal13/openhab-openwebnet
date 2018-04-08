/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.openwebnet.handler;

import static org.openhab.binding.openwebnet.OpenWebNetBindingConstants.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.openwebnet.OpenWebNetBindingConstants;
import org.openwebnet.OpenGateway;
import org.openwebnet.message.BaseOpenMessage;
import org.openwebnet.message.Lighting;
import org.openwebnet.message.OpenMessageFactory;
import org.openwebnet.message.What;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenWebNetDeviceHandler} is responsible for handling commands for a OpenWebNet device.
 *
 * @author Massimo Valla - Initial contribution
 */
public class OpenWebNetDeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OpenWebNetDeviceHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = OpenWebNetBindingConstants.DEVICE_SUPPORTED_THING_TYPES;

    private double lastBrightnessChangeSentTS = 0; // timestamp when last brightness change was sent to the device
    private static final int BRIGHTNESS_CHANGE_DELAY = 1500; // ms delay to wasit before sending a bri status request

    private boolean brightnessLevelRequested = false; // was the brightness level requested ?
    private int latestBrightnessWhat = -1; // latest brightness WHAT value (-1 = unknown)
    private int latestBrightnessWhatBeforeOff = -1; // latest brightness WHAT value before device was set to off

    private OpenWebNetBridgeHandler bridgeHandler;
    private OpenGateway gateway;
    private String ownId; // OpenWebNet identifier for this device
    private Lighting.Type lightingType = Lighting.Type.ZIGBEE;

    public OpenWebNetDeviceHandler(Thing thing) {
        super(thing);
        logger.debug("==OWN:DeviceHandler== constructor");
    }

    @Override
    public void initialize() {
        logger.debug("==OWN:DeviceHandler== initialize() device={}", getThing().getUID());
        Bridge bridge = getBridge();
        if (bridge != null) {
            if (bridge.getHandler() != null) {
                bridgeHandler = (OpenWebNetBridgeHandler) bridge.getHandler();
                gateway = bridgeHandler.getGateway();
                if (bridgeHandler.isBusGateway()) {
                    lightingType = Lighting.Type.POINT_TO_POINT;
                }
                ownId = (String) getConfig().get(CONFIG_PROPERTY_WHERE);
                // FIXME deviceWhere : create a final deviceWhere to be set at initialization and used later
                bridgeHandler.registerDevice(ownId, getThing().getUID());
                logger.debug("==OWN:DeviceHandler== associated device to bridge with ownId={}", ownId);
                updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "waiting state update...");
                // TODO handleCommand(REFRESH) : is it called automatically ? otherwise do here a:
                // bridgeHandler.requestDeviceState(getThing().getUID());
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "No bridge associated, please assign a bridge in thing configuration.");
        }
    }

    /**
     * Handles a command arrived from OH2 to this device
     *
     */
    @Override
    public void handleCommand(ChannelUID channel, Command command) {
        logger.debug("==OWN:DeviceHandler== handleCommand() (command={} - channel={})", command, channel);
        if (gateway == null) {
            logger.error("==OWN:DeviceHandler== Device {} is not associated to any gateway, skipping command",
                    getThing().getUID());
            return;
        }
        if (!gateway.isConnected()) {
            logger.debug("==OWN:DeviceHandler== Gateway is NOT connected, setting device={} to OFFLINE",
                    thing.getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        if (command instanceof RefreshType) {
            logger.debug("==OWN:DeviceHandler== Refreshing channel {}", channel);
            requestChannelState(channel);
            return;
        }
        switch (channel.getId()) {
            case CHANNEL_BRIGHTNESS:
            case "dimmerLevel":
                handleBrightnessCommand(command);
                break;
            case CHANNEL_SWITCH:
            case CHANNEL_SWITCH_01:
            case CHANNEL_SWITCH_02:
                handleSwitchCommand(channel, command);
                break;
            default: {
                logger.error("==OWN:DeviceHandler== Unsupported channel UID {}", channel);
            }
        }
        // TODO
        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
    }

    /**
     * Handles Lighting switch command
     *
     * @param channel
     * @param command
     */
    private void handleSwitchCommand(ChannelUID channel, Command command) {
        logger.debug("==OWN:DeviceHandler== handleSwitchCommand() (command={} - channel={})", command, channel);
        if (command instanceof OnOffType) {
            if (OnOffType.ON.equals(command)) {
                gateway.send(Lighting.requestTurnOn(toWhere(channel), lightingType));
            } else if (OnOffType.OFF.equals(command)) {
                gateway.send(Lighting.requestTurnOff(toWhere(channel), lightingType));
            }
        } else {
            logger.error("==OWN:DeviceHandler== Unsupported command {}", command);
        }

    }

    /**
     * Helper method to dim light to a valid OWN value
     *
     * @param whatInt new WHAT (int value)
     * @param command original OH2 Command received
     */
    private void dimLightTo(int whatInt, Command command) {
        final String channel = CHANNEL_BRIGHTNESS;
        final String where = toWhere(BaseOpenMessage.UNIT_01);
        int newWhatInt = whatInt;
        logger.debug("$$$ START---dimLightTo latestBriWhat={} latestBriBeforeOff={} briLevelRequested={}",
                latestBrightnessWhat, latestBrightnessWhatBeforeOff, brightnessLevelRequested);
        What newWhat;
        if (OnOffType.ON.equals(command) && latestBrightnessWhat <= 0) {
            // ON after OFF/Unknown -> we reset channel to last value before OFF (if exists)
            if (latestBrightnessWhatBeforeOff > 0) {
                newWhatInt = latestBrightnessWhatBeforeOff;
                updateState(channel, new PercentType(newWhatInt * 10));
            } else {
                newWhatInt = 10;
            }
        }
        logger.debug("$ new={}, latest={}", newWhatInt, latestBrightnessWhat);
        if (newWhatInt != latestBrightnessWhat) {
            if (newWhatInt >= 0 && newWhatInt <= 10) {
                newWhat = Lighting.WHAT.fromValue(newWhatInt);
                if (newWhat.equals(Lighting.WHAT.ON)) {
                    // change it to WHAT.DIM_20 (dimming to 10% is not allowed in OWN)
                    newWhat = Lighting.WHAT.DIM_20;
                }
                lastBrightnessChangeSentTS = System.currentTimeMillis();
                gateway.send(Lighting.requestDimTo(where, newWhat, lightingType));
                logger.debug("################### {}", lastBrightnessChangeSentTS);
                if (!(command instanceof PercentType)) {
                    updateState(channel, new PercentType(newWhatInt * 10));
                }
                updateState("dimmerLevel", new DecimalType(newWhatInt));
                if (newWhatInt == 0) {
                    latestBrightnessWhatBeforeOff = latestBrightnessWhat;
                }
                latestBrightnessWhat = newWhatInt;
            } else {
                logger.debug("$ do nothing");
            }
        } else {
            logger.debug("$ do nothing");
        }
        logger.debug("$$$ END ---dimLightTo latestBriWhat={} latestBriBeforeOff={} briLevelRequested={}",
                latestBrightnessWhat, latestBrightnessWhatBeforeOff, brightnessLevelRequested);
    }

    /**
     * Handles Lighting brightness command (ON, OFF, xx%, INCREASE, DECREASE)
     *
     * @param command
     */
    private void handleBrightnessCommand(Command command) {
        logger.debug("==OWN:DeviceHandler== handleBrightnessCommand() (command={})", command);
        if (command instanceof PercentType) {
            int percent = ((PercentType) command).intValue();
            if (percent > 0 && percent < 10) {
                dimLightTo(1, command);
            } else {
                dimLightTo((int) Math.floor(percent / 10.0), command);
            }
        } else if (command instanceof IncreaseDecreaseType) {
            if (IncreaseDecreaseType.INCREASE.equals(command)) {
                dimLightTo(latestBrightnessWhat + 1, command);
            } else { // DECREASE
                dimLightTo(latestBrightnessWhat - 1, command);
            }
        } else if (command instanceof OnOffType) {
            if (OnOffType.ON.equals(command)) {
                dimLightTo(latestBrightnessWhat, command);
            } else { // OFF
                dimLightTo(0, command);
            }
        }
        // FIXME DEBUG MODE: this is the other channel (level)
        else if (command instanceof DecimalType) {
            dimLightTo(((DecimalType) command).intValue(), command);
        } else {
            logger.error("==OWN:DeviceHandler== Cannot handle command {} for thing {}", command, getThing().getUID());
            return;
        }
    }

    /**
     * Request to gateway state for device channel. Sets a schedule to put device OFFLINE if no answer is received after
     * DEVICE_STATE_REQ_TIMEOUT
     *
     * @param channel ChannleUID to be requested
     */
    public void requestChannelState(ChannelUID channel) {
        logger.debug("==OWN==  DeviceHandler.requestChannelState() thingUID={} channel={}", thing.getUID(),
                channel.getId());
        gateway.send(Lighting.requestStatus(toWhere(channel), lightingType));
        scheduler.schedule(() -> {
            // if state is still unknown after timer ends, set the device as OFFLINE
            if (thing.getStatus().equals(ThingStatus.UNKNOWN)) {
                logger.info(
                        "==OWN:DeviceHandler== Device state request timer expired, still unknown. Setting device={} to OFFLINE",
                        thing.getUID());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Could not get channel state");
                logger.debug("==OWN:DeviceHandler== Device OFFLINE");
            }
        }, DEVICE_STATE_REQ_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Updates light state based on a Lighting message received from the OWN network
     */
    protected void updateLightState(Lighting msg) {
        logger.debug("==OWN:DeviceHandler== updateLightState() for thing: {}", getThing().getUID());
        // update status to ONLINE if not already online
        if (ThingStatus.ONLINE != getThing().getStatus()) {
            updateStatus(ThingStatus.ONLINE);
        }
        ThingTypeUID thingType = getThing().getThingTypeUID();
        if (THING_TYPE_DIMMER.equals(thingType) || THING_TYPE_BUS_DIMMER.equals(thingType)) {
            updateLightBrightnessState(msg);
        } else {
            updateLightOnOffState(msg);
        }
    }

    /**
     * Updates brightness level based on a Lighting message received from the OWN network
     */
    private synchronized void updateLightBrightnessState(Lighting msg) {
        final String channel = CHANNEL_BRIGHTNESS;
        String where = toWhere(BaseOpenMessage.UNIT_01);
        logger.debug("==OWN:DeviceHandler== updateLightBrightnessState() msg={}", msg);
        logger.debug("$$$ START---updateLightBr latestBriWhat={} latestBriBeforeOff={} brightnessLevelRequested={}",
                latestBrightnessWhat, latestBrightnessWhatBeforeOff, brightnessLevelRequested);
        double now = System.currentTimeMillis();
        double delta = now - lastBrightnessChangeSentTS;
        logger.debug("$bri now={} delta={}", now, delta);
        if (msg.isOn() && !brightnessLevelRequested) {
            if (delta >= BRIGHTNESS_CHANGE_DELAY) {
                // we send a light brightness status request ONLY if last brightness change
                // was sent >BRIGHTNESS_CHANGE_DELAY ago
                logger.debug("$bri change sent >={}ms ago, sending requestStatus...", BRIGHTNESS_CHANGE_DELAY);
                brightnessLevelRequested = true;
                gateway.send(Lighting.requestStatus(where, lightingType));
            } else {
                logger.debug("$bri change sent {}<{}ms, NO requestStatus needed", delta, BRIGHTNESS_CHANGE_DELAY);
            }
        } else {
            logger.debug("$bri update from network -> level should be present in WHAT part of the message");
            int newLevel = msg.getWhat().value();
            logger.debug("$bri latest {} ----> new {}", latestBrightnessWhat, newLevel);
            if (latestBrightnessWhat != newLevel) {
                if (delta >= BRIGHTNESS_CHANGE_DELAY) {
                    logger.debug("$bri change sent >={}ms ago, updating state...", BRIGHTNESS_CHANGE_DELAY);
                    updateState(channel, new PercentType(newLevel * 10));
                } else if (msg.isOff()) {
                    logger.debug("$bri change just sent, but OFF from network received, updating state...");
                    updateState(channel, new PercentType(newLevel * 10));
                } else {
                    logger.debug("$bri change just sent, NO update needed.");
                }
                updateState("dimmerLevel", new DecimalType(newLevel));
                if (msg.isOff()) {
                    latestBrightnessWhatBeforeOff = latestBrightnessWhat;
                }
                latestBrightnessWhat = newLevel;
            } else {
                logger.debug("$bri no change");
            }
            brightnessLevelRequested = false;
        }
        logger.debug("$$$ END  ---updateLightBr latestBriWhat={} latestBriBeforeOff={} brightnessLevelRequested={}",
                latestBrightnessWhat, latestBrightnessWhatBeforeOff, brightnessLevelRequested);

    }

    /**
     * Updates on/off state based on a Lighting message received from the OWN network
     */
    private void updateLightOnOffState(Lighting msg) {
        String channelID;
        if (bridgeHandler.isBusGateway()) {
            channelID = CHANNEL_SWITCH;
        } else {
            if (BaseOpenMessage.UNIT_02.equals(OpenMessageFactory.getUnit(msg.getWhere()))) {
                channelID = CHANNEL_SWITCH_02;
            } else {
                channelID = CHANNEL_SWITCH_01;
            }
        }
        if (msg.isOn()) {
            updateState(channelID, OnOffType.ON);
        } else if (msg.isOff()) {
            updateState(channelID, OnOffType.OFF);
        } else {
            logger.error(
                    "==OWN:DeviceHandler== updateLightOnOffState() NOT SUPPORTED OWN LIGHTING COMMAND for thing {}, ignoring it. Frame={}",
                    getThing().getUID(), msg);
        }
    }

    @Override
    public void handleRemoval() {
        logger.debug("==OWN:DeviceHandler== handleRemoval() device={}", getThing().getUID());
        if (bridgeHandler != null) {
            bridgeHandler.unregisterDevice(ownId);
        }
        super.handleRemoval();
    }

    @Override
    public void dispose() {
        logger.debug("==OWN:DeviceHandler== dispose() device={}", getThing().getUID());
        super.dispose();
    }

    /**
     * Returns a WHERE address based on bridge type and unit (optional)
     *
     * @param unit device unit
     */
    private String toWhere(String unit) {
        if (bridgeHandler.isBusGateway()) {
            return ownId.replace('h', '#');
        } else {
            return ownId + unit;
        }
    }

    /**
     * Returns a WHERE address based on channel
     *
     * @param channel channel
     */
    private String toWhere(ChannelUID channel) {
        if (bridgeHandler.isBusGateway()) {
            return ownId.replace('h', '#');
        } else if (channel.getId().equals(CHANNEL_SWITCH_02)) {
            return ownId + BaseOpenMessage.UNIT_02;
        } else { // BRIGHTNESS or CHANNEL_SWITCH_01
            return ownId + BaseOpenMessage.UNIT_01;
        }
    }

}