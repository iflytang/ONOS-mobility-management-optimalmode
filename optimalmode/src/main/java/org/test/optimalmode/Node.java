package org.test.optimalmode;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * Created by tsf on 4/14/17.
 *
 * @Description set attachment and store attachment history for UE
 */

// ========= test ok ============
public class Node {
    private String hwaddr;    // UE's MAC
    private UeAssociation attachment;   // Attachment or Association instance
    private List<UeAssociation> attachment_history;   // for storing Attachment history

    //construction function 1
    public Node(String hwaddr, UeAssociation attachment, List<UeAssociation> attachment_history) {
        this.hwaddr = hwaddr;
        this.attachment = attachment;
        this.attachment_history = attachment_history;

    }
    //construction function 2
    public Node() {
        this.hwaddr = null;
        this.attachment = null;
        this.attachment_history = new ArrayList<UeAssociation>();

    }

    //set attachment for node
    public void  setAttachment(UeAssociation attachment) {
        if(this.attachment != null){
            this.attachment.setActive("false");
            this.attachment_history.add(this.attachment);
        }
        this.attachment = attachment;
    }

    // get previous attachment
    public UeAssociation getPreviousAttachment() {
        if(this.attachment_history != null){
            return this.attachment_history.get(attachment_history.size() - 1);   // return previous attachment
        }
        return null;
    }

    //TODO implement dict with Map, instead of get*() function
//   public Dictionary<String, AttachmentEvent> {}

    // get current attachment
    public UeAssociation getCurrentAttachment() {
        if(this.attachment != null){
            return this.attachment;
        }
        return null;
    }

    public String getHwaddr() {
        if(this.hwaddr != null){
            return this.hwaddr;
        }
        return "";
    }
}
