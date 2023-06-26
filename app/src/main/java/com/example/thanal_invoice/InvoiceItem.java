package com.example.thanal_invoice;

public class InvoiceItem {
    private String item;
    private int quantity;
    private double rate;
    private double price;

    public InvoiceItem(String item, int quantity, double rate) {
        this.item = item;
        this.quantity = quantity;
        this.rate = rate;
        this.price = quantity * rate;
    }

    public String getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getRate() {
        return rate;
    }

    public double getPrice() {
        return price;
    }
}
