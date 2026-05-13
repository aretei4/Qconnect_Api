package com.api.distr.docs.upload;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

    @Autowired
    private TemplateDao templateDao;

    public void saveTemplate(ExcelTemplate template) {
        templateDao.saveOrUpdateTemplate(template);
    }

    public ExcelTemplate getTemplate(String name) {
        return templateDao.getTemplateByName(name);
    }
    public List<String> getAllTemplateNames() {
        return templateDao.getAllTemplateNames();
    }

    public List<ExcelTemplate> getSalesCompanies() {
        return templateDao.getSalesCompanies();
    }

    public List<String> getDistinctCompanies() {
        return templateDao.getDistinctCompanies();
    }

    public List<ExcelTemplate> getTemplatesByCompany(String companyName) {
        return templateDao.getTemplatesByCompany(companyName);
    }
}

