package org.test.optimalmode;

/**
 * Created by tsf on 4/14/17.
 *
 * @Description set an attribute "active" to indicate Attachment state
 */

// ======== test ok =======
public class UeAttachment {
    private boolean active;
    protected String device;
    protected int port;

    // construction function
    public UeAttachment(String device, int port){
        this.active = true;
        this.device = device;
        this.port = port;
    }

    // inquiry whether attachment is active or not
    public boolean isActive(){
        return this.active;
    }

    // set value of active
    public void setActive(String booleanValue){
        if(booleanValue.equals("false"))
            this.active = false;
        else
            this.active = true;
    }

    //TODO implement dictionary instead of get*() function
    public String getDevice() {
        return this.device;
    }

    public int getPort() {
        return this.port;
    }
}