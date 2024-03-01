package org.orosoft.userservice.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;


/**
 * @param <T>
 *           This is the builder class used to make a response when ApiResponse needs to carry any type of object or data to client.
 *           Since it is a builder class any field can be used or ignored to build an object.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataObject<T> {

    private String userId;
    private String name;
    private String sessionId;
    private T data;

}
