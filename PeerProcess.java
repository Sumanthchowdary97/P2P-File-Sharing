
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static java.lang.System.*;

public class PeerProcess {

    static List<PeerClientHandler> pcli = Collections.synchronizedList(new ArrayList<>());
    static PeerClientHandler optmstcUnchkdNeighbour;
    static byte[] b_Field, rscPayload, fullRsc;
    static AtomicBoolean[] Reqdpieces;
    static Logger Logobj;

    ScheduledExecutorService eventschdlr = Executors.newScheduledThreadPool(3);

    Integer port = 8000;
    static Integer pprocID;
    static ServerSocket serverSocket;

    public static void main(String[] args) throws Exception {

        PeerProcess pprocObj = new PeerProcess();
        pprocID = Integer.parseInt(args[0]);

        Config cfgObj;
        cfgObj = new Config();
        Logobj = ProcessLogger.Loggermethod(pprocID);

        List<RemotePeer> connPeers = new ArrayList<>();
        List<RemotePeer> yTConnPeers = new ArrayList<>();

        boolean hasFileAvl = false;
        for (RemotePeer rpeerinf : Config.peersList) {
            if (Integer.parseInt(rpeerinf.peerId) < pprocID) {
                connPeers.add(rpeerinf);
            } else if (Integer.parseInt(rpeerinf.peerId) == pprocID) {
                pprocObj.port = Integer.parseInt(rpeerinf.peerPort);
                if (rpeerinf.peerHasFile.equals("1"))
                    hasFileAvl = true;
            } else {
                yTConnPeers.add(rpeerinf);
            }
        }
        b_Field = new byte[Config.BytesCount];
        Reqdpieces = new AtomicBoolean[Config.pieceCount];
        Arrays.fill(Reqdpieces, new AtomicBoolean(false));
        rscPayload = new byte[Config.file_size];
        fullRsc = new byte[Config.BytesCount];
        setVariables(hasFileAvl, Config.pieceCount);
        hearToConnPeers(connPeers, cfgObj);
        serverSocket = new ServerSocket(pprocObj.port);
        Logobj.info("Socket Opened on port: " + pprocObj.port);
        lstnToNextPeers(yTConnPeers, cfgObj);
        selectOptmstcUnchkdNeighbour();
        beginTaskschdlr(pprocObj);
    }

    public static void readRscFile() throws IOException {
        try {
            File rsc = new File("peer_" + PeerProcess.pprocID + "/" + Config.file_name);
            FileInputStream fpayload = new FileInputStream(rsc);
            fpayload.read(rscPayload);
            fpayload.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static void setVariables(boolean fileAvl, int pieces) throws IOException {
        Arrays.fill(fullRsc, (byte) 255);
        if (fileAvl) {
            readRscFile();
            Arrays.fill(b_Field, (byte) 255);
            if (pieces % 8 != 0) {
                int end = pieces % 8;
                b_Field[b_Field.length - 1] = 0;
                fullRsc[b_Field.length - 1] = 0;
                while (end != 0) {
                    b_Field[b_Field.length - 1] |= (1 << (8 - end));
                    fullRsc[b_Field.length - 1] |= (1 << (8 - end));
                    end--;
                }
            }
        } else {
            if (pieces % 8 != 0) {
                int end = pieces % 8;
                fullRsc[b_Field.length - 1] = 0;
                while (end != 0) {
                    fullRsc[b_Field.length - 1] |= (1 << (8 - end));
                    end--;
                }
            }
        }
    }

    public static void hearToConnPeers(List<RemotePeer> connPeers, Config cfgObj) {

        for (RemotePeer peerInf : connPeers) {
            try {
                PeerClientHandler cli = new PeerClientHandler(new Socket(peerInf.peerAddress, Integer.parseInt(peerInf.peerPort)),
                        true, peerInf.peerId, cfgObj);

                cli.start();
                pcli.add(cli);
                Logobj.info("Peer " + pprocID + " makes a connection to Peer " + peerInf.peerId + ".");
            } catch (Exception ex) {
                ex.printStackTrace();
                Logobj.info(ex.toString());
            }

        }

    }

    public static void selectOptmstcUnchkdNeighbour() {
        List<PeerClientHandler> intrstdAndChkdNeighbour = new ArrayList<>();

        for (PeerClientHandler pcli : pcli) {
            if (pcli.clntIntrstd && pcli.isClntChkd) {
                intrstdAndChkdNeighbour.add(pcli);
            }
        }

        if (intrstdAndChkdNeighbour.isEmpty()) {
            optmstcUnchkdNeighbour = null;
        } else {
            optmstcUnchkdNeighbour = intrstdAndChkdNeighbour
                    .get(new Random().nextInt(intrstdAndChkdNeighbour.size()));
        }
    }

    public static void lstnToNextPeers(List<RemotePeer> yTConnPeers, Config cfgObj) {
        try {
            for (RemotePeer rpeerInfObj : yTConnPeers) {
                Runnable peerConn = () -> {
                    try {
                        PeerClientHandler nextPeer = new PeerClientHandler(serverSocket.accept(), false, rpeerInfObj.peerId,
                                cfgObj);
                        Logobj.info(
                                "Peer " + pprocID + " is connected from Peer " + rpeerInfObj.peerId + ".");
                        pcli.add(nextPeer);
                        nextPeer.start();
                    } catch (IOException e) {
                        Logobj.info(e.getMessage());
                    }
                };
                new Thread(peerConn).start();
            }
        } catch (Exception ex) {
            Logobj.info("Exception while listening to future peers :" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void beginPrefNeighbourScheduler(int x, int y) {
        Runnable detectprefNeighbours = () -> refPrefNeighbours(x);
        eventschdlr.scheduleAtFixedRate(detectprefNeighbours, y, y, TimeUnit.SECONDS);
    }

    public void refPrefNeighbours(int PrefNeighboursCount) {
        try {
            pcli.sort((ct1, ct2) -> ct2.dwnldSpeed.compareTo(ct1.dwnldSpeed));
            int cntr = 0;
            List<String> preflist = new ArrayList<>();

            for (PeerClientHandler client : pcli) {
                if (client.clntIntrstd) {
                    if (cntr < PrefNeighboursCount) {
                        if (client.isClntChkd) {
                            client.isClntChkd = false;
                            client.sendMsg(client.msgHandler.UnChokeMsgConstructor());
                        }
                        preflist.add(client.pID);
                    } else {

                        if (!client.isClntChkd && client != optmstcUnchkdNeighbour) {
                            client.isClntChkd = true;
                            client.sendMsg(client.msgHandler.ChokeMsgConstructor());
                        }
                    }

                    cntr++;
                }
            }
            Logobj.info("Peer " + pprocID + " with preferred neighbours:" + preflist);
        } catch (Exception e) {
            Logobj.info(e.toString());
        }
    }

    public void BeginOptmstcPrefScheduler(int m) {

        Runnable detectoptmstcprefNeigh = () -> {
            try {
                refoptmstcprenei();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        eventschdlr.scheduleAtFixedRate(detectoptmstcprefNeigh, m, m, TimeUnit.SECONDS);
    }

    public void refoptmstcprenei() throws Exception {

        AtomicReference<ArrayList<PeerClientHandler>> interestedAndChokedNeighbour;
        interestedAndChokedNeighbour = new AtomicReference<>(new ArrayList<>());

        pcli.stream().filter(peerClient -> peerClient.clntIntrstd && peerClient.isClntChkd).forEachOrdered(peerClient -> interestedAndChokedNeighbour.get().add(peerClient));

        if (!interestedAndChokedNeighbour.get().isEmpty()) {
            if (optmstcUnchkdNeighbour != null) {
                optmstcUnchkdNeighbour.isClntChkd = true;
                optmstcUnchkdNeighbour.sendMsg(optmstcUnchkdNeighbour.msgHandler.ChokeMsgConstructor());
            }
            optmstcUnchkdNeighbour = interestedAndChokedNeighbour.get()
                    .get(new Random().nextInt(interestedAndChokedNeighbour.get().size()));
            optmstcUnchkdNeighbour.isClntChkd = false;
            optmstcUnchkdNeighbour.sendMsg(optmstcUnchkdNeighbour.msgHandler.UnChokeMsgConstructor());

            if (optmstcUnchkdNeighbour != null)
                Logobj.info("Peer: " + pprocID + " has the optimistically unchoked neighbor Peer: "
                        + optmstcUnchkdNeighbour.pID);
        } else {
            if (optmstcUnchkdNeighbour != null) {
                if (!optmstcUnchkdNeighbour.isClntChkd) {
                    optmstcUnchkdNeighbour.isClntChkd = true;
                    optmstcUnchkdNeighbour.sendMsg(optmstcUnchkdNeighbour.msgHandler.ChokeMsgConstructor());
                }
                optmstcUnchkdNeighbour = null;
                if (optmstcUnchkdNeighbour != null)
                    Logobj.info("Peer: " + pprocID + " has the optimistically unchoked neighbor Peer: "
                            + optmstcUnchkdNeighbour.pID);
            } else {
                if (optmstcUnchkdNeighbour != null)
                    Logobj.info("Peer: " + pprocID + " has the optimistically unchoked neighbor Peer: "
                            + optmstcUnchkdNeighbour.pID);
            }
        }

    }

    public void chkwholeFile() {
        Runnable seeForPFile;
        seeForPFile = this::run;
        ScheduledFuture<?> scheduledFuture = eventschdlr.scheduleAtFixedRate(seeForPFile, 10, 5, TimeUnit.SECONDS);
    }

    public boolean isAllClientsRcvd() {
        AtomicBoolean hasfileRcvd;
        hasfileRcvd = new AtomicBoolean(true);
        ListIterator<PeerClientHandler> currPeer;
        currPeer = (ListIterator<PeerClientHandler>) pcli;
        while (currPeer.hasNext()) {
            PeerClientHandler i = currPeer.next();
            if (!CheckArray(b_Field, fullRsc)) {
                Logobj.info("Peer " + i.pID + " yet to receive the full file.");
                hasfileRcvd.set(false);
                break;
            }
        }
        return hasfileRcvd.get();
    }

    public void chkAndStopSoc(boolean isSendingSuccess) throws IOException {
        Logobj.info("File Transfer Success Status : " + isSendingSuccess);
        if (isSendingSuccess && CheckArray(b_Field, fullRsc)) {
            ListIterator<PeerClientHandler> currPeer;
            currPeer = (ListIterator<PeerClientHandler>) pcli;
            while (currPeer.hasNext()) {
                PeerClientHandler i = currPeer.next();
                i.TerminatingCondition(true);
            }
            eventschdlr.shutdown();
            try {
                if (serverSocket.isClosed() == false) serverSocket.close();
            } finally {
                Logobj.info("peer : " + pprocID + " shutdown");
                exit(0);
            }
        }
    }

    public static boolean CheckArray(byte[] one, byte[] two) {

        return  Arrays.equals(one,two);
    }

    public static void beginTaskschdlr(PeerProcess peerProc) {
        peerProc.beginPrefNeighbourScheduler(Config.peerPrefCount, Config.unchokeIntrPeers);
        peerProc.BeginOptmstcPrefScheduler(Config.optUnchokeIntr);
        peerProc.chkwholeFile();
    }

    private void run() {
        try {
            PeerProcess.this.chkAndStopSoc(PeerProcess.this.isAllClientsRcvd());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}