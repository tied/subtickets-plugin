package com.subtickets;

import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private VelocityEngine engine;

    public TemplateEngine() {
        engine = new VelocityEngine();
        engine.setProperty("runtime.log.logsystem.class", org.apache.velocity.runtime.log.NullLogChute.class);
        try {
            engine.init();
        } catch (Exception e) {
            log.error("Failed to init velocity template engine", e);
        }
    }

    public String renderTemplate(String fileName, Map data) {
        InputStream inputStream = ClassLoaderUtils.getResourceAsStream(fileName, getClass());

        InputStreamReader reader = new InputStreamReader(inputStream);
        VelocityContext context = new VelocityContext();
        context.put("data", data);
        Writer writer = new StringWriter();

        try {
            if (!engine.evaluate(context, writer, "", reader)) {
                log.error("Velocity engine failed to render template: " + fileName);
            }
        } catch (IOException e) {
            log.error("Velocity engine failed to render template: " + fileName);
        }

        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("", e);
        }
        return writer.toString();
    }

    public InputStream renderAsStream(String fileName, Map data) {
        return new ByteArrayInputStream(renderTemplate(fileName, data).getBytes(StandardCharsets.UTF_8));
    }

}
