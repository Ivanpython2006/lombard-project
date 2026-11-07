package com.lombard.model;

import jakarta.persistence.*;

@Entity
@Table(name = "number_sequences")
public class NumberSequence {

    @Id
    @Column(length = 20)
    private String prefix;

    @Column(name = "next_value", nullable = false)
    private Long nextValue = 1L;

    public NumberSequence() {}

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public Long getNextValue() { return nextValue; }
    public void setNextValue(Long nextValue) { this.nextValue = nextValue; }
}