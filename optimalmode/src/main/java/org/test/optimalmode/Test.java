package org.test.optimalmode;

import org.apache.felix.scr.annotations.*;
import org.onosproject.dhcp.AssociationEvent;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.event.EventDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by tsf on 4/24/17.
 */

@Component(immediate = true)
public class Test {
    private Logger log = LoggerFactory.getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UeRuleService ueRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService service;


//    protected EventDeliveryService service = dhcpService.getEventDispatcher();

    @Activate
    public void activate() {
        log.info("Test start");
//        ueRuleService.instanll2HostRule("pof:0000000000000001",1,"10.168.109.1"); // coa
//        test_UeAssociation();
//        test_DHCPLease();
//        test_CalculatePath();
//        try{
//            Thread.currentThread().sleep(40);
//        }catch (Exception e){
//            System.out.print(e);
//        }
////        test_changePath_1();
//        test_DHCPLease_1();
//        try{
//            Thread.currentThread().sleep(40);
//        }catch (Exception e){
//            System.out.print(e);
//        }
////        test_changePath_2();
//       test_DHCPLease_2();

    }


    public void test_changePath_1() {
        log.info("============= [ test new changePath ] =================");
        ueRuleService.changePath("pof:0000000000000006","pof:0000000000000002",
                new UeAssociation("pof:0000000000000001",1,"10.168.109.3"),1,
                "10.39.234.31","10.168.109.4");

    }
    public void test_changePath_2() {
        log.info("============= [ test old changePath ] =================");
        ueRuleService.changePath("pof:0000000000000006","pof:0000000000000001",
                new UeAssociation("pof:0000000000000002",1,"10.168.109.4"),1,
                "10.39.234.31","10.168.109.3");

    }

    public  void deactivate() {
        log.info("Test stopped.");

    }

    public void test_CalculatePath() {
        log.info("[ test_calculatePath]");
        AssociationEvent event = new AssociationEvent(AssociationEvent.Type.CALCULATE_PATH,"calculatePath");
        service.post(event);
        log.info("[ back in calculatePath]");
    }

    public void test_UeDeAssociation() {
        AssociationEvent test_UeDeAssociation = new AssociationEvent(AssociationEvent.Type.UE_DEASSOCIATION,
                "UeAssociationEvent","2a:ac:c7:27:ea:1f","pof:0000000000000002",
                1,"10.39.234.31");  //hoa
        log.info("================= raise UeDeAssociationEvent =========");
        service.post(test_UeDeAssociation);

    }

    public void test_UeAssociation() {
        AssociationEvent test_UeAssociation = new AssociationEvent(AssociationEvent.Type.UE_ASSOCIATION,
                "UeAssociationEvent","2a:ac:c7:27:ea:1f","pof:0000000000000002",
                1,"10.39.234.31"); // hoa
        log.info("================= raise UeAssociationEvent =========");
        service.post(test_UeAssociation);
    }

    public void test_DHCPLease_1() {
        // ============== test DHCPLease event ============
        AssociationEvent Dhcpevent1 = new AssociationEvent(AssociationEvent.Type.DHCPLease, "dhcpLease",
                "2a:ac:c7:27:ea:1f", "pof:0000000000000001", 1, "10.168.109.3"); // coa
        service.post(Dhcpevent1);
        log.info("post DHCPLease1 --> event type: {}", Dhcpevent1.type());
        log.info("hwaddr:{}, deviceId:{}, port:{}, coa:{}", Dhcpevent1.getHwaddr(), Dhcpevent1.getDeviceId(), Dhcpevent1.getPort(), Dhcpevent1.getCoa());

//        AssociationEvent Dhcpevent2 = new AssociationEvent(AssociationEvent.Type.DHCPLease, "dhcpLease",
//                "2a:ac:c7:27:ea:1f", "pof:0000000000000001", 1, "10.168.109.199");
//        service.post(Dhcpevent2);
    }

    public void test_DHCPLease_2() {
        // ============== test DHCPLease event ============
        AssociationEvent Dhcpevent2 = new AssociationEvent(AssociationEvent.Type.DHCPLease, "dhcpLease",
                "2a:ac:c7:27:ea:1f", "pof:0000000000000002", 1, "10.168.109.4"); // coa
        service.post(Dhcpevent2);
        log.info("post DHCPLease2 --> event type: {}", Dhcpevent2.type());
        log.info("hwaddr:{}, deviceId:{}, port:{}, coa:{}", Dhcpevent2.getHwaddr(), Dhcpevent2.getDeviceId(), Dhcpevent2.getPort(), Dhcpevent2.getCoa());

//        AssociationEvent Dhcpevent2 = new AssociationEvent(AssociationEvent.Type.DHCPLease, "dhcpLease",
//                "2a:ac:c7:27:ea:1f", "pof:0000000000000001", 1, "10.168.109.199");
//        service.post(Dhcpevent2);
    }

    public void test_UeRule() {
        log.info("===================[ test install2HostFlowRule ]======================");
//        ueRuleService.instanll2HostRule("pof:0000000000000001", 2, "10.168.109.122");
//        log.info("============[ back in test] ===========");

//        log.info("==================[ test calculateGWip ] ============");
//        ueRuleService.calculateGWip("pof:0000000000000001",1);
//        log.info("============[ back in test] ===========");

//        log.info("==================[ test UeRule ] ============");
//        List<Integer> port = new ArrayList<Integer>();
//        port.add(1);
//        port.add(2);
//        port.add(3);
////        ueRuleService.calSRH(port);
//        ueRuleService.installInterSwitchFlowRule("0a112233","pof:0000000000000003", 2, 1);
//        log.info("============[ back in test] ===========");

        log.info("========= test path ======");
        // from mn to cn
//        ueRuleService.instanll2HostRule("pof:0000000000000001",2,"10.0.0.1");
//        ueRuleService.instanll2HostRule("pof:0000000000000004",1,"10.0.0.1");
//        ueRuleService.instanll2HostRule("pof:0000000000000005",3,"10.0.0.1");
//        ueRuleService.instanll2HostRule("pof:0000000000000006",2,"10.0.0.1");
        // from cn to mn
//        ueRuleService.instanll2HostRule("pof:0000000000000001",1,"10.0.0.3");
//        ueRuleService.instanll2HostRule("pof:0000000000000004",3,"10.0.0.3");
//        ueRuleService.instanll2HostRule("pof:0000000000000005",1,"10.0.0.3");
//        ueRuleService.instanll2HostRule("pof:0000000000000006",1,"10.0.0.3");
//        log.info("=========== a new path to test ping ======");
//        UE ue = new UE(1, "11:22:33:44:55:66", "10.0.0.3" );
//        ue.setAttachment(new UeAssociation("pof:0000000000000001",1,"10.168.109.121"));
//        ueRuleService.changePath("pof:0000000000000006","pof:0000000000000002",ue.getCurrentAttachment(),
//          1,"10.0.0.3","10.168.109.121");

    }


}
