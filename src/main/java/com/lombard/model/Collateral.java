package com.lombard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collateral")
public class Collateral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_number", unique = true, length = 20)
    private String itemNumber;

    private String name;
    private String description;

    @Column(name = "estimated_price", precision = 19, scale = 2)
    private BigDecimal estimatedPrice;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "collateral_photos",
            joinColumns = @JoinColumn(name = "collateral_id")
    )
    @Column(name = "photo_url", length = 500)
    @OrderColumn(name = "sort_order")
    private List<String> photoUrls = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @PrePersist
    public void prePersist() {
        if (itemNumber == null || itemNumber.isBlank()) {
            itemNumber = "M_UNKNOWN";
        }
    }

    public Collateral() {}

    public Collateral(String name, String description, BigDecimal estimatedPrice) {
        this.name = name;
        this.description = description;
        this.estimatedPrice = estimatedPrice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getItemNumber() { return itemNumber; }
    public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    @Transient
    public String getMainPhoto() {
        if (photoUrls == null || photoUrls.isEmpty()) return null;
        return photoUrls.get(0);
    }

    @Transient
    public boolean hasPhotos() {
        return photoUrls != null && !photoUrls.isEmpty();
    }
}