package io.github.aminbhst.quartzautoconfigxml;

import org.w3c.dom.Element;


public class JobElement {
    Element jobTask;
    Element job;
    Element jobTrigger;
    StoreType storeType;


    public JobElement(Element jobTask, Element job, Element jobTrigger, StoreType storeType) {
        this.jobTask = jobTask;
        this.job = job;
        this.jobTrigger = jobTrigger;
        this.storeType = storeType;
    }

    public JobElement() {
    }

    public Element getJobTask() {
        return this.jobTask;
    }

    public Element getJob() {
        return this.job;
    }

    public Element getJobTrigger() {
        return this.jobTrigger;
    }

    public StoreType getStoreType() {
        return this.storeType;
    }

    public void setJobTask(Element jobTask) {
        this.jobTask = jobTask;
    }

    public void setJob(Element job) {
        this.job = job;
    }

    public void setJobTrigger(Element jobTrigger) {
        this.jobTrigger = jobTrigger;
    }

    public void setStoreType(StoreType storeType) {
        this.storeType = storeType;
    }
}
