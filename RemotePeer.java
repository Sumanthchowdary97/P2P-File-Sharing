public class RemotePeer {
    public String peerId;
    public String peerAddress;
    public String peerPort;
    public String peerHasFile;

    public RemotePeer(String pId, String pAddress, String pPort, String pHasFile) {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
        peerHasFile = pHasFile;
    }
}