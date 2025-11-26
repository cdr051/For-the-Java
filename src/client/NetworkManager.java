package client;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> onMessageReceived;

    public void setOnMessageReceived(Consumer<Message> listener) { this.onMessageReceived = listener; }

    public boolean connect(String ip, int port, String playerName) {
        try {
            socket = new Socket(ip, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(playerName);
            out.flush();
            new Thread(this::receive).start();
            return true;
        } catch (Exception e) { return false; }
    }

    private void receive() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (onMessageReceived != null) {
                    if (obj instanceof Message) onMessageReceived.accept((Message) obj);
                    else if (obj instanceof String) onMessageReceived.accept(new Message(Message.Type.CHAT, (String) obj));
                }
            }
        } catch (Exception e) {}
    }

    public void send(Message msg) { try { out.writeObject(msg); out.flush(); } catch (IOException e) {} }
    public void sendChat(String text) { try { out.writeObject(text); out.flush(); } catch (IOException e) {} }
}