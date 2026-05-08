package com.rapidalert.template.dto.request;

import java.util.List;

public record NotificationOptionsRequest(
        List<String> options
) {
}
