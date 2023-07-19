package com.example.thanal_invoice;

public class InvoiceItem {
    String clientName;
    String item;
    String qty;
    String rate;
    String price;

    public InvoiceItem(String clientName, String item, String qty, String rate, String price) {
        this.clientName = clientName;
        this.item = item;
        this.qty = qty;
        this.rate = rate;
        this.price = price;
    }

    public String getClientName() {
        return clientName;
    }

    public String getItem() {
        return item;
    }

    public String getQty() {
        return qty;
    }

    public String getRate() {
        return rate;
    }

    public double getPrice() {
        return Double.parseDouble(price);
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
