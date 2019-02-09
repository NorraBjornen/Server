import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Dispatcher extends Constants implements Runnable{
    private static List<Dispatcher> Dispatchers = new ArrayList<>();
    private DataBaseDispatcher DataBase = new DataBaseDispatcher();
    private Socket Socket;

    Dispatcher(Socket socket){
        Socket = socket;
        Dispatchers.add(this);
        System.out.println("Connected: Dispatcher " + String.valueOf(Dispatchers.indexOf(this) + 1));
    }

    static List<Dispatcher> getDispatchers() {
        return Dispatchers;
    }

    void send(String message){
        try{
            OutputStream output = Socket.getOutputStream();
            output.write(message.getBytes("cp1251"));
            //output.write(message.getBytes(StandardCharsets.UTF_8));
            output.flush();
            System.out.println(message + " sent");
        }
        catch (Exception e){
            //System.out.println(e + " send() in Dispatcher");
        }
    }

    public void run() {
        try(InputStream in = Socket.getInputStream()){

            byte[] buf = new byte[32 * 1024];
            int readBytes;

            readBytes = in.read(buf);
            String message = new String(buf, 0, readBytes);
            System.out.println("Received message: "+message);
            adapt(message);

            String drivers = "[getdrv~" + DataBase.getDrivers() + "]";
            String orders = "[get~" + DataBase.getOrdersForCommand() + "]";
            String offeredPrices = "[getoff~" + DataBase.getOfferedPricesForCommand() + "]";
            String percents = "[perc~" + DataBase.getPercents() + "]";

            message = drivers + orders + offeredPrices + percents;

            send(message);

            new Thread(()->{
                while (!Socket.isClosed()){
                    try{Thread.sleep(2000);} catch (Exception e){System.out.println("pinging to dispatcher thread interrupted");}
                    send("[ping~ok]");
                }
            }).start();

            while (!Socket.isClosed()){
                try{
                    readBytes = in.read(buf);
                    message = new String(buf, 0, readBytes);
                    System.out.println("Received message: "+message);
                    adapt(message);
                } catch (Exception e){
                    Socket.close();
                }
            }
            System.out.println("Dispatcher "+(Dispatchers.indexOf(this) + 1)+" disconnected");
            Dispatchers.remove(this);
        } catch (Exception e){
            System.out.println(e + " dispatcher");
        }
    }

    private void adapt(String message){
        String[] messages = message.split("]");
        for(String str : messages) {
            str = str.substring(1);
            identify(str);
        }
        send("[clr~ok]");
    }

    private void identify(String message) {
        try {
            String command = message.split(COMMAND_DELIMITER)[0];
            String command_text = message.split(COMMAND_DELIMITER)[1];
            switch (command) {
                case COMMAND_NEW:
                    String elements[] = command_text.split(ELEMENT_DELIMITER);
                    String from = elements[0];
                    String to = elements[1];
                    String price = elements[2];
                    String phone = elements[3];
                    String description = elements[4];
                    if(!to.equals("-") && !from.equals("КОСТАНАЙ,") && !phone.equals("-")) {
                        new OrderOperation("0").addToDataBase(from, to, price, phone, description);
                    }
                    break;
                case COMMAND_UPDATE:
                    String id = command_text.split(TOKEN_DELIMITER)[0];
                    String text = command_text.split(TOKEN_DELIMITER)[1];
                    elements = text.split(ELEMENT_DELIMITER);
                    from = elements[0];
                    to = elements[1];
                    price = elements[2];
                    phone = elements[3];
                    description = elements[4];
                    OrderOperation orderOperation = new OrderOperation(id);
                    orderOperation.updateOrder(from, to, price, phone, description);
                    break;
                case COMMAND_DELETE:
                    id = command_text;
                    orderOperation = new OrderOperation(id);
                    orderOperation.deleteOrder();
                    break;
                case COMMAND_GO:
                    String orderNumber = command_text.split(ELEMENT_DELIMITER)[0];
                    String driverId = command_text.split(ELEMENT_DELIMITER)[1];
                    orderOperation = new OrderOperation(orderNumber);
                    orderOperation.chose(driverId);
                    break;
                case "disp":
                    DataBase.updateName(command_text);
                    break;
                case "trp":
                    orderOperation = new OrderOperation(command_text.split(ELEMENT_DELIMITER)[1]);
                    orderOperation.ok();
                    break;
                case "takeoff":
                    orderOperation = new OrderOperation(command_text.split(ELEMENT_DELIMITER)[0]);
                    orderOperation.takeoff(command_text.split(ELEMENT_DELIMITER)[1]);
                    break;
                case "deldrv":
                    DataBaseDriver dataBase = new DataBaseDriver();
                    driverId = command_text;
                    if(!dataBase.isDriverOnOrder(driverId)) {
                        String balance = String.valueOf(Integer.parseInt(dataBase.exitLine(driverId)) + 500);

                        for (Dispatcher dsp : Dispatcher.getDispatchers()) {
                            dsp.send("[exit~" + driverId + "]");
                        }

                        Driver driver = Driver.getDriver(driverId);
                        if (driver != null) {
                            driver.send("[exit~" + balance + "]");
                            Driver.getDrivers().remove(driver);
                        }

                        System.out.println("Driver " + driverId + " exit");
                    }
                    break;
                default:

                    break;
            }
        } catch (Exception e){
            System.out.println(e + " identify() in Dispatcher");
        }
    }
}
