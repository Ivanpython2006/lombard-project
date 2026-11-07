package com.lombard.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CheckoutForm {

    @NotBlank
    @Pattern(regexp = "DELIVERY|PICKUP", message = "DELIVERY или PICKUP")
    private String deliveryType;

    private String city;
    private String street;
    private String building;
    private String apartment;
    private String pickupAddress;

    @NotBlank
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Формат +7XXXXXXXXXX")
    private String contactPhone;

    private String comment;

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getApartment() { return apartment; }
    public void setApartment(String apartment) { this.apartment = apartment; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}