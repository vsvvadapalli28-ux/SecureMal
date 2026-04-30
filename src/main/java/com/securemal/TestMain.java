package com.securemal;

import java.io.File;
import java.io.FileWriter;

import com.securemal.auth.AuthService;
import com.securemal.controllers.AnalysisController;
import com.securemal.models.AnalysisReport;
import com.securemal.models.UploadedFile;
import com.securemal.services.FileManagementService;

public class TestMain {
    public static void main(String[] args) throws Exception {
        // 1. Create a dummy exe file
        File dummyExe = new File("test_malware.exe");
        try (FileWriter fw = new FileWriter(dummyExe)) {
            fw.write("MZ... mock executable with powershell cmd.exe encrypt http://bad.com");
        }

        // 2. Register user
        AuthService auth = new AuthService();
        auth.registerUser("testuser", "test@test.com", "password123");

        // 3. Upload file
        FileManagementService fms = new FileManagementService();
        UploadedFile uf = fms.uploadFile(dummyExe, 1);
        System.out.println("Uploaded File ID: " + uf.getId());

        // 4. Run Analysis
        AnalysisReport report = AnalysisController.runStaticAnalysis(uf.getId());
        System.out.println("Risk Label: " + report.getRiskLabel());
        System.out.println("Summary: " + report.getPlainSummary());
        System.out.println("Timeline: " + report.getTimelineJson());

        // 5. Cleanup
        dummyExe.delete();
    }
}
