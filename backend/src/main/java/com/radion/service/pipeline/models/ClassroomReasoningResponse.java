package com.radion.service.pipeline.models;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomReasoningResponse {
    private String topic;
    private String priority;
    private List<String> actionItems;
    private String reminderStrategy;
    private boolean isActionRequired;
    private String summary;
}
