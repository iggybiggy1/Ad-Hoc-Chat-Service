package client;

public enum MessageType {
    FREE,   // no collision
    BUSY,   // collision
    DATA,   // payload, headers, network setup
    SENDING, // start of transmission
    DONE_SENDING, // end of transmission
    DATA_SHORT, // ACKs, ICMP response
    END,    // end the connection (like FIN in TCP)
    HELLO,   // used for connection setup, ICMP
    LINKSTATE // used for link state information
}