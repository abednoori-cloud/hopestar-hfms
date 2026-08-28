package com.hopestar.hfms.module.finance.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatusResponseDTO {

    private Long id;
    private String name;
    private String description;
}
