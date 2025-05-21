package com.learn.common.constants;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class NacosConstants {
    @Value("${server.domain}")
    private String domain;
}
