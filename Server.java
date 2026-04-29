import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

public class Server {

    static SearchEngine searchEngine = new SearchEngine();
    static HashMap<String, Vendor>  vendors  = new HashMap<>();
    static HashMap<String, Wallet>  wallets  = new HashMap<>();
    static HashMap<String, Order>   orders   = new HashMap<>();
    static HashMap<String, String>  vendorPasswords = new HashMap<>();
    // BUG 6 FIX: Track which orders have been confirmed by the customer
    static HashMap<String, Boolean> customerConfirmedCollect = new HashMap<>();
    // BUG 4 FIX: Track vendors who ended their day early
    static HashMap<String, Boolean> dayEndedEarly = new HashMap<>();

    public static void main(String[] args) throws Exception {
        seedData();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/vendors",              Server::handleGetVendors);
        server.createContext("/api/search",               Server::handleSearch);
        server.createContext("/api/vendor/register",      Server::handleVendorRegister);
        server.createContext("/api/vendor/login",         Server::handleVendorLogin);
        server.createContext("/api/vendor/move",          Server::handleMove);
        server.createContext("/api/vendor/transit",       Server::handleTransit);
        server.createContext("/api/vendor/status",        Server::handleVendorStatus);
        server.createContext("/api/vendor/orders",        Server::handleVendorOrders);
        server.createContext("/api/vendor/accept",        Server::handleAcceptOrder);
        server.createContext("/api/vendor/food_ready",    Server::handleFoodReady);
        server.createContext("/api/vendor/end_day",       Server::handleEndDay);
        server.createContext("/api/order/place",          Server::handlePlaceOrder);
        server.createContext("/api/order/collect",        Server::handleCollect);
        server.createContext("/api/order/customer_ready", Server::handleCustomerReady);
        server.createContext("/api/order/status",         Server::handleOrderStatus);
        server.createContext("/api/review/add",           Server::handleAddReview);
        server.createContext("/api/review/get",           Server::handleGetReviews);
        server.createContext("/api/wallet/balance",       Server::handleWalletBalance);
        server.createContext("/api/wallet/topup",         Server::handleWalletTopup);
        server.createContext("/api/eta",                  Server::handleETA);

        server.start();
        System.out.println("Server running on http://localhost:8080");
    }

    static void seedData() {
        Vendor v1 = new Vendor("Vicky's Veggie Delight", 800);
        v1.add_stop("Karve Road", 10, 15);
        v1.add_stop("Cummins College", 12, 20);
        v1.add_stop("Kothrud Stand", 8, 15);
        v1.add_menuItem("Vada Pav", 20);
        v1.add_menuItem("Samosa", 25);
        v1.add_menuItem("Misal", 80);
        vendors.put("vicky", v1);
        vendorPasswords.put("vicky", "vicky123");
        wallets.put("vicky", new Wallet("vicky", 0));
        searchEngine.addToSearch(v1);

        Vendor v2 = new Vendor("Sai Snacks Center", 900);
        v2.add_stop("Ideal Colony", 10, 20);
        v2.add_stop("MIT College", 15, 25);
        v2.add_stop("Vanaz", 10, 20);
        v2.add_menuItem("Cold Coffee", 30);
        v2.add_menuItem("Sandwich", 50);
        v2.add_menuItem("Burger", 70);
        vendors.put("sai", v2);
        vendorPasswords.put("sai", "sai123");
        wallets.put("sai", new Wallet("sai", 0));
        searchEngine.addToSearch(v2);

        Vendor v3 = new Vendor("The Pizza Hub", 1000);
        v3.add_stop("Pune Camp", 20, 30);
        v3.add_stop("Swargate", 15, 25);
        v3.add_stop("Shivajinagar", 10, 20);
        v3.add_menuItem("Pizza Slice", 99);
        v3.add_menuItem("Coke", 40);
        vendors.put("pizza", v3);
        vendorPasswords.put("pizza", "pizza123");
        wallets.put("pizza", new Wallet("pizza", 0));
        searchEngine.addToSearch(v3);

        wallets.put("demo", new Wallet("demo", 500));
    }

    static void send(HttpExchange e, int code, String body) throws IOException {
        e.getResponseHeaders().set("Content-Type", "application/json");
        e.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        e.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        e.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if (e.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            e.sendResponseHeaders(204, -1); return;
        }
        byte[] bytes = body.getBytes("UTF-8");
        e.sendResponseHeaders(code, bytes.length);
        OutputStream os = e.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static String readBody(HttpExchange e) throws IOException {
        return new String(e.getRequestBody().readAllBytes(), "UTF-8");
    }

    static String param(HttpExchange e, String key) {
        String query = e.getRequestURI().getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key))
                return kv[1].replace("%20", " ").replace("+", " ");
        }
        return null;
    }

    static String vendorToJson(String key, Vendor v) {
        if (v == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"key\":\"").append(key).append("\",");
        sb.append("\"name\":\"").append(v.vendor_name.replace("\"","\\\"")).append("\",");
        // Natural day-end (departed last stop) OR vendor pressed End Day Early
        boolean isDayOver = v.dayOver || dayEndedEarly.getOrDefault(key, false);
        String vendorStatus = isDayOver ? "DAY-ENDED" : v.reached ? "ON-SITE" : "IN-TRANSIT";
        sb.append("\"status\":\"").append(vendorStatus).append("\",");
        sb.append("\"dayEnded\":").append(isDayOver).append(",");
        if (v.current_stop != null && !isDayOver) {
            if (!v.reached) {
        // IN-TRANSIT: current_stop is WHERE VENDOR DEPARTED FROM
        // The destination is current_stop.next
            Node dest = v.current_stop.next;
            sb.append("\"currentStop\":\"").append(dest.location_name).append("\",");
            boolean destIsLast = (dest == v.tail);
            String nextName = destIsLast ? "End of Day" : dest.next.location_name;
            sb.append("\"nextStop\":\"").append(nextName).append("\",");
    } else {
        // ON-SITE: current_stop is exactly where vendor is
        sb.append("\"currentStop\":\"").append(v.current_stop.location_name).append("\",");
        boolean isLastStop = (v.current_stop == v.tail);
        String nextName = isLastStop ? "End of Day" : v.current_stop.next.location_name;
        sb.append("\"nextStop\":\"").append(nextName).append("\",");
    }
} 
        else if (isDayOver) {
            String lastStop = v.tail != null ? v.tail.location_name : "Last Stop";
            sb.append("\"currentStop\":\"").append(lastStop).append("\",");
            sb.append("\"nextStop\":\"End of Day\",");
        } else {
            sb.append("\"currentStop\":\"N/A\",\"nextStop\":\"N/A\",");
        }
        sb.append("\"rating\":").append(String.format("%.1f", v.reviews.averageRating())).append(",");
        sb.append("\"menu\":{");
        boolean first = true;
        for (Map.Entry<String,Integer> entry : v.menu.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey().replace("\"","\\\"")).append("\":").append(entry.getValue());
            first = false;
        }
        sb.append("},\"stops\":[");
        if (v.head != null) {
            Node temp = v.head; boolean fs = true;
            do {
                if (!fs) sb.append(",");
                sb.append("\"").append(temp.location_name).append("\"");
                fs = false; temp = temp.next;
            } while (temp != v.head);
        }
        sb.append("]}");
        return sb.toString();
    }

    static String orderToJson(Order o) {
        boolean custReady = customerConfirmedCollect.getOrDefault(o.orderId, false);
        return "{\"orderId\":\""+o.orderId+"\"," +
               "\"status\":\""+o.status+"\"," +
               "\"bill\":"+o.bill+"," +
               "\"customer\":\""+o.Customer_name+"\"," +
               "\"location\":\""+o.location+"\"," +
               "\"customerReady\":"+custReady+"," +
               "\"items\":"+listToJson(o.orders)+"}";
    }

    static String listToJson(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i).replace("\"","\\\"")).append("\"");
        }
        return sb.append("]").toString();
    }

    static void handleGetVendors(HttpExchange e) throws IOException {
        ArrayList<Vendor> all = searchEngine.getAllVendors();
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Vendor v : all) {
            String key = getKeyForVendor(v);
            if (!first) sb.append(",");
            sb.append(vendorToJson(key, v));
            first = false;
        }
        send(e, 200, sb.append("]").toString());
    }

    static String getKeyForVendor(Vendor target) {
        for (Map.Entry<String,Vendor> entry : vendors.entrySet()) {
            if (entry.getValue() == target) return entry.getKey();
        }
        return "unknown";
    }

    static void handleSearch(HttpExchange e) throws IOException {
        String keyword = param(e, "q");
        if (keyword == null) { send(e, 400, "{\"error\":\"No keyword\"}"); return; }
        ArrayList<Vendor> results = searchEngine.search(keyword);
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Vendor v : results) {
            if (!first) sb.append(",");
            sb.append(vendorToJson(getKeyForVendor(v), v));
            first = false;
        }
        send(e, 200, sb.append("]").toString());
    }

    static void handleVendorRegister(HttpExchange e) throws IOException {
        String body = readBody(e);
        String name = extractJson(body, "name");
        String pw   = extractJson(body, "password");
        if (name == null || name.isEmpty()) { send(e, 400, "{\"error\":\"name required\"}"); return; }
        if (pw   == null || pw.isEmpty())   { send(e, 400, "{\"error\":\"password required\"}"); return; }

        String key = name.toLowerCase().replaceAll("[^a-z0-9]", "_");
        if (vendors.containsKey(key)) {
            send(e, 400, "{\"error\":\"Vendor key already exists. Try a different name.\"}"); return;
        }

        Vendor v = new Vendor(name, 800);

        int stopsIdx = body.indexOf("\"stops\"");
        if (stopsIdx != -1) {
            int arrS = body.indexOf("[", stopsIdx);
            int arrE = body.lastIndexOf("]");
            if (arrS != -1 && arrE > arrS) {
                String arr = body.substring(arrS+1, arrE);
                for (String obj : arr.split("\\},\\s*\\{")) {
                    obj = obj.replaceAll("[\\[\\]\\{\\}]", "");
                    String sname   = extractJson("{"+obj+"}", "name");
                    String sstay   = extractJson("{"+obj+"}", "stay");
                    String stravel = extractJson("{"+obj+"}", "travel");
                    if (sname != null && !sname.isEmpty()) {
                        int stay   = sstay   != null ? safeInt(sstay,   15) : 15;
                        int travel = stravel != null ? safeInt(stravel, 10) : 10;
                        v.add_stop(sname, stay, travel);
                    }
                }
            }
        }

        int menuIdx = body.indexOf("\"menu\"");
        if (menuIdx != -1) {
            int objS = body.indexOf("{", menuIdx);
            int objE = body.indexOf("}", objS);
            if (objS != -1 && objE > objS) {
                String obj = body.substring(objS+1, objE);
                for (String pair : obj.split(",")) {
                    pair = pair.trim();
                    int colon = pair.lastIndexOf(":");
                    if (colon == -1) continue;
                    String item  = pair.substring(0, colon).trim().replaceAll("\"","");
                    String price = pair.substring(colon+1).trim().replaceAll("[^0-9]","");
                    if (!item.isEmpty() && !price.isEmpty())
                        v.add_menuItem(item, safeInt(price, 0));
                }
            }
        }

        vendors.put(key, v);
        vendorPasswords.put(key, pw);
        wallets.put(key, new Wallet(key, 0));
        searchEngine.addToSearch(v);
        send(e, 200, "{\"key\":\""+key+"\",\"name\":\""+name+"\",\"message\":\"Registered\"}");
    }

    static void handleVendorLogin(HttpExchange e) throws IOException {
        String body = readBody(e);
        String key  = extractJson(body, "key");
        String pw   = extractJson(body, "password");
        if (key == null || pw == null) { send(e, 400, "{\"error\":\"key and password required\"}"); return; }
        key = key.toLowerCase().trim();
        Vendor v = vendors.get(key);
        String stored = vendorPasswords.get(key);
        if (v == null || stored == null || !stored.equals(pw)) {
            send(e, 401, "{\"error\":\"Invalid vendor key or password\"}"); return;
        }
        send(e, 200, vendorToJson(key, v).replace("}", ",\"loginOk\":true}"));
    }

    static void handleMove(HttpExchange e) throws IOException {
        String key = param(e, "vendor");
        Vendor v = vendors.get(key);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        v.move();
        String curName = v.dayOver ? (v.tail != null ? v.tail.location_name : "End") : v.current_stop.location_name;
        send(e, 200, "{\"message\":\"Moved\",\"currentStop\":\""+curName+"\",\"dayOver\":"+v.dayOver+"}");
    }

    static void handleTransit(HttpExchange e) throws IOException {
        String key = param(e, "vendor");
        Vendor v = vendors.get(key);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        v.transit();
        send(e, 200, "{\"message\":\"In transit\"}");
    }

    static void handleVendorStatus(HttpExchange e) throws IOException {
        String key = param(e, "vendor");
        Vendor v = vendors.get(key);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        send(e, 200, vendorToJson(key, v));
    }

    // BUG 1 FIX: Only return orders that belong to THIS vendor's queue
    static void handleVendorOrders(HttpExchange e) throws IOException {
        String key = param(e, "vendor");
        Vendor v = vendors.get(key);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        StringBuilder pending = new StringBuilder();
        StringBuilder ready   = new StringBuilder();
        boolean fp = true, fr = true;
        for (QueueNode t = v.pending_orders.front; t != null; t = t.next) {
            // BUG 1 FIX: Only include orders where the vendor reference matches
            if (t.order.v != v) continue;
            if (!fp) pending.append(","); pending.append(orderToJson(t.order)); fp = false;
        }
        for (QueueNode t = v.ready_orders.front; t != null; t = t.next) {
            if (t.order.v != v) continue;
            if (!fr) ready.append(","); ready.append(orderToJson(t.order)); fr = false;
        }
        send(e, 200, "{\"pending\":["+pending+"],\"ready\":["+ready+"]}");
    }

    // BUG 1 FIX: Verify order belongs to this vendor before accepting
    static void handleAcceptOrder(HttpExchange e) throws IOException {
        String body = readBody(e);
        String vkey = extractJson(body, "vendor");
        String oid  = extractJson(body, "orderId");
        String ch   = extractJson(body, "choice");
        Vendor v = vendors.get(vkey); Order o = orders.get(oid);
        if (v == null || o == null) { send(e, 404, "{\"error\":\"Not found\"}"); return; }
        // BUG 1 FIX: Security check — order must belong to this vendor
        if (o.v != v) { send(e, 403, "{\"error\":\"Unauthorized: Order does not belong to this vendor\"}"); return; }
        v.take_order(o, "true".equals(ch));
        if ("DECLINED".equals(o.status)) {
            Wallet cw = wallets.get(o.Customer_name);
            if (cw != null) {
            cw.refundCustomer(o);
    }
}
        send(e, 200, "{\"status\":\""+o.status+"\"}");
    }

    // BUG 1 FIX: Verify order belongs to this vendor before marking food ready
    static void handleFoodReady(HttpExchange e) throws IOException {
        String body = readBody(e);
        String vkey = extractJson(body, "vendor");
        String oid  = extractJson(body, "orderId");
        Vendor v = vendors.get(vkey); Order o = orders.get(oid);
        if (v == null || o == null) { send(e, 404, "{\"error\":\"Not found\"}"); return; }
        if (o.v != v) { send(e, 403, "{\"error\":\"Unauthorized: Order does not belong to this vendor\"}"); return; }
        v.food_ready(o);
        send(e, 200, "{\"status\":\""+o.status+"\"}");
    }

    // BUG 4 FIX: New endpoint — vendor ends day early
    static void handleEndDay(HttpExchange e) throws IOException {
        String body = readBody(e);
        String vkey = extractJson(body, "vendor");
        Vendor v = vendors.get(vkey);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        dayEndedEarly.put(vkey, true);
        send(e, 200, "{\"message\":\"Day ended early. Customers will be notified.\",\"dayEnded\":true}");
    }

    static void handlePlaceOrder(HttpExchange e) throws IOException {
        String body     = readBody(e);
        String vkey     = extractJson(body, "vendor");
        String customer = extractJson(body, "customer");
        String location = extractJson(body, "location");
        String itemsRaw = extractJson(body, "items");
        Vendor v = vendors.get(vkey);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }

        // Block new orders if vendor ended day early OR naturally completed route
        if (v.dayOver || dayEndedEarly.getOrDefault(vkey, false)) {
            send(e, 400, "{\"error\":\"VENDOR_DAY_ENDED\",\"message\":\"This vendor has ended their day early and is no longer accepting orders.\"}");
            return;
        }

        ArrayList<String> items = new ArrayList<>();
        if (itemsRaw != null) {
            for (String item : itemsRaw.replaceAll("[\\[\\]\"]","").split(","))
                if (!item.trim().isEmpty()) items.add(item.trim());
        }

        Order o = new Order(items, v, location != null ? location : "", customer != null ? customer : "Guest");
        o.calculate_bill();
        orders.put(o.orderId, o);
        v.pending_orders.add_order(o);

        wallets.putIfAbsent(customer, new Wallet(customer, 500));
        Wallet cw = wallets.get(customer);
        boolean locked = o.bill > 0 && o.valid_order() && cw.lockTokens(o);

        // BUG 5 FIX: Calculate ETA to the CUSTOMER'S location specifically
        String customerLoc   = location != null ? location.trim() : "";
        int etaToCustomer    = ETACalculator.calculateETA(v, customerLoc);
        // If location is not found on route (returns -1), use current stop transit time as fallback
        if (etaToCustomer < 0 && v.current_stop != null) {
            etaToCustomer = v.current_stop.time_to_reach;
        }

        String vendorCurStop = (v.current_stop != null) ? v.current_stop.location_name : "";
        String vendorStat    = v.reached ? "ON-SITE" : "IN-TRANSIT";

        send(e, 200, "{\"orderId\":\""+o.orderId+"\"," +
            "\"bill\":"+o.bill+"," +
            "\"valid\":"+o.valid_order()+"," +
            "\"tokenLocked\":"+locked+"," +
            "\"walletBalance\":"+cw.getBalance()+"," +
            "\"status\":\""+o.status+"\"," +
            "\"vendorCurrentStop\":\""+vendorCurStop+"\"," +
            "\"etaToYourLocation\":"+etaToCustomer+"," +
            "\"vendorStatus\":\""+vendorStat+"\"}");
    }

    // BUG 6 FIX: Customer must confirm arrival before vendor can mark collected
    static void handleCustomerReady(HttpExchange e) throws IOException {
        String body = readBody(e);
        String oid  = extractJson(body, "orderId");
        Order o = orders.get(oid);
        if (o == null) { send(e, 404, "{\"error\":\"Order not found\"}"); return; }
        if (!"READY".equals(o.status)) {
            send(e, 400, "{\"error\":\"Order is not in READY state yet\"}"); return;
        }
        customerConfirmedCollect.put(oid, true);
        send(e, 200, "{\"message\":\"You have confirmed arrival. The vendor will now complete the handoff.\",\"customerReady\":true}");
    }

    // BUG 6 FIX: Vendor collect only allowed after customer confirms
    static void handleCollect(HttpExchange e) throws IOException {
        String body = readBody(e);
        String vkey = extractJson(body, "vendor");
        String oid  = extractJson(body, "orderId");
        Vendor v = vendors.get(vkey); Order o = orders.get(oid);
        if (v == null || o == null) { send(e, 404, "{\"error\":\"Not found\"}"); return; }
        if (o.v != v) { send(e, 403, "{\"error\":\"Unauthorized: Order does not belong to this vendor\"}"); return; }
        // BUG 6 FIX: Vendor cannot complete collect until customer has confirmed
        if (!customerConfirmedCollect.getOrDefault(oid, false)) {
            send(e, 400, "{\"error\":\"CUSTOMER_NOT_READY\",\"message\":\"Customer has not confirmed arrival yet. Please wait.\"}");
            return;
        }
        v.order_collected(o);
        customerConfirmedCollect.remove(oid);
        Wallet cw = wallets.get(o.Customer_name);
        Wallet vw = wallets.get(vkey);
        if (cw == null) cw = new Wallet(o.Customer_name, 0);
        if (vw == null) { vw = new Wallet(vkey, 0); wallets.put(vkey, vw); }
        cw.payVendor(vw, o);
        send(e, 200, "{\"status\":\""+o.status+"\",\"vendorTokens\":"+vw.getBalance()+"}");
    }

    static void handleOrderStatus(HttpExchange e) throws IOException {
        String oid = param(e, "id");
        Order o = orders.get(oid);
        if (o == null) { send(e, 404, "{\"error\":\"Order not found\"}"); return; }
        send(e, 200, orderToJson(o));
    }

    static void handleAddReview(HttpExchange e) throws IOException {
        String body     = readBody(e);
        String vkey     = extractJson(body, "vendor");
        String customer = extractJson(body, "customer");
        String rstr     = extractJson(body, "rating");
        String comment  = extractJson(body, "comment");
        Vendor v = vendors.get(vkey);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        int rating = rstr != null ? safeInt(rstr, 5) : 5;
        v.reviews.push(customer != null ? customer : "Anonymous", rating, comment != null ? comment : "");
        send(e, 200, "{\"message\":\"Review added\",\"avgRating\":"+String.format("%.1f", v.reviews.averageRating())+"}");
    }

    static void handleGetReviews(HttpExchange e) throws IOException {
        String key = param(e, "vendor");
        Vendor v = vendors.get(key);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        ArrayList<ReviewNode> reviews = v.reviews.getAllReviews();
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ReviewNode r : reviews) {
            if (!first) sb.append(",");
            sb.append("{\"customer\":\"").append(r.customerName)
              .append("\",\"rating\":").append(r.rating)
              .append(",\"comment\":\"").append(r.comment.replace("\"","\\\"")).append("\"}");
            first = false;
        }
        send(e, 200, sb.append("]").toString());
    }

    static void handleWalletBalance(HttpExchange e) throws IOException {
        String customer = param(e, "customer");
        if (customer == null) { send(e, 400, "{\"error\":\"customer required\"}"); return; }
        wallets.putIfAbsent(customer, new Wallet(customer, 500));
        Wallet w = wallets.get(customer);
        send(e, 200, "{\"customer\":\""+customer+"\",\"balance\":"+w.getBalance()+"}");
    }

    static void handleWalletTopup(HttpExchange e) throws IOException {
        String body     = readBody(e);
        String customer = extractJson(body, "customer");
        String amtStr   = extractJson(body, "amount");
        if (customer == null) { send(e, 400, "{\"error\":\"customer required\"}"); return; }
        wallets.putIfAbsent(customer, new Wallet(customer, 500));
        Wallet w = wallets.get(customer);
        int amt = safeInt(amtStr, 100);
        w.balance += amt;
        send(e, 200, "{\"balance\":"+w.getBalance()+"}");
    }

    static void handleETA(HttpExchange e) throws IOException {
        String key    = param(e, "vendor");
        String target = param(e, "target");
        Vendor v = vendors.get(key);
        if (v == null) { send(e, 404, "{\"error\":\"Vendor not found\"}"); return; }
        int eta = ETACalculator.calculateETA(v, target != null ? target : "");
        send(e, 200, "{\"eta\":"+eta+",\"target\":\""+target+"\"}");
    }

    static String extractJson(String body, String key) {
        if (body == null || key == null) return null;
        String search = "\"" + key + "\"";
        int idx = body.indexOf(search);
        if (idx == -1) return null;
        int colon = body.indexOf(":", idx + search.length());
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < body.length() && (body.charAt(start)==' '||body.charAt(start)=='\t'||body.charAt(start)=='\n'||body.charAt(start)=='\r')) start++;
        if (start >= body.length()) return null;
        if (body.charAt(start) == '"') {
            int end = body.indexOf('"', start + 1);
            return end == -1 ? null : body.substring(start + 1, end);
        } else if (body.charAt(start) == '[') {
            int depth = 0, end = start;
            while (end < body.length()) {
                if (body.charAt(end)=='[') depth++;
                else if (body.charAt(end)==']') { depth--; if (depth==0) break; }
                end++;
            }
            return body.substring(start, end + 1);
        } else if (body.charAt(start) == '{') {
            int depth = 0, end = start;
            while (end < body.length()) {
                if (body.charAt(end)=='{') depth++;
                else if (body.charAt(end)=='}') { depth--; if (depth==0) break; }
                end++;
            }
            return body.substring(start, end + 1);
        } else {
            int end = start;
            while (end < body.length() && body.charAt(end)!=',' && body.charAt(end)!='}' && body.charAt(end)!=']') end++;
            return body.substring(start, end).trim();
        }
    }

    static int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
