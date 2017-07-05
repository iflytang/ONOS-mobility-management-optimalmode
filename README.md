### Description

  The project supports mobility management based POF on ONOS.
  
  *dhcp-server1*  is located in ONOS's apps directory, serving for AP1.
  
  *dhcp-server2*  is outside ONOS's apps, which can be moved in onos-app 
  directory, serving for AP2.
  
  *optimalmode*  is core module for mobility management, which setups new
  flow rules for new path and removes ones on old path when handover happens.




### start app
  Start the mobility management optimal app have a fixed order because of dependency in maven architecture.
  - activate dhcp-server1
  ```
  app activate org.onosproject.dhcp
  ```
  
  - activate dhcp-server2
  ```
  app activate org.test.dhcp
  ```
  
  - activate optimal
  ```
  app activate org.test.optimal
  ```
  