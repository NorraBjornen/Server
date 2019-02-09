import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Driver extends Constants implements Runnable{
    private static final String info = "1)\t29.11.18 будет увеличена посуточная оплата до 250 тг/сутки\n\n";
    private static String version = "20";
    private static List<Driver> Drivers = new ArrayList<>();
    private DataBaseDriver DataBase;
    private Socket Socket;
    private String DriverId;

    Driver(Socket socket){
        Socket = socket;
        DataBase = new DataBaseDriver();
    }

    public void run() {
        try(InputStream in = Socket.getInputStream()){

            send("[version~" + version + "~" + info + "]");

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
                    System.out.println("Received message: "+message);
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
        DataBase.setDatePing(DriverId);
        send("[1~1]");
    }

    private void identify(String message) {
        try {
            String command = message.split(COMMAND_DELIMITER)[0];
            String command_text = message.split(COMMAND_DELIMITER)[1];
            DriverId = message.split(COMMAND_DELIMITER)[2];
            switch (command) {
                case "i":
                    DriverId = command_text;
                    String string = DataBase.checkReg(DriverId);
                    message = "[auth~" + string + "]";
                    if(string.equals("ALREADY")){
                        Iterator<Driver> iterator = Driver.getDrivers().iterator();
                        while (iterator.hasNext()) {
                            Driver driver = iterator.next();
                            if (driver.getDriverId().equals(DriverId)) {
                                iterator.remove();
                                System.out.println("Disconnected driver has been removed from the list");
                                break;
                            }
                        }

                        DriverId = command_text;

                        message += "[ping~ok]";

                        Drivers.add(this);
                        System.out.println("Driver added: ALREADY");
                        new Thread(()->{
                            while (!Socket.isClosed()){
                                try{Thread.sleep(60000);} catch (Exception e){}
                                if(DriverId != null){
                                    send("[ping~ok]");
                                }
                            }
                        }).start();
                    }
                    send(message);
                    break;
                case COMMAND_ONLINE:
                    if(!DataBase.isOnline(DriverId)) {
                        String xPos = command_text.split("\\$")[1].split(",")[0];
                        String yPos = command_text.split("\\$")[1].split(",")[1];

                        String geo = xPos + "," + yPos;

                        DataBase.goOnline(DriverId, command_text.split("\\$")[0], command_text.split("\\$")[1]);
                        String msg = DataBase.getDriver(DriverId);
                        msg = "[online~" + msg + "]";
                        for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                            msg = msg + "[geo~" + DriverId + "|" + geo + "]";
                            dispatcher.send(msg);
                        }

                        String balance = DataBase.getBalance(DriverId);
                        String status = DataBase.getStatus(DriverId);

                        balance = "[bal~" + balance + "]";
                        status = "[sts~" + status + "]";
                        message = "[auth~ADDED]" + balance + status + "[ping~ok]";

                        send(message);

                        Drivers.add(this);
                        System.out.println("Driver added: ONLINE");
                        new Thread(()->{
                            while (!Socket.isClosed()){
                                try{Thread.sleep(60000);} catch (Exception e){}
                                if(DriverId != null){
                                    send("[ping~ok]");
                                }
                            }
                        }).start();
                    }
                    break;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                case COMMAND_OFFER:
                    String orderNumber = command_text.split(ELEMENT_DELIMITER)[0];
                    String offeredPrice = command_text.split(ELEMENT_DELIMITER)[1];
                    String time = command_text.split(ELEMENT_DELIMITER)[2];
                    new OfferedPriceOperation(orderNumber, DriverId).addToDataBase(offeredPrice, time);
                    break;
                case COMMAND_UPDATE_PRICE:
                    orderNumber = command_text.split(ELEMENT_DELIMITER)[0];
                    offeredPrice = command_text.split(ELEMENT_DELIMITER)[1];
                    time = command_text.split(ELEMENT_DELIMITER)[2];
                    new OfferedPriceOperation(orderNumber, DriverId).updateOfferedPrice(offeredPrice, time);
                    break;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                case COMMAND_ACCEPT:
                    OrderOperation orderOperation = new OrderOperation(command_text);
                    orderOperation.acceptOrder(DriverId);
                    setStatus("2");
                    break;
                case COMMAND_REFUSE:
                    orderOperation = new OrderOperation(command_text);
                    orderOperation.refuseOrder(DriverId);
                    send("[ref~" + command_text + "]");
                    setStatus("1");
                    break;
                case COMMAND_HURRY:
                    orderOperation = new OrderOperation(command_text);
                    orderOperation.hurry(DriverId);
                    break;
                case COMMAND_COMPLETED:
                    orderOperation = new OrderOperation(command_text);
                    orderOperation.completeOrder(DriverId);
                    send("[comp~" + command_text + "]");
                    setStatus("1");
                    break;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                case COMMAND_GEO:
                    for (Dispatcher dsp : Dispatcher.getDispatchers()) {
                        dsp.send("[" + message + "]");
                    }
                    String position = command_text.split(TOKEN_DELIMITER)[1];
                    DataBase.setGeo(DriverId, position);
                    break;
                case COMMAND_SWITCH_STATUS:
                    String status = DataBase.getStatus(DriverId);
                    if(status.equals("1")){
                        status = "2";
                    } else {
                        status = "1";
                    }
                    setStatus(status);
                    send("[sw~" + status + "]");
                    break;
                case COMMAND_STATUS:
                    status = command_text;
                    setStatus(status);
                    break;
                case COMMAND_RADIUS:
                    String radius = String.valueOf(Double.parseDouble(command_text) * 0.001);
                    DataBase.setRadius(DriverId, radius);
                    for(Dispatcher dispatcher : Dispatcher.getDispatchers()){
                        dispatcher.send("[rds~"+DriverId+"$"+radius+"]");
                    }
                    break;
                case COMMAND_GET_CALLSIGN:
                    String callsign = DataBase.getCallSign(DriverId);
                    send("[getcall~" + callsign + "]");
                    break;
                case COMMAND_GET_BALANCE:
                    send("[getbal~" + String.valueOf(Integer.parseInt(DataBase.getBalance(DriverId)) - 500) + "]");
                    break;
                case COMMAND_SYNCHRONIZATION:
                    if(DriverId == null){
                        break;
                    }

                    String orderNumbersList = command_text.split(TOKEN_DELIMITER)[0];
                    orderNumbersList = orderNumbersList.split(">")[1];
                    String go = command_text.split(TOKEN_DELIMITER)[1];
                    go = go.split(">")[1];

                    List<String> driverOderNumbers = new ArrayList<>();
                    if (!orderNumbersList.equals("NULL")) {
                        if (orderNumbersList.contains("$")) {
                            driverOderNumbers.addAll(Arrays.asList(orderNumbersList.split(ELEMENT_DELIMITER)));
                        } else {
                            driverOderNumbers.add(orderNumbersList);
                        }
                    }

                    List<String> ServerOrderNumber = new ArrayList<>();


                    String serverOrderNumbers = DataBase.getOrderNumbers(DriverId);


                    if (!serverOrderNumbers.equals("NULL")) {
                        if (serverOrderNumbers.contains("$")) {
                            ServerOrderNumber.addAll(Arrays.asList(serverOrderNumbers.split(ELEMENT_DELIMITER)));
                        } else {
                            ServerOrderNumber.add(serverOrderNumbers);
                        }
                    }

                    StringBuilder stringBuilder = new StringBuilder();

                    for (String driverNumber : driverOderNumbers) {
                        boolean toDelete = true;
                        for (String serverNumber : ServerOrderNumber) {
                            if (driverNumber.equals(serverNumber)) {
                                toDelete = false;
                            }
                        }
                        if (toDelete) {
                            stringBuilder.append("[del~" + driverNumber + "]");
                        }
                    }

                    for (String serverNumber : ServerOrderNumber) {
                        boolean toNew = true;
                        for (String driverNumber : driverOderNumbers) {
                            if (driverNumber.equals(serverNumber)) {
                                toNew = false;
                            }
                        }
                        if (toNew) {
                            stringBuilder.append("[new~" + DataBase.getOrderInfo(serverNumber) + "]");
                        }
                    }

                    stringBuilder.append("[getoff~" + DataBase.getOfferedPricesForCommand(DriverId) + "]");

                    String checkMarket = DataBase.checkIfChosen(DriverId);
                    if(checkMarket == null){
                        checkMarket = "NULL";
                    }

                    if (!checkMarket.equals("NULL")) {
                        if (go.equals("no")) {
                            stringBuilder.append("[go~" + checkMarket + "]");
                        }
                    }

                    message = stringBuilder.toString();
                    send(message);

                    break;
                case COMMAND_EXIT:
                    exit();
                    break;
                default:

                    break;
            }
        } catch (Exception e){
            System.out.println(e + " identify() in Driver");
        }
    }

    void setStatus(String status){
        for(Dispatcher dispatcher : Dispatcher.getDispatchers()){
            dispatcher.send("[sts~"+DriverId+"$"+status+"]");
        }
        DataBase.setStatus(DriverId, status);
    }

    private void exit(){
        if(!DataBase.isDriverOnOrder(DriverId)) {
            String balance = String.valueOf(Integer.parseInt(DataBase.exitLine(DriverId)) + 500);
            send("[exit~" + balance + "]");

            for (Dispatcher dsp : Dispatcher.getDispatchers()) {
                dsp.send("[exit~" + DriverId + "]");
            }

            Drivers.remove(this);

            System.out.println("Driver " + DriverId + " exit");
        }
    }

    String getDriverId(){
        return DriverId;
    }

    static List<Driver> getDrivers() {
        return Drivers;
    }

    static Driver getDriver(String driverId){
        for(Driver driver : Drivers){
            if(driver.getDriverId().equals(driverId)){
                return driver;
            }
        }
        return null;
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
