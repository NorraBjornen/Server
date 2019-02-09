import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

class DataBaseOrder {
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

    private String query(String sql, boolean r) throws Exception{
        Connection Connection = DriverManager.getConnection(url, username, password);
        Statement statement = Connection.createStatement();
        statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet generatedKeys = statement.getGeneratedKeys();
        String id = null;

        if(generatedKeys.next())
            id = String.valueOf(generatedKeys.getInt(1));

        generatedKeys.close();
        statement.close();
        Connection.close();
        return id;
    }

    boolean IsExist(String orderNumber){
        try {
            String sql = "SELECT id FROM journal_operative WHERE id=" + orderNumber;

            Request request = new Request(sql);
            if(request.next()){
                request.close();
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            System.out.println(e + " checkIfExist() in DataBaseOrder");
            return false;
        }
    }

    String getOrderInfo(String orderNumber) throws Exception{
        String sql = "SELECT * FROM journal_operative WHERE id=" + orderNumber;
        Request request = new Request(sql);
        request.next();
        String order = String.valueOf(request.getInt("id")) + "$" +
                request.getString("FromW") + "$" +
                request.getString("ToW") + "$" +
                request.getString("Price") + "$" +
                request.getString("Phone") + "$" +
                request.getString("Description") + "$" +
                request.getString("DateStart") + "$" +

                getCallSignFromDriverId(request.getString("DriverId")) + "$" +

                request.getString("Status") + "$" +
                request.getString("ExecTime");
        request.close();
        return order;
    }

    String newOrder(String from, String to, String price, String phone, String description, String dateStart) throws Exception{
        String sql = "INSERT INTO journal_operative (`FromW`,`ToW`,`Price`,`Phone`,`Description`,`DateStart`, `OriginalPrice`) VALUES ('" + from + "','" + to + "','" + price + "','" + phone + "','" + description + "','" + dateStart + "','" + price + "')";
        return query(sql, true);
    }

    void updateOrder(String orderNumber, String from, String to, String price, String phone, String description) throws Exception{
        String sql = "UPDATE journal_operative set FromW='" + from + "', ToW='" + to + "', Price='" + price + "', Phone='" + phone + "', Description='" + description + "' WHERE id='" + orderNumber + "'";
        query(sql);
    }

    void chose(String orderNumber, String driverId) throws Exception{
        String sql = "UPDATE journal_operative SET Status=1 WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET DriverId=" + driverId + " WHERE id=" + orderNumber;
        query(sql);
    }

    void complete(String orderNumber, String driverId) throws Exception{
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
        java.util.Date now = new java.util.Date();
        String dateFinish = sdfDate.format(now);

        String sql = "SELECT Balance FROM drivers WHERE DriverId=" + driverId;
        Request request = new Request(sql);
        request.next();
        String balance = request.getString("Balance");
        request.close();

        sql = "SELECT Name FROM service";
        request = new Request(sql);
        request.next();
        String name = request.getString("Name");
        request.close();

        String offeredPrice;
        sql = "SELECT OfferedPrice FROM market WHERE DriverId="+driverId+" AND OrderNumber="+orderNumber;
        request = new Request(sql);
        if(request.next()) {
            offeredPrice = request.getString("OfferedPrice");
            request.close();
        } else {
            sql = "SELECT Price FROM journal_operative WHERE id="+orderNumber;
            request = new Request(sql);
            request.next();
            offeredPrice = request.getString("Price");
            request.close();
        }

        String newBalance;
        try{newBalance = String.valueOf((int) (Double.parseDouble(balance) - Double.parseDouble(offeredPrice)*0.00));} catch (Exception e){newBalance = balance;}

        sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) " +
                "SELECT FromW, ToW, '" + offeredPrice + "', Phone, DateStart, '" + dateFinish + "', '9', '" + driverId + "', '" + name + "' FROM journal_operative WHERE id="+orderNumber;
        query(sql);
        sql = "DELETE FROM journal_operative WHERE id="+orderNumber;
        query(sql);
        sql = "DELETE FROM market WHERE OrderNumber="+orderNumber;
        query(sql);
        sql = "UPDATE drivers SET Balance="+newBalance+" WHERE DriverId="+driverId;
        query(sql);
    }

    void acceptOrder(String orderNumber, String driverId) throws Exception {
        String offeredPrice = null;

        String sql = "SELECT OfferedPrice FROM market WHERE DriverId=" + driverId + " AND OrderNumber=" + orderNumber;
        Request request = new Request(sql);
        if (request.next()){
            offeredPrice = request.getString("OfferedPrice");
            request.close();
        }

        sql = "UPDATE journal_operative SET Status=2 WHERE id=" + orderNumber;
        query(sql);

        sql = "UPDATE journal_operative SET ExecTime=0 WHERE id=" + orderNumber;
        query(sql);

        if(offeredPrice != null) {
            sql = "UPDATE journal_operative SET Price=" + offeredPrice + " WHERE id=" + orderNumber;
            query(sql);
        }

        sql = "DELETE FROM market WHERE OrderNumber!=" + orderNumber + " AND DriverId=" + driverId;
        query(sql);
    }

    void refuseOrder(String orderNumber, String driverId) throws Exception{
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
        java.util.Date now = new java.util.Date();
        String dateFinish = sdfDate.format(now);

        String sql = "SELECT Name FROM service";
        Request request = new Request(sql);
        request.next();
        String name = request.getString("Name");
        request.close();

        sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) " +
                                        "SELECT FromW, ToW, Price, Phone, DateStart, '" + dateFinish + "', '-1', '" + driverId + "', '" + name + "' FROM journal_operative WHERE id="+orderNumber;

        query(sql);

        sql = "UPDATE journal_operative SET DriverId='-' WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET ExecTime='-' WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET Status='0' WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET Price=OriginalPrice WHERE id=" + orderNumber;
        query(sql);

        sql = "DELETE FROM market WHERE DriverId=" + driverId + " AND OrderNumber='" + orderNumber + "'";
        query(sql);
    }

    void takeoff(String orderNumber, String driverId) throws Exception{
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
        java.util.Date now = new java.util.Date();
        String dateFinish = sdfDate.format(now);

        String sql = "SELECT Name FROM service";
        Request request = new Request(sql);
        request.next();
        String name = request.getString("Name");
        request.close();

        sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) " +
                "SELECT FromW, ToW, Price, Phone, DateStart, '" + dateFinish + "', '-5', '" + driverId + "', '" + name + "' FROM journal_operative WHERE id="+orderNumber;

        query(sql);

        sql = "UPDATE journal_operative SET DriverId='-' WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET ExecTime='-' WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET Status='0' WHERE id=" + orderNumber;
        query(sql);
        sql = "UPDATE journal_operative SET Price=OriginalPrice WHERE id=" + orderNumber;
        query(sql);

        sql = "DELETE FROM market WHERE DriverId=" + driverId + " AND OrderNumber='" + orderNumber + "'";
        query(sql);
    }

    String deleteOrder(String orderNumber) throws Exception{
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
        java.util.Date now = new java.util.Date();
        String dateFinish = sdfDate.format(now);

        String sql = "SELECT Name FROM service";
        Request request = new Request(sql);
        request.next();
        String name = request.getString("Name");
        request.close();

        sql = "SELECT DriverId FROM journal_operative WHERE id=" + orderNumber;
        request = new Request(sql);
        request.next();
        String driverId = request.getString("DriverId");
        request.close();

        sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) " +
                                        "SELECT FromW, ToW, Price, Phone, DateStart, '" + dateFinish + "', '-9', DriverId , '" + name + "' FROM journal_operative WHERE id="+orderNumber;
        query(sql);

        sql = "DELETE FROM journal_operative WHERE id=" + orderNumber;
        query(sql);
        sql = "DELETE FROM market WHERE OrderNumber=" + orderNumber;
        query(sql);

        return driverId;
    }

    void hurry(String orderNumber) throws Exception{
        String sql = "UPDATE journal_operative SET Status=3 WHERE id="+orderNumber;
        query(sql);
    }

    String ok(String orderNumber) throws Exception{
        String sql = "UPDATE journal_operative SET Status=4 WHERE id="+orderNumber;
        query(sql);

        sql = "SELECT DriverId FROM journal_operative WHERE id=" + orderNumber;
        Request request = new Request(sql);
        request.next();
        String driverId = request.getString("DriverId");
        request.close();
        return driverId;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void updateExecTime(String orderNumber, String execTime){
        try{
            String sql = "UPDATE journal_operative set ExecTime="+execTime+" WHERE id="+orderNumber;
            query(sql);
        } catch (Exception e){
            System.out.println(e + " updateExecTime() in DataBaseOrder");
        }
    }

    boolean isFree(String orderNumber, String price){
        try{
            String sql = "SELECT Price FROM journal_operative WHERE id="+orderNumber+" AND DriverId='-'";
            Request request = new Request(sql);
            if(request.next()){
                if(request.getString("Price").equals(price)) {
                    request.close();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e){
            System.out.println(e + " isFree() in DataBaseOrder");
            return false;
        }
    }

    boolean isOrderRunning(String orderNumber){
        try{
            String sql = "SELECT id FROM journal_operative WHERE id="+orderNumber+" AND ExecTime!='-'";
            Request request = new Request(sql);
            if(request.next()){
                request.close();
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            System.out.println(e + " isOrderRunning() in DataBaseOrder");
            return false;
        }
    }

    List<String> getRunningOrders(){
        List<String> list = new ArrayList<>();
        try {
            String sql = "SELECT id, ExecTime FROM journal_operative WHERE ExecTime!='-'";
            Request request = new Request(sql);

            while (request.next()) {
                String line = String.valueOf(request.getInt("id")) + "$" +
                        request.getString("ExecTime");

                list.add(line);
            }
        } catch (Exception e){
            System.out.println(e + " getRunningOrders() in DataBaseOrder");
        }
        return list;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void updateOfferedPrice(String orderNumber, String driverId, String newOfferedPrice, String newOfferedTime){
        try{
            String sql = "UPDATE market set OfferedPrice=" + newOfferedPrice + ", Time='" + newOfferedTime + "' WHERE OrderNumber='" + orderNumber + "' AND DriverId='" + driverId + "'";
            query(sql);
        } catch (Exception e){
            System.out.println(e + " updateOfferedPrice() in DataBaseOrder");
        }
    }

    void newOfferedPrice(String orderNumber, String driverId, String offeredPrice, String offeredTime){
        try{
            String sql = "INSERT INTO market (`DriverId`,`OrderNumber`,`OfferedPrice`,`Time`) VALUES ('" + driverId + "','" + orderNumber + "','" + offeredPrice + "','" + offeredTime + "')";
            query(sql);
        } catch (Exception e){
            System.out.println(e + " newOfferedPrice() in DataBaseOrder");
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
