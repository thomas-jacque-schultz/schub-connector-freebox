package schultz.thomas.schub.connector.freebox.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FreeboxResponse<T>(
        boolean success,
        String msg,
        @JsonProperty("error_code") String errorCode,
        T result
) {
}
