package org.test.optimalmode;

/**
 * Created by tsf on 4/14/17.
 *
 * @Description set an attribute "coa" to indicate Ue's COA
 */

//============ test ok ==========
public class UeAssociation extends UeAttachment {
    String device;
    int port;
    String coa; // COA, Care of Address

    // construction function 1
    public UeAssociation(String device, int port, String coa) {
        super(device, port);
        this.device = device;
        this.port = port;
        this.coa = coa;   // for hosts
    }

    // construction function 2
    public UeAssociation(String device, int port) {
        super(device, port);
        this.device = device;
        this.port = port;
        this.coa = ""; // for ues
    }

    public String getDevice() {
        return this.device;
    }

    public int getPort() {
        return this.port;
    }

    public String getCoa() {
        if(this.coa != null)
            return this.coa;
        return null;
    }
}
