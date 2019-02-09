import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataBase {
    private Connection Connection;
    private Statement Statement;

    public DataBase(){
        String url = "jdbc:mysql://127.0.0.1:3306/work?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String username = "root";
        String password = "deepundo99";
        try{
            Connection = DriverManager.getConnection(url, username, password);
            Statement = Connection.createStatement();
        } catch (SQLException e) {
            System.out.print(e);
        }
    }

    public void query(String sql){
        try {
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public boolean isOnline(String driverId){
        try {
            String sql = "SELECT * FROM online WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            if(resultSet.next()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            return false;
        }
    }

    public void goOnline(String driverId, String status, String position){
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
        Date now = new Date();
        String dateNow = sdfDate.format(now);

        try {
            String sql = "SELECT * FROM online WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            if(!resultSet.next()) {
                sql = "SELECT * FROM drivers WHERE DriverId=" + driverId;
                resultSet = Statement.executeQuery(sql);
                resultSet.next();
                String callsign = resultSet.getString("Callsign");
                String carNumber = resultSet.getString("CarNumber");
                String description = resultSet.getString("Description");
                String balance = resultSet.getString("Balance");
                sql = "INSERT INTO online (`DriverId`, `Callsign`, `CarNumber`, `Description`, `Balance`, `Status`, `Position`, `DatePing`) VALUES ('" + driverId + "', '" + callsign + "', '" + carNumber + "', '" + description + "', '" + balance + "', '" + status + "', '" + position + "', '"+dateNow+"')";
                query(sql);
            }
        } catch (Exception e){}
    }

    public String[] getDriverInfo(String driverId){
        try {
            String sql = "SELECT * FROM online WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            resultSet.next();
            String[] info = new String[]{
                    resultSet.getString("Callsign"), resultSet.getString("CarNumber"), resultSet.getString("Description"),
                    resultSet.getString("Balance"), resultSet.getString("Status")
            };
            return info;
        } catch (Exception e){
            return null;
        }
    }

    public void newOrder(String orderNumber, String from, String to, String price, String phone, String description, String dateStart){
        try {
            String sql = "INSERT INTO journal_operative (`id`,`FromW`,`ToW`,`Price`,`Phone`,`Description`,`DateStart`) VALUES ('" + orderNumber + "','" + from + "','" + to + "','" + price + "','" + phone + "','" + description + "','" + dateStart + "')";
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void updateOrder(String orderNumber, String from, String to, String price, String phone, String description){
        try {
            String sql = "UPDATE journal_operative set FromW='" + from + "', ToW='" + to + "', Price='" + price + "', Phone='" + phone + "', Description='" + description + "' WHERE id='" + orderNumber + "'";
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void chose(String orderNumber, String driverId){
        try {
            String sql = "UPDATE market set Status=1 WHERE OrderNumber="+orderNumber+" AND DriverId="+driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }


    public String complete(String orderNumber, String from, String to, String offeredPrice, String phone, String dateStart, String driverId){
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new Date();
            String dateFinish = sdfDate.format(now);

            String sql = "SELECT Balance FROM online WHERE DriverId=" + driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            resultSet.next();
            String balance = resultSet.getString("Balance");

            sql = "SELECT Name FROM service";
            resultSet = Statement.executeQuery(sql);
            resultSet.next();
            String name = resultSet.getString("Name");

            String newBalance = String.valueOf((int) (Double.parseDouble(balance) - Double.parseDouble(offeredPrice)*0.00));

            sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) VALUES" +
                    "('"+from+"','"+to+"','"+offeredPrice+"','"+phone+"','"+dateStart+"','"+dateFinish+"','9','"+driverId+"', '"+name+"')";
            Statement.executeUpdate(sql);
            sql = "DELETE FROM journal_operative WHERE id="+orderNumber;
            Statement.executeUpdate(sql);
            sql = "DELETE FROM market WHERE OrderNumber="+orderNumber;
            Statement.executeUpdate(sql);
            sql = "UPDATE online SET Balance="+newBalance+" WHERE DriverId="+driverId;
            Statement.executeUpdate(sql);
            sql = "UPDATE drivers SET Balance="+newBalance+" WHERE DriverId="+driverId;
            Statement.executeUpdate(sql);
            return newBalance;
        } catch (Exception e){
            return null;
        }
    }

    public void acceptOrder(String orderNumber, String driverId){
        try{
            String sql = "UPDATE market set Status=2 WHERE OrderNumber=" + orderNumber + " AND DriverId=" + driverId;
            Statement.executeUpdate(sql);

            sql = "DELETE FROM market WHERE OrderNumber!=" + orderNumber + " AND DriverId=" + driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void refuseOrder(String orderNumber, String from, String to, String offeredPrice, String phone, String dateStart, String driverId){
        try{
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new Date();
            String dateFinish = sdfDate.format(now);

            String sql = "SELECT Name FROM service";
            ResultSet resultSet = Statement.executeQuery(sql);
            resultSet.next();
            String name = resultSet.getString("Name");

            sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) VALUES" +
                    "('"+from+"','"+to+"','"+offeredPrice+"','"+phone+"','"+dateStart+"','"+dateFinish+"','-1','"+driverId+"', '"+name+"')";
            Statement.executeUpdate(sql);

            try {
                sql = "DELETE FROM market WHERE DriverId=" + driverId + " AND OrderNumber=" + orderNumber;
                Statement.executeUpdate(sql);
            } catch (Exception e){}
        } catch (Exception e){
            System.out.println(e + " while deleting");
        }
    }

    public void deleteOrder(String orderNumber, String from, String to, String offeredPrice, String phone, String dateStart, String driverId){
        try {

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new Date();
            String dateFinish = sdfDate.format(now);

            String sql = "SELECT Name FROM service";
            ResultSet resultSet = Statement.executeQuery(sql);
            resultSet.next();
            String name = resultSet.getString("Name");

            sql = "INSERT INTO journal_archive (`FromW`,`ToW`,`Price`,`Phone`,`DateStart`,`DateFinish`,`Status`,`DriverId`, `Name`) VALUES" +
                    "('"+from+"','"+to+"','"+offeredPrice+"','"+phone+"','"+dateStart+"','"+dateFinish+"','-9','"+driverId+"', '"+name+"')";
            Statement.executeUpdate(sql);

            sql = "DELETE FROM journal_operative WHERE id=" + orderNumber;
            Statement.executeUpdate(sql);
            sql = "DELETE FROM market WHERE OrderNumber=" + orderNumber;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void hurry(String orderNumber, String driverId){
        try{
            String sql = "UPDATE market set Status=3 WHERE OrderNumber="+orderNumber+" AND DriverId="+driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void ok(String orderNumber, String driverId){
        try{
            String sql = "UPDATE market set Status=4 WHERE OrderNumber="+orderNumber+" AND DriverId="+driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }


    public String getCallsign(String driverId){
        try {
            String sql = "SELECT Callsign FROM drivers WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            String line;
            resultSet.next();
            line = resultSet.getString("Callsign");
            return line;
        } catch (Exception e){
            return null;
        }
    }

    public String getOrders(){
        List<String> orderList = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM journal_operative";
            ResultSet resultSet = Statement.executeQuery(sql);
            String line;
            while (resultSet.next()){
                line = "";
                for(int i = 1; i <= 7; i++){
                    line = line + resultSet.getString(i) + "$";
                }
                line = line.substring(0, line.length() - 1);
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
        } catch (Exception e){}
        return result;
    }

    public String getOfferedPrices(){
        List<String> offeredPricesList = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM market";
            ResultSet resultSet = Statement.executeQuery(sql);
            String line;
            while (resultSet.next()){
                line = "";
                for(int i = 2; i <= 7; i++){
                    line = line + resultSet.getString(i) + "$";
                }
                line = line.substring(0, line.length() - 1);
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
        } catch (Exception e){}
        return result;
    }

    public String getDrivers(){
        List<String> drivers = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM online";
            ResultSet resultSet = Statement.executeQuery(sql);
            String line;
            while (resultSet.next()){
                line = "";
                for(int i = 2; i < 10; i++)  line = line + resultSet.getString(i) + "$";
                line = line.substring(0, line.length() - 1);
                drivers.add(line);
            }
            for(String token : drivers) result = result + token + "|";
            if(drivers.isEmpty()){
                result = "NULL";
            } else {
                result = result.substring(0, result.length() - 1);
            }
        } catch (Exception e){}
        return result;

    }

    public String getDriver(String driverId){
        List<String> drivers = new ArrayList<>();
        String result = "";
        try {
            String sql = "SELECT * FROM online WHERE DriverId=" + driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            String line;
            while (resultSet.next()){
                line = "";
                for(int i = 2; i < 9; i++)  line = line + resultSet.getString(i) + "$";
                line = line.substring(0, line.length() - 1);
                drivers.add(line);
            }
            for(String token : drivers) result = result + token + "|";
            if(drivers.isEmpty()){
                result = "NULL";
            } else {
                result = result.substring(0, result.length() - 1);
            }
        } catch (Exception e){}
        return result;

    }

    public String getStreets(){
        String line = "";
        try {
            String sql = "SELECT * FROM correct_streets";
            ResultSet resultSet = Statement.executeQuery(sql);
            while (resultSet.next()){
                if(!resultSet.getString("Correct").equals("-")) {
                    line = line + "|" + resultSet.getString("Wrong") + "$" + resultSet.getString("Correct");
                }
            }

            if(line.equals("")){
                line = "NULL";
            } else {
                line = line.substring(1);
            }

        } catch (Exception e){
            System.out.println(e);
        }
        return line;
    }

    public String getClients(){
        String line = "";
        try {
            String sql = "SELECT * FROM clients";
            ResultSet resultSet = Statement.executeQuery(sql);
            while (resultSet.next())    line = line + resultSet.getString("Phone") + "$" + resultSet.getString("Address") +"|";
            if(line.equals("")){
                return "NULL";
            } else {
                line = line.substring(0, line.length() - 1);
            }
        } catch (Exception e){}
        return line;
    }



    public String checkMarket(String driverId){
        try {
            String sql = "SELECT OrderNumber FROM market WHERE DriverId=" + driverId + " AND Status!=0";
            ResultSet resultSet = Statement.executeQuery(sql);
            if(resultSet.next()){
                return resultSet.getString("OrderNumber");
            } else {
                return "NULL";
            }
        } catch (Exception e){}
        return null;
    }

    public String getBalance(String driverId){
        try {
            String sql = "SELECT Balance FROM online WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            resultSet.next();
            String balance = resultSet.getString("Balance");
            return balance;
        } catch (Exception e){
            try {
                String sql = "SELECT Balance FROM drivers WHERE DriverId=" + driverId;
                ResultSet resultSet = Statement.executeQuery(sql);
                resultSet.next();
                String balance = resultSet.getString("Balance");
                return balance;
            } catch (Exception a){
                System.out.println(a + " from driver");
            }
        }
        return null;
    }

    public String checkReg(String driverId){
        int balanceThreshold = 100;
        try {
            String sql = "SELECT * FROM drivers WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);

            if(resultSet.next()){

                String callsign = resultSet.getString("Callsign");
                if(callsign.equals("0")){
                    return "NOT_READY";
                } else {
                    String balance = resultSet.getString("Balance");
                    int balanceInt = Integer.parseInt(balance);
                    sql = "SELECT Callsign FROM online WHERE DriverId=" + driverId;
                    resultSet = Statement.executeQuery(sql);
                    if (resultSet.next()) {
                        return "ALREADY";
                    } else {
                        sql = "SELECT * FROM drivers WHERE DriverId="+driverId;
                        resultSet = Statement.executeQuery(sql);
                        resultSet.next();

                        SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
                        Date now = new Date();
                        String dateNow = sdfDate.format(now);

                        String time1 = resultSet.getString("DateLastPing");
                        if(time1.equals("0")){
                            time1 = "000000000000";
                        }

                        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
                        Date date1 = format.parse(time1);
                        Date date2 = format.parse(dateNow);
                        double difference = (date2.getTime() - date1.getTime()) / 1000;

                        if(difference <= 7200){
                            balanceThreshold = 100;
                        }

                        if (balanceInt >= balanceThreshold) {
                            return "OK";
                        } else {
                            return "NO_BALANCE";
                        }
                    }
                }
            } else {
                return "NO_REG";
            }
        } catch (Exception e){
            System.out.println(e + " check");
        }
        return null;
    }

    public void updateExecTimeInMarket(String orderNumber, String driverId, String execTime){
        try{
            String sql = "UPDATE market set ExecTime="+execTime+" WHERE OrderNumber="+orderNumber+" AND DriverId="+driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public String getFreeId(){
        try {
            String sql = "SELECT id FROM journal_operative";
            ResultSet resultSet = Statement.executeQuery(sql);
            String line = "0";
            while (resultSet.next()){
                line = resultSet.getString("id");
            }
            line = String.valueOf(Integer.parseInt(line) + 1);
            return line;
        } catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    public void setStatus(String driverId, String status){
        try{
            String sql = "UPDATE online SET Status=" + status + " WHERE DriverId=" + driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){
            System.out.println(e.toString() + " setStatus");
        }
    }

    public String exitLine(String driverId){
        try{
            String balance = getBalance(driverId);

            String sql = "DELETE FROM online WHERE DriverId=" + driverId;
            Statement.executeUpdate(sql);

            sql = "UPDATE drivers SET Balance=" + balance + " WHERE DriverId=" + driverId;
            Statement.executeUpdate(sql);

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            java.util.Date now = new Date();
            String dateLastPing = sdfDate.format(now);

            sql = "UPDATE drivers SET DateLastPing=" + dateLastPing + " WHERE DriverId=" + driverId;
            Statement.executeUpdate(sql);

            return balance;
        } catch (Exception e){
            return null;
        }
    }

    public void updateOfferedPrice(String orderNumber, String driverId, String newOfferedPrice, String newOfferedTime){
        try{
            String sql = "UPDATE market set OfferedPriceOperation=" + newOfferedPrice + ", Time='" + newOfferedTime + "' WHERE OrderNumber='" + orderNumber + "' AND DriverId='" + driverId + "'";
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void newOfferedPrice(String orderNumber, String driverId, String offeredPrice, String offeredTime, String status){
        try{
            String sql = "INSERT INTO market (`DriverId`,`OrderNumber`,`OfferedPriceOperation`,`Time`,`Status`) VALUES ('" + driverId + "','" + orderNumber + "','" + offeredPrice + "','" + offeredTime + "','" + status + "')";
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void removeDriversPrices(String driverId){
        try{
            String sql = "DELETE FROM market WHERE DriverId="+driverId;
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void updateName(String name){
        try{
            String sql = "UPDATE service SET Name='"+name+"'";
            System.out.println(sql);
            Statement.executeUpdate(sql);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void newClient(String phone, String address){
        try{
            String sql = "INSERT INTO clients (`Phone`,`Address`) VALUES ('" + phone + "','" + address + "')";
            Statement.executeUpdate(sql);
        } catch (Exception e){}
    }

    public void newStreet(String street){
        try{
            String sql = "INSERT INTO correct_streets (`Wrong`) VALUES ('" + street + "')";
            Statement.executeUpdate(sql);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void setGeo(String driverId, String geo){
        try{
            String sql = "UPDATE online SET Position='"+geo+"' WHERE DriverId='"+driverId+"'";
            Statement.executeUpdate(sql);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void setDatePing(String driverId){
        try{
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
            Date now = new Date();
            String dateNow = sdfDate.format(now);

            String sql = "UPDATE online SET DatePing='"+dateNow+"' WHERE DriverId='"+driverId+"'";
            Statement.executeUpdate(sql);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void setRadius(String driverId, String radius){
        try{
            String sql = "UPDATE online SET Radius='"+radius+"' WHERE DriverId='"+driverId+"'";
            Statement.executeUpdate(sql);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public boolean deleteDriverFromLine(String driverId){
        try {
            String sql = "SELECT DatePing FROM online";
            ResultSet resultSet = Statement.executeQuery(sql);
            if(resultSet.next()){
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
                Date now = new Date();
                String dateNow = sdfDate.format(now);

                String time1 = resultSet.getString("DatePing");
                if(time1.equals("0")){
                    time1 = "000000000000";
                }

                SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
                Date date1 = format.parse(time1);
                Date date2 = format.parse(dateNow);
                double difference = (date2.getTime() - date1.getTime()) / 1000;

                if(difference >= 1800){
                    return true;
                } else {
                    return false;
                }

            } else {
                return true;
            }

        } catch (Exception e){
            return true;
        }
    }

    public List<String> getOnlineDrivers(){
        List<String> list = new ArrayList<>();
        try {
            String sql = "SELECT DriverId FROM online";
            ResultSet resultSet = Statement.executeQuery(sql);
            while (resultSet.next()){
                list.add(resultSet.getString("DriverId"));
            }
        } catch (Exception e){
            System.out.println(e);
        }
        return list;
    }

    public String getStatus(String driverId){
        try {
            String sql = "SELECT Status FROM online WHERE DriverId="+driverId;
            ResultSet resultSet = Statement.executeQuery(sql);
            resultSet.next();
            return resultSet.getString("Status");
        } catch (Exception e){
            System.out.println(e);
        }
        return null;
    }
}
