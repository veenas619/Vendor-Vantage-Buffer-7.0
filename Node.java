public class Node {
    //the role of this file is to define the structure of one location
    String location_name;
    int stay_time;    //time to stay at current location
    int time_to_reach;  //time to reach the next location
    Node next;  //pointer to the next node
    //define a parameterised constructor to assign location details
    Node(String location,int st,int ttr)
    {
        location_name=location;
        stay_time=st;
        time_to_reach=ttr;
        next=null;  //assume this is the last location
    }
    
}
