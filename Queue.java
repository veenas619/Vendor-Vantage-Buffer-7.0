public class Queue{

    QueueNode front;
    QueueNode back;
    Queue(){  
        front=null;
        back=null;
    }
    void add_order(Order o){
       QueueNode newOrder=new QueueNode(o);
       
            if(front==null)
            {
                //it is an empty list
                front=newOrder;
                back=front;

            }
            else{//link the node
                back.next=newOrder;
                back=newOrder;
                back.next=null;
        }
    }

    void remove_order(){
        if(front==null) return;
        if (front == back) 
        {                // only one item
            front = null;
            back = null;
            } 
        else {
            front = front.next;
            }
        }

    void remove_order(Order o) {
        if (front == null) return;
        // If front matches, delegate to remove_order()
        if (front.order.orderId.equals(o.orderId)) {
            remove_order();
            return;
        }
        // Walk the list looking for the node before the target
        QueueNode prev = front;
        while (prev.next != null && !prev.next.order.orderId.equals(o.orderId)) {
            prev = prev.next;
        }
        if (prev.next == null) return; // not found
        if (prev.next == back) {       // removing the back node
            prev.next = null;
            back = prev;
        } else {
            prev.next = prev.next.next;
        }
    }
}




    

