
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


public class MessageReader
{
    public String readHSMsg(InputStream ip) {
        try {
            isValidHeader(ip);
            ip.read(new byte[10]);
            byte[] peerId = new byte[4];
            ip.read(peerId);
            return new String(peerId);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "";
    }
    public void isValidHeader(InputStream ip) throws IOException {
        byte[] ipheader;
        ipheader = new byte[18];
        ip.read(ipheader);
        if (!("P2PFILESHARINGPROJ".equals(new String(ipheader))))
            throw new RuntimeException("Header Mismatch");
    }
    public int pieceIndToReq(byte[] pProcBF, byte[] pCliBF,
                             AtomicBoolean[] wantBF) {
        byte[] need = new byte[pProcBF.length];
        byte[] temp = new byte[pProcBF.length];
        Arrays.fill(temp, (byte)0);
        byte[] reqBFByte = ByteArrayHandler.boolArraytoByteArray(wantBF, temp);
        byte[] free = new byte[pProcBF.length];
        ArrayList<Integer> list = new ArrayList<Integer>();
        int i = 0;
        while (i < pProcBF.length) {
            free[i] = (byte) (pProcBF[i] & reqBFByte[i]);
            need[i] = (byte) ((free[i] ^ pCliBF[i]) & ~free[i]);

            if (need[i] != 0)
                list.add(i);
            i++;
        }
        return getPieceIndex(list, need);
    }

    public synchronized byte[] readBFPayload(InputStream ip) {
        byte[] clientBF = new byte[0];
        try {
            byte[] msgLength = new byte[4];
            ip.read(msgLength);
            clientBF = readBitfieldPL(ip, ByteArrayHandler.conversionToInt(msgLength) - 1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return clientBF;
    }

    public byte[] readMsgPayload(InputStream ip, int payloadsize) {
        byte[] res = new byte[0];
        int sizeToRead = payloadsize;
        try {
            while (sizeToRead != 0) {
                int bytesAvailable = ip.available();
                int read = 0;
                if (payloadsize > bytesAvailable) read = bytesAvailable;
                else read = payloadsize;

                byte[] r;
                r = new byte[read];
                if (read != 0) {
                    ip.read(r);
                    res = ByteArrayHandler.mergeConstructor(res, r);
                    sizeToRead = sizeToRead - read;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return res;
    }


    public boolean shouldSendIntrMessage(byte[] peerProcessBitField, byte[] peerClientBitField) {
        byte isByteSet;
        int startInd = 0;
        while (startInd < peerProcessBitField.length) {
            isByteSet = (byte) (~peerProcessBitField[startInd] & peerClientBitField[startInd]);
            if (isByteSet != 0) {
                return true;
            }
            startInd++;
        }
        return false;
    }

    public int getRandomInteger(int high) {
        return new Random().nextInt(high);
    }

    public int getrandonSetBit(byte msg) {
        int bitInd,i;
        bitInd = getRandomInteger(8);
        i = 0;
        while (i < 8) {
            if ((msg & (1 << i)) != 0) {
                bitInd = i;
                break;
            }
            i++;
        }
        return bitInd;
    }

    public int getPieceIndex(List<Integer> list, byte[] need) {
        if(list.isEmpty())
            return -1;
        int bitInd;
        int byteInd;
        byte rand;
        byteInd = list.get(getRandomInteger(list.size()));
        rand = need[byteInd];
        bitInd = getrandonSetBit(rand);
        return (byteInd << 3) + (7-bitInd);
    }

    public byte[] readBitfieldPL(InputStream ins, int length) throws IOException {
        byte val;
        byte[] type;
        byte[] clientBitField;
        clientBitField = new byte[length];
        type = new byte[1];
        ins.read(type);
        val = ByteArrayHandler.intToByteArray(MessageType.bitfield.ordinal())[3];
        if(type[0] == val) ins.read(clientBitField);
        return clientBitField;
    }

}