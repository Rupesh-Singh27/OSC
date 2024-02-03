package org.orosoft.userservice.dto.productDto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MasterDataDto {

    private String code;

    @JsonProperty("dataObject")
    private DataObjectDto dataObject;
}
