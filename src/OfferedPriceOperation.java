
class OfferedPriceOperation extends Constants{
    private DataBaseOrder DataBase = new DataBaseOrder();
    private String OrderNumber, DriverId;

    OfferedPriceOperation(String orderNumber, String driverId){
        OrderNumber = orderNumber;
        DriverId = driverId;
    }

    void addToDataBase(String offeredPrice, String offeredTime){
        DataBase.newOfferedPrice(OrderNumber, DriverId, offeredPrice, offeredTime);

        for (Dispatcher dispatcher : Dispatcher.getDispatchers()) {
            dispatcher.send("[offer~" + DriverId + "$" + OrderNumber + "$" + offeredPrice + "$" + offeredTime + "]");
        }

        OrderOperation orderOperation = new OrderOperation(OrderNumber);
        if (orderOperation.isFree(OrderNumber, offeredPrice)) {
            orderOperation.chose(DriverId);
        }
    }

    void updateOfferedPrice(String newOfferedPrice, String offeredTime){
        DataBase.updateOfferedPrice(OrderNumber, DriverId, newOfferedPrice, offeredTime);

        for(Dispatcher dispatcher : Dispatcher.getDispatchers()){
            dispatcher.send("[updoff~"+DriverId+"$"+OrderNumber+"$"+newOfferedPrice+"$"+offeredTime+"]");
        }
    }
}
