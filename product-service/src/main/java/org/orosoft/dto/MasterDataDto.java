package org.orosoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MasterDataDto {

    private String code;

    @JsonProperty("dataObject")
    private DataObjectDto dataObject;
}
