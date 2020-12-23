public class Handshaking {
    String pID;
    String Header;
    byte[] zeroBits;


    public Handshaking(String pID) {
        this.pID = pID;
        this.Header = "P2PFILESHARINGPROJ";
        this.zeroBits = new byte[]{0,0,0,0,0,0,0,0,0,0};
    }

    public String getpID() {
        return pID;
    }

    public void setpID(String pID) {
        this.pID = pID;
    }

    public String getHeader() {
        return Header;
    }

    public void setHeader(String Header) {
        this.Header = Header;
    }


    public byte[] getZeroBits() {
        return zeroBits;
    }

    public void setZeroBits(byte[] zeroBits) {
        this.zeroBits = zeroBits;
    }

    public byte[] HandshakeMessageConstruction()
    {
        byte[] zerobitsHeader = ByteArrayHandler.mergeConstructor(getHeader().getBytes(), getZeroBits());
        byte[] handshake_message = ByteArrayHandler.mergeConstructor(zerobitsHeader, getpID().getBytes());
        return handshake_message;

    }
}