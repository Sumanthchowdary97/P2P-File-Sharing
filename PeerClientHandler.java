
import java.net.*;
import java.io.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;


public class PeerClientHandler extends Thread {

    Socket reqSocket;
    BufferedOutputStream outputStream;
    BufferedInputStream inputStream;

    boolean isClient;
    String pID;
    byte[] pBitField;


    boolean clntIntrstd = true;
    boolean isClntChkd = true;
    AtomicBoolean procKill = new AtomicBoolean(false);
    Float dwnldSpeed = 1.0f;


    Config cfgObj;
    MessageReader msgReaderObj;
    MessageHandler msgHandler;


    public PeerClientHandler(Socket s, boolean isPeerClnt, String pID, Config cfg) {
        set(s, isPeerClnt, pID, cfg);

    }
    private void set(Socket sock, boolean isClient, String pID, Config cfgObj){
        this.cfgObj = cfgObj;
        this.reqSocket = sock;
        this.isClient = isClient;
        msgReaderObj = new MessageReader();
        msgHandler = new MessageHandler();

        try {
            outputStream = new BufferedOutputStream(reqSocket.getOutputStream());
            outputStream.flush();
            inputStream = new BufferedInputStream(reqSocket.getInputStream());
            if (!isClient) setServer();
            else setClient(pID);
            beginComm();

        } catch (IOException ex) {
            ex.printStackTrace();
            PeerProcess.Logobj.info("Exception: " + ex.toString());
        }
    }

    public void run() {
        try {
            long procTime;
            long totalTime;
            byte[] messageType;
            byte[] messageLen;
            int reqstdIdx;
            int piecesRecvd;

            procTime = 0l;
            totalTime = 0l;
            messageType = new byte[1];
            messageLen = new byte[4];
            reqstdIdx = 0;
            piecesRecvd = 0;

            while (true) {
                if (procKill.get()) {
                    break;
                }

                inputStream.read(messageLen);
                inputStream.read(messageType);
                int ordinal = new BigInteger(messageType).intValue();
                MessageType message_type = MessageType.values()[ordinal];

                if (Objects.equals(message_type,MessageType.choke)) {
                    PeerProcess.Logobj.info("Peer: " + pID + " choked Peer: " + PeerProcess.pprocID);
                    handleChkMsg(reqstdIdx);
                } else if (Objects.equals(message_type,MessageType.unchoke)) {
                    PeerProcess.Logobj.info("Peer: " + pID + " unchoked Peer:" + PeerProcess.pprocID);
                    int pieceInx = msgReaderObj.pieceIndToReq(PeerProcess.b_Field, pBitField, PeerProcess.Reqdpieces);
                    if (pieceInx >= 0) {
                        reqstdIdx = pieceInx;
                        PeerProcess.Reqdpieces[pieceInx].set(true);
                        sendMsg(msgHandler.RequestMsgConstructor(pieceInx));
                        procTime = System.nanoTime();
                    }
                } else if (Objects.equals(message_type,MessageType.interested)) {
                    handleIntrstdMsg();
                } else if (Objects.equals(message_type,MessageType.not_interested)) {
                    handleNotIntrstdMsg();
                } else if (Objects.equals(message_type,MessageType.have)) {
                    handleHaveMsg();
                } else if (Objects.equals(message_type,MessageType.request)) {
                    handleReqMsg();
                } else if (Objects.equals(message_type,MessageType.piece)) {
                    byte[] pInd = new byte[4];
                    inputStream.read(pInd);

                    int pieceIdx;
                    int messageLength;

                    pieceIdx = new BigInteger(pInd).intValue();
                    messageLength = new BigInteger(messageLen).intValue();

                    byte[] pyld = msgReaderObj.readMsgPayload(inputStream, messageLength - 5);

                    PeerProcess.b_Field[pieceIdx / 8] |= 1 << (7 - pieceIdx % 8);

                    int begin;
                    begin=pieceIdx * Config.piece_size;
                    int i = 0;
                    while (i < pyld.length) {
                        PeerProcess.rscPayload[begin + i] = pyld[i];
                        i++;
                    }
                    piecesRecvd++;
                    PeerProcess.Logobj.info("The piece " + pieceIdx + "is downloaded by Peer: " + PeerProcess.pprocID + " from Peer: "
                            + pID + ". The number of pieces with it at this time is : " + piecesRecvd);

                    totalTime = totalTime + (System.nanoTime() - procTime);
                    dwnldSpeed = (float) ((piecesRecvd * Config.piece_size) / totalTime);

                    sendHaveMsgToAllClients(pInd);

                    pieceIdx = msgReaderObj.pieceIndToReq(PeerProcess.b_Field, pBitField, PeerProcess.Reqdpieces);

                    if (pieceIdx >= 0) {
                        reqstdIdx = pieceIdx;
                        PeerProcess.Reqdpieces[pieceIdx].set(true);
                        sendMsg(msgHandler.RequestMsgConstructor(pieceIdx));
                        procTime = System.currentTimeMillis();
                    } else {
                        checkAndPublishNotInterestedMessage();
                    }
                }
            }
        }

        catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    public void TerminatingCondition(boolean stop)  {
        procKill.set(stop);
        if (procKill.get()) {
            PeerProcess.Logobj.info("Socket is being closed");
            try {
                if (!reqSocket.isClosed())
                    reqSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsg(byte[] msg) {
        sendMsgConstructor(msg);
    }

    public void handleChkMsg(int reqIdx) {
        handleChkMsgConstructor(reqIdx);
    }

    public void handleIntrstdMsg() {
        handleIntrstdMsgConstructor();
    }

    public void handleNotIntrstdMsg() {
        handleNotIntrstdMsgConstructor();
    }

    public void handleHaveMsg() {

        handleHaveMsgConstructor();
    }

    public void setClient(String cId) {
        setClientConstructor(cId);
    }

    public void handleReqMsg() {
        handleReqMsgConstructor();
    }

    public void setServer() {
        setServerConstructor();
    }

    public void beginComm() {
        beginCommConstructor();
    }


    public void makeToFile() throws IOException {
        File dir = new File("peer_" + PeerProcess.pprocID);
        if(!dir.exists()) {
            dir.mkdir();
        }
        File file = new File( "peer_" + PeerProcess.pprocID + "/" + Config.file_name);
        if(!file.exists()){
            file.createNewFile();
        }
        FileOutputStream fdata = new FileOutputStream(file);
        fdata.write(PeerProcess.rscPayload);
        fdata.close();
        PeerProcess.Logobj.info("Peer " + PeerProcess.pprocID + "downloaded the whole file.");
    }

    public void sendHaveMsgToAllClients(byte[] pieceIndex) {
        publishHaveMessageToAllClientsConstructor(pieceIndex);
    }

    public void checkAndPublishNotInterestedMessage() throws IOException {
        sendMsg(msgHandler.NotInterestedMsgConstructor());
        if (CheckArray(PeerProcess.b_Field, PeerProcess.fullRsc)) {
            List<PeerClientHandler> pCli = PeerProcess.pcli;
            int i = 0;
            while (i < pCli.size()) {
                PeerClientHandler ct = pCli.get(i);
                ct.sendMsg(ct.msgHandler.NotInterestedMsgConstructor());
                i++;
            }
            makeToFile();
        }
    }

    private void sendMsgConstructor(byte[] messg){
        try {
            outputStream.write(messg);
            outputStream.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void handleChkMsgConstructor(int reqIndex){
        byte idxByte = PeerProcess.b_Field[reqIndex / 8];
        if (((1 << (7 - (reqIndex % 8))) & idxByte) == 0) {
            PeerProcess.Reqdpieces[reqIndex].set(false);
        }
    }

    private void handleIntrstdMsgConstructor(){
        PeerProcess.Logobj.info("Peer " + PeerProcess.pprocID + " got the 'interested' message from peer:" + pID);
        clntIntrstd = true;
    }

    private void handleNotIntrstdMsgConstructor(){
        PeerProcess.Logobj
                .info("Peer " + PeerProcess.pprocID + " got the 'not interested' message from Peer: " + pID);
        clntIntrstd = false;
        isClntChkd = true;
    }

    private void handleHaveMsgConstructor(){
        byte[] pieceIdxbytes;
        int pieceIdx;
        byte idxBytes;
        pieceIdxbytes = msgReaderObj.readMsgPayload(inputStream, 4);
        pieceIdx = new BigInteger(pieceIdxbytes).intValue();
        PeerProcess.Logobj.info(String.format("Peer: %d received the 'have' message from Peer: %s for the piece index:%d", PeerProcess.pprocID, pID, pieceIdx));
        pBitField[pieceIdx / 8] |= (1 << (7 - (pieceIdx % 8)));
        idxBytes = PeerProcess.b_Field[pieceIdx / 8];
        if (((1 << (7 - (pieceIdx % 8))) & idxBytes) == 0) sendMsg(msgHandler.InterestedMsgConstructor());
        else sendMsg(msgHandler.NotInterestedMsgConstructor());
    }



    private void handleReqMsgConstructor(){
        byte[] pyld;
        int startIdx;
        int pieceIdx;

        pyld = msgReaderObj.readMsgPayload(inputStream, 4);
        pieceIdx = ByteArrayHandler.conversionToInt(pyld);
        PeerProcess.Logobj.info(String.format("Peer: %d received the 'request' message from Peer: %s for the pieceIndex: %d", PeerProcess.pprocID, pID, pieceIdx));
        startIdx = pieceIdx * Config.piece_size;
        try {
            byte[] inf;

            if ((Config.file_size - startIdx) < Config.piece_size)
                inf = Arrays.copyOfRange(PeerProcess.rscPayload, startIdx, Config.file_size);

            else inf = Arrays.copyOfRange(PeerProcess.rscPayload, startIdx, startIdx + Config.piece_size);

            if (!isClntChkd) {
                sendMsg(msgHandler.PieceMsgConstructor(pieceIdx, inf));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }



    private void setClientConstructor(String idOfClient){
        this.pID = idOfClient;
        Handshaking hshakemsg;
        hshakemsg=new Handshaking(String.valueOf(PeerProcess.pprocID));
        sendMsg(hshakemsg.HandshakeMessageConstruction());
        msgReaderObj.readHSMsg(inputStream);
        PeerProcess.Logobj.info(String.format("Peer %d starts connecting to Peer:%s", PeerProcess.pprocID, pID));
    }



    private void setServerConstructor(){
        this.pID = msgReaderObj.readHSMsg(inputStream);
        Handshaking hshakemsg;
        hshakemsg=new Handshaking(String.valueOf(PeerProcess.pprocID));
        sendMsg(hshakemsg.HandshakeMessageConstruction());
        PeerProcess.Logobj.info(String.format("Peer: %d starts connecting to Peer: %s", PeerProcess.pprocID, pID));
    }



    private void beginCommConstructor()
    {
        PeerProcess.Logobj.info(String.format("Peer: %d is connected from Peer: %s", PeerProcess.pprocID, pID));
        sendMsg(msgHandler.BitFieldMsgConstructor(PeerProcess.b_Field));
        pBitField = msgReaderObj.readBFPayload(inputStream);
        sendMsg(msgReaderObj.shouldSendIntrMessage(PeerProcess.b_Field, pBitField) ? msgHandler.InterestedMsgConstructor() : msgHandler.NotInterestedMsgConstructor());
    }





    private void publishHaveMessageToAllClientsConstructor(byte[] pieceIndex){
        List<PeerClientHandler> pcli = PeerProcess.pcli;
        int i = 0;
        while (i < pcli.size()) {
            PeerClientHandler ct = pcli.get(i);
            ct.sendMsg(ct.msgHandler.HaveMsgConstructor(pieceIndex));
            i++;
        }
    }


    private static boolean CheckArray(byte[] one, byte[] two) {

        return  Arrays.equals(one,two);
    }
}
