public class MessageHandler {

    public byte[] InterestedMsgConstructor() {
        return InterestedMsg();
    }
    public byte[] NotInterestedMsgConstructor() {
        return NotInterestedMsg();
    }

    public byte[] ChokeMsgConstructor() {
        return ChokeMsg();
    }

    public byte[] UnChokeMsgConstructor() {
        return  UnchokeMsg();
    }

    public byte[] HaveMsgConstructor(byte[] pieceIndex) {
        return HaveMsg(pieceIndex);
    }

    public byte[] BitFieldMsgConstructor(byte[] payload) {
        return BitFieldMsg(payload);
    }

    public byte[] PieceMsgConstructor(int idx, byte[] payload) {
        return PieceMsg(idx,payload);
    }

    public byte[] RequestMsgConstructor(int index) {
        return RequestMsg(index);
    }

    private byte[] InterestedMsg(){
        PeerProcess.
                Logobj.info("INTERESTED Message construction in progress");
        byte b = ByteArrayHandler.intToByteArray(MessageType.interested.ordinal())[3];
        byte[] length = ByteArrayHandler.intToByteArray(1);
        byte[] response = ByteArrayHandler.mergewithByteConstructor(length, b);
        return response;
    }

    private byte[] NotInterestedMsg(){
        PeerProcess.
                Logobj.info("NOTINTERESTED Message construction in progress");
        byte b = ByteArrayHandler.intToByteArray(MessageType.not_interested.ordinal())[3];
        byte[] length = ByteArrayHandler.intToByteArray(1);
        byte[] response = ByteArrayHandler.mergewithByteConstructor(length, b);
        return response;
    }

    private byte[] ChokeMsg(){
        PeerProcess.
                Logobj.info("CHOKE Message construction in progress");
        byte b = ByteArrayHandler.intToByteArray(MessageType.choke.ordinal())[3];
        byte[] length = ByteArrayHandler.intToByteArray(1);
        byte[] response = ByteArrayHandler.mergewithByteConstructor(length, b);
        return response;
    }

    private byte[] UnchokeMsg(){
        PeerProcess.
                Logobj.info("UNCHOKE Message construction in progress");
        byte b = ByteArrayHandler.intToByteArray(MessageType.unchoke.ordinal())[3];
        byte[] length = ByteArrayHandler.intToByteArray(1);
        byte[] response = ByteArrayHandler.mergewithByteConstructor(length, b);
        return response;
    }

    private byte[] RequestMsg(int idx){
        PeerProcess.
                Logobj.info("REQUEST Message construction in progress");
        OriginalMessage orgnlMsg = new OriginalMessage(MessageType.request, ByteArrayHandler.intToByteArray(idx));
        return orgnlMsg.OriginalMessageInBytes();
    }

    private byte[] HaveMsg(byte[] pieceIdx){
        PeerProcess.
                Logobj.info("HAVE message construction in progress");
        byte[] length = ByteArrayHandler.intToByteArray(5);
        byte b = ByteArrayHandler.intToByteArray(MessageType.have.ordinal())[3];
        byte[] response = ByteArrayHandler.mergeConstructor(ByteArrayHandler.mergewithByteConstructor(length, b), pieceIdx);
        return response;
    }



    private byte[] BitFieldMsg(byte[] payload){
        PeerProcess.
                Logobj.info("BITFIELD Message construction in progress");
        OriginalMessage orgnlMsg = new OriginalMessage(MessageType.bitfield, payload);
        return orgnlMsg.OriginalMessageInBytes();
    }


    private byte[] PieceMsg(int idx,byte[] payload){
        PeerProcess.
                Logobj.info("PIECE Message construction in progress");
        OriginalMessage orgnlMsg = new OriginalMessage(MessageType.piece,
                ByteArrayHandler.mergeConstructor(ByteArrayHandler.intToByteArray(idx), payload));
        return orgnlMsg.OriginalMessageInBytes();
    }

}