import java.util.ArrayList;
import java.util.UUID;
//this file is for determining the structure of one order
public class Order{
    String orderId; //unique
    String status;  //pending,accepted, declined
    ArrayList<String> orders=new ArrayList<>(); //items to be ordered
    int bill;   //bill of the order
    Vendor v;   //chosen vendor
    String location;
    String Customer_name;
    Order(ArrayList<String> orders, Vendor v, String location, String name)
    {
        this.orders=orders;
        this.v=v;
        bill=0;
        this.location=location;
        orderId=UUID.randomUUID().toString();
        status="PENDING";
        Customer_name=name;
    }
    boolean valid_order(){
        //search for the items in array list.
        for(String order:orders){
            if(!v.menu.containsKey(order))
            {
                return false;
            }
            
        }
        return true;
    }
    void calculate_bill()
    {
        if(valid_order()){
            for(String order:orders){
                bill+=v.menu.get(order);
            }
        }
    }


}