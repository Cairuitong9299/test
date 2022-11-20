package com.example.test.utils;

/**
 * @Auther: CAI
 * @Date: 2022/11/2 - 11 - 02 - 21:53
 * @Description: com.example.test.utils
 * @version: 1.0
 */
public class TypeDomain {

    private String sourceType;
    private String targetType;
    private boolean valid;
    private boolean isMultipledId = false;

    public TypeDomain(String sourceType, String targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isMultipledId() {
        return isMultipledId;
    }

    public void setMultipledId(boolean multipledId) {
        isMultipledId = multipledId;
    }
}
