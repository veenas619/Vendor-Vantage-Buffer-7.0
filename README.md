# Vendor Vantage

## Overview

Vendor Vantage is a *DSA-based smart street food vendor management platform* built using **Java (backend)** and **HTML/CSS/JavaScript (frontend)**. It connects mobile food vendors with customers through real-time route tracking, searchable menus, order management, wallet payments, ETA prediction, and reviews.

The project demonstrates practical use of Data Structures & Algorithms in a real-world application.

---

## Key Features

### Customer Features

* Customer login
* Search vendors by name or food item
* Browse menus and place multi-item orders
* Enter your pick up location
* Wallet system with balance and prepaid deposit
* View vendor ETA to selected stops
* Submit ratings and reviews

### Vendor Features

* Vendor registration and login
* Manage/add route and stops
* Update live movement status (in transit)
* Accept or decline customer orders
* Mark food as ready for pickup
* Track earnings through wallet

---

## DSA Concepts Used

| Data Structure / Algorithm | Usage in Project                                              |
| -------------------------- | --------------------------------------------------------      |
| Circular Linked List       | Stores vendor route stops and enables cyclic movement         |
| Queue                      | Manages pending and ready orders                              |
| Stack                      | Stores customer reviews in order of most to least recent      |
| HashMap                    | Fast lookup for vendors, menu items orders, storing ratings   |
| Search Indexing            | Keyword-based vendor/menu search                              |
| Traversal Algorithm        | ETA calculation across route stops                            |

---

## Project Structure

```text
VendorVantage/
│── Server.java            # Main backend server
│── Vendor.java            # Vendor model and route logic
│── SearchEngine.java      # Search functionality
│── Queue.java             # Order queue implementation
│── ReviewStack.java       # Review stack implementation
│── ETACalculator.java     # ETA prediction logic
│── Wallet.java            # Wallet management
│── Order.java             # Order model
│── index.html             # Landing page
│── dashboard.html         # Customer dashboard
│── vendor-dashboard.html  # Vendor dashboard
│── style.css              # Styling
│── script.js              # Frontend interactions
```

---

## Tech Stack

* **Backend:** Java HTTP Server (`com.sun.net.httpserver`)
* **Frontend:** HTML, CSS, JavaScript
* **Storage:** In-memory data structures
* **Architecture:** Client-Server model

---

# How to Run
Follow these steps to set up the environment and launch the Vendor Vantage platform.
1. Open Terminal in Project Folder.
Navigate to the root directory where the source files are located. This ensures the compiler can locate all the necessary .java and frontend files.
bash
cd VendorVantage_fixed_new

2. Compile Java Source Files.
Since this project uses a modular Java backend with custom data structure implementations (like Queue and ReviewStack), you must compile all dependencies together. Run the following command to generate the .class bytecode files:
bash
javac Node.java QueueNode.java Order.java Queue.java ReviewNode.java ReviewStack.java Wallet.java Vendor.java ETACalculator.java SearchEngine.java Server.java
(Note: Ensure you have the Java Development Kit (JDK) installed and added to your system's PATH.)

3. Start the Backend Server
Once compiled, launch the Server.java file. This starts the com.sun.net.httpserver which handles all the API requests from the frontend and manages the in-memory data structures.
bash
java Server

Expected Output:
text
Server running on http://localhost:8080
(Keep this terminal window open to ensure the backend remains active.)

4. Launch the Frontend
Vendor Vantage uses a Client-Server model. To access the user interface:
Locate the index.html file in the project folder.
Right-click the file and select Open with your preferred web browser (Chrome, Firefox, or Edge).
Alternatively, if you are using VS Code, you can use the Live Server extension for a smoother experience.
---

## Demo Vendor Accounts

| Vendor Key | Password |
| vicky      | vicky123 |
| sai        | sai123   |
| pizza      | pizza123 |

---

## Sample Workflow

1. Customer Discovery & Search
Customer Action: The user logs into the Customer Dashboard.
System Action: Using the SearchEngine.java (powered by HashMap and Search Indexing), the customer searches for "Pizza" or a specific vendor like "Vicky."

Result: The system returns a filtered list of vendors matching the criteria with high performance.


2. Route Tracking & ETA Calculation
Customer Action: The customer selects a vendor and enters their current pickup location.
System Action: The ETACalculator.java uses a Traversal Algorithm on the vendor’s Circular Linked List of stops to calculate how long it will take for the vendor to reach the customer's specific stop.

Result: The customer sees a real-time ETA prediction.


3. Placing the Order
Customer Action: The customer browses the menu, adds items to their cart, and places a multi-item order.
System Action: The system checks the Wallet.java to ensure the customer has a sufficient prepaid balance. If verified, the order is created.

Result: The order is sent to the vendor’s terminal, and funds are held.


4. Vendor Management & Order Processing
Vendor Action: The vendor (e.g., "Vicky") logs in and sees a new order in their Pending Queue.
System Action: The backend uses Queue.java to manage orders in a First-In-First-Out (FIFO) manner, ensuring customers are served in the order they arrived.
Vendor Action: The vendor clicks "Accept" and, once the food is prepared, marks the order as "Ready for Pickup."

5. Collection & Finalization
Customer Action: Once the vendor arrives at the designated stop, the customer collects their food.
Vendor Action: The vendor marks the order as completed.
System Action: The payment is officially transferred to the vendor’s wallet, and the earnings are updated.

6. Review & Feedback (The Stack)
Customer Action: After the meal, the customer submits a 5-star rating and a comment.
System Action: This review is pushed onto the ReviewStack.java.
Result: Because a Stack (LIFO) is used, the most recent reviews appear at the top of the vendor's profile for other customers to see first.
---

## Learning Outcomes

* Real-world implementation of core DSA concepts
* Frontend-backend integration
* State management using in-memory structures
* Problem solving through modular design

---

## Future Enhancements

* Database integration (MySQL / MongoDB)
* GPS live tracking with maps
* Online payment gateway
* Push notifications
* Recommendation engine

---

## Authors

Developed for BUFFER by : 
Veena Shastri
Hitanshi Shah
Pranjal Patil
Swarali Budruk
---

## License

For educational and academic use.
