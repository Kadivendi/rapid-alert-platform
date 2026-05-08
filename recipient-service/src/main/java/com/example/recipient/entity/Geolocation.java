package com.rapidalert.recipient.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Geolocation {

    private double latitude;
    private double longitude;
}
