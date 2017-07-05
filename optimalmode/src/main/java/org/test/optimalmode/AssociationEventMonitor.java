package org.test.optimalmode;

//import com.sun.deploy.association.AssociationService;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.AssociationEventListener;
import org.onosproject.dhcp.AssociationEvent;
import org.onosproject.floodlightpof.protocol.action.OFAction;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.DefaultPofActions;
import org.onosproject.net.flow.instructions.DefaultPofInstructions;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.table.FlowTableId;
import org.onosproject.net.table.FlowTableService;
import org.onosproject.net.table.FlowTableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by tsf on 4/14/17.
 *
 * @Description listen to AssociationEvent, and process them in function event()
 */

@Component(immediate = true)
public class AssociationEventMonitor {
    private static Logger log = LoggerFactory.getLogger(AssociationEventMonitor.class);

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected AssociationEventService service;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableService flowTableService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableStore flowTableStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected  UeRuleService ueRule;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    private final InternalAssociationEventListener listener = new InternalAssociationEventListener();
//    protected FlowTableStore flowTableStore = ueRule.getFlowTableStore();
//    static  FlowRule.Builder flowRule;

    protected  Dijkstra dijkstra = new Dijkstra();

    @Activate
    protected void activate(){
//        service.addListener(listener);
        dhcpService.addListener(listener);
        log.info("AssociationEventMonitor Started.");
    }

    @Deactivate
    protected void deactivate(){
//        service.removeListener(listener);
        dhcpService.removeListener(listener);
        log.info("AssociationEventMonitor Stopped.");
    }



    protected class InternalAssociationEventListener implements AssociationEventListener {
        int globalTableId = NetworkBoot.globalTableId();  // keep globalTableId consistent
        // Ue's attribute
        protected String[] switches = new String[6];
        protected Map<String, UE> ues = new HashMap<String, UE>();
        protected Map<String, UE> hosts = new HashMap<String, UE>();
        protected int last_ue_id = 0;
        protected Map<String, String> path_src_ips = new HashMap<String, String>();

        // for real-time
//        protected Date date = new Date();
//        protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS");


        // construct function
        public InternalAssociationEventListener() {
            // ============= set attachment for cn, switch is pof:6 ========="=====
            ues.put("00:1b:cd:03:0b:f0", new UE(1, "00:1b:cd:03:0b:f0", "10.3.11.240"));
            ues.get("00:1b:cd:03:0b:f0").setAttachment(new UeAssociation("pof:0000000000000006", 2));
            hosts.put("00:1b:cd:03:0b:f0", new UE(1, "00:1b:cd:03:0b:f0","10.3.11.240"));
            hosts.get("00:1b:cd:03:0b:f0").setAttachment(new UeAssociation("pof:0000000000000006", 2));
            last_ue_id = 1;
//            path_src_ips = new HashMap<String, String>();  // {src_ip : dst_coa}
        }

        public void handleUeAssociation(AssociationEvent event) {
            String hwaddr = event.getHwaddr();
            String device = event.getDeviceId();
            int port = event.getPort();
            String hoa = event.getCoa();

            // ============= set attachment for mn, switch is pof:1/2 ==========
            if(ues.containsKey(hwaddr)) {
//                log.info("============== [ set Attachment for Ue ] ========");
//                Date date = new Date();
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
                ues.get(hwaddr).setAttachment(new UeAssociation(device, port));
                System.out.printf("<UE[id:%d] [hwaddr: %s] [HOA: %s] connected to swithch<%s> <port:%d>.\n",
                        last_ue_id, hwaddr, hoa, device, port);
                log.info("<UE[id:{}]:[hwaddr: {}] [HOA: {}] connected to switch<{}> <port: {}>.",
                        Integer.toString(last_ue_id),hwaddr, hoa, device, Integer.toString(port));
//                log.info("UE<id: {}> set attachment at time <{}>.", Integer.toString(last_ue_id), dateFormat.format(date));
            }
            else{
//                log.info("============= put ues' information =====");
//                Date date = new Date();
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
                ues.put(hwaddr, new UE(last_ue_id++, hwaddr, hoa));   // ip is hoa
                ues.get(hwaddr).setAttachment(new UeAssociation(device, port));
                System.out.printf("<UE[id:%d] [hwaddr: %s] [HOA: %s] connected to swithch<%s> <port:%d>.\n",
                        last_ue_id, hwaddr, hoa, device, port);
                log.info("<UE[id:{}]:[hwaddr: {}] [HOA: {}] connected to switch<{}> <port: {}>.",
                        Integer.toString(last_ue_id),hwaddr, hoa, device, Integer.toString(port));
//                log.info("UE contains hwaddr:{} is {}", hwaddr, ues.containsKey(hwaddr));
            }

        }

        public void handleUeDeassociation(AssociationEvent event) {
            String hwaddr = event.getHwaddr();
            String device = event.getDeviceId();
            int port = event.getPort();
            String hoa = event.getCoa();  // hoa
//            System.out.printf("UE<id:%d> [hwaddr: %s] [HOA: %s] remove from switch <%s> port <%d> at time <%s>.",
//                    last_ue_id, hwaddr, hoa, device, port, dateFormat.format(date));
//            Date date = new Date();
//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
            System.out.printf("UE<id:%d> [hwaddr: %s] [HOA: %s] remove from switch <%s> port <%d>.\n",
                    last_ue_id, hwaddr, hoa, device, port);
            log.info("UE<id:{}>:[hwaddr: {}] [HOA: {}] remove from switch<{}> <port: {}>.",
                    last_ue_id,hwaddr, hoa, device, Integer.toString(port));

//            log.info("[hosts contains hwaddr:{} is {}]",hwaddr, hosts.containsKey(hwaddr));
//            log.info("====== [Ue.get(hwaddr):{}] ====", hosts.get(hwaddr).getCurrentAttachment());

            // ================= delete pre_coa and remove all flow entries on the old AP ================
            if(hosts.containsKey(hwaddr)){
                // print information about deassociation
                System.out.printf("UE<id:%d> [hwaddr: %s] [HOA: %s] remove from switch <%s> port <%d>.",
                        last_ue_id, hwaddr, hoa, device, port);
                log.info("UE<id:{}>:[hwaddr: {}] [HOA: {}] remove from switch<{}> <port: {}>.",
                        last_ue_id,hwaddr, hoa, device, Integer.toString(port));
//                System.out.println(dateFormat.format(date));
//                log.info("UE<id: {}> remove attachment at time <{}>.", Integer.toString(last_ue_id), dateFormat.format(date));

                int DIP = 1;
                String coa = hosts.get(hwaddr).getCurrentAttachment().getCoa();
//                log.info("===== [ coa: {}, coa_hex: {}",coa, ueRule.ip2HexStr(coa));
                // match pre_coa
                TrafficSelector.Builder match_pre_coa = DefaultTrafficSelector.builder();
                ArrayList<Criterion> matchList = new ArrayList<Criterion>();
                matchList.add(Criteria.matchOffsetLength((short) DIP, (short) 240, (short) 32, ueRule.ip2HexStr(coa), "FFFFFFFF"));
                match_pre_coa.add(Criteria.matchOffsetLength(matchList));

                // get all flow entries for associated switch
//                log.info("remove flows begin");
                Map<Integer, FlowRule> flowRules = new HashMap<Integer, FlowRule>();
                flowRules = flowTableStore.getFlowEntries(DeviceId.deviceId(device), FlowTableId.valueOf(globalTableId));
                if(flowRules != null) {
                    for(Integer flowEntryId : flowRules.keySet()) {
                        if(flowRules.get(flowEntryId).selector().equals(match_pre_coa.build())) {
                            // delete flow entry for coa
                            flowTableService.removeFlowEntryByEntryId(DeviceId.deviceId(device), globalTableId,
                                    flowEntryId);
//                            log.info("[entry id: {}]",flowEntryId);
                        }
                    }

                }
//                log.info("=========== [ remove flow entries ]=========");
            }

        }

        public void handleCalculatePath(AssociationEvent event) {
            int DIP = 1;
            InboundPacket packet = event.getContext().inPacket();
            Ethernet ethpkt = packet.parsed();
            IPv4 ippkt = (IPv4) ethpkt.getPayload();
            String dst_mac = ethpkt.getDestinationMAC().toString(); // mn
            String dst_ip = Integer.toHexString(ippkt.getDestinationAddress());  // have been hex, dst_hoa

            String src_mac = ethpkt.getSourceMAC().toString(); // cn
//            String src_mac = "2a:ac:c7:27:ea:1f";
            String src_ip = Integer.toHexString(ippkt.getSourceAddress());
//            String src_ip = "10.192.3.210";
//            String src_deviceId = "pof:0000000000000006";
            String src_deviceId = packet.receivedFrom().deviceId().toString(); // should be pof:6
//            log.info("handleCalculatePath: dst_mac(mn): {}", dst_mac);
//            log.info("handleCalculatePath: dst_ip(mn): {}", dst_ip);
//            log.info("handleCalculatePath: src_mac_pkt(cn): {}", src_mac_pkt);
//            log.info("handleCalculatePath: src_mac(cn): {}", src_mac);
//            log.info("handleCalculatePath: src_ip(cn): {}", src_ip_pkt);
//            log.info("handleCalculatePath: src_deviceId_pkt(cn-should be pof:6): {}", src_deviceId_pkt);

            /*String dst_mac = "2a:ac:c7:27:ea:1f"; // mn
            String src_mac = "11:22:33:44:55:66"; // cn mac
            String dst_ip = "10.39.234.31";  // hoa
            String src_ip ="10.168.109.2"; // cn ip
            String coa = "10.168.109.3";
            String src_deviceId = "pof:0000000000000006"; // cn
            String deviceId = "pof:0000000000000001"; // mn2
            hosts.put(dst_mac, new UE(10,dst_mac,dst_ip)); // for mn
            hosts.get(dst_mac).setAttachment(new UeAssociation(deviceId, 1,coa));
            hosts.put(src_mac, new UE(11,src_mac,src_ip)); // for cn
            hosts.get(src_mac).setAttachment(new UeAssociation(src_deviceId, 2));*/

            // ================ calculate path and download flow entry for device on the path =========
            if (hosts.containsKey(dst_mac) && hosts.get(dst_mac).getCurrentAttachment() != null) {
//                log.info("============================================================");
                String dst_coa = hosts.get(dst_mac).getCurrentAttachment().getCoa();  // mn
                String dst_hoa = dst_ip;
                String dst_deviceId = hosts.get(dst_mac).getCurrentAttachment().getDevice();
                int dst_port = hosts.get(dst_mac).getCurrentAttachment().getPort();
//                log.info("handleCalculatePath if contains {}: coa:{}, hoa:{}], deviceId:{} port:{}",
//                        dst_mac, dst_coa,dst_hoa,dst_deviceId,dst_port);

//                log.info("[UeId: {}]",hosts.get(dst_mac).getUeId());

                // ========== topology port ========== port = ports[path.get(index1)][path.get(index2)]
                int[][] ports = new int[7][7];
                ports[1][4] = 2;
                ports[4][1] = 3;
                ports[3][4] = 3;
                ports[4][3] = 2;
                ports[2][3] = 2;
                ports[3][2] = 1;
                ports[3][5] = 2;
                ports[5][3] = 2;
                ports[4][5] = 1;
                ports[5][4] = 1;
                ports[5][6] = 3;
                ports[6][5] = 1;

                // =============== calculate path if src(cn) associated ==============
                if (hosts.get(src_mac).getCurrentAttachment().getDevice().equals(src_deviceId)) {
//                    log.info("[UeId: {}]",hosts.get(src_mac).getUeId());
//                    log.info("handleCalculatePath equals src_deviceId:{} if contains {}: coa:{}, hoa:{}], deviceId:{} port:{}",
//                            src_deviceId,dst_mac, dst_coa,dst_hoa,dst_deviceId,dst_port);

                    List<Integer> path_list = new ArrayList<Integer>();
                    List<Integer> port_list = new ArrayList<Integer>();
                    int mn = Integer.parseInt(dst_deviceId.substring(4));
                    int cn = Integer.parseInt(src_deviceId.substring(4));
                    path_list = dijkstra.getShortestPath(cn, mn);  // from dst_mn to src_cn
                    for (int i = 0; i < path_list.size() - 1; i++) {
                        int port = ports[path_list.get(i)][path_list.get(i + 1)];
                        port_list.add(port);
                    }
                    port_list.add(dst_port);
//                    System.out.print("path_list: ");
//                    System.out.println(path_list);
//                    System.out.print("port_list: ");
//                    System.out.println(port_list);

                    // ============= download flow entry for devices ==========
                    // match0 for coa and match1 for hoa
                    TrafficSelector.Builder match0 = DefaultTrafficSelector.builder();
                    ArrayList<Criterion> matchList0 = new ArrayList<Criterion>();
                    matchList0.add(Criteria.matchOffsetLength((short) DIP, (short) 240, (short) 32, ueRule.ip2HexStr(dst_coa), "FFFFFFFF"));
                    match0.add(Criteria.matchOffsetLength(matchList0));

                    TrafficSelector.Builder match1 = DefaultTrafficSelector.builder();
                    ArrayList<Criterion> matchList1 = new ArrayList<Criterion>();
                    matchList1.add(Criteria.matchOffsetLength((short) DIP, (short) 240, (short) 32, ueRule.ip2HexStr(dst_hoa), "FFFFFFFF"));
                    match1.add(Criteria.matchOffsetLength(matchList1));

                    // actions : action0 for set field(if hoa,set coa), action1 for output(if coa,forward)
                    List<OFAction> actions_set_output = new ArrayList<OFAction>();
//                    List<OFAction> actions_output = new ArrayList<OFAction>();
//                    log.info("[dst_coa: {}]",ueRule.ip2HexStr(dst_coa));
                    OFAction action0 = DefaultPofActions.setField((short) DIP, (short) 240, (short) 32, ueRule.ip2HexStr(dst_coa), "FFFFFFFF").action();
                    actions_set_output.add(action0);  // set field
//                    log.info("[ actions: {}",actions_set_output);
                    TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
//                    TrafficTreatment.Builder outputbuilder = DefaultTrafficTreatment.builder();
                    if (path_src_ips.containsKey(src_ip) && path_src_ips.get(src_ip).equals(dst_coa))
                        ;
                    else {
                        for (int i = 0; i < path_list.size() - 1; i++) {
                            String path_deviceId = "pof:000000000000000" + Integer.toString(path_list.get(i));
                            System.out.println(path_deviceId);
//                            if(path_deviceId.equals("pof:0000000000000004")){
//                                globalTableId = 121;
//                            }
                            long newEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(path_deviceId),globalTableId);
                            OFAction action1 = DefaultPofActions.output((short) DIP, (short) 240, (short) 32, port_list.get(i)).action();
//                            log.info("[ OFAction action1: {} ]",action1);
                            if (i == 0) {
                                // set field and output
                                actions_set_output.add(action1);
//                                log.info("[set field and output:{}]",actions_set_output);
                                ppbuilder.add(DefaultPofInstructions.applyActions(actions_set_output));
                                FlowRule.Builder flowRule = DefaultFlowRule.builder()
                                        .forDevice(DeviceId.deviceId(path_deviceId))
                                        .forTable(globalTableId)
                                        .withSelector(match1.build())
                                        .withTreatment(ppbuilder.build())
                                        .withCookie(newEntryId)
                                        .withPriority(1)
                                        .makePermanent();

                                flowRuleService.applyFlowRules(flowRule.build());
//                                log.info("[set field and output actions ok]");
                            } else {
                                // output if i != 0
//                                actions_output.remove(0);  // remove setField
                                List<OFAction> actions_output = new ArrayList<OFAction>();
                                actions_output.add(action1);
//                                log.info("[========== output action :{} ==========]",actions_output);
                                TrafficTreatment.Builder outputbuilder = DefaultTrafficTreatment.builder();
                                outputbuilder.add(DefaultPofInstructions.applyActions(actions_output));
//                                log.info("============ output.builder:{} ==========",outputbuilder);
                                FlowRule.Builder flowRule = DefaultFlowRule.builder()
                                        .forDevice(DeviceId.deviceId(path_deviceId))
                                        .forTable(globalTableId)
                                        .withSelector(match0.build())
                                        .withTreatment(outputbuilder.build())
                                        .withCookie(newEntryId)
                                        .withPriority(1)
                                        .makePermanent();

                                flowRuleService.applyFlowRules(flowRule.build());
//                                log.info("[output action ok]");
//                                actions_output.remove(action1);  // remove setField
//                                log.info("[========== output action removed :{} ==========]",actions_output);
                            }
//                            actions_output.add(action1);  // for removing error, no use
                        }
                        path_src_ips.put(src_ip, dst_coa);
                    }
                }
            }
        }

        public void handleDHCPLease(AssociationEvent event) {

            // ============ DHCPLease event test ==========
//            log.info("begin handle DHCPLease in handleDHCPLease().");

            String deviceId = event.getDeviceId();  // should be pof:01/02
            String hwaddr = event.getHwaddr();
            String coa = event.getCoa();
            int port = event.getPort();
//            log.info("handleDhcpLease: deviceId:{}, hwaddr:{}, coa:{}, port:{}", deviceId,hwaddr,coa,port);

            String[] mac = hwaddr.split(":");
            String hoa = "10." + Integer.toString(Integer.valueOf(mac[3], 16)) + "." +
                    Integer.toString(Integer.valueOf(mac[4], 16)) + "." + Integer.toString(Integer.valueOf(mac[5], 16)) ;
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
//            System.out.printf("handleDhcpLese event: <deviceId: %s> <port: %d> < hwaddr: %s> <assignedIP: %s> at time <%s>.\n",
//                    deviceId,port,hwaddr,coa,dateFormat.format(date));
            //            log.info("hwaddr:{} hoa in handleDHCPLease: {}", hwaddr, hoa);
//            log.info("hwaddr:{} coa in handleDHCPLease: {}", hwaddr, coa);
          /*  // ============== step1: download flowTable; step1 test ok===========
            // construct flow rule
            TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
            short DIP = 1; // DIP field
            ArrayList<Criterion> matchList = new ArrayList<Criterion>();
            matchList.add(Criteria.matchOffsetLength((short) DIP,(short) 240,(short) 32, ueRule.ip2HexStr(coa),"FFffFFff"));
            pbuilder.add(Criteria.matchOffsetLength(matchList)); // match DIP

            TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(DefaultPofActions.output((short) 0,(short) 0,(short) 0, port).action());
            ppbuilder.add(DefaultPofInstructions.applyActions(actions));

            TrafficSelector selector = pbuilder.build();
            TrafficTreatment treatment = ppbuilder.build();

            if(deviceId.equals("pof:0000000000000004")){
                globalTableId = 121;
            }
            long newFlowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(deviceId), globalTableId);
            FlowRule.Builder flowRule = DefaultFlowRule.builder()
                    .forTable(globalTableId)
                    .forDevice(DeviceId.deviceId(deviceId))
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(1)
                    .withCookie(newFlowEntryId)
                    .makePermanent();
            flowRuleService.applyFlowRules(flowRule.build());
            log.info("============ download flowrule ok =========");*/

            // ================ step2: download flowRule for UeManagement to set attachment ================
            if(hosts.containsKey(hwaddr) && hosts.get(hwaddr).getCurrentAttachment().getCoa() == coa){
//                log.info("========= dhcpLease do nothing.=====");    // ip has assigned to coa, do nothing
            }
            else if(hosts.containsKey(hwaddr) && hosts.get(hwaddr).getCurrentAttachment().getCoa() != coa) {
//                log.info("=========== change path ==========");
                ueRule.changePath("pof:0000000000000006", deviceId, hosts.get(hwaddr).getCurrentAttachment(), port, hoa, coa);
//                log.info("========== install 2 host rule ==========");
                ueRule.instanll2HostRule(deviceId, port, hwaddr, coa);
                hosts.get(hwaddr).setAttachment(new UeAssociation(deviceId, port, coa));  // set new attachment
//                log.info("=============== change path successfully ======");
            }
            else {
//                log.info("===== ues.contains:{}=======", ues.get("2a:ac:c7:27:ea:1f"));
                last_ue_id ++;
//                log.info("ue_id: {}", last_ue_id);
                ueRule.instanll2HostRule(deviceId, port, hwaddr, coa);
                if(hosts.containsKey(hwaddr)) {
                    hosts.get(hwaddr).setAttachment(new UeAssociation(deviceId, port, coa));
                } else {
                    hosts.put(hwaddr, new UE(last_ue_id, hwaddr, hoa));
                    hosts.get(hwaddr).setAttachment(new UeAssociation(deviceId, port, coa));
                }
//                log.info("host.get(hwaddr):{}",hosts.get(hwaddr));

            }
        }


        @Override
        public void event(AssociationEvent event){

            //================= handle UE_ASSOCIATION event ===============
            if(event.type().equals(AssociationEvent.Type.UE_ASSOCIATION)) {
                //TODO implement UE_association
//                log.info("in process event UE_ASSOCIATION");
                handleUeAssociation(event);
//                log.info("handle UE_ASSOCIATION event ok");

            }

            //================== handle UE_DEASSOCIATION event ===============
            if(event.type().equals(AssociationEvent.Type.UE_DEASSOCIATION)) {
                //TODO implement UE_deassociation
//                log.info("in process event UE_DEASSOCIATION");
                handleUeDeassociation(event);
//                log.info("handle UE_DEASSOCIATION event ok");
            }

            // ================= handle CALCULATE_PATH event ===============
            if(event.type().equals(AssociationEvent.Type.CALCULATE_PATH)){
                //TODO implement Calculate_Path
//                log.info("in process event UE_CALCULATEPATH");
                handleCalculatePath(event);
//                log.info("handle UE_CALCULATEPATH event ok");
            }

            // ================= handle DHCPLease event ====================
            if(event.type().equals(AssociationEvent.Type.DHCPLease)) {
                // TODO implement DHCPLease
//                log.info("receive DHCPLease.");
                handleDHCPLease(event);
                // =========== DHCPLease test =========
//                log.info("handle DHCPLease in AssociationEventMonitor ok.");
            }
        }

    }
}
