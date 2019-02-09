import java.net.*;
import java.text.SimpleDateFormat;
import java.util.List;


public class Main {
    public static void main(String[] args){

        new OrderOperation().startRunningOrders();

        new Thread(()->{
            while (true){
                try{Thread.sleep(60000);} catch (Exception e){}
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
                java.util.Date now = new java.util.Date();
                String dateNow = sdfDate.format(now);
                String hourStr = dateNow.substring(6, 8);
                String minuteStr = dateNow.substring(8, 10);
                if(minuteStr.equals("00")) {
                    int hour = Integer.parseInt(hourStr);
                    if (hour == 8 || hour == 20) {
                        new DataBaseDispatcher().createReport();
                        for(Dispatcher dispatcher : Dispatcher.getDispatchers()){
                            dispatcher.send("[perc~" + new DataBaseDispatcher().getPercents() + "]");
                        }
                    }
                }
            }
        }).start();

        new Thread(()->{
            DataBaseDriver dataBase = new DataBaseDriver();
            while (true) {
                try {Thread.sleep(120000);} catch (Exception e) {}
                List<String> driverIds = dataBase.getDriversOffline();
                for (String driverId : driverIds) {
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
                }
                if (driverIds.isEmpty()) {
                    //System.out.println("empty");
                }
            }
        }).start();

        new Thread(()->{
            try {
                ServerSocket serverSocket = new ServerSocket(1490);
                System.out.println("Server for drivers started");
                while (true){
                    Socket driver = serverSocket.accept();
                    new Thread(new Driver(driver)).start();
                    System.out.println("Driver connected");
                }
            } catch (Exception e){
                System.out.println(e + " server for drivers");
            }
        }).start();

        new Thread(()->{
            try {
                ServerSocket serverSocket = new ServerSocket(1489);
                System.out.println("Server for dispatcher started");
                while (true){
                    Socket dispatcher = serverSocket.accept();
                    new Thread(new Dispatcher(dispatcher)).start();
                }
            } catch (Exception e){
                System.out.println(e + " server for dispatchers");
            }
        }).start();

        try {
            ServerSocket serverSocket = new ServerSocket(1488);
            System.out.println("Server for clients started");
            while (true){
                Socket client = serverSocket.accept();
                new Thread(new Client(client)).start();
                System.out.println("Client connected");
            }
        } catch (Exception e){
            System.out.println(e + " server for clients");
        }

    }
}
