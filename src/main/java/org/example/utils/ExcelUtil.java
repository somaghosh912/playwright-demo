package org.example.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExcelUtil {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelUtil() {}

    public static List<Map<String, String>> readSheet(String path, String sheetName) {
        try (FileInputStream in = new FileInputStream(path);
             Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + sheetName);

            List<Map<String, String>> rows = new ArrayList<>();
            Row header = sheet.getRow(0);
            if (header == null) return rows;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> record = new LinkedHashMap<>();
                for (int c = 0; c < header.getLastCellNum(); c++) {
                    Cell hc = header.getCell(c);
                    Cell vc = row.getCell(c);
                    record.put(FORMATTER.formatCellValue(hc), FORMATTER.formatCellValue(vc));
                }
                rows.add(record);
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read excel: " + path, e);
        }
    }
}
