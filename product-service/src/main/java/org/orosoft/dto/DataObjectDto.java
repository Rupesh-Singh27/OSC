package org.orosoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DataObjectDto {

    @JsonProperty("data")
    List<MainDataDto> data;
}
