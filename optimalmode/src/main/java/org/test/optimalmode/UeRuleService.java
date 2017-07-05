package org.test.optimalmode;

import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.table.FlowTableStore;

import java.util.List;

/**
 * Created by tsf on 4/25/17.
 */
public interface UeRuleService {

    void installPktOut(String deviceId, PacketContext packet, int dst_port, int src_port, int DIP_FIELD);

    void installGWFlowRule(String dst_ip, String deviceId, List<Integer> out_port, int DIP_FIELD);

    String calSRH(List<Integer> out_port);

    void ModifyGWFlowRule(String dst_ip, String deviceId, List<Integer> out_port, int entry_id, int DIP_FIELD);

    void installInterSwitchFlowRule(String gw_ip, String deviceId, int out_port, int DIP_FIELD);

    String calculateGWip(String deviceId, int dst_port);

    String ip2HexStr(String ip);

    void instanll2HostRule(String deviceId, int port,String hwaddr, String coa);

//    void remove2HostRule(String pre_deviceId,int pre_port, String pre_coa);

//    void removeCNRule(String deviceId, String hoa);

    void changePath(String cnSwitch, String mnSwich, UeAssociation pre_attachment, int dst_port,
                    String hoa, String coa);

    FlowTableStore getFlowTableStore();
}
