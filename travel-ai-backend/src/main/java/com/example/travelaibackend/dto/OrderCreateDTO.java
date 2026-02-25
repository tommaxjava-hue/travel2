package com.example.travelaibackend.dto;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OrderCreateDTO {
    @NotNull(message = "景点ID不能为空")
    private Integer spotId;

    @NotBlank(message = "景点名称不能为空")
    private String spotName;

    @NotNull(message = "订单金额不能为空")
    @Min(value = 0, message = "金额不能为负数")
    private Double price;
}