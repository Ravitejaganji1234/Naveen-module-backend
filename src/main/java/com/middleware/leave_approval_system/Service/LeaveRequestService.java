package com.middleware.leave_approval_system.Service;


import com.middleware.leave_approval_system.Entity.LeaveRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestService {

    LeaveRequest submitLeaveRequest(LeaveRequest leaveRequest);
    LeaveRequest approveLeaveRequest(Long id);
    LeaveRequest rejectLeaveRequest(Long id, String leaveReason);
    List<LeaveRequest> getAllLeaveRequests();
    List<LeaveRequest> getLeaveRequestsByStatus(String managerId,LeaveRequest.LeaveStatus leaveStatus);
    List<LeaveRequest> getAllPendingLeaveRequestsEmployee(String employeeId);
    List<LeaveRequest> getAllApprovedLeaveRequestsEmployee(String employeeId);
    List<LeaveRequest> getAllRejectedLeaveRequestsEmployee(String employeeId);
    List<LeaveRequest> getAllManagerId(String managerId);
    List<LeaveRequest> getAllEmployeeId(String employeeId);
    LeaveRequest updateLeaveRequest(Long id, LeaveRequest leaveRequest);
    String deleteLeaveRequest(Long id);
    
}
