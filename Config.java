import java.io.IOException;
import java.util.ArrayList;

public class Config {
    static Integer port = 8000;
    static ArrayList<RemotePeer> peersList;
    static Integer file_size;
    static String file_name;
    static Integer BytesCount;
    static Integer peerPrefCount;
    static Integer optUnchokeIntr;
    static Integer unchokeIntrPeers;
    static Integer pieceCount;
    static Integer piece_size;


    public Config() throws IOException {
        peersList = new ArrayList<RemotePeer>();
        IOHandler.CommonConfigurationParser();
        IOHandler.getPeerInfo(peersList);
        pieceCount = (file_size % piece_size == 0) ? file_size / piece_size : (file_size / piece_size) + 1;
        if (pieceCount % 8 == 0) BytesCount = pieceCount / 8;
        else BytesCount = (pieceCount / 8) + 1;

    }
}
