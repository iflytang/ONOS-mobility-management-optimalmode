package org.onosproject.dhcp;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.packet.PacketContext;

//import org.onosproject.net.DeviceId;
//import org.onosproject.net.Port;

/**
 * Created by tsf on 4/14/17.
 *
 * @Descrip Ue_Association or Ue_deassociation
 */
public class AssociationEvent extends AbstractEvent<AssociationEvent.Type, String> {
    protected String hwaddr;
    protected String deviceId;
    protected int port;
    protected String coa;    // bind switch+port+coa
    //    protected String coa;   // only for DHCPLease
    protected PacketContext context;  // only for CALCULATE_PATH


    public enum Type {
        UE_ASSOCIATION,      //UE associated

        UE_DEASSOCIATION,    //UE unassociated

        CALCULATE_PATH,       // calculate path for UE

        DHCPLease             // DHCP lease coa for UE
    }

    // for Default initialization
    public AssociationEvent(AssociationEvent.Type type, String subject) {
        super(type, subject);
    }

    // for Ue De/Association initialization
    public AssociationEvent(AssociationEvent.Type type, String subject, String hwaddr, String deviceId,
                            int port, String coa){
        super(type, subject);
        this.hwaddr = hwaddr;
        this.deviceId = deviceId.toString();
        this.port = port;
        this.coa = coa;
        this.context = null;
    }

    // for Ue calculate path
    public AssociationEvent(AssociationEvent.Type type, String subject, PacketContext context) {
        super(type, subject);
        this.context = context;
        /*this.hwaddr = null;
        this.deviceId = null;
        this.port = 0;
        this.coa = null;*/
    }


    public String getHwaddr() {
        return hwaddr;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getPort() {
        return port;
    }

    public String getCoa() {
        return coa;
    }

    public PacketContext getContext() {
        return context;
    }


}
