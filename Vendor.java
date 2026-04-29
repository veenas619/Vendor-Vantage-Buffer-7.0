/*import java.util.HashMap;
public class Vendor {
    String vendor_name;
    HashMap<String,Integer> menu = new HashMap<>();
    int start_time;
    Node head;
    Node tail;
    Node current_stop;
    Queue pending_orders = new Queue();
    Queue ready_orders = new Queue();
    ReviewStack reviews = new ReviewStack();
    Wallet wallet;
    boolean reached;
    boolean dayOver;   // true once the last stop has been departed

    Vendor(String name, int start) {
        vendor_name = name;
        start_time = start;
        head = null;
        tail = null;
        current_stop = null;
        reached = true;
        dayOver = false;
        wallet = new Wallet(name, 0);
    }

    void add_menuItem(String item, int price) {
        menu.put(item, price);
    }

    void add_stop(String location, int st, int ttr) {
        Node newnode = new Node(location, st, ttr);
        if (head == null) {
            head = newnode;
            newnode.next = newnode;   // circular: single node points to itself
            tail = newnode;
            current_stop = newnode;  // vendor starts at the first stop
        } else {
            tail.next = newnode;
            newnode.next = head;     // keep circular
            tail = newnode;
        }
    }

    // Called when vendor clicks "Reached Next Location"
    // current_stop was already advanced by transit() to the destination.
     
    // Just confirm arrival — do NOT advance again.
    void move() {
        if (current_stop != null && !dayOver) {
            reached = true;
        }
    }

    // Called when vendor clicks "Depart Current Location"
    // Advance current_stop to the next stop (destination).
    // If we are AT the last stop (tail) and next would wrap to head,
    // the day is over instead of looping.
    void transit() {
        if (current_stop == null || dayOver) return;
        reached=false;
        if (current_stop == tail) {
            // We are at the last stop. Departing it ends the day — do not loop.
            dayOver = true;
            reached = false;
        } else {
            current_stop = current_stop.next;
            reached = false;
        }
    }

    void take_order(Order o, boolean choice) {
        if (o.status.equals("PENDING")) {
            if (choice) {
                o.status = "ACCEPTED";
            } else {
                o.status = "DECLINED";
                pending_orders.remove_order(o);
            }
        }
    }

    void food_ready(Order o) {
        o.status = "READY";
        pending_orders.remove_order(o);
        ready_orders.add_order(o);
    }

    void order_collected(Order o) {
        o.status = "COLLECTED";
        ready_orders.remove_order(o);
    }

    /* String getStatus() {
        if (current_stop == null) return "No route set";
        if (dayOver)  return "Day ended at: " + tail.location_name;
        if (reached)  return "Selling at: " + current_stop.location_name +
                             " | Next stop: " + (current_stop == tail ? "End of Day" : current_stop.next.location_name);
        return "In transit to: " + current_stop.location_name;
    }
    String getStatus() {
        if (current_stop == null) return "No route set";
        if (dayOver && reached) return "Day ended at: " + tail.location_name;
        if (dayOver && !reached) return "Departed final stop: " + tail.location_name + " | Closing for today.";

        if (reached) {
            String next = (current_stop == tail) ? "End of Day" : current_stop.next.location_name;
            return "Selling at: " + current_stop.location_name + " | Next stop: " + next;
        } else {
            // This fixes the "Skipping" bug visually for the customer
            return "In transit: Departed " + current_stop.location_name + " 🚚 Moving to " + current_stop.next.location_name;
        }
    }
}*/
import java.util.HashMap;

public class Vendor {
    String vendor_name;
    HashMap<String, Integer> menu = new HashMap<>();
    int start_time;
    Node head;
    Node tail;
    Node current_stop;
    
    // Logic Fix: Ensure these are initialized to prevent NullPointerErrors
    Queue pending_orders = new Queue();
    Queue ready_orders = new Queue();
    ReviewStack reviews = new ReviewStack();
    
    Wallet wallet;
    boolean reached;
    boolean dayOver;

    Vendor(String name, int start) {
        this.vendor_name = name;
        this.start_time = start;
        this.head = null;
        this.tail = null;
        this.current_stop = null;
        this.reached = true; // Starts at first stop reached
        this.dayOver = false;
        this.wallet = new Wallet(name, 0);
    }

    void add_menuItem(String item, int price) {
        menu.put(item, price);
    }

    void add_stop(String location, int st, int ttr) {
        Node newnode = new Node(location, st, ttr);
        if (head == null) {
            head = newnode;
            newnode.next = newnode; 
            tail = newnode;
            current_stop = newnode; 
        } else {
            tail.next = newnode;
            newnode.next = head; 
            tail = newnode;
        }
    }

    // --- TRACKING LOGIC FIX: Prevents Stop Skipping ---

    // Called when vendor clicks "Depart Current Location"
    void transit() {
        if (current_stop == null || dayOver) return;

        // Visual Logic: We stay at the current stop but mark it as NOT reached.
        // This allows the frontend to show "Departed from [Current]"
        reached = false; 

        if (current_stop == tail) {
            dayOver = true; 
        }
    }

    // Called when vendor clicks "Reached Next Location"
    void move() {
        if (current_stop != null && !dayOver && !reached) {
            // ONLY now do we advance the pointer to the actual next stop
            current_stop = current_stop.next;
            reached = true;
        }
    }

    // --- ORDER LOGIC FIX: Proper Queue Management ---

    void take_order(Order o, boolean choice) {
        if (o.status.equals("PENDING")) {
            if (choice) {
                o.status = "ACCEPTED";
                // Order stays in pending_orders until food_ready()
            } else {
                o.status = "DECLINED";
                pending_orders.remove_order(o);
            }
        }
    }

    void food_ready(Order o) {
        if (o.status.equals("ACCEPTED")) {
            o.status = "READY";
            pending_orders.remove_order(o);
            ready_orders.add_order(o);
        }
    }

    void order_collected(Order o) {
        o.status = "COLLECTED";
        ready_orders.remove_order(o);
    }

    // --- DISPLAY LOGIC FIX: Shows destination during transit ---

    String getStatus() {
        if (current_stop == null) return "No route set";
        if (dayOver && reached) return "Day ended at: " + tail.location_name;
        if (dayOver && !reached) return "Departed final stop: " + tail.location_name + " | Closing for today.";

        if (reached) {
            String next = (current_stop == tail) ? "End of Day" : current_stop.next.location_name;
            return "Selling at: " + current_stop.location_name + " | Next stop: " + next;
        } else {
            // This fixes the "Skipping" bug visually for the customer
            return "In transit: Departed " + current_stop.location_name + " 🚚 Moving to " + current_stop.next.location_name;
        }
    }
}
