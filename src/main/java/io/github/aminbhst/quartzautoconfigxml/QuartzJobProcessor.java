package io.github.aminbhst.quartzautoconfigxml;

import com.google.auto.service.AutoService;
import io.github.aminbhst.quartzautoconfigxml.annotation.QuartzJob;
import org.dom4j.DocumentException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("ir.viratech.quartzprocessor.annotation.QuartzJob")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class QuartzJobProcessor extends AbstractProcessor {

    private Filer filer;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<TypeElement> quartzJobs = roundEnvironment.getElementsAnnotatedWith(QuartzJob.class)
                .stream()
                .map(element -> ((TypeElement) element))
                .collect(Collectors.toSet());

        try {
            String jobsXml = new JobXMLGenerator().generateXML(quartzJobs);
            writeFile(jobsXml);
        } catch (TransformerException | IOException | DocumentException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        return true;
    }

    private void writeFile(String content) throws IOException {
        FileObject f = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "applicationContext-jobs.xml");
        try (Writer w = new OutputStreamWriter(f.openOutputStream(), StandardCharsets.UTF_8)) {
            w.write(content);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.filer = processingEnv.getFiler();
    }

}
