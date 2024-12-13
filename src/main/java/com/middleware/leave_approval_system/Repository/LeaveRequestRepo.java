package com.middleware.leave_approval_system.Repository;


import com.middleware.leave_approval_system.Entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepo extends JpaRepository<LeaveRequest, Long> {


    List<LeaveRequest> findByLeaveStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByManagerId(String managerId);
    List<LeaveRequest> findByEmployeeId(String employeeId);
    List<LeaveRequest> findByManagerIdAndLeaveStatus(String managerId, LeaveRequest.LeaveStatus leaveStatus);
    List<LeaveRequest> findByEmployeeIdAndLeaveStatus(String employeeId, LeaveRequest.LeaveStatus leaveStatus);
    Optional<LeaveRequest> findByEmployeeIdAndLeaveStartDateAndLeaveEndDate(String employeeId, LocalDate leaveStartDate, LocalDate leaveEndDate);
    long countByEmployeeIdAndLeaveType(String employeeId, LeaveRequest.LeaveType leaveType);
    Optional<LeaveRequest> findByEmployeeIdAndLeaveType(String employeeId, LeaveRequest.LeaveType leaveType);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId = :employeeId " +
            "AND lr.leaveStartDate <= :leaveEndDate AND lr.leaveEndDate >= :leaveStartDate")
    List<LeaveRequest> findOverlappingLeaves(@Param("employeeId") String employeeId,
                                             @Param("leaveStartDate") LocalDate leaveStartDate,
                                             @Param("leaveEndDate") LocalDate leaveEndDate);



    @Query("SELECT COALESCE(SUM(DATEDIFF(lr.leaveEndDate, lr.leaveStartDate) + 1), 0) " +
            "FROM LeaveRequest lr " +
            "WHERE lr.employeeId = :employeeId AND lr.leaveType = :leaveType " +
            "AND lr.leaveStatus != 'REJECTED'")
    Optional<Integer> getTotalLeaveDaysByEmployeeIdAndLeaveType(@Param("employeeId") String employeeId,
                                                                @Param("leaveType") LeaveRequest.LeaveType leaveType);

}
