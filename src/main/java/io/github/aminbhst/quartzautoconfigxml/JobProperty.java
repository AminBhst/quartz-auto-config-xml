package io.github.aminbhst.quartzautoconfigxml;

public class JobProperty {
    private String firstAttr;
    private String secondAttrName;
    private String secondAttr;

    public JobProperty(String firstAttr, String secondAttrName, String secondAttr) {
        this.firstAttr = firstAttr;
        this.secondAttrName = secondAttrName;
        this.secondAttr = secondAttr;
    }

    public String getFirstAttr() {
        return this.firstAttr;
    }

    public String getSecondAttrName() {
        return this.secondAttrName;
    }

    public String getSecondAttr() {
        return this.secondAttr;
    }

    public void setFirstAttr(String firstAttr) {
        this.firstAttr = firstAttr;
    }

    public void setSecondAttrName(String secondAttrName) {
        this.secondAttrName = secondAttrName;
    }

    public void setSecondAttr(String secondAttr) {
        this.secondAttr = secondAttr;
    }
}
