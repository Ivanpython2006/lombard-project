package com.lombard.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "item_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal itemPrice;

    @Column(name = "item_number", length = 20)
    private String itemNumber;

    @Column(name = "item_description", columnDefinition = "text")
    private String itemDescription;

    @Column(name = "item_photo", length = 500)
    private String itemPhoto;

    public OrderItem() {}

    public OrderItem(Order order, Loan loan, String itemName, BigDecimal itemPrice) {
        this.order = order;
        this.loan = loan;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        if (loan != null && !loan.getCollaterals().isEmpty()) {
            Collateral collateral = loan.getCollaterals().get(0);
            this.itemNumber = collateral.getItemNumber();
            this.itemDescription = collateral.getDescription();
            this.itemPhoto = collateral.getMainPhoto();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getItemPrice() { return itemPrice; }
    public void setItemPrice(BigDecimal itemPrice) { this.itemPrice = itemPrice; }
    public String getItemNumber() { return itemNumber; }
    public String getItemDescription() { return itemDescription; }
    public String getItemPhoto() { return itemPhoto; }
}
