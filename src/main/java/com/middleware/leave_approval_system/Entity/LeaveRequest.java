package com.middleware.leave_approval_system.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.middleware.leave_approval_system.Exception.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;



@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;
    private String firstName;
    private String lastName;


    private String email;
    private String position;
    private String phone;

    private String managerId;
    private String managerName;
    private String managerEmail;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate leaveStartDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate leaveEndDate;

    private String leaveReason;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;
    private Double duration;
    private String durationType;
    private String comments;

    private String medicalDocument;

    @Enumerated(EnumType.STRING)
    private LeaveStatus leaveStatus;

    public enum LeaveStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum LeaveType{
        SICK,
        VACATION,
        CASUAL,
        MARRIAGE,
        PATERNITY,
        MATERNITY,
        OTHERS
    }


    public void calculateDuration(List<LocalDate> nationalHolidays) {
        if (leaveStartDate == null || leaveEndDate == null) {
            throw new ResourceNotFoundException("Leave start and end dates cannot be null.");
        }

        if (leaveEndDate.isBefore(leaveStartDate)) {
            throw new ResourceNotFoundException("Leave end date cannot be before the start date.");
        }

        // Calculate business days excluding weekends and national holidays
        double businessDays = calculateBusinessDays(leaveStartDate, leaveEndDate, nationalHolidays);

        // Each business day is equivalent to 8 working hours
        int workingHoursPerDay = 8;
        int totalWorkingHours = (int) businessDays * workingHoursPerDay;

        // Store the calculated duration
        this.duration =  businessDays; // Total business days
        this.durationType = "Days"; // Indicates the unit used
    }

    public double calculateBusinessDays(LocalDate startDate, LocalDate endDate, List<LocalDate> nationalHolidays) {
        return startDate.datesUntil(endDate.plusDays(1)) // Inclusive of endDate
                .filter(date -> !isWeekend(date) && !nationalHolidays.contains(date))
                .count();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
