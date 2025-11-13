package com.api.distr.docs;

public class ExcelRunner {

    public void runImport() {
        // Create object of DeliveryExcelImport
        DeliveryExcelImport importer = new DeliveryExcelImport();

        // Call instance method (not main)
        importer.process("D:/data/delivery_data.xlsx");
    }

    public static void main(String[] args) {
        // Create ExcelRunner (non-static) and call method
        ExcelRunner runner = new ExcelRunner();
        runner.runImport();
    }
}

