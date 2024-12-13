package com.middleware.leave_approval_system.Service;

import com.middleware.leave_approval_system.Entity.LeaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;


    // Method to send a basic email
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to); // Set recipient email address
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        }catch (Exception e) {
            throw new RuntimeException("Error sending email",e);
        }
    }

    // Method to send an email to the manager when an employee submits a leave request
    public void sendLeaveRequestEmail(String managerEmail,LeaveRequest leaveRequest) {
        String subject = "Leave Approval Request from "+leaveRequest.getLastName()+" "+leaveRequest.getFirstName();
        String body="Hi Sir/Madam,\n\n"+
                leaveRequest.getLastName()+" "+leaveRequest.getFirstName()+" has requested "+leaveRequest.getLeaveType()+" leave.\n"+
                "Details:\n"+
                "Employee Id: "+leaveRequest.getEmployeeId()+"\n"+
                "Employee Email: "+leaveRequest.getEmail()+"\n"+
                "Phone: "+leaveRequest.getPhone()+"\n"+
                "Position: "+leaveRequest.getPosition()+"\n"+
                "Leave Type: "+leaveRequest.getLeaveType()+"\n"+
                "Start Date: "+leaveRequest.getLeaveStartDate()+"\n"+
                "End Date: "+leaveRequest.getLeaveEndDate()+"\n"+
                "Duration: "+leaveRequest.getDuration()+" Days"+"\n"+
                "Comments: "+(leaveRequest.getComments()!=null ? leaveRequest.getComments():"N/A")+"\n\n"+
                "Please click one of the options below:\n"+
                "[Approve Leave](http://localhost:8080/leave/approve/"+leaveRequest.getId()+")\n"+
                "[Reject Leave](http://localhost:8080/leave/reject/"+leaveRequest.getId()+")\n\n"+
                "Regards,\n"+
                leaveRequest.getLastName()+" "+leaveRequest.getFirstName();

        // Send the email to the manager
        sendEmail(managerEmail, subject, body);
    }

    // Method to send an email to the employee after a leave request has been approved
    public void sendResponseToEmployee(LeaveRequest.LeaveStatus leaveStatus,LeaveRequest leaveRequest) {
        String subject = "Leave request "+leaveStatus.name();
        String body="Hi "+leaveRequest.getLastName()+" "+leaveRequest.getFirstName()+",\n\n"+
                "Your leave request from "+leaveRequest.getLeaveStartDate()+" to "+leaveRequest.getLeaveEndDate()+
                " has been "+leaveStatus.name()+".\n\nRegards,\n\n Manager";
        sendEmail(leaveRequest.getEmail(), subject, body);
    }

    // Method to send an email to the employee after a leave request has been rejected
    public void sendApprovalNotification(LeaveRequest.LeaveStatus leaveStatus,LeaveRequest leaveRequest) {

        // Set email subject with approval/rejection status
        String subject = "Leave approval request "+leaveStatus.name();
        String body="Dear "+leaveRequest.getLastName()+" "+leaveRequest.getFirstName()+",\n\n"+
                "Your leave request has been "+leaveStatus.name().toLowerCase()+".\n\n"+
                "Reason: "+leaveRequest.getLeaveReason()+"\n\n"+
                "If You have any questions, please contact Manager.\n\n"+
                "Regards,\n\n Manager";

        // Send the email to the employee
        sendEmail(leaveRequest.getEmail(), subject, body);
    }
}
