package com.workhive.module.activity.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitRosterResponseDTO {
    private UUID unitId;
    private String unitType; // "DEPARTMENT" or "TEAM"
    private String unitName;
    private String description;
    private String leadName;
    private String leadRole;
    private String leadAvatarUrl;
    private long totalMembers;
    private long liveOnlineCount;
    private long activeTasksCount;
    private List<UnitMemberLiveDTO> members;
}
