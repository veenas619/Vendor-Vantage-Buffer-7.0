import java.util.ArrayList;

public class ReviewStack {
    private ReviewNode top;
    private int size;

    ReviewStack() {
        top = null;
        size = 0;
    }

    void push(String customerName, int rating, String comment) {
        ReviewNode newNode = new ReviewNode(customerName, rating, comment);
        newNode.next = top;
        top = newNode;
        size++;
    }

    ReviewNode peek() {
        return top;
    }

    double averageRating() {
        if (top == null) return 0;
        double sum = 0;
        int count = 0;
        ReviewNode temp = top;
        while (temp != null) {
            sum += temp.rating;
            count++;
            temp = temp.next;
        }
        return sum / count;
    }

    ArrayList<ReviewNode> getAllReviews() {
        ArrayList<ReviewNode> list = new ArrayList<>();
        ReviewNode temp = top;
        while (temp != null) {
            list.add(temp);
            temp = temp.next;
        }
        return list;
    }

    int getSize() { return size; }
}

