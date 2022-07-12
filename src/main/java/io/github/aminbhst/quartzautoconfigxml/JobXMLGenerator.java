package io.github.aminbhst.quartzautoconfigxml;

import io.github.aminbhst.quartzautoconfigxml.annotation.ConstructorArg;
import io.github.aminbhst.quartzautoconfigxml.annotation.QuartzJob;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.lang.model.element.TypeElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JobXMLGenerator implements XMLConstants {

    private final Document document;
    private final Element beansRoot;
    public List<JobElement> jobElements = new ArrayList<>();

    public JobXMLGenerator() throws ParserConfigurationException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        this.document = documentBuilder.newDocument();
        Element beansRoot = document.createElement("beans");
        beansRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        beansRoot.setAttribute("xmlns", "http://www.springframework.org/schema/beans");
        beansRoot.setAttribute("xsi:schemaLocation", "            http://www.springframework.org/schema/beans" +
                "http://www.springframework.org/schema/beans/spring-beans-4.0.xsd");
        document.appendChild(beansRoot);
        this.beansRoot = beansRoot;
    }

    public String generateXML(Collection<TypeElement> classes) throws TransformerException, DocumentException, IOException {
        classes.forEach(this::generateJobElement);
        jobElements.forEach(this::appendJobElementsToRoot);
        Element beansWeb = createBeansWithProfileWeb();
        Element inMemorySchedulerBean = createInMemorySchedulerBean();
        beansWeb.appendChild(inMemorySchedulerBean);
        Element dataSourceSchedulerBean = createDataSourceSchedulerBean();
        beansWeb.appendChild(dataSourceSchedulerBean);
        return generatePrettyXml();
    }

    private String generatePrettyXml() throws IOException, TransformerException, DocumentException {
        org.dom4j.Document document = DocumentHelper.parseText(createInitialXml());
        StringWriter sw = new StringWriter();
        XMLWriter writer = new XMLWriter(sw, prettyFormat());
        writer.write(document);
        return sw.toString();
    }

    private String createInitialXml() throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamResult streamResult = new StreamResult(os);
        transformer.transform(domSource, streamResult);
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    private OutputFormat prettyFormat() {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndentSize(2);
        format.setSuppressDeclaration(false);
        format.setEncoding("UTF-8");
        return format;
    }

    private Element createDataSourceSchedulerBean() {
        List<JobElement> dataSourceJobs = collectJobElementsByStoreType(StoreType.DATA_SOURCE);
        Element dataSourceScheduler = createSimpleBeanByName("dataSourceScheduler", "org.springframework.scheduling.quartz.SchedulerFactoryBean");
        createDataSourceJobProperties().forEach(prop -> this.appendProperties(dataSourceScheduler, prop));
        Element jobDetails = createProperty("jobDetails");
        Element jobDetailsList = createList();
        dataSourceJobs.forEach(jobElement -> this.appendRefsToList(jobDetailsList, getBeanId(jobElement.getJob())));
        jobDetails.appendChild(jobDetailsList);
        dataSourceScheduler.appendChild(jobDetails);

        Element triggers = createProperty("triggers");
        Element triggersList = createList();
        dataSourceJobs.forEach(jobElement -> this.appendRefsToList(triggersList, getBeanId(jobElement.getJobTrigger())));
        triggers.appendChild(triggersList);
        dataSourceScheduler.appendChild(triggers);

        return dataSourceScheduler;
    }

    private void appendJobElementsToRoot(JobElement jobElement) {
        beansRoot.appendChild(jobElement.getJobTask());
        beansRoot.appendChild(jobElement.getJob());
        beansRoot.appendChild(jobElement.getJobTrigger());
    }


    private List<JobProperty> createDataSourceJobProperties() {
        JobProperty configLocation = new JobProperty("configLocation", VALUE, "classpath:quartz.properties");
        JobProperty contextSchedulerContextKey = new JobProperty("applicationContextSchedulerContextKey", VALUE, "applicationContext");
        JobProperty dataSource = new JobProperty("dataSource", REF, "dataSource");
        JobProperty schedulerName = new JobProperty("schedulerName", VALUE, "productionScheduler");
        JobProperty overwriteExistingJobs = new JobProperty("overwriteExistingJobs", VALUE, "false");
        return Arrays.asList(configLocation, contextSchedulerContextKey, dataSource, schedulerName, overwriteExistingJobs);
    }

    private Element createInMemorySchedulerBean() {
        List<JobElement> inMemoryJobs = collectJobElementsByStoreType(StoreType.IN_MEMORY);

        Element inMemoryScheduler = createSimpleBeanByName("inMemoryScheduler", "org.springframework.scheduling.quartz.SchedulerFactoryBean");
        Element jobDetails = createProperty("jobDetails");
        Element jobDetailsList = createList();
        inMemoryJobs.forEach(jobElement -> this.appendRefsToList(jobDetailsList, jobElement.getJob().getAttribute("id")));
        jobDetails.appendChild(jobDetailsList);
        inMemoryScheduler.appendChild(jobDetails);

        Element triggers = createProperty("triggers");
        Element triggerList = createList();
        inMemoryJobs.forEach(jobElement -> this.appendRefsToList(triggerList, jobElement.getJobTrigger().getAttribute("id")));
        triggers.appendChild(triggerList);
        inMemoryScheduler.appendChild(triggers);
        return inMemoryScheduler;
    }

    private Element createBeansWithProfileWeb() {
        Element beans = document.createElement("beans");
        beans.setAttribute("profile", "web");
        beansRoot.appendChild(beans);
        return beans;
    }

    private List<JobElement> collectJobElementsByStoreType(StoreType storeType) {
        return jobElements.stream()
                .filter(jobElement -> jobElement.getStoreType() == storeType)
                .collect(Collectors.toList());
    }

    private String getBeanId(Element bean) {
        return bean.getAttribute("id");
    }

    public void generateJobElement(TypeElement clazz) {
        QuartzJob quartzJob = clazz.getAnnotation(QuartzJob.class);
        ConstructorArg[] constructorArgs = clazz.getAnnotationsByType(ConstructorArg.class);

        if (quartzJob == null)
            return;

        Element jobTask = createJobTask(clazz, constructorArgs);
        Element job = createJob(clazz, jobTask, quartzJob);
        Element jobTrigger = createJobTrigger(clazz, job, quartzJob);
        JobElement jobElement = new JobElement(jobTask, job, jobTrigger, quartzJob.storeType());
        jobElements.add(jobElement);
    }

    private Element createJobTrigger(TypeElement clazz, Element job, QuartzJob quartzJob) {
        Element trigger = createSimpleBeanById(uncapitalize(clazz) + "Trigger", "org.springframework.scheduling.quartz.CronTriggerFactoryBean");
        JobProperty jobDetail = new JobProperty(JOB_DETAIL, REF, job.getAttribute("id"));
        JobProperty cron = new JobProperty(CRON_EXPRESSION, VALUE, quartzJob.cron());
        appendProperties(trigger, jobDetail);
        appendProperties(trigger, cron);
        if (!quartzJob.concurrent()) {
            JobProperty concurrent = new JobProperty("concurrent", VALUE, "false");
            appendProperties(trigger, concurrent);
        }
        return trigger;
    }


    private Element createJob(TypeElement clazz, Element jobTask, QuartzJob quartzJob) {
        Element bean = createSimpleBeanById(uncapitalize(clazz), "MethodInvokingJobDetailFactoryBean");
        JobProperty firstProp = new JobProperty(TARGET_OBJECT, REF, jobTask.getAttribute("id"));
        JobProperty secondProp = new JobProperty(TARGET_METHOD, VALUE, quartzJob.targetMethod());
        appendProperties(bean, firstProp);
        appendProperties(bean, secondProp);
        return bean;
    }

    private Element createProperty(String propertyName) {
        Element property = document.createElement("property");
        property.setAttribute("name", propertyName);
        return property;
    }

    private void appendProperties(Element bean, JobProperty prop) {
        Element property = document.createElement(PROPERTY);
        property.setAttribute(NAME, prop.getFirstAttr());
        property.setAttribute(prop.getSecondAttrName(), prop.getSecondAttr());
        bean.appendChild(property);
    }

    private void appendRefsToList(Element list, String refBean) {
        Element ref = document.createElement("ref");
        ref.setAttribute("bean", refBean);
        list.appendChild(ref);
    }


    private Element createJobTask(TypeElement clazz, ConstructorArg[] constructorArgs) {
        Element bean = createSimpleBeanById(uncapitalize(clazz) + "Task", clazz.getQualifiedName().toString());
        if (constructorArgs != null && constructorArgs.length > 0) {
            for (ConstructorArg arg : constructorArgs) {
                String[] results = findNonNullConstructorArgProperty(arg);
                if (results == null)
                    continue;

                appendChildNode(CONSTRUCTOR_ARGS, results[0], results[1], bean);
            }

        }
        return bean;
    }

    private void appendChildNode(String nodeName, String attrName, String attrValue, Element parentNode) {
        Element element = document.createElement(nodeName);
        element.setAttribute(attrName, attrValue);
        parentNode.appendChild(element);
    }

    private Element createSimpleBeanByName(String name, String clazz) {
        Element bean = document.createElement("bean");
        bean.setAttribute("name", name);
        bean.setAttribute("class", clazz);
        return bean;
    }

    private Element createSimpleBeanById(String id, String clazz) {
        Element bean = document.createElement("bean");
        bean.setAttribute("id", id);
        bean.setAttribute("class", clazz);
        return bean;
    }

    private Element createList() {
        return document.createElement("list");
    }

    private String uncapitalize(TypeElement clazz) {
        return WordUtils.uncapitalize(clazz.getSimpleName().toString());
    }

    private String[] findNonNullConstructorArgProperty(ConstructorArg constructorArg) {
        if (StringUtils.isNotBlank(constructorArg.index()))
            return new String[]{"index", constructorArg.index()};
        if (StringUtils.isNotBlank(constructorArg.name()))
            return new String[]{"name", constructorArg.name()};
        if (StringUtils.isNotBlank(constructorArg.ref()))
            return new String[]{"ref", constructorArg.ref()};
        if (StringUtils.isNotBlank(constructorArg.type()))
            return new String[]{"type", constructorArg.type()};
        if (StringUtils.isNotBlank(constructorArg.value()))
            return new String[]{"value", constructorArg.value()};
        else return null;
    }
}
