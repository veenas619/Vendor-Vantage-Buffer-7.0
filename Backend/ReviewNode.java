public class ReviewNode {
    String customerName;
    int rating;
    String comment;
    ReviewNode next;

    ReviewNode(String customerName, int rating, String comment) {
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.next = null;
    }
}
