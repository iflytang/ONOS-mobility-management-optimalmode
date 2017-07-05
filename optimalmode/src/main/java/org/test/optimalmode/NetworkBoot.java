package org.test.optimalmode;

//import com.sun.org.apache.regexp.internal.RE;
//import com.sun.xml.internal.ws.api.message.Packet;
//import javafx.beans.binding.IntegerBinding;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
//import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.floodlightpof.protocol.OFMatch20;
import org.onosproject.floodlightpof.protocol.table.OFFlowTable;
import org.onosproject.floodlightpof.protocol.table.OFTableType;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostEvent;
import  org.onosproject.floodlightpof.protocol.OFProtocol;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.packet.*;
import org.onosproject.net.table.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.dhcp.AssociationEvent;
import org.onosproject.dhcp.DhcpService;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by tsf on 4/10/17.
 */

@Component(immediate = true)
public class NetworkBoot {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected ListenerRegistry listenerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableStore flowTableStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableService flowTableService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService associationService;   // eventdispatcher

    protected ApplicationId appId;
    private static int globalTableId;
    private byte smallTableId;
//    protected EventDeliveryService associationService = dhcpService.getEventDispatcher();


    //    protected final internalNetworkListener listener = new internalNetworkListener();
    protected ReactivePacketInProcessor processor = new ReactivePacketInProcessor();
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.test.mobility");
//        deviceService.addListener(listener);
        log.info("handle portStatus.");
        handlePortStatus();
        try{
            Thread.currentThread().sleep(1);
        }catch (Exception e) {
            log.info("Sleep wrong in NetworkBoot");
        }

        log.info("handle connectionUp");
        handleConnectionUp();

        packetService.addProcessor(processor, PacketProcessor.director(2));  // processor to process packet
        log.info("NetworkBoot Started.");
        log.info("appId: {}", appId.toString());

//        AssociationEvent Dhcpevent = new AssociationEvent(AssociationEvent.Type.DHCPLease, "dhcpLease",
//                "11:22:33:44:55:66", "pof:0000000000000002", 1, "192.168.109.120");
//        associationService.post(Dhcpevent);
//        log.info("post DHCPLease --> event type: {}", Dhcpevent.type());
//        log.info("hwaddr:{}, deviceId:{}, port:{}, coa:{}", Dhcpevent.getHwaddr(), Dhcpevent.getDeviceId(), Dhcpevent.getPort(), Dhcpevent.getCoa());
    }

    @Deactivate
    public void deactivate(){
//        deviceService.removeListener(listener);
        packetService.removeProcessor(processor);
        log.info("remove flow tables in NetworkBoot.");
        handleConnectionDown();
        log.info("NetworkBoot Stop.");
    }


    public static int globalTableId() {
        return globalTableId;
    }



    protected void addProtocol(String protocol, List<OFMatch20> fieldList) {
        OFProtocol ofProtocol = new OFProtocol();
        ofProtocol.setProtocolName(protocol);
        ofProtocol.setFieldList(fieldList);
    }

    // ================ test ok =============
    public String calculateIP(String deviceId, int port) {  // calculate gw_IP
        //deviceId like : "pof:0000000000000001" -> 8B, we emit zero
//        String deviceId = deviceIds.toString();
        String device_str = deviceId.substring(deviceId.length()-2 ,deviceId.length());  // the last two

//        int port = (int) ports.number().toLong();
        String port_str = Integer.toHexString(port).substring(0); //port ranges from 0 to a

        if(device_str.length() < 2)
            device_str = "0" + device_str;

        if(port_str.length() < 2)
            port_str = "0" + port_str;

        String gw_ip = "0a00" + device_str + port_str;

        return gw_ip;
    }

    // ===== test ok ====
    public void handleConnectionUp() { // when connected up, download a flow table
        List<DeviceId> deviceIdList = new ArrayList<DeviceId>();
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000001"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000002"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000003"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000004"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000005"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000006"));


        for (DeviceId deviceId : deviceIdList) {
            short DIP = 1; //field id
            globalTableId = flowTableStore.getNewGlobalFlowTableId(deviceId, OFTableType.OF_MM_TABLE);
//            log.info("===============[global TableId: {}] =======================",globalTableId);
//            globalTableId =  0;
            smallTableId = flowTableStore.parseToSmallTableId(deviceId, globalTableId);
            log.info("NetworkBoot ===> globalTableId:{}, smallTableId:{}",globalTableId,smallTableId);

            //construct OFMatch20 object
            OFMatch20 ofMatch20 = new OFMatch20();
            ofMatch20.setFieldName("DIP");
            ofMatch20.setFieldId(DIP);
            ofMatch20.setOffset((short) 240);
            ofMatch20.setLength((short) 32);

            ArrayList<OFMatch20> ofMatch20ArrayList = new ArrayList<OFMatch20>();
            ofMatch20ArrayList.add(ofMatch20);

            //construct OFMatch FlowTable
            OFFlowTable ofFlowTable = new OFFlowTable();
            ofFlowTable.setTableName("FirstEntryTable");
//            ofFlowTable.setTableId(smallTableId);
            ofFlowTable.setTableId((byte) globalTableId);
            ofFlowTable.setTableSize(32);
            ofFlowTable.setMatchFieldList(ofMatch20ArrayList);
            ofFlowTable.setTableType(OFTableType.OF_MM_TABLE);

            //build flow table
            FlowTable.Builder flowTable = DefaultFlowTable.builder()
                    .withFlowTable(ofFlowTable)
                    .forTable(globalTableId)
                    .forDevice(deviceId)
                    .fromApp(appId);
            flowTableService.applyFlowTables(flowTable.build());   // flowTable ok.
        }

//        log.info("download flowTables successfully.");
    }

    // ===== test ok =====
    public void handleConnectionDown() {
        List<DeviceId> deviceIdList = new ArrayList<DeviceId>();
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000001"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000002"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000003"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000004"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000005"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000006"));

        for(DeviceId deviceId:deviceIdList) {
//            if(deviceId.toString().equals("pof:0000000000000004")){
//                globalTableId = 121;
//            }
            flowTableService.removeFlowTablesByTableId(deviceId, FlowTableId.valueOf(globalTableId));
//            log.info("remove {} flow tables successfully.",deviceId);
        }
//        log.info("remove flow tables successfully.");

    }

    // ======= test ok =====
    public void handlePortStatus() {
        List<DeviceId> deviceIdList = new ArrayList<DeviceId>();
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000001"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000002"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000003"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000004"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000005"));
        deviceIdList.add(DeviceId.deviceId("pof:0000000000000006"));

        for(DeviceId deviceId:deviceIdList) {
            if(deviceId.equals(deviceIdList.get(0)) || deviceId.equals(deviceIdList.get(1)) || deviceId.equals(deviceIdList.get(5))) {
                deviceService.changePortState(deviceId, PortNumber.portNumber(1), true);
                deviceService.changePortState(deviceId, PortNumber.portNumber(2), true);
            }
            else {
                deviceService.changePortState(deviceId, PortNumber.portNumber(1), true);
                deviceService.changePortState(deviceId, PortNumber.portNumber(2), true);
                deviceService.changePortState(deviceId, PortNumber.portNumber(3), true);
            }
        }
//        log.info("============ open port ==============");
    }
//    }

    /**
     * implement to process PacketIn message
     */
    boolean flag_association = true;
    boolean flag_deassociation = false;
    protected class ReactivePacketInProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {  // parse packetIn message and process
            if(context.isHandled())
                return;

            InboundPacket pkt = context.inPacket();
            DeviceId deviceId = pkt.receivedFrom().deviceId();
            int port = 1; // AP1 and AP2 receive packetOut from port1
//            log.info("process packet from deviceId:<{}>, port:<{}>",deviceId.toString(), port);
            Ethernet packet = pkt.parsed();

            // LLC packet
            if (packet.getEtherType() == 0x0006) {
//                log.info("*******************************************");
                short a = pkt.unparsed().getShort(12);  // return 2 Bytes : 0x0006
                short b = pkt.unparsed().getShort(14);  // 0x0001
                short c = pkt.unparsed().getShort(16); // af81
//                log.info("a = {}, b = {}, c = {}", Integer.toHexString(a), Integer.toHexString(b), Integer.toHexString(Short.toUnsignedInt(c)));

                //TODO what values are nums.
                if (a == (short) 0x0006 && b == (short) 0x0001 && c == (short) 0xaf81) {
                    String hwaddr = pkt.parsed().getSourceMAC().toString();
//                    log.info("[LLC: hostMAC: {} ]", hwaddr);  // MAC consists of :
                    String[] src_mac = hwaddr.split(":");
                    String src_ip = "10." + Integer.toString(Integer.parseInt(src_mac[3],16)) + "." +
                            Integer.toString(Integer.parseInt(src_mac[4], 16)) + "." + Integer.toString(Integer.parseInt(src_mac[5], 16));
//                    log.info("[LLC:deviceId: <{}> port: <{}> hostHoA: <{}>", deviceId, port, src_ip);
//                    if(flag_association) {
                    AssociationEvent event = new AssociationEvent(AssociationEvent.Type.UE_ASSOCIATION, "Ue_Association",
                            hwaddr, deviceId.toString(), port, src_ip);
                    associationService.post(event);

                    log.info("*******************************************");
                    log.info("post UE Association in LLC.");
                    log.info("UEAssociationEvent in LLC: deviceId:{}",deviceId);
                    log.info("UEAssociationEvent in LLC: hwaddr:{}",hwaddr);
                    log.info("UEAssociationEvent in LLC: port:{}",port);
                    log.info("UEAssociationEvent in LLC: hoa:{}",src_ip);

//                    Date date = new Date();
//                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
//                    System.out.printf("NetworkBoot: UE [hwaddr: %s] [HOA: %s] connected to switch <%s> port <%d> at time <%s>.\n",
//                            hwaddr, src_ip, deviceId, port, dateFormat.format(date));
//                    flag_association = false;
//                    flag_deassociation = true;
//                    }
                }
            }

              /*  IPacket llcPacket = packet.getPayload();
            if(packet.getEtherType() == 0x0006)
            {
                log.info("*******************************************");
                log.info("PACKET_IN: object type: {}", llcPacket.getClass());
                log.info("PACKET_IN: llc ethType: {}", packet.getEtherType());
                log.info("PACKET_IN: llc payload: {}", llcPacket);
                byte[] llcData = {(byte) 0x0,(byte)0x1,(byte)0xaf,(byte)0x81,(byte)0x1,(byte)0x0};
                Data llcPayload = new Data(llcData);
                log.info("self-defined llcPayload: {}", llcPayload);
                log.info("llcPacket.equals(llcPayload)?:{}",llcPacket.equals(llcPayload));
                log.info("*******************************************");
            }*/


              if(packet.getEtherType() != (short) 0x0800 ) {
                  if(packet.getEtherType() == (short) 0x86dd)
                      return;
                  log.info("******************************");
                  short ethType = pkt.parsed().getEtherType();
                  short a1 = pkt.unparsed().getShort(12);  // return 2 Bytes : 0x0006
                  short b1 = pkt.unparsed().getShort(14);  // 0x0001
                  short c1 = pkt.unparsed().getShort(16); // af81
                  String src_host_mac = packet.getSourceMAC().toString();
                  String dst_host_mac = packet.getDestinationMAC().toString();
                  log.info("NetwokBoot: ethType: {}", Integer.toHexString(Short.toUnsignedInt(ethType)));
                  log.info("NetwokBoot: a1: {}", Integer.toHexString(Short.toUnsignedInt(a1)));
                  log.info("NetwokBoot: b1: {}", Integer.toHexString(Short.toUnsignedInt(b1)));
                  log.info("NetwokBoot: c1: {}", Integer.toHexString(Short.toUnsignedInt(c1)));
                  log.info("NetwokBoot: src_host_mac: {}", src_host_mac);
                  log.info("NetwokBoot: dst_host_mac: {}", dst_host_mac);
              }



            if (packet.getEtherType() == (short) 0x886c) {
                short a = pkt.unparsed().getShort(14);
                short b = pkt.unparsed().getShort(16);
//                log.info("[ethType:a:{}, b:{}", Integer.toHexString(Short.toUnsignedInt(a)), Integer.toHexString(Short.toUnsignedInt(b)));

                if(a == (short) 0x8001 && b == (short) 0x006a) {
                    String hwaddr_to_remove = "3C:46:D8:42:33:0b";
//                    String hwaddr_to_remove = "60:57:18:a2:17:14";
//                    String hwaddr_to_remove = "1C:CD:E5:11:A4:11";
                    String[] src_mac = hwaddr_to_remove.split(":");

//                    String[] src_mac = packet.getSourceMAC().toString().split(":"); // six String array
                    String hwaddr = src_mac[0] + src_mac[1] + src_mac[2] + src_mac[3] + src_mac[4] + src_mac[5];
                    String src_ip = "10." + Integer.toString(Integer.parseInt(src_mac[3],16)) + "." +
                            Integer.toString(Integer.parseInt(src_mac[4], 16)) + "." + Integer.toString(Integer.parseInt(src_mac[5], 16));
//                    log.info("[Leave 886c:deviceId: <{}> port: <{}> hostHoA: <{}>", deviceId, port, src_ip);
//                    if(flag_deassociation) {
                        AssociationEvent event = new AssociationEvent(AssociationEvent.Type.UE_DEASSOCIATION, "Ue_Deassociation",
                                hwaddr_to_remove, deviceId.toString(), port, src_ip);
                        associationService.post(event);
                        log.info("*******************************************");
                        log.info("post UE Deassociation in 0x886c.");
                        log.info("UeDeassociationEvent in 0x886c:deviceId:{}", deviceId);
                        log.info("UeDeassociationEvent in 0x886c:src_hwaddr:{}", hwaddr_to_remove);
//                    log.info("UeDeassociationEvent in 0x886c:dst_hwaddr:{}",packet.getDestinationMAC());
                        log.info("UeDeassociationEvent in 0x886c:port:{}", port);
                        log.info("UeDeassociationEvent in 0x886c:hoa:{}", src_ip);
//                        flag_deassociation = false;
//                        flag_association = true;
//                    }

                }
            }

            short ipv4_type = Ethernet.TYPE_IPV4;
            short arp_type = Ethernet.TYPE_ARP;
            if(packet.getEtherType() == ipv4_type && packet.getEtherType() != arp_type) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();
                if(ipv4Packet.getProtocol() != IPv4.PROTOCOL_UDP) {
                    if(ipv4Packet.getDestinationAddress() == 0xffFFffFF ||
                       ipv4Packet.getDestinationAddress() == 0xE00000FB ||
                       ipv4Packet.getDestinationAddress() == 0x08080808 ||
                       packet.getDestinationMAC().toString().equals("4f:4f:4f:4f:4f:4f") ||
                       packet.getDestinationMAC().toString().equals("3f:3f:3f:3f:3f:3f")) {
                        // do nothing
                    } else {
                        //TODO how to define Calculate_path, then get its attributes for event
                        AssociationEvent event = new AssociationEvent(AssociationEvent.Type.CALCULATE_PATH, "Calculate_Path", context);
                        associationService.post(event);
//                        log.info("Calculate Path.");
                    }
                }
            }
        }

    }
}



