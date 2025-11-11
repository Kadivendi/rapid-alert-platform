package com.rapidalert.recipient.builder;

public interface Product {

    String toJson();

    default String format(String string) {
        return string == null ? "null" : "\"" + string + "\"";
    }
}
