package com.tamo.listener;

import com.tamo.service.EmailService;
import com.tamo.service.JsonDataService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
/**
 * Implementation of AppContextListener.
 * Provides necessary operations and logic for the system.
 */
public class AppContextListener implements ServletContextListener {
    @Override
    /**
     * Executes the contextInitialized operation.
     * @param sce the sce argument
     */
    /**
     * Executes the contextInitialized operation.
     * @param sce the sce argument
     */
    public void contextInitialized(ServletContextEvent sce) {
        JsonDataService.ensureInitialized(JsonDataService.resolveClassesPath(sce.getServletContext()));

        String imagesPath = sce.getServletContext().getRealPath("/static/images");
        if (imagesPath == null) {
            imagesPath = new java.io.File("src/main/webapp/static/images").getAbsolutePath();
        }
        EmailService.getInstance().setImagesBasePath(imagesPath);
    }
}
