import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderOperation extends Constants implements Runnable{
    private DataBaseOrder DataBase = new DataBaseOrder();
    private String ExecTime = "-";
    private String OrderNumber;

    OrderOperation(String orderNumber){
        OrderNumber = orderNumber;
    }

    OrderOperation(){}

    void addToDataBase(String from, String to, String price, String phone, String description){
        try {
            String dateStart = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
            OrderNumber = DataBase.newOrder(from, to, price, phone, description, dateStart);

            for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                dispatcher.send("[new~" + OrderNumber + "$" + from + "$" + to + "$" + price + "$" + phone + "$" + description + "$" + dateStart + "$-$0$-]");
            }

            for (Driver driver : Driver.getDrivers()) {
                driver.send("[new~" + OrderNumber + "$" + from + "$" + to + "$" + price + "$" + phone + "$" + description + "]");
            }
        } catch (Exception e){
            System.out.println(e + " addToDataBase() in OrderOperation");
        }
    }

    void updateOrder(String from, String to, String price, String phone, String description){
        if(DataBase.IsExist(OrderNumber)) {
            try {
                DataBase.updateOrder(OrderNumber, from, to, price, phone, description);
                String orderInfo = DataBase.getOrderInfo(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[upd~" + orderInfo + "]");
                }

                for (Driver driver : Driver.getDrivers()) {
                    driver.send("[upd~" + OrderNumber + "|" + from + "$" + to + "$" + price + "$" + phone + "$" + description + "]");
                }
            } catch (Exception e){
                System.out.println(e + " updateOrder() in OrderOperation");
            }
        }
    }

    void chose(String driverId){
        if(DataBase.IsExist(OrderNumber)) {
            try {
                DataBase.chose(OrderNumber, driverId);
                String orderInfo = DataBase.getOrderInfo(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[sts~" + driverId + "$2][upd~" + orderInfo + "]");
                }

                Driver driver = Driver.getDriver(driverId);
                if (driver != null) {
                    driver.send("[go~" + OrderNumber + "]");
                }
            } catch (Exception e) {
                System.out.println(e + " chose() in OrderOperation");
            }
        }
    }

    void acceptOrder(String driverId){
        if(DataBase.IsExist(OrderNumber) && !DataBase.isOrderRunning(OrderNumber)) {
            try {
                DataBase.acceptOrder(OrderNumber, driverId);
                startExecution("-");

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[acc~" + driverId + "$" + OrderNumber + "]");
                }

                for (Driver driver : Driver.getDrivers()) {
                    if (!driver.getDriverId().equals(driverId)) {
                        driver.send("[del~" + OrderNumber + "]");
                    }
                }
            } catch (Exception e){
                System.out.println(e + " acceptOrder() in OrderOperation");
            }
        }
    }

    void refuseOrder(String driverId){
        if(DataBase.IsExist(OrderNumber)){
            try {
                DataBase.refuseOrder(OrderNumber, driverId);
                String orderInfo = DataBase.getOrderInfo(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[ref~" + driverId + "$" + OrderNumber + "]");
                }

                String[] elements = orderInfo.split(ELEMENT_DELIMITER);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[upd~" + orderInfo + "]");
                }

                for (Driver driver : Driver.getDrivers()) {
                    driver.send("[new~" + OrderNumber + "$" + elements[1] + "$" + elements[2] + "$" + elements[3] + "$" + elements[4] + "$" + elements[5] + "]");
                }

            } catch (Exception e){
                System.out.println(e + " refuseOrder() in OrderOperation");
            }
        }
    }

    void hurry(String driverId){
        if(DataBase.IsExist(OrderNumber)) {
            try {
                DataBase.hurry(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[hur~" + driverId + "$" + OrderNumber + "]");
                }
            } catch (Exception e){
                System.out.println(e + " hurry() in OrderOperation");
            }
        }
    }

    void ok(){
        if(DataBase.IsExist(OrderNumber)) {
            try {
                String driverId = DataBase.ok(OrderNumber);
                String orderInfo = DataBase.getOrderInfo(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[upd~" + orderInfo + "]");
                }

                Driver driver = Driver.getDriver(driverId);
                if (driver != null) {
                    driver.send("[trp~ok]");
                }

            } catch (Exception e){
                System.out.println(e + " ok() in OrderOperation");
            }
        }
    }

    void completeOrder(String driverId){
        if(DataBase.IsExist(OrderNumber)){
            try {
                DataBase.complete(OrderNumber, driverId);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[comp~" + driverId + "$" + OrderNumber + "]");
                }

                for (Driver driver : Driver.getDrivers()) {
                    if (!driver.getDriverId().equals(driverId)) {
                        driver.send("[del~" + OrderNumber + "]");
                    }
                }
            } catch (Exception e){
                System.out.println(e + " completeOrder() in OrderOperation");
            }
        }
    }

    void takeoff(String driverId){
        if(DataBase.IsExist(OrderNumber)){
            try {
                DataBase.takeoff(OrderNumber, driverId);
                String orderInfo = DataBase.getOrderInfo(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[ref~" + driverId + "$" + OrderNumber + "]");
                }

                String[] elements = orderInfo.split(ELEMENT_DELIMITER);

                for (Driver driver : Driver.getDrivers()) {
                    if(driver.getDriverId().equals(driverId)){
                        driver.send("[takeoff~ok]");
                    } else {
                        driver.send("[new~" + OrderNumber + "$" + elements[1] + "$" + elements[2] + "$" + elements[3] + "$" + elements[4] + "$" + elements[5] + "]");
                    }
                }

                new DataBaseDriver().setStatus(driverId, "1");

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[upd~" + orderInfo + "][sts~" + driverId + "$1]");
                }

            } catch (Exception e){
                System.out.println(e + " takeoff() in OrderOperation");
            }
        }
    }

    void deleteOrder(){
        if(DataBase.IsExist(OrderNumber)){
            try {
                String driverId = DataBase.deleteOrder(OrderNumber);

                for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                    dispatcher.send("[del~" + OrderNumber + "]");
                }

                for (Driver driver : Driver.getDrivers()) {
                    driver.send("[del~" + OrderNumber + "]");
                }

                if(!driverId.equals("-")) {
                    new DataBaseDriver().setStatus(driverId, "1");
                    for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                        dispatcher.send("[sts~" + driverId + "$1]");
                    }
                }

            } catch (Exception e){
                System.out.println(e + " deleteOrder() in OrderOperation");
            }
        } else {
            for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
                dispatcher.send("[del~" + OrderNumber + "]");
            }

            for (Driver driver : Driver.getDrivers()) {
                driver.send("[del~" + OrderNumber + "]");
            }
        }
    }

    void startRunningOrders(){
        List<String> runningOrders = DataBase.getRunningOrders();
        for(String order : runningOrders){
            String[] elements = order.split(ELEMENT_DELIMITER);
            OrderNumber = elements[0];
            new OrderOperation(OrderNumber).startExecution(elements[1]);
        }
    }

    public void run(){
        if(ExecTime.equals("-")) {
            ExecTime = "0";
        }
        System.out.println("EXECUTION STARTED. TIME: " + ExecTime);
        while (DataBase.isOrderRunning(OrderNumber)){
            try{
                for(Dispatcher dispatcher : Dispatcher.getDispatchers()){
                    dispatcher.send("[time~"+OrderNumber+"$"+ExecTime+"]");
                }
                DataBase.updateExecTime(OrderNumber, ExecTime);
                Thread.sleep(60000);
                ExecTime = String.valueOf(Integer.parseInt(ExecTime) + 1);
            } catch (Exception e){
                break;
            }
        }
        System.out.println("EXECUTION STOPPED");
    }

    private void startExecution(String time){
        ExecTime = time;
        new Thread(this).start();
    }

    boolean isFree(String orderNumber, String price){
        return DataBase.isFree(orderNumber, price);
    }
}
