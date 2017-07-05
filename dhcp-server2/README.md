  ONOS has existen org.onosproject.dhcp in apps directory. However, ONOS does not
support another dhcp app. We have two approaches to solve it:

  1. copy org.onosproject.dhcp in apps, but it doesn't work well because involvement of OSGI.
  
  2. rebuild dhcp app outside apps, easy and work effectively.
     
     modify : DistributedDhcpStore  
                                    
                                    -- line 62, .withName("onos-mydhcp-assignedIP")
                                    -- line 75, .withName("onos-mydhcp-freeIP")
                                    
                                    
  
  To support POF, we modify function sendReply(), where we change builder.setOutput to PofActions.