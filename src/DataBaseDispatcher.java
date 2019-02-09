import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DataBaseDispatcher {
    private static String url = "jdbc:mysql://127.0.0.1:3306/kek?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static String  username = "root";
    private static String  password = "deepundo99";

    private void query(String sql) throws Exception{
        Connection Connection = DriverManager.getConnection(url, username, password);
        Statement statement = Connection.createStatement();
        statement.executeUpdate(sql);
        statement.close();
        Connection.close();
    }

    String getDrivers(){
        List<String> drivers = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM dc INNER JOIN online ON dc.DriverId=online.DriverId " +
                    "INNER JOIN drivers ON dc.DriverId=drivers.DriverId";

            Request request = new Request(sql);

            String line;
            while (request.next()){
                line = request.getString("DriverId") + "$" +
                        request.getString("CallSign") + "$" +
                        request.getString("CarNumber") + "$" +
                        request.getString("Colour") + " " + request.getString("Model") + "$" +
                        request.getString("Status") + "$" +
                        request.getString("Position") +"$" +
                        request.getString("DateStatus");

                drivers.add(line);
            }

            for(String token : drivers) result = result + token + "|";
            if(drivers.isEmpty()){
                result = "NULL";
            } else {
                result = result.substring(0, result.length() - 1);
            }
        } catch (Exception e){
            System.out.println(e + " getDrivers() in DataBaseDispatcher");
        }
        return result;
    }

    String getOrdersForCommand(){
        List<String> orderList = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM journal_operative";

            Request request = new Request(sql);

            String line;
            while (request.next()){
                line = String.valueOf(request.getInt("id")) + "$" +
                        request.getString("FromW") + "$" +
                        request.getString("ToW") + "$" +
                        request.getString("Price") + "$" +
                        request.getString("Phone") + "$" +
                        request.getString("Description") + "$" +
                        request.getString("DateStart") + "$" +

                        getCallSignFromDriverId(request.getString("DriverId")) + "$" +

                        request.getString("Status") + "$" +
                        request.getString("ExecTime");

                orderList.add(line);
            }

            for(String token : orderList){
                result = result + token + "|";
            }
            if(orderList.isEmpty()){
                result = "NULL";
            } else {
                result = result.substring(0, result.length() - 1);
            }
        } catch (Exception e){
            System.out.println(e + " getOrdersForCommand() in DataBaseDispatcher");
        }
        return result;
    }

    void createReport(){
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");

            java.util.Date now = new java.util.Date(System.currentTimeMillis() - (12 * 60 * 60 * 1000));
            String dateNow = sdfDate.format(now);

            String hourStr = dateNow.substring(6, 8);
            int hour = Integer.parseInt(hourStr);

            String dateFrom;
            String dayStr;
            String dayOrNight;

            if(hour < 8){
                now = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
                dateNow = sdfDate.format(now);
                dayStr = dateNow.substring(0, 6);
                dayOrNight = " ночь";
                dateFrom = dayStr + "200000";
            } else if(hour >= 20){
                dayStr = dateNow.substring(0, 6);
                dayOrNight = " ночь";
                dateFrom = dayStr + "200000";
            } else {
                dayStr = dateNow.substring(0, 6);
                dayOrNight = " день";
                dateFrom = dayStr + "080000";
            }

            String monthStr = dayStr.substring(2,4);
            dayStr = dayStr.substring(4);

            String dateSmena = dayStr + "." + monthStr + dayOrNight;

            System.out.println(dateSmena);

            String sql = "SELECT SUM(Price) FROM journal_archive WHERE Status=9 AND DateFinish>"+dateFrom;
            Request request = new Request(sql);
            request.next();
            String sum = request.getString("SUM(Price)");
            request.close();

            if(sum == null){
                sum = "0";
            }

            sum = String.valueOf((int)Double.parseDouble(sum) * 0.1);
            String price = String.valueOf((int)Double.parseDouble(sum) * 0.1 * 0.3);

            sql = "SELECT COUNT(Price) FROM journal_archive WHERE Status=9 AND DateFinish>"+dateFrom;
            request = new Request(sql);
            request.next();
            String count = request.getString("COUNT(Price)");
            request.close();

            List<String> driverIds = new ArrayList<>();
            sql = "SELECT DriverId FROM drivers WHERE DateLastPing>" + dateFrom;
            request = new Request(sql);
            while (request.next()){
                driverIds.add(request.getString("DriverId"));
            }

            sql = "SELECT DriverId FROM online WHERE DatePing>" + dateFrom;
            request = new Request(sql);
            while (request.next()){
                boolean add = true;
                for(String id : driverIds){
                    if(id.equals(request.getString("DriverId"))){
                        add = false;
                    }
                }
                if(add) {
                    driverIds.add(request.getString("DriverId"));
                }
            }

            String driversCount = String.valueOf(driverIds.size());

            sql = "INSERT INTO report (`Date`, `Count`, `Cash`, `Price`, `DriversCount`) VALUES ('" + dateSmena + "','" + count + "','" + sum + "','" + price + "','" + driversCount + "')";
            query(sql);

        } catch (Exception e){
            System.out.println(e + " createReport() in DataBaseDispatcher");
        }
    }

    String getPercents(){
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");

            java.util.Date now = new java.util.Date();
            String dateNow = sdfDate.format(now);

            String hourStr = dateNow.substring(6, 8);
            int hour = Integer.parseInt(hourStr);

            String dateFrom;

            if(hour < 8){
                now = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
                dateNow = sdfDate.format(now);
                String dayStr = dateNow.substring(0, 6);
                dateFrom = dayStr + "200000";
            } else if(hour >= 20){
                String dayStr = dateNow.substring(0, 6);
                dateFrom = dayStr + "200000";
            } else {
                String dayStr = dateNow.substring(0, 6);
                dateFrom = dayStr + "080000";
            }

            System.out.println(dateFrom);

            String sql = "SELECT SUM(Price) FROM journal_archive WHERE Status=9 AND DateFinish>"+dateFrom;
            Request request = new Request(sql);
            request.next();
            String sum = request.getString("SUM(Price)");
            request.close();

            if(sum == null){
                sum = "0";
            }

            sql = "SELECT COUNT(Price) FROM journal_archive WHERE Status=9 AND DateFinish>"+dateFrom;
            request = new Request(sql);
            request.next();
            String count = request.getString("COUNT(Price)");
            request.close();

            double summa = Double.parseDouble(sum) * 0.1 * 0.3;
            return String.valueOf((int) summa) + "$" + count;
        } catch (Exception e){
            System.out.println(e + " getPercents() in DataBaseDispatcher");
        }
        return "-";
    }

    String getOfferedPricesForCommand(){
        List<String> offeredPricesList = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM market";

            Request request = new Request(sql);

            String line;
            while (request.next()){
                line =  request.getString("DriverId") + "$" +
                        request.getString("OrderNumber") + "$" +
                        request.getString("OfferedPrice") + "$" +
                        request.getString("Time");

                offeredPricesList.add(line);
            }

            for(String token : offeredPricesList){
                result = result + token + "|";
            }
            if(offeredPricesList.isEmpty()){
                result = "NULL";
            } else {
                result = result.substring(0, result.length() - 1);
            }
        } catch (Exception e){
            System.out.println(e + " getOfferedPricesForCommand() in DataBaseDispatcher");
        }
        return result;
    }

    void updateName(String name){
        try{
            String sql = "UPDATE service SET Name='"+name+"'";
            query(sql);
        } catch (Exception e){
            System.out.println(e + " updateName() in DataBaseDispatcher");
        }
    }

    private String getCallSignFromDriverId(String driverId){
        try {
            if(driverId.equals("-"))
                return "-";

            String sql = "SELECT CallSign FROM drivers WHERE DriverId="+driverId;

            Request request = new Request(sql);
            if(request.next()) {
                String callSign = request.getString("CallSign");
                request.close();
                return callSign;
            } else {
                return "-";
            }

        } catch (Exception e){
            System.out.println(e + " getCallSignFromDriverId() in DataBaseDispatcher");
        }
        return "-";
    }

    private class Request{
        private Connection connection;
        private Statement statement;
        private ResultSet resultSet;

        Request(String sql) throws Exception{
            this.connection = DriverManager.getConnection(url, username, password);
            this.statement = connection.createStatement();
            this.resultSet = statement.executeQuery(sql);
        }

        boolean next() throws Exception{
            if(resultSet.next()){
                return true;
            } else {
                close();
                return false;
            }
        }

        int getInt(String name) throws Exception{
            return resultSet.getInt(name);
        }

        String getString(String name) throws Exception{
            return resultSet.getString(name);
        }

        private void close() throws Exception{
            resultSet.close();
            statement.close();
            connection.close();
        }
    }
}
