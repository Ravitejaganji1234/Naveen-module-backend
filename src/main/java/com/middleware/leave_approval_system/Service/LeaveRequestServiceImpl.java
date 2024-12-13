package com.middleware.leave_approval_system.Service;


import com.middleware.leave_approval_system.Entity.LeaveRequest;
import com.middleware.leave_approval_system.Exception.ResourceNotFoundException;
import com.middleware.leave_approval_system.Repository.LeaveRequestRepo;
import com.middleware.leave_approval_system.Util.HolidaysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;


@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    // Injecting EmailService for sending notifications
    @Autowired
    private EmailService emailService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // Injecting LeaveRequestRepo for database operations
    @Autowired
    private LeaveRequestRepo leaveRequestRepository;

    // Defining maximum allowable leave limits for each leave type
    private static final int MAX_SICK_LEAVES = 6;
    private static final int MAX_VACATION_LEAVES = 4;
    private static final int MAX_CASUAL_LEAVES = 4;
    private static final int MAX_MARRIAGE_LEAVES = 3;
    private static final int MAX_PATERNITY_LEAVES = 2;
    private static final int MAX_MATERNITY_LEAVES = 4;
    private static final int MAX_OTHER_LEAVES = 2;


    public LeaveRequest submitLeaveRequest(LeaveRequest leaveRequest) {

        // Validate if overlapping leaves exist if the status is REJECTED
        if (leaveRequest.getLeaveStatus() == LeaveRequest.LeaveStatus.REJECTED) {
            List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findOverlappingLeaves(
                    leaveRequest.getEmployeeId(), leaveRequest.getLeaveStartDate(), leaveRequest.getLeaveEndDate()
            );
            if (!overlappingLeaves.isEmpty()) {
                throw new ResourceNotFoundException("You have already applied for overlapping leaves.");
            }
        }

        // Check if leave start and end dates are provided
        if (leaveRequest.getLeaveStartDate() == null || leaveRequest.getLeaveEndDate() == null) {
            throw new ResourceNotFoundException("Leave start date and end date must be provided.");
        }

        // Validate leave balance based on the leave type
        validateLeaveBalance(leaveRequest);

        // Set status to PENDING and calculate leave duration
        leaveRequest.setLeaveStatus(LeaveRequest.LeaveStatus.PENDING);
        int year = leaveRequest.getLeaveStartDate().getYear();
        List<LocalDate> nationalHolidays = HolidaysUtil.getNationalHolidays(year);
        leaveRequest.calculateDuration(nationalHolidays);

        // Save the leave request and send an email notification to the manager
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        emailService.sendLeaveRequestEmail(leaveRequest.getManagerEmail(), leaveRequest);
        return savedRequest;
    }


    // Method to validate if the employee has remaining leave balance for the requested type
    public void validateLeaveBalance(LeaveRequest leaveRequest) {
//        long leaveCount = leaveRequestRepository.countByEmployeeIdAndLeaveType(leaveRequest.getEmployeeId(), leaveRequest.getLeaveType());
        Integer totalLeaveDaysTaken = leaveRequestRepository.getTotalLeaveDaysByEmployeeIdAndLeaveType(leaveRequest.getEmployeeId(), leaveRequest.getLeaveType()).orElse(0);

        // Determine the max leaves based on the leave type
        int maxLeaves = switch (leaveRequest.getLeaveType()) {
            case SICK -> MAX_SICK_LEAVES;
            case VACATION -> MAX_VACATION_LEAVES;
            case CASUAL -> MAX_CASUAL_LEAVES;
            case MARRIAGE -> MAX_MARRIAGE_LEAVES;
            case PATERNITY -> MAX_PATERNITY_LEAVES;
            case MATERNITY -> MAX_MATERNITY_LEAVES;
            case OTHERS -> MAX_OTHER_LEAVES;
            default -> throw new ResourceNotFoundException("Invalid leave type.");
        };

        double requestedLeaveDays = leaveRequest.calculateBusinessDays(leaveRequest.getLeaveStartDate(), leaveRequest.getLeaveEndDate(), HolidaysUtil.getNationalHolidays(leaveRequest.getLeaveStartDate().getYear()));

        if (totalLeaveDaysTaken + requestedLeaveDays > maxLeaves) {
            throw new ResourceNotFoundException("You have exhausted your " + leaveRequest.getLeaveType().name().toLowerCase() + " leave limit of " + maxLeaves + " days.");
        }

    }

    private LeaveRequest getLeaveBalance(String employeeId, LeaveRequest.LeaveType leaveType) {
        Optional<LeaveRequest> leaveBalance = leaveRequestRepository.findByEmployeeIdAndLeaveType(employeeId, leaveType);
        return leaveBalance.orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for employee: " + employeeId));
    }


    // Method to approve a leave request
    @Override
    public LeaveRequest approveLeaveRequest(Long id) {

        // Fetch leave request by ID and update status to APPROVED
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave Request Id Not Found"));
        leaveRequest.setLeaveStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequestRepository.save(leaveRequest);
        emailService.sendResponseToEmployee(leaveRequest.getLeaveStatus(), leaveRequest);
        return leaveRequest;
    }


    // Method to reject a leave request and provide a reason
    @Override
    public LeaveRequest rejectLeaveRequest(Long id, String leaveReason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave Request Id Not Found"));
        leaveRequest.setLeaveStatus(LeaveRequest.LeaveStatus.REJECTED);
        leaveRequest.setLeaveReason(leaveReason);
        leaveRequestRepository.save(leaveRequest);
        emailService.sendApprovalNotification(leaveRequest.getLeaveStatus(), leaveRequest);
        return leaveRequest;
    }

    // Method to retrieve a leave request by ID
    public LeaveRequest getLeaveRequestById(Long id) {
        Optional<LeaveRequest> leaveRequest = leaveRequestRepository.findById(id);
        if (!leaveRequest.isPresent()) {
            throw new ResourceNotFoundException("Leave Request Id Not Found");
        }
        return leaveRequest.get();
    }


    // Method to retrieve all leave requests
    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }


    // Method to retrieve leave requests by manager ID and status
    @Override
    public List<LeaveRequest> getLeaveRequestsByStatus(String managerId, LeaveRequest.LeaveStatus leaveStatus) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByManagerIdAndLeaveStatus(managerId, leaveStatus);
        if (leaveRequests.isEmpty()) {
            throw new ResourceNotFoundException("No " + leaveStatus.name().toLowerCase() + " leave requests found for manager ID: " + managerId);
        }
        return leaveRequests;
    }


    // Method to retrieve all pending leave requests for an employee
    @Override
    public List<LeaveRequest> getAllPendingLeaveRequestsEmployee(String employeeId) {
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByEmployeeIdAndLeaveStatus(employeeId, LeaveRequest.LeaveStatus.PENDING);
        if (pendingRequests.isEmpty()) {
            throw new ResourceNotFoundException("No pending leave requests found for employeeID: " + employeeId);
        }
        return pendingRequests;
    }

    // Method to retrieve all approved leave requests for an employee
    @Override
    public List<LeaveRequest> getAllApprovedLeaveRequestsEmployee(String employeeId) {
        List<LeaveRequest> approvedRequests = leaveRequestRepository.findByEmployeeIdAndLeaveStatus(employeeId, LeaveRequest.LeaveStatus.APPROVED);
        if (approvedRequests.isEmpty()) {
            throw new ResourceNotFoundException("No approved leave requests for employeeID: " + employeeId);
        }
        return approvedRequests;
    }

    // Method to retrieve all rejected leave requests for an employee
    @Override
    public List<LeaveRequest> getAllRejectedLeaveRequestsEmployee(String employeeId) {
        List<LeaveRequest> rejectRequests = leaveRequestRepository.findByEmployeeIdAndLeaveStatus(employeeId, LeaveRequest.LeaveStatus.REJECTED);
        if (rejectRequests.isEmpty()) {
            throw new ResourceNotFoundException("No rejected leave requests for employeeID: " + employeeId);
        }
        return rejectRequests;
    }


    // Method to retrieve all leave requests by manager ID
    @Override
    public List<LeaveRequest> getAllManagerId(String managerId) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByManagerId(managerId);
        if (leaveRequests.isEmpty()) {
            throw new ResourceNotFoundException("No leave requests found for manager ID: " + managerId);
        }
        return leaveRequests;
    }


    // Method to retrieve all leave requests by employee ID
    @Override
    public List<LeaveRequest> getAllEmployeeId(String employeeId) {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeId(employeeId);
        if (leaveRequests.isEmpty()) {
            throw new ResourceNotFoundException("No leave requests found for employeeID: " + employeeId);
        }
        return leaveRequests;
    }

    // Method to update an existing leave request
    @Override
    public LeaveRequest updateLeaveRequest(Long id, LeaveRequest leaveRequest) {
        LeaveRequest existingLeaveRequest = leaveRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found for ID: " + id));
        if (existingLeaveRequest.getLeaveStatus() != LeaveRequest.LeaveStatus.PENDING) {
            throw new ResourceNotFoundException("Only PENDING leave requests can be updated.");
        }
        if (leaveRequest.getLeaveStartDate().isBefore(LocalDate.now()) || leaveRequest.getLeaveEndDate().isBefore(leaveRequest.getLeaveStartDate())) {
            throw new ResourceNotFoundException("Leave start and end dates must be valid and cannot be in the past.");
        }

        existingLeaveRequest.setLeaveStartDate(leaveRequest.getLeaveStartDate());
        existingLeaveRequest.setLeaveEndDate(leaveRequest.getLeaveEndDate());
        existingLeaveRequest.setLeaveType(leaveRequest.getLeaveType());
        return leaveRequestRepository.save(existingLeaveRequest);
    }

    // Method to delete an existing leave request
    @Override
    public String deleteLeaveRequest(Long id) {
        LeaveRequest deleteRequest = leaveRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found for ID: " + id));
        if (deleteRequest.getLeaveStatus() != LeaveRequest.LeaveStatus.PENDING) {
            throw new ResourceNotFoundException("Only PENDING leave requests can be deleted.");
        }
        leaveRequestRepository.delete(deleteRequest);
        return "Leave request deleted successfully";
    }

}
