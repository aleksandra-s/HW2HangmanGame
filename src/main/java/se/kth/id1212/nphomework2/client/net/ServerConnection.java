/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.nphomework2.client.net;

/**
 *
 * @author aleks_uuia3ly
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
/*import se.kth.id1212.nio.textprotocolchat.common.Constants;
import se.kth.id1212.nio.textprotocolchat.common.MessageException;
import se.kth.id1212.nio.textprotocolchat.common.MessageSplitter;
import se.kth.id1212.nio.textprotocolchat.common.MsgType;*/

/**
 * Manages all communication with the server, all operations are non-blocking.
 */
public class ServerConnection implements Runnable {
    private static final String FATAL_COMMUNICATION_MSG = "Lost connection.";
    private static final String FATAL_DISCONNECT_MSG = "Could not disconnect, will leave ungracefully.";

    private final ByteBuffer msgFromServer = ByteBuffer.allocateDirect(8192); //Max message length 8192
    private final Queue<ByteBuffer> messagesToSend = new ArrayDeque<>();
    //private final MessageSplitter msgSplitter = new MessageSplitter();
    //private final List<CommunicationListener> listeners = new ArrayList<>();
    private InetSocketAddress serverAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private boolean connected;
    private volatile boolean timeToSend = false;

    /**
     * The communicating thread, all communication is non-blocking. First, server connection is
     * established. Then the thread sends messages submitted via one of the <code>send</code>
     * methods in this class and receives messages from the server.
     */
    @Override
    public void run() {
        try {
            initConnection();
            initSelector();

            while (connected || !messagesToSend.isEmpty()) {
                if (timeToSend) {
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    timeToSend = false;
                }

                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        completeConnection(key);
                    } else if (key.isReadable()) {
                        recvFromServer(key);
                    } else if (key.isWritable()) {
                        sendToServer(key);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(FATAL_COMMUNICATION_MSG);
        }
        try {
            doDisconnect();
        } catch (IOException ex) {
            System.err.println(FATAL_DISCONNECT_MSG);
        }
    }
    
     public void disconnect() throws IOException {
        connected = false;
        //sendMsg(MsgType.DISCONNECT.toString(), null);
        //IMPLEMENT OWN SENDMSG METHOD TO SEND STUFF TO SERVER
    }
    
    private void doDisconnect() throws IOException {
        socketChannel.close();
        socketChannel.keyFor(selector).cancel();
        //notifyDisconnectionDone();
    }
    
     private void initSelector() throws IOException {
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void initConnection() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(serverAddress);
        connected = true;
    }
    
    private void sendToServer(SelectionKey key) throws IOException {
        ByteBuffer msg;
        synchronized (messagesToSend) {
            while ((msg = messagesToSend.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return;
                }
                messagesToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }
    
    private void completeConnection(SelectionKey key) throws IOException {
        socketChannel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
                //notifyConnectionDone(remoteAddress);
        } catch (IOException couldNotGetRemAddrUsingDefaultInstead) {
                //notifyConnectionDone(serverAddress);
        }
    }
    
    private void recvFromServer(SelectionKey key){
        //have to figure out how to implement this best
    }
}
