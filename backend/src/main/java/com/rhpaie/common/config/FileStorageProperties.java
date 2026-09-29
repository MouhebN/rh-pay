package com.rhpaie.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    private String uploadDir = "uploads/leave-documents";

    private String payslipUploadDir = "uploads/payslips";

    private String contractUploadDir = "uploads/contracts";

    private String employeeExitUploadDir = "uploads/employee-exits";

    private String chatFaqUploadDir = "uploads/chat-faq";
}
