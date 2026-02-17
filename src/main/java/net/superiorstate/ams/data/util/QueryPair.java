package net.superiorstate.ams.data.util;

public class QueryPair {
    private String parameterName;
    private String stringValue;
    private int intValue;
    private Long longValue;

    public QueryPair(String parameterName, int intValue){
        this.parameterName = parameterName;
        this.stringValue = "";
        this.intValue = intValue;
        this.longValue = 0L;
    }
    public QueryPair(String parameterName, Long longValue){
        this.parameterName = parameterName;
        this.stringValue = "";
        this.intValue = 0;
        this.longValue = longValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }
}
