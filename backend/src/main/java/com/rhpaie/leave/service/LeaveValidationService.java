package com.rhpaie.leave.service;

import com.rhpaie.leave.dto.LeaveValidationResponse;
import com.rhpaie.leave.entity.LeaveRequest;

public interface LeaveValidationService {

    LeaveValidationResponse validateBeforeApproval(LeaveRequest leaveRequest);
}
