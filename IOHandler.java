import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;



public class IOHandler {
    private static BufferedReader bReader;

    public static void getPeerInfo(List<RemotePeer> result) throws IOException {
        FileReader fReader = new FileReader("PeerInfo.cfg");
        bReader = new BufferedReader(fReader);
        String line = bReader.readLine();
        while (line != null) {
            String[] values = line.split("\\s+");
            result.add(new RemotePeer(values[0], values[1], values[2], values[3]));
            line = bReader.readLine();
        }
    }

    public static void CommonConfigurationParser() throws IOException {
        FileReader fReader = new FileReader("Common.cfg");
        bReader = new BufferedReader(fReader);
        String line = bReader.readLine();
        while (line != null) {
            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {

                case "UnchokingInterval":
                    Config.unchokeIntrPeers = Integer.parseInt(tokens[1]);
                    break;

                case "OptimisticUnchokingInterval":
                    Config.optUnchokeIntr = Integer.parseInt(tokens[1]);
                    break;

                case "NumberOfPreferredNeighbors":
                    Config.peerPrefCount = Integer.parseInt(tokens[1]);
                    break;

                case "PieceSize":
                    Config.piece_size = Integer.parseInt(tokens[1]);
                    break;

                case "FileName":
                    Config.file_name = tokens[1];
                    break;

                case "FileSize":
                    Config.file_size = Integer.parseInt(tokens[1]);
                    break;


            }
            line = bReader.readLine();
        }

    }

}