import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

class DataBaseClient {
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

    String register(){
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new java.util.Date();
            String dateNow = sdfDate.format(now);

            String sql = "INSERT INTO clients (`ClientId`) VALUES ('"+dateNow+"')";
            query(sql);
            return dateNow;
        } catch (Exception e){
            System.out.println(e + " register() in DataBaseClient");
        }
        return null;
    }

    String registerNumber(String clientId, String number){
        try {
            Random r = new Random();
            String code = String.valueOf(r.nextInt((999999 - 100000) + 1) + 100000);

            String sql = "UPDATE clients SET Phone="+number+" WHERE ClientId="+clientId;
            query(sql);
            sql = "UPDATE clients SET KeyCode="+code+" WHERE ClientId="+clientId;
            query(sql);
            return code;
        } catch (Exception e){
            System.out.println(e + " registerNumber() in DataBaseClient");
        }
        return null;
    }

    boolean confirm(String clientId, String keyCode){
        try {
            String sql = "SELECT KeyCode FROM clients WHERE ClientId='"+clientId+"'";
            Request request = new Request(sql);
            request.next();
            if(String.valueOf(request.getInt("KeyCode")).equals(keyCode)){
                sql = "UPDATE clients SET Confirmed=1 WHERE ClientId='"+clientId+"'";
                query(sql);
                return true;
            }
            return false;
        } catch (Exception e){
            System.out.println(e + " confirm() in DataBaseClient");
        }
        return false;
    }

    String checkReg(String clientId){
        try {
            String sql = "SELECT Phone FROM clients WHERE ClientId='"+clientId+"'";
            Request request = new Request(sql);

            if(request.next()){

                String phone = request.getString("Phone");
                request.close();

                if(Long.parseLong(phone) == 0)
                    return "NO_REG";

                sql = "SELECT Confirmed FROM clients WHERE ClientId='"+clientId+"'";
                request = new Request(sql);
                request.next();

                if(request.getInt("Confirmed") == 0)
                    return "NOT_CONFIRMED";

                return "OK";

            } else {
                return register();
            }
        } catch (Exception e){
            System.out.println(e + " checkReg() in DataBaseClient");
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

                    balance = String.valueOf(Integer.parseInt(balance) - 100);

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
