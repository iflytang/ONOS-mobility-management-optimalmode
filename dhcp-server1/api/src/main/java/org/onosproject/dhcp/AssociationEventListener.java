package org.onosproject.dhcp;


import org.onosproject.event.EventListener;

/**
 * Created by tsf on 4/14/17.
 *
 * @Description define AssociationEventListen,
 *              will override function event() in class AssociationEventMonitor
 */

public interface AssociationEventListener extends EventListener<AssociationEvent> {
}
