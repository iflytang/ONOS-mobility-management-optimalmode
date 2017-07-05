package org.test.optimalmode;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.*;
import org.onosproject.floodlightpof.protocol.OFPacketOut;
import org.onosproject.floodlightpof.protocol.action.OFAction;
import org.onosproject.floodlightpof.protocol.table.OFTableType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.DefaultPofActions;
import org.onosproject.net.flow.instructions.DefaultPofInstructions;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.table.FlowTableId;
import org.onosproject.net.table.FlowTableService;
import org.onosproject.net.table.FlowTableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by tsf on 4/15/17.
 *
 * @Description install Ue's flow rule and define how to calculate path.
 */

// ============== test ok ========
@Component(immediate = true)
@Service
public class UeRule implements UeRuleService{

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableService flowTableService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableStore flowTableStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private final Logger log = LoggerFactory.getLogger(getClass());
    //    static FlowRule.Builder flowRule;
    protected Dijkstra dijkstra = new Dijkstra();
    protected OFPacketOut ofPacketOut;
    int globalTableId = NetworkBoot.globalTableId();

    @Activate
    public void activate() {
        log.info("======== UeRuleService Start.=======");
    }

    @Deactivate
    public void deactivate() {
        log.info("======== UeRuleService Stop.=======");
    }

    @Override
    public FlowTableStore getFlowTableStore() {
        return this.flowTableStore;
    }

    // reference in OFPacketOut.class
    //TODO should we build TrafficTreatment?
    @Override
    public void installPktOut(String deviceId, PacketContext packet, int dst_port, int src_port, int DIP_FIELD) {
        List<OFAction> actionsList = new ArrayList<OFAction>();
        actionsList.add(DefaultPofActions.output((short) 0, (short) 0, (short) 0, dst_port).action());
       /* ofPacketOut.setActions(actionsList);
        ofPacketOut.setIn
        Port(src_port);
        ofPacketOut.setPacketData(packet.outPacket().data().array());
        packet.treatmentBuilder()
//        packet.send();
//        ofPacketOut.send();
//        ofPacketOut.writeTo();   //TODO how to send to device
        packetService.emit((OutboundPacket) ofPacketOut);*/
        packet.treatmentBuilder().add(DefaultPofInstructions.applyActions(actionsList));
        packet.send();
    }


    // download flow entry to GW
    //TODO keep mind DIP_field = 1 and Criteria.matchOffsetLength in line 57
    @Override
    public void installGWFlowRule(String dst_ip, String deviceId, List<Integer> out_port, int DIP_FIELD) {
        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        ArrayList<Criterion> list = new ArrayList<Criterion>();
        list.add(Criteria.matchOffsetLength((short) DIP_FIELD, (short) 240, (short) 32, ip2HexStr(dst_ip), "FFFFFFFF"));
        pbuilder.add(Criteria.matchOffsetLength(list));
        log.info("DIP_FIELD:{}",DIP_FIELD);

        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(DefaultPofActions.addField((short) DIP_FIELD, (short) 112,
                (out_port.size()+1)*8, calSRH(out_port)).action());
        actions.add(DefaultPofActions.output((short) 0, (short) 0, (short) 0, (int) out_port.get(0)).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));

        long newFlowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(deviceId), globalTableId);
        log.info("========== [entry id: {}]",newFlowEntryId);

        FlowRule.Builder flowRule = DefaultFlowRule.builder()
                .forTable(globalTableId)
                .forDevice(DeviceId.deviceId(deviceId))
                .withSelector(pbuilder.build())
                .withTreatment(ppbuilder.build())
                .withPriority(0)
                .withCookie(newFlowEntryId)   // set entry id
                .makePermanent()
                ;

        flowRuleService.applyFlowRules(flowRule.build());
    }


    // calculate SRH, out_port is int List, SRH format -> type + ttl + out_port
    // attention: List<Integer> out_port = new ArrayList<Integer>(); have tested ok.
    @Override
    public String calSRH(List<Integer> out_port) {
        if(out_port.size() > 2){
            String ttl = Integer.toHexString(out_port.size()-2);  // no prefix 0x
            if(ttl.length() < 2)
                ttl = "0" + ttl;
            String ports_str = "";
            for(Object port:out_port){
                String port_str = Integer.toHexString((int) port);
                if(port_str.length() < 2)
                    port_str = "0" + port_str;
                ports_str = port_str + ports_str;
            }
            String SRH = "0908" + ttl + ports_str;
            log.info("============= SRH: {}", SRH);
            return SRH;
        }
        else
            return null;
    }


    //TODO keep mind Criteria.matchOffsetLength and DIP_FIELD = 1
    @Override
    public void ModifyGWFlowRule(String dst_ip, String deviceId, List<Integer> out_port, int entry_id, int DIP_FIELD) {
        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        pbuilder.add(Criteria.matchOffsetLength((short) DIP_FIELD,(short) 240, (short) 32, ip2HexStr(dst_ip), "FFFFFFFF"));

        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(DefaultPofActions.addField((short) DIP_FIELD, (short) 112,
                (out_port.size() + 1)*8, calSRH(out_port)).action());
        actions.add(DefaultPofActions.output((short) 0, (short) 0, (short) 0, out_port.get(0)).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));

//        long newFlowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(deviceId), globalTableId);

        FlowRule.Builder flowRule = DefaultFlowRule.builder()
                .forDevice(DeviceId.deviceId(deviceId))
                .forTable(globalTableId)
                .withSelector(pbuilder.build())
                .withTreatment(ppbuilder.build())
                .withPriority(1)
                .makePermanent()
                .withCookie((long) entry_id); // set flow entry id

        //modify flow entry
        FlowTableId tableId = FlowTableId.valueOf((long) globalTableId);
        flowTableStore.modifyFlowEntry(DeviceId.deviceId(deviceId), tableId, flowRule.build());
    }


    // install flow rules for interSwitch
    @Override
    public void installInterSwitchFlowRule(String gw_ip, String deviceId, int out_port, int DIP_FIELD) {
        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        ArrayList<Criterion> list = new ArrayList<Criterion>();
        list.add(Criteria.matchOffsetLength((short) DIP_FIELD, (short) 240, (short) 32, gw_ip, "FFFFFFFF"));
        pbuilder.add(Criteria.matchOffsetLength(list));

        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(DefaultPofActions.output((short) 0, (short) 0,(short) 0, out_port).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));

        long newFlowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(deviceId), globalTableId);

        FlowRule.Builder flowRule = DefaultFlowRule.builder()
                .forDevice(DeviceId.deviceId(deviceId))
                .forTable(globalTableId)
                .withSelector(pbuilder.build())
                .withTreatment(ppbuilder.build())
                .withPriority(0)
                .withCookie(newFlowEntryId)
                .makePermanent();

        flowRuleService.applyFlowRules(flowRule.build());
    }



    @Override
    public String calculateGWip(String deviceId, int dst_port) {
        int deviceId_int = Integer.parseInt(deviceId.substring(4));  // deviceId "pof:00000000001"
        String deviceId_hex_str = Integer.toHexString(deviceId_int);
        String port_hex_str = Integer.toHexString(dst_port);

        if(deviceId_hex_str.length() < 2)
            deviceId_hex_str = "0" + deviceId_hex_str;
        if(port_hex_str.length() < 2)
            port_hex_str = "0" + port_hex_str;

        String gw_ip = "0a00" + deviceId_hex_str + port_hex_str;  // gw_ip has been hex_str

        log.info("===========[ gw_ip:{}]=========",gw_ip);

        return gw_ip;
    }


    // convert ip to hexString, if ip has point
    @Override
    public String ip2HexStr(String ip) {
        String[] ip_str = ip.split("\\.");
        String[] temp_ip = new String[4];
        String ip_hex_str = "";
        for(int i=0; i<4; i++){
            temp_ip[i] = Integer.toHexString(Integer.valueOf(ip_str[i]));
            if(temp_ip[i].length() < 2){
                temp_ip[i] = "0" + temp_ip[i];
            }
            ip_hex_str = ip_hex_str + temp_ip[i];
        }

        return ip_hex_str;
    }


    @Override
    public void instanll2HostRule(String deviceId, int port, String hwaddr, String coa) {
        int DIP_FIELD = 1;

        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        ArrayList<Criterion> list = new ArrayList<Criterion>();
        list.add(Criteria.matchOffsetLength((short) 1, (short) 240, (short) 32, ip2HexStr(coa), "FFFFFFFF"));
        pbuilder.add(Criteria.matchOffsetLength(list));

        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(DefaultPofActions.output((short) 0, (short) 0, (short) 0, port).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));

        long newFlowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(deviceId), globalTableId);

        FlowRule.Builder flowRule = DefaultFlowRule.builder()
                .forDevice(DeviceId.deviceId(deviceId))
                .forTable(globalTableId)
                .withSelector(pbuilder.build())
                .withTreatment(ppbuilder.build())
                .withPriority(1)
                .withCookie(newFlowEntryId)
                .makePermanent();

        flowRuleService.applyFlowRules(flowRule.build());

//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
        System.out.println("new_path_deviceId: " + deviceId);
        System.out.printf("changePath destination to <deviceId: %s> <port: %d> < hwaddr: %s> <assignedIP: %s>.\n",
                deviceId,port,hwaddr,coa);
        System.out.println();
    }


    // calculate path
    @Override
    public void changePath(String cnSwitch, String mnSwich, UeAssociation pre_attachment, int dst_port,
                           String hoa, String coa) {
        // ======= topology graph ==========
        Dijkstra dijkstra = new Dijkstra();
//        dijkstra.addVertex(1, Arrays.asList(dijkstra.new Vertex(4,1)));  // AP1
//        dijkstra.addVertex(2, Arrays.asList(dijkstra.new Vertex(3,1)));  // AP2
//        dijkstra.addVertex(3, Arrays.asList(dijkstra.new Vertex(2,1),
//                dijkstra.new Vertex(4,1), dijkstra.new Vertex(5,1)));
//        dijkstra.addVertex(4, Arrays.asList(dijkstra.new Vertex(1,1),
//                dijkstra.new Vertex(3,1), dijkstra.new Vertex(5,1)));
//        dijkstra.addVertex(5, Arrays.asList(dijkstra.new Vertex(3,1),
//                dijkstra.new Vertex(4,1), dijkstra.new Vertex(6,1)));
//        dijkstra.addVertex(6, Arrays.asList(dijkstra.new Vertex(5,1))); // AP3

        // ========== topology port ========== port = ports[path.get(index1)][path.get(index2)]
        int[][] ports = new int[7][7];
        ports[1][4] = 2;    ports[4][1] = 3;    ports[3][4] = 3;    ports[4][3] = 2;
        ports[2][3] = 2;    ports[3][2] = 1;    ports[3][5] = 2;    ports[5][3] = 2;
        ports[4][5] = 1;    ports[5][4] = 1;    ports[5][6] = 3;    ports[6][5] = 1;
        int DIP = 1; // DIP field
        int cn = Integer.parseInt(cnSwitch.substring(4));   // switch -> pof:0000000000000001
        int mn = Integer.parseInt(mnSwich.substring(4));

        // ============== remove old flow rule on the old path ============
        int pre_mn = Integer.parseInt(pre_attachment.getDevice().substring(4));
        List<Integer> old_path_list = dijkstra.getShortestPath(cn, pre_mn);
        List<Integer> old_port_list = new ArrayList<Integer>();

        if(old_path_list.size() > 0){
            // add old port to old_port_list
            for(int i=0; i < old_path_list.size()-1; i++){
                int port = ports[old_path_list.get(i)][old_path_list.get(i+1)];
                old_port_list.add(port);
            }
            old_port_list.add(pre_attachment.getPort());  // old port from cn to mn

            // remove old flow rule, flow = 0 modified in constructing new path, include AP's flow
            for(int i=0 ; i < old_path_list.size() ; i++) {
                // construct match, match pre_coa
                TrafficSelector.Builder match_coa = DefaultTrafficSelector.builder();
                ArrayList<Criterion> coa_list = new ArrayList<Criterion>();
                coa_list.add(Criteria.matchOffsetLength((short) DIP, (short) 240, (short) 32,
                        ip2HexStr(pre_attachment.getCoa()), "FFFFFFFF"));
//                log.info("old coa : {}",ip2HexStr(pre_attachment.getCoa()));
                match_coa.add(Criteria.matchOffsetLength(coa_list));

                TrafficSelector.Builder match_hoa = DefaultTrafficSelector.builder();
                ArrayList<Criterion> hoa_list = new ArrayList<Criterion>();
                hoa_list.add(Criteria.matchOffsetLength((short) 1, (short) 240, (short) 32, ip2HexStr(hoa),"ffFFffFF"));
                match_hoa.add(Criteria.matchOffsetLength(hoa_list));
                String deviceId = "pof:000000000000000" + Integer.toString(old_path_list.get(i));

                // get all flow entries
                Map<Integer, FlowRule> flowRules = new HashMap<Integer, FlowRule>();
                flowRules = flowTableStore.getFlowEntries(DeviceId.deviceId(deviceId),
                        FlowTableId.valueOf(globalTableId));
                if(flowRules != null){
                    for(Integer flowEntryId : flowRules.keySet()){
                        //TODO match equals?
//                        if(flowRules.get(flowEntryId).equals(match))
                        if(flowRules.get(flowEntryId).selector().equals(match_hoa.build())){
                            // delete flow entry
                            flowTableService.removeFlowEntryByEntryId(DeviceId.deviceId(deviceId), globalTableId, flowEntryId);
//                            flowTableStore.deleteFlowEntry(DeviceId.deviceId(deviceId), FlowTableId.valueOf(flowEntry.tableId()), (int) flowEntry.id().value());
//                            log.info("============ [ remove flow rule on {} ] =======",deviceId);
                            continue;
                        }else if(flowRules.get(flowEntryId).selector().equals(match_coa.build())) {
//                            if(deviceId.equals("pof:0000000000000004")) {
//                                globalTableId = 121;
//                            }
                            flowTableService.removeFlowEntryByEntryId(DeviceId.deviceId(deviceId), globalTableId, flowEntryId);
//                            flowTableStore.deleteFlowEntry(DeviceId.deviceId(deviceId), FlowTableId.valueOf(flowEntry.tableId()), (int) flowEntry.id().value());
//                            log.info("============ [ remove flow rule on {} ] =======",deviceId);
                            continue;
                        }

                    }
                }
            }
        }
//        log.info("       [ remove old_path_list entries ok]   ");



        // ============== calculate new path, from cn to mn =====================
        List<Integer> path_list = dijkstra.getShortestPath(cn, mn);   // store path list from cn to mn
        List<Integer> port_list = new ArrayList<Integer>();

        // ================ set new flow rules on new path ==================
        // add port to port_list
        if(path_list.size() > 0){
            // add port list, cannot get last port(dst_port) in for loop
            for(int i = 0 ; i < path_list.size()-1 ; i++){
                int port = ports[path_list.get(i)][path_list.get(i+1)];
                port_list.add(port);
            }
            port_list.add(dst_port);
            System.out.print("new_path_list: ");
            System.out.println(path_list);
            System.out.print("new_port_list: ");
            System.out.println(port_list);

            // match0 and match1 , should be build()
//            log.info("===== begin change path ======");
            TrafficSelector.Builder match0 = DefaultTrafficSelector.builder();
            ArrayList<Criterion> matchlist0 = new ArrayList<Criterion>();
            matchlist0.add(Criteria.matchOffsetLength((short) DIP, (short) 240, (short) 32, ip2HexStr(coa), "FFFFFFFF"));
            match0.add(Criteria.matchOffsetLength(matchlist0));
//            log.info("[new coa: {}]",ip2HexStr(coa));

            TrafficSelector.Builder match1 = DefaultTrafficSelector.builder();
            ArrayList<Criterion> matchlist1 = new ArrayList<Criterion>();
            matchlist1.add(Criteria.matchOffsetLength((short) DIP, (short) 240, (short) 32, ip2HexStr(hoa), "FFFFFFFF"));
            match1.add(Criteria.matchOffsetLength(matchlist1));

            // action0
            TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
            List<OFAction> actions_setField_output = new ArrayList<OFAction>();
            OFAction action0 = DefaultPofActions.setField((short) DIP, (short) 240, (short) 32, ip2HexStr(coa),
                    "FFFFFFFF").action();
//            log.info("[ changePath: setField actions: {}",action0);
            actions_setField_output.add(action0);  // set field

            for(int i=0 ; i < path_list.size()-1 ; i++){
                OFAction action1 = DefaultPofActions.output((short) 0, (short) 0, (short) 0, port_list.get(i)).action();
//                actions_setField_output.add(action1);
                String deviceId = "pof:000000000000000" + Integer.toString(path_list.get(i));
//                long newflowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(cnSwitch),globalTableId);
                System.out.print("new_path_deviceId: ");
                System.out.println(deviceId);
                if(i == 0) {
                    actions_setField_output.add(action1);   // ins = [action0, action1]
                    ppbuilder.add(DefaultPofInstructions.applyActions(actions_setField_output));
//                    log.info("[changPath: set field and output:{}]",actions_setField_output);
                    // add flow entry
                    long newflowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(cnSwitch),globalTableId);
                    FlowRule.Builder flowRule = DefaultFlowRule.builder()
                            .forDevice(DeviceId.deviceId(deviceId))
                            .forTable(globalTableId)
                            .withSelector(match1.build()) // match hoa
                            .withTreatment(ppbuilder.build())
                            .withPriority(1)
                            .withCookie(newflowEntryId)  // entry id
                            .makePermanent();
                    flowRuleService.applyFlowRules(flowRule.build());
//                    log.info("===========[ add flow entry ] =======");

                    Map<Integer, FlowRule> flowRules = new HashMap<Integer, FlowRule>();
//                    FlowRule.Builder flow_rule = DefaultFlowRule.builder();
                    // get all flow entry in cnSwitch using getFlowEntries
                    flowRules = flowTableStore.getFlowEntries(DeviceId.deviceId(cnSwitch),
                            FlowTableId.valueOf(globalTableId));
/*                    for(Integer flowEntryId : flowRules.keySet()) {
                        // construct flow entry for cnSwitch
                        log.info("========== [ changPath: entry id: {}]",flowEntryId);
                        log.info("======= [ changPath:match equals? {} ]========",flowRules.get(flowEntryId).selector().equals(match1.build()));
                        // if match hoa
                        if(true){
                            FlowTableId tableId = FlowTableId.valueOf((long) globalTableId);
                            flowTableService.removeFlowEntryByEntryId(DeviceId.deviceId(deviceId), globalTableId, flowEntryId);
                            long newflowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(cnSwitch),globalTableId);
                            FlowRule.Builder flowRule = DefaultFlowRule.builder()
                                    .forDevice(DeviceId.deviceId(deviceId))
                                    .forTable(globalTableId)
                                    .withSelector(match1.build()) // match hoa
                                    .withTreatment(ppbuilder.build())
                                    .withPriority(1)
                                    .withCookie(newflowEntryId)  // entry id
                                    .makePermanent();
                            flowRuleService.applyFlowRules(flowRule.build());
                            log.info("========= [ modify field ] ========");
                            break;
                        }
                        else {
                            // add flow entry
                            long newflowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(cnSwitch),globalTableId);
                            FlowRule.Builder flowRule = DefaultFlowRule.builder()
                                    .forDevice(DeviceId.deviceId(deviceId))
                                    .forTable(globalTableId)
                                    .withSelector(match1.build()) // match hoa
                                    .withTreatment(ppbuilder.build())
                                    .withPriority(1)
                                    .withCookie(newflowEntryId)  // entry id
                                    .makePermanent();
                            flowRuleService.applyFlowRules(flowRule.build());
                            log.info("===========[ add flow entry ] =======");
                            break;
                        }
                    }*/
                }
                else {
                    // download flow entry to intermediate switch when i != 0
//                    if(deviceId.equals("pof:0000000000000004")){
//                        globalTableId = 121;
//                    }
                    List<OFAction> actions_output = new ArrayList<OFAction>();
                    actions_output.add(action1);
//                    log.info("[========== output action :{} ==========]",actions_output);
                    TrafficTreatment.Builder outputbuilder = DefaultTrafficTreatment.builder();
                    long newFlowEntryId = flowTableStore.getNewFlowEntryId(DeviceId.deviceId(deviceId), globalTableId);

                    outputbuilder.add(DefaultPofInstructions.applyActions(actions_output));  // ins = [action1]
                    FlowRule.Builder flowRule = DefaultFlowRule.builder()
                            .forDevice(DeviceId.deviceId(deviceId))
                            .forTable(globalTableId)
                            .withSelector(match0.build())  // match coa
                            .withTreatment(outputbuilder.build())
                            .withPriority(1)
                            .withCookie(newFlowEntryId)
                            .makePermanent();
                    flowRuleService.applyFlowRules(flowRule.build());

//                    log.info("======== [ download flow rules on the new path ] =============");

                }
            }

        }
    }

}

