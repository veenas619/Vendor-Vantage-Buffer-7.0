public class ETACalculator {

    // Calculate ETA from vendor's current stop to a target location name
    static int calculateETA(Vendor v, String targetLocation) {
        if (v.current_stop == null) return -1;
        if (v.current_stop.location_name.equals(targetLocation)) return 0;

        int eta = 0;
        Node temp = v.current_stop;

        // If vendor is in transit, add remaining travel time to next stop
        if (!v.reached) {
            eta += temp.time_to_reach;
            temp = temp.next;
        }

        // Traverse the circular linked list
        int steps = 0;
        int totalStops = countStops(v);

        while (steps < totalStops) {
            if (temp.location_name.equals(targetLocation)) return eta;
            eta += temp.stay_time + temp.time_to_reach;
            temp = temp.next;
            steps++;
        }

        return -1; // target not found in route
    }

    // Count total stops in the circular linked list
    static int countStops(Vendor v) {
        if (v.head == null) return 0;
        int count = 0;
        Node temp = v.head;
        do {
            count++;
            temp = temp.next;
        } while (temp != v.head);
        return count;
    }

    // Get next stop name
    static String nextStop(Vendor v) {
        if (v.current_stop == null) return "No route set";
        return v.current_stop.next.location_name;
    }
}
