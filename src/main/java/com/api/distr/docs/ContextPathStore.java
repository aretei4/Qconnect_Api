package com.api.distr.docs;

import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ContextPathStore implements ApplicationListener<ServletWebServerInitializedEvent> {

    private static String contextPath;

    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        contextPath = event.getApplicationContext().getServletContext().getContextPath();
        System.out.println("Server started with context path: " + contextPath);
    }

    public static String getContextPath() {
        return contextPath;
    }
}

