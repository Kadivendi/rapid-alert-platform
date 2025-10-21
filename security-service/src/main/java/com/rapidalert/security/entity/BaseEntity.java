package com.rapidalert.security.entity;

import java.io.Serializable;

public interface BaseEntity<T extends Serializable> {

    T getId();
}
