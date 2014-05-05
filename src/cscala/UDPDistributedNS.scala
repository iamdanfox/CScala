package cscala

/**
 * Functions as part of a system of nameservers, each maintaining the same state.  
 * 
 * On startup, send an "AnyoneAwake" message out over UDP. Subject to a timeout, listen for "OfferFill"
 * messages. Respond to one of them with a "RequestFill" message.  Wait for a "Fill" message to prepopulate the registry.
 * 
 * Then start responding to `registerForeign` requests.
 * 
 * Also broadcast updates over UDP. 
 * 
 * 
 */
class UDPDistributedNS extends NameServer {

}