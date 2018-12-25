package com.project.cloud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private String taskId;

    private String taskName;

    private String taskDescription;

    private String taskCreatedBy;

    private String taskCreationDate;

    private String taskDueDate;

}
