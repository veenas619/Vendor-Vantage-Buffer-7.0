import java.util.HashMap;

public class Wallet {
    String owner;
    int balance;
    HashMap<String, Integer> escrow = new HashMap<>();

    Wallet(String owner, int initialBalance) {
        this.owner = owner;
        this.balance = initialBalance;
    }

    boolean lockTokens(Order o) {
        int amount = o.bill / 4;
        if (balance < amount) return false;
        balance -= amount;
        escrow.put(o.orderId, amount);
        return true;
    }

    void payVendor(Wallet vendorWallet, Order o) {
        int amount = escrow.getOrDefault(o.orderId, 0);
        vendorWallet.balance += amount;
        escrow.remove(o.orderId);
    }

    void refundCustomer(Order o) {
        int amount = escrow.getOrDefault(o.orderId, 0);
        balance += amount;
        escrow.remove(o.orderId);
    }

    int getBalance() {
        return balance;
    }
}