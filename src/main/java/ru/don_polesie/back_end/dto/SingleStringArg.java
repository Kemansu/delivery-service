package ru.don_polesie.back_end.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SingleStringArg implements Serializable {
    private String arg;
}
