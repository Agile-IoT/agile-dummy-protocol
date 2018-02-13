/*******************************************************************************
 * Copyright (C) 2017 Create-Net / FBK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Create-Net / FBK - initial API and implementation
 ******************************************************************************/
package org.eclipse.agail.protocol.dummy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.agail.Protocol;
import org.eclipse.agail.ProtocolManager;
import org.eclipse.agail.object.AbstractAgileObject;
import org.eclipse.agail.object.DeviceOverview;
import org.eclipse.agail.object.DeviceStatusType;
import org.eclipse.agail.object.StatusType;

public class DummyProtocol extends AbstractAgileObject implements Protocol {

  private final Logger logger = LoggerFactory.getLogger(DummyProtocol.class);

  private static final String AGILE_DUMMY_PROTOCOL_BUS_NAME = "org.eclipse.agail.protocol.Dummy";

  private static final String AGILE_DUMMY_PROTOCOL_BUS_PATH = "/org/eclipse/agail/protocol/Dummy";

  /**
   * DBus bus path for found new device signal
   */
  private static final String AGILE_NEW_DEVICE_SIGNAL_PATH = "/org/eclipse/agail/NewDevice";

  /**
   * DBus bus path for for new record/data reading
   */
  private static final String AGILE_NEW_RECORD_SIGNAL_PATH = "/org/eclipse/agail/NewRecord";

  /**
   * Protocol name
   */
  private static final String PROTOCOL_NAME = "Dummy Protocol";

  private static final String RUNNING = "RUNNING";

  private static final String DUMMY = "DUMMY";
   // Device status
  public static final String CONNECTED = "CONNECTED";

  public static final String AVAILABLE = "AVAILABLE";

  /**
   * Device list
   */
  protected List<DeviceOverview> deviceList = new ArrayList<DeviceOverview>();

  protected byte[] lastRecord;

  private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  private ScheduledFuture discoveryFuture;
  
  private ScheduledFuture subscriptionFuture;

  // protected final State state = new State();

  public DummyProtocol() {
    try {
      dbusConnect(AGILE_DUMMY_PROTOCOL_BUS_NAME, AGILE_DUMMY_PROTOCOL_BUS_PATH, this);
    } catch (DBusException e) {
      e.printStackTrace();
    }
    logger.debug("Dummy protocol started!");
  }

  public static void main(String[] args) {
    new DummyProtocol();
  }

  public boolean isRemote() {
    return false;
  }

  public String Status() {
    return RUNNING;
  }

  public String Driver() {
    return DUMMY;
  }

  public String Name() {
    return PROTOCOL_NAME;
  }

  public byte[] Data() {
    return lastRecord;
  }

  public List<DeviceOverview> Devices() {
    return deviceList;
  }

  public void Connect(String deviceAddress) throws DBusException {
    logger.debug("Device connected {}",deviceAddress);
  }

  public void Disconnect(String deviceAddress) throws DBusException {
    logger.debug("Device disconnected {}",deviceAddress);
  }

  public String DiscoveryStatus() throws DBusException {
    if (discoveryFuture != null) {
      if (discoveryFuture.isCancelled()) {
        return "NONE";
      } else {
        return RUNNING;
      }
    }
    return "NONE";
  }

  public void StartDiscovery() throws DBusException {
    if (discoveryFuture != null) {
      logger.info("Discovery already running");
      return;
    }

    logger.info("Started discovery of Dummy devices");
    Runnable task = () -> {
      logger.debug("Checking for new devices");
      if (deviceList.isEmpty()) {
        DeviceOverview deviceOverview = new DeviceOverview("00:11:22:33:44:55", AGILE_DUMMY_PROTOCOL_BUS_NAME,
            "Dummy", AVAILABLE);
        deviceList.add(deviceOverview);
        logger.debug("Found new device {}",deviceOverview.id);
        try {
          ProtocolManager.FoundNewDeviceSignal foundNewDevSig = new ProtocolManager.FoundNewDeviceSignal(
              AGILE_NEW_DEVICE_SIGNAL_PATH, deviceOverview);
          connection.sendSignal(foundNewDevSig);
        } catch (DBusException e) {
          e.printStackTrace();
        }
      }
    };
    discoveryFuture = executor.scheduleWithFixedDelay(task, 0, 5, TimeUnit.SECONDS);
  }

  public void StopDiscovery() {
    if (discoveryFuture != null) {
      discoveryFuture.cancel(true);
      discoveryFuture = null;
    }
  }

  public void Write(String deviceAddress, Map<String, String> profile, byte[] payload) throws DBusException {
  }

  public byte[] Read(String deviceAddress, Map<String, String> profile) throws DBusException {
    lastRecord = generateRandomData();
    return lastRecord;
    }

  public byte[] NotificationRead(String deviceAddress, Map<String, String> profile) throws DBusException {
    return null;
  }

  public void Subscribe(String deviceAddress, Map<String, String> profile) throws DBusException {
    if(subscriptionFuture == null){
      Runnable task = () ->{
        lastRecord = generateRandomData();
        try {
          Protocol.NewRecordSignal newRecordSignal = new Protocol.NewRecordSignal(AGILE_NEW_RECORD_SIGNAL_PATH,
              lastRecord, deviceAddress, profile);
          logger.debug("Notifying {}", this);
          connection.sendSignal(newRecordSignal);
        } catch (Exception e) {
           e.printStackTrace();
        }
       };
       subscriptionFuture = executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }
  }

  public void Unsubscribe(String deviceAddress, Map<String, String> profile) throws DBusException {
    if(subscriptionFuture != null){
      subscriptionFuture.cancel(true);
      subscriptionFuture = null;
    }
  }

  public StatusType DeviceStatus(String deviceAddress) {
      return new StatusType(DeviceStatusType.CONNECTED.toString());
  }

  /**
   * generates random byte array of size 4
   * @return
   */
  private byte[] generateRandomData(){
    byte[] dummyData = new byte[1];
     new Random().nextBytes(dummyData);
     return dummyData;
  }
}
