package com.ebrahim65.ats.match_simulator.dto;

/**
 *
 * @author Ebrahim
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    private String jobDescription;
    private String cvContent;
    private MultipartFile cvFile;
}
