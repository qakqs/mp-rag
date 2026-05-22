package org.rag.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class GenerateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -2777572081111734997L;

    String model;
    String message;
    String ragTag;
}
