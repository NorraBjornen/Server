import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Client extends Constants implements Runnable{
    private String ClientId;
    private Socket Socket;

    Client(Socket socket){
        Socket = socket;
    }

    public void run() {
        try(InputStream in = Socket.getInputStream()){

            send("[connected~ok]");

            byte[] buf = new byte[32 * 1024];
            int readBytes;
            String message;

            new Thread(()->{
                while (!Socket.isClosed()){
                    try{Thread.sleep(10000);}catch (Exception e){}
                    send("[1~1]");
                }
            }).start();

            while (!Socket.isClosed()){
                try{
                    readBytes = in.read(buf);
                    message = new String(buf, 0, readBytes);
                    //System.out.println("Received message: "+message);
                    adapt(message);
                } catch (Exception e){
                    Socket.close();
                }
            }
        } catch (Exception e){
            System.out.println(e + " driver");
        }
    }

    private void adapt(String message){
        String[] messages = message.split("]");
        for(String str : messages) {
            str = str.substring(1);
            identify(str);
        }
    }

    private void identify(String message) {
        try {
            String command = message.split(COMMAND_DELIMITER)[0];
            String command_text = message.split(COMMAND_DELIMITER)[1];
            ClientId = message.split(COMMAND_DELIMITER)[2];
            switch (command) {
                case "msg":
                    System.out.println(command_text);
                    break;
                case "i":
                        send("[auth~" + new DataBaseClient().checkReg(ClientId) + "]");
                    break;
                case "numb":
                    SMSCSender sd= new SMSCSender();
                    sd.sendSms("+7"+command_text, "Ваш пароль: " + new DataBaseClient().registerNumber(ClientId, command_text), 1, "", "", 0, "", "");
                    sd.getBalance();
                    send("[auth~" + new DataBaseClient().checkReg(ClientId) + "]");
                    break;
                case "reg":
                    if(new DataBaseClient().confirm(ClientId, command_text))
                        send("[auth~OK]");
                    else
                        send("[info~Неверный код]");
                    break;
                default:

                    break;
            }
        } catch (Exception e){
            System.out.println(e + " identify() in Driver");
        }
    }

    void send(String message){
        try{
            OutputStream output = Socket.getOutputStream();
            output.write(message.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
        catch (Exception e){
            //System.out.println(e + " send() in Driver");
        }
    }
}
