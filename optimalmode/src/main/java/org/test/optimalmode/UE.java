package org.test.optimalmode;

/**
 * Created by tsf on 4/14/17.
 *
 * @Description set "UeId" and "hoa" to identify ue.
 */

// ============== test ok ==========
public class UE extends Node {
    protected int UeId;
    protected String hwaddr;
    protected String hoa;  // Home Of Address

    public UE(int UeId,String hwaddr, String hoa){
        super();   // initialize root class Node
        this.UeId = UeId;
        this.hwaddr = hwaddr;
        this.hoa = hoa;
    }

    //TODO override Dictionary function in Node, instead of get*() function
    public int getUeId() {
        return this.UeId;
    }

    public String getHwaddr() {
        return this.hwaddr;
    }

    public String getHoa() {
        return this.hoa;
    }


}



