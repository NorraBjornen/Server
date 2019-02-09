import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DataBaseDriver {
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

    String checkReg(String driverId){
        int balanceThreshold = 500;
        try {
            String sql = "SELECT Balance FROM drivers WHERE DriverId="+driverId;
            Request request = new Request(sql);

            if(request.next()){

                int balanceInt = Integer.parseInt(request.getString("Balance"));
                request.close();

                sql = "SELECT DriverId FROM online WHERE DriverId=" + driverId;
                request = new Request(sql);
                if (request.next()) {
                    request.close();
                    return "ALREADY";
                } else {
                    sql = "SELECT DateAccess FROM drivers WHERE DriverId="+driverId;
                    request = new Request(sql);
                    request.next();

                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
                    java.util.Date now = new java.util.Date();
                    String dateNow = sdfDate.format(now);

                    String time1 = request.getString("DateAccess");
                    if(time1.equals("0")){
                        time1 = "000000000000";
                    }

                    SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
                    java.util.Date date1 = format.parse(time1);
                    java.util.Date date2 = format.parse(dateNow);
                    double difference = (date2.getTime() - date1.getTime());

                    if(difference < 24 * 60 * 60 * 1000){
                        balanceThreshold = 0;
                    }

                    request.close();

                    if (balanceInt >= balanceThreshold) {
                        return "OK";
                    } else {
                        return "NO_BALANCE";
                    }
                }

            } else {
                return "NO_REG";
            }
        } catch (Exception e){
            System.out.println(e + " checkReg() in DataBaseDriver");
        }
        return null;
    }

    String getOrderNumbers(String driverId){
        try {
            String sql = "SELECT id FROM journal_operative WHERE DriverId=" + driverId + " OR DriverId='-'";
            Request request = new Request(sql);

            StringBuilder orderNumbersBuilder = new StringBuilder();

            if(request.next()){
                orderNumbersBuilder.append(String.valueOf(request.getInt("id")));
            } else {
                return "NULL";
            }

            while (request.next()){
                orderNumbersBuilder.append("$").append(String.valueOf(request.getInt("id")));
            }

            System.out.println(orderNumbersBuilder.toString());
            return orderNumbersBuilder.toString();

        } catch (Exception e){
            System.out.println(e + " getOrderNumbers() in DataBaseDriver");
        }
        return "NULL";
    }

    String getOfferedPricesForCommand(String driverId){
        List<String> offeredPricesList = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM market WHERE DriverId=" + driverId;

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
            System.out.println(e + " getOfferedPricesForCommand() in DataBaseDriver");
        }
        return result;
    }

    String getOrderInfo(String orderNumber){
        try {
            String sql = "SELECT * FROM journal_operative WHERE id=" + orderNumber;

            Request request = new Request(sql);
            request.next();
            String order = String.valueOf(request.getInt("id")) + "$" +
                    request.getString("FromW") + "$" +
                    request.getString("ToW") + "$" +
                    request.getString("Price") + "$" +
                    request.getString("Phone") + "$" +
                    request.getString("Description");
            request.close();

            return order;
        } catch (Exception e){
            System.out.println(e + " getOrderInfo() in DataBaseDriver");
            return "NULL";
        }
    }

    String checkIfChosen(String driverId){
        try {
            String sql = "SELECT id FROM journal_operative WHERE DriverId=" + driverId + " AND Status!=0";
            Request request = new Request(sql);
            if(request.next()){
                String id = String.valueOf(request.getInt("id"));
                request.close();
                return id;
            } else {
                return "NULL";
            }
        } catch (Exception e){
            System.out.println(e + " checkIfChosen() in DataBaseDriver");
        }
        return null;
    }

    boolean isOnline(String driverId){
        try {
            String sql = "SELECT DriverId FROM online WHERE DriverId="+driverId;

            Request request = new Request(sql);
            if(request.next()){
                request.close();
                return true;
            } else {
                return false;
            }

        } catch (Exception e){
            System.out.println(e + " isOnline() in DataBaseDriver");
            return false;
        }
    }

    void goOnline(String driverId, String status, String position){
        try {
            String sql = "SELECT DriverId FROM online WHERE DriverId=" + driverId;
            Request request = new Request(sql);
            if(!request.next()){
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
                java.util.Date now = new java.util.Date();
                String dateNow = sdfDate.format(now);

                sql = "SELECT DateAccess FROM drivers WHERE DriverId="+driverId;
                request = new Request(sql);
                request.next();
                String dateAccess = request.getString("DateAccess");
                request.close();

                Date date = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
                String dateDayAgo = sdfDate.format(date);

                if(Long.parseLong(dateAccess) <= Long.parseLong(dateDayAgo)){
                    String balance = getBalance(driverId);

                    balance = String.valueOf(Integer.parseInt(balance) - 250);

                    sql = "UPDATE drivers SET Balance="+balance+" WHERE DriverId="+driverId;
                    query(sql);
                    sql = "UPDATE drivers SET DateAccess="+dateNow+" WHERE DriverId="+driverId;
                    query(sql);
                }

                sql = "INSERT INTO online (`DriverId`, `Status`, `Position`, `DatePing`, `DateStatus`) VALUES ('" + driverId + "', '" + status + "', '" + position + "', '" + dateNow + "', '" + dateNow + "')";
                query(sql);
            } else {
                request.close();
            }
        } catch (Exception e){
            System.out.println(e + " goOnline() in DataBaseDriver");
        }
    }

    String getCallSign(String driverId){
        try {
            String sql = "SELECT CallSign FROM drivers WHERE DriverId="+driverId;
            Request request = new Request(sql);
            request.next();
            String callSign = request.getString("CallSign");
            request.close();
            return callSign;
        } catch (Exception e){
            System.out.println(e + " getCallSign() in DataBaseDriver");
            return null;
        }
    }


    String getDriver(String driverId){
        List<String> drivers = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM dc INNER JOIN online ON dc.DriverId=online.DriverId " +
                    "INNER JOIN drivers ON dc.DriverId=drivers.DriverId WHERE dc.DriverId=" + driverId;
            Request request = new Request(sql);
            String line;
            while (request.next()){
                line = request.getString("DriverId") + "$" +
                        request.getString("CallSign") + "$" +
                        request.getString("CarNumber") + "$" +
                        request.getString("Colour") + " " +
                        request.getString("Model") + "$" +
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
            System.out.println(e + " getDriver() in DataBaseDriver");
        }
        return result;
    }

    String getBalance(String driverId){
        try {
            String sql = "SELECT Balance FROM drivers WHERE DriverId="+driverId;
            Request request = new Request(sql);
            request.next();
            String balance = request.getString("Balance");
            request.close();

            return balance;
        } catch (Exception e){
            System.out.println(e + " getBalance() in DataBaseDriver");
        }
        return null;
    }

    String getStatus(String driverId){
        try {
            String sql = "SELECT Status FROM online WHERE DriverId="+driverId;
            Request request = new Request(sql);
            request.next();
            String balance = request.getString("Status");
            request.close();

            return balance;
        } catch (Exception e){
            System.out.println(e + " getStatus() in DataBaseDriver");
        }
        return null;
    }

    void setStatus(String driverId, String status){
        try{
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new java.util.Date();
            String dateNow = sdfDate.format(now);

            String sql = "UPDATE online SET Status=" + status + " WHERE DriverId=" + driverId;
            query(sql);

            sql = "UPDATE online SET DateStatus=" + dateNow + " WHERE DriverId=" + driverId;
            query(sql);
        } catch (Exception e){
            System.out.println(e + " setStatus() in DataBaseDriver");
        }
    }

    String exitLine(String driverId){
        try{
            String sql = "DELETE FROM online WHERE DriverId=" + driverId;
            query(sql);

            sql = "DELETE FROM market WHERE DriverId="+driverId;
            query(sql);

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new java.util.Date();
            String dateLastPing = sdfDate.format(now);

            sql = "UPDATE drivers SET DateLastPing=" + dateLastPing + " WHERE DriverId=" + driverId;
            query(sql);

            return getBalance(driverId);
        } catch (Exception e){
            System.out.println(e + " exitLine() in DataBaseDriver");
            return null;
        }
    }

    void setGeo(String driverId, String geo){
        try{
            String sql = "UPDATE online SET Position='"+geo+"' WHERE DriverId='"+driverId+"'";
            query(sql);
        } catch (Exception e){
            System.out.println(e + " setGeo() in DataBaseDriver");
        }
    }

    void setDatePing(String driverId){
        try{
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new java.util.Date();
            String dateNow = sdfDate.format(now);

            String sql = "UPDATE online SET DatePing='"+dateNow+"' WHERE DriverId='"+driverId+"'";
            query(sql);
        } catch (Exception e){
            System.out.println(e + " setDatePing() in DataBaseDriver");
        }
    }

    void setRadius(String driverId, String radius){
        try{
            String sql = "UPDATE drivers SET Radius='"+radius+"' WHERE DriverId='"+driverId+"'";
            query(sql);
        } catch (Exception e){
            System.out.println(e + " setRadius() in DataBaseDriver");
        }
    }

    List<String> getDriversOffline(){
        List<String> driverIds = new ArrayList<>();
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");

            Date date = new Date(System.currentTimeMillis() - (30 * 60 * 1000));
            String dateKickFree = sdfDate.format(date);

            date = new Date(System.currentTimeMillis() - (3 * 60 * 60 * 1000));
            String dateKickBusy = sdfDate.format(date);

            date = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
            String dateDayAgo = sdfDate.format(date);

            String sql = "SELECT drivers.DriverId FROM online INNER JOIN drivers ON online.DriverId=drivers.DriverId WHERE (online.Status=1 AND online.DatePing<"+dateKickFree+") OR (online.Status=2 AND online.DatePing<"+dateKickBusy+") OR (drivers.DateAccess<"+dateDayAgo+")";
            Request request = new Request(sql);
            while (request.next()){
                driverIds.add(request.getString("drivers.DriverId"));
            }

        } catch (Exception e){
            System.out.println(e + " getDriversOffline() in DataBaseDriver");
        }
        return driverIds;
    }

    boolean isDriverOnOrder(String driverId){
        try {
            String sql = "SELECT id FROM journal_operative WHERE DriverId="+driverId;
            Request request = new Request(sql);
            if(request.next()){
                request.close();
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            System.out.println(e + " isDriverOnOrder() in DataBaseDriver");
            return true;
        }
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
