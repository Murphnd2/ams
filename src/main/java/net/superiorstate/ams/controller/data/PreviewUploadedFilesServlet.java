package net.superiorstate.ams.controller.data;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.service.Importer;
import net.superiorstate.ams.data.service.Importer.TableMapping;
import net.superiorstate.ams.model.upload.UploadPreviewResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/PreviewUploadedFiles")
public class PreviewUploadedFilesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        File uploadDir = new File("C:/ams/uploads"); // or Helper.CSV_DIR
        File[] files = uploadDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(csv|xlsx|xls)$"));

        List<UploadPreviewResult> resultList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                UploadPreviewResult result = new UploadPreviewResult();
                result.fileName = file.getName();

                Set<String> headers = Importer.extractHeaders(file);
                result.headerMissing = headers.isEmpty();
                result.headers.addAll(headers);

                for (TableMapping mapping : Importer.TABLE_MAPPINGS) {
                    boolean matched = false;

                    if (mapping.requiresHeaders()) {
                        Set<String> expected = new HashSet<>(mapping.columns());
                        expected = expected.stream()
                                .map(h -> mapping.headerOverrides().getOrDefault(h, h))
                                .map(h -> h.trim().toLowerCase().replaceAll("[^a-z0-9]", ""))
                                .collect(Collectors.toSet());

                        Set<String> normalizedHeaders = headers.stream()
                                .map(h -> h.trim().toLowerCase().replaceAll("[^a-z0-9]", ""))
                                .collect(Collectors.toSet());

                        if (normalizedHeaders.containsAll(expected)) {
                            matched = true;
                        }
                    } else if (mapping.customHandler()) {
                        if (!headers.isEmpty() &&
                                file.getName().toLowerCase().contains(mapping.filePrefix().toLowerCase())) {
                            matched = true;
                        }
                    } else {
                        if (file.getName().toLowerCase().startsWith(mapping.filePrefix().toLowerCase())) {
                            matched = true;
                        }
                    }

                    if (matched) {
                        result.matchedMappings.add(mapping.filePrefix());
                    }
                }

                resultList.add(result);
            }
        }

        req.setAttribute("uploadPreviewResults", resultList);
        req.getRequestDispatcher("/uploadPreview.jsp").forward(req, res);
    }
}

