package com.ticketbooking.event.domain.model;

/**
 * Aggregate Root del bounded context de Venues.
 * POJO puro. Referenciado por Event mediante su ID (no como objeto embebido — DDD).
 */
public class Venue {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private int totalCapacity;

    public Venue(String name, String address, String city, String country, int totalCapacity) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.country = country;
        this.totalCapacity = totalCapacity;
    }

    public Venue(Long id, String name, String address, String city, String country, int totalCapacity) {
        this(name, address, city, country, totalCapacity);
        this.id = id;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public int getTotalCapacity() { return totalCapacity; }
}
